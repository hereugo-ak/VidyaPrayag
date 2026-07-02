/*
 * File: PewsDailyJob.kt
 * Module: feature.pews
 *
 * Scheduled orchestrator that runs the full PEWS pipeline once a day per school:
 *
 *     SENSE  → PewsSnapshotService.recomputeSchool   (deterministic, always runs)
 *     TRIAGE → TriageService.triage                   (CLASSIFY prefilter + cohort dedup)
 *     REASON → PewsReasoningService.reasonForSchool   (LLM explains; degrades to null)
 *     ACT    → PewsInterventionService.actOnSnapshots (open task + Notify owner)
 *
 * PEWS 2.0: TRIAGE filters the full cohort to the cases needing deep review.
 * REASON then runs only on the deep-look subset, saving expensive LLM calls.
 * If triage is killed or unavailable, falls back to v1 behavior (reason all medium+).
 *
 * Pattern is copied verbatim from PulseWeeklyJob: a long-running coroutine
 * launched at startup that checks every hour whether it's the target UTC hour,
 * with a per-day run-guard so a restart can't double-run. Default target hour is
 * 00 UTC (≈ 05:30 IST) — the recompute is ready before the school day. The hour
 * is env-tunable via PEWS_RUN_HOUR_UTC and the whole feature gated by PEWS_ENABLED.
 *
 * SENSE always runs (it's pure SQL/Kotlin and gives a working, honest cohort
 * even with zero AI). TRIAGE + REASON + ACT layer on top and degrade gracefully.
 */
package com.littlebridge.enrollplus.feature.pews

import com.littlebridge.enrollplus.db.PewsConfigTable
import com.littlebridge.enrollplus.db.PewsRiskSnapshotsTable
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.feature.pews.act.ManagedCaseworkService
import com.littlebridge.enrollplus.feature.pews.caseworker.CaseworkerService
import com.littlebridge.enrollplus.feature.pews.learn.LearnService
import com.littlebridge.enrollplus.feature.pews.queue.PewsJobQueue
import com.littlebridge.enrollplus.feature.pews.triage.TriageService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Properties
import java.util.UUID

object PewsDailyJob {
    private const val TAG = "PewsDailyJob"
    private val log = LoggerFactory.getLogger(TAG)

    private val snapshotService = PewsSnapshotService()
    private val triageService = TriageService()
    private val caseworkerService = CaseworkerService()
    private val managedCasework = ManagedCaseworkService()
    private val learnService = LearnService()
    private val reasoningService = PewsReasoningService()
    private val interventionService = PewsInterventionService()

    @Volatile
    private var lastRunDate: LocalDate? = null

    // ── env tuning ──────────────────────────────────────────────────────────
    private val localProps: Properties by lazy {
        Properties().apply {
            runCatching {
                val f = File("local.properties")
                if (f.exists()) f.inputStream().use { load(it) }
            }
        }
    }
    private fun env(key: String): String? =
        (System.getenv(key) ?: localProps.getProperty(key))?.takeIf { it.isNotBlank() }

    private val targetHourUtc: Int get() = env("PEWS_RUN_HOUR_UTC")?.toIntOrNull() ?: 0
    private val enabled: Boolean get() = env("PEWS_ENABLED")?.lowercase() != "false"
    private val targetDayOfWeek: DayOfWeek get() =
        env("PEWS_RUN_DAY_OF_WEEK")?.let { runCatching { DayOfWeek.valueOf(it.uppercase()) }.getOrNull() }
            ?: DayOfWeek.MONDAY

    fun start(scope: CoroutineScope) {
        if (!enabled) {
            log.info("[$TAG] PEWS_ENABLED=false — daily job not started")
            return
        }
        // Start the async job queue worker
        PewsJobQueue.startWorker(scope) { schoolId -> runSchool(schoolId) }
        scope.launch {
            log.info("[$TAG] Started — daily PEWS recompute at hour {} UTC (hourly check)", targetHourUtc)
            while (true) {
                delay(60 * 60 * 1000) // 1 hour
                runCatching { checkAndRun() }
                    .onFailure { log.warn("[$TAG] checkAndRun failed: {}", it.message) }
            }
        }
    }

    private suspend fun checkAndRun() {
        val now = ZonedDateTime.now(ZoneOffset.UTC)
        val today = now.toLocalDate()
        if (now.hour != targetHourUtc) return
        if (lastRunDate == today) return
        lastRunDate = today
        runAll(today)
    }

    /**
     * Run the full pipeline for every active school. Used by the scheduler and
     * by the admin manual-trigger endpoint (runNow).
     */
    suspend fun runAll(runDate: LocalDate = LocalDate.now()): Int {
        val schoolIds = snapshotService.activeSchoolIds()
        log.info("[$TAG] Running PEWS pipeline for {} schools (runDate={})", schoolIds.size, runDate)
        var totalAtRisk = 0
        for (schoolId in schoolIds) {
            // Per-school frequency check: weekly schools only run on the target day of week
            val freq = runCatching {
                dbQuery {
                    PewsConfigTable.selectAll().where {
                        PewsConfigTable.id eq org.jetbrains.exposed.dao.id.EntityID(schoolId, PewsConfigTable)
                    }.firstOrNull()?.get(PewsConfigTable.runFrequency)
                }
            }.getOrDefault("daily") ?: "daily"

            if (freq == "weekly" && runDate.dayOfWeek != targetDayOfWeek) {
                log.info("[$TAG] school {} skipped — weekly schedule, today is {} (target {})",
                    schoolId, runDate.dayOfWeek, targetDayOfWeek)
                continue
            }

            runCatching { runSchool(schoolId, runDate) }
                .onSuccess { totalAtRisk += it }
                .onFailure { log.warn("[$TAG] school {} pipeline failed: {}", schoolId, it.message) }
        }
        log.info("[$TAG] PEWS pipeline complete — {} at-risk snapshots across {} schools",
            totalAtRisk, schoolIds.size)
        return totalAtRisk
    }

    /** Full Sense→Triage→Reason→Act pipeline for one school. Returns at-risk count. */
    suspend fun runSchool(schoolId: UUID, runDate: LocalDate = LocalDate.now()): Int {
        // SENSE — always runs (deterministic)
        val snapshots = snapshotService.recomputeSchool(schoolId, runDate)
        if (snapshots.isEmpty()) return 0

        // TRIAGE — CLASSIFY prefilter + cohort dedup (degrades to v1 if killed)
        val deepLookCodes: Set<String> = runCatching {
            val triageResult = triageService.triage(schoolId, snapshots)
            log.info("[$TAG] triage: school={} → {} of {} need deep look ({} clusters, model={})",
                schoolId, triageResult.deepLookCount, triageResult.totalStudents,
                triageResult.clusterCount, triageResult.modelUsed)
            triageResult.decisions.filter { it.needsDeepLook }.map { it.studentCode }.toSet()
        }.onFailure { e ->
            if (e is com.littlebridge.enrollplus.feature.pews.core.PewsDisabledException) {
                log.info("[$TAG] triage disabled for school {} — falling back to v1 (reason all medium+)", schoolId)
            } else {
                log.warn("[$TAG] triage stage failed for {}: {} — falling back to v1", schoolId, e.message)
            }
        }.getOrDefault(snapshots.filter {
            it.riskLevel == "medium" || it.riskLevel == "high"
        }.map { it.studentCode }.toSet())

        // CASEWORKER — Tier 2 agent on deep-look subset (best-effort)
        val reasonTargets = snapshots.filter { it.studentCode in deepLookCodes }
        var caseResults: List<com.littlebridge.enrollplus.feature.pews.caseworker.CaseworkerService.CaseworkerResult>? = null
        if (reasonTargets.isNotEmpty()) {
            // Try the agentic caseworker first (Tier 2)
            caseResults = runCatching { caseworkerService.reviewBatch(schoolId, reasonTargets) }
                .onFailure { e ->
                    if (e is com.littlebridge.enrollplus.feature.pews.core.PewsDisabledException) {
                        log.info("[$TAG] caseworker disabled for school {} — falling back to v1 reasoning", schoolId)
                    } else {
                        log.warn("[$TAG] caseworker stage failed for {}: {} — falling back to v1", schoolId, e.message)
                    }
                }.getOrNull()

            if (caseResults != null) {
                log.info("[$TAG] caseworker: school={} → {} case files ({} model-generated, {} grounded)",
                    schoolId, caseResults.size,
                    caseResults.count { it.modelUsed },
                    caseResults.count { it.grounded })
                // Write case file narrative back to snapshot for API/UI visibility
                persistCaseFileNarratives(schoolId, caseResults, runDate)
            } else {
                // Fallback to v1 reasoning service
                runCatching { reasoningService.reasonForSchool(schoolId, reasonTargets) }
                    .onFailure { log.warn("[$TAG] v1 reason stage failed for {}: {}", schoolId, it.message) }
            }
        }

        // ACT — Tier 3 managed casework (sequenced plans, SLA, escalation)
        if (caseResults != null) {
            runCatching { managedCasework.actOnCaseFiles(schoolId, snapshots, caseResults) }
                .onFailure { log.warn("[$TAG] act (managed) failed for {}: {}", schoolId, it.message) }
        } else {
            runCatching { managedCasework.actOnSnapshots(schoolId, snapshots) }
                .onFailure { log.warn("[$TAG] act (v1 fallback) failed for {}: {}", schoolId, it.message) }
        }

        // Escalation sweep — check SLA breaches on open interventions
        runCatching { managedCasework.runEscalationSweep(schoolId) }
            .onFailure { log.warn("[$TAG] escalation sweep failed for {}: {}", schoolId, it.message) }

        // LEARN — Tier 4: auto-measure outcomes + update effectiveness priors
        runCatching { learnService.measureOutcomes(schoolId, runDate) }
            .onFailure { log.warn("[$TAG] learn stage failed for {}: {}", schoolId, it.message) }

        return snapshots.size
    }

    /**
     * Write the caseworker's Case File narrative/cause/recommendation back to
     * the snapshot rows so they're visible via the API (cohort, student detail).
     * This mirrors what PewsReasoningService does in the v1 path.
     */
    private suspend fun persistCaseFileNarratives(
        schoolId: UUID,
        caseResults: List<CaseworkerService.CaseworkerResult>,
        runDate: LocalDate,
    ) {
        for (result in caseResults) {
            val cf = result.caseFile
            val narrative = cf.narrative ?: continue
            val cause = cf.hypotheses.firstOrNull()?.cause
            val recommendation = cf.plan.firstOrNull()?.let { step ->
                "${step.action}${step.rationale?.let { ": $it" } ?: ""}"
            }
            val provider = result.providerUsed

            runCatching {
                dbQuery {
                    PewsRiskSnapshotsTable.update({
                        (PewsRiskSnapshotsTable.schoolId eq schoolId) and
                            (PewsRiskSnapshotsTable.studentCode eq result.studentCode) and
                            (PewsRiskSnapshotsTable.runDate eq runDate)
                    }) {
                        it[PewsRiskSnapshotsTable.aiNarrative] = narrative
                        it[PewsRiskSnapshotsTable.aiCause] = cause
                        it[PewsRiskSnapshotsTable.aiRecommendation] = recommendation
                        if (provider != null) it[PewsRiskSnapshotsTable.aiProviderUsed] = provider
                    }
                }
            }.onFailure { log.warn("[$TAG] failed to persist narrative for {}: {}", result.studentCode, it.message) }
        }
    }
}

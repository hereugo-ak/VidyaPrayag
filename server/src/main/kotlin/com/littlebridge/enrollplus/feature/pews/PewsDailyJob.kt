/*
 * File: PewsDailyJob.kt
 * Module: feature.pews
 *
 * Scheduled orchestrator that runs the full PEWS pipeline once a day per school:
 *
 *     SENSE  → PewsSnapshotService.recomputeSchool   (deterministic, always runs)
 *     REASON → PewsReasoningService.reasonForSchool   (LLM explains; degrades to null)
 *     ACT    → PewsInterventionService.actOnSnapshots (open task + Notify owner)
 *
 * Pattern is copied verbatim from PulseWeeklyJob: a long-running coroutine
 * launched at startup that checks every hour whether it's the target UTC hour,
 * with a per-day run-guard so a restart can't double-run. Default target hour is
 * 00 UTC (≈ 05:30 IST) — the recompute is ready before the school day. The hour
 * is env-tunable via PEWS_RUN_HOUR_UTC and the whole feature gated by PEWS_ENABLED.
 *
 * SENSE always runs (it's pure SQL/Kotlin and gives a working, honest cohort
 * even with zero AI). REASON + ACT layer on top and degrade gracefully.
 */
package com.littlebridge.enrollplus.feature.pews

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.io.File
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Properties
import java.util.UUID

object PewsDailyJob {
    private const val TAG = "PewsDailyJob"
    private val log = LoggerFactory.getLogger(TAG)

    private val snapshotService = PewsSnapshotService()
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

    fun start(scope: CoroutineScope) {
        if (!enabled) {
            log.info("[$TAG] PEWS_ENABLED=false — daily job not started")
            return
        }
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
            runCatching { runSchool(schoolId, runDate) }
                .onSuccess { totalAtRisk += it }
                .onFailure { log.warn("[$TAG] school {} pipeline failed: {}", schoolId, it.message) }
        }
        log.info("[$TAG] PEWS pipeline complete — {} at-risk snapshots across {} schools",
            totalAtRisk, schoolIds.size)
        return totalAtRisk
    }

    /** Full Sense→Reason→Act pipeline for one school. Returns at-risk count. */
    suspend fun runSchool(schoolId: UUID, runDate: LocalDate = LocalDate.now()): Int {
        // SENSE — always runs (deterministic)
        val snapshots = snapshotService.recomputeSchool(schoolId, runDate)
        if (snapshots.isEmpty()) return 0

        // REASON — best-effort (degrades to null narratives if no provider)
        runCatching { reasoningService.reasonForSchool(schoolId, snapshots) }
            .onFailure { log.warn("[$TAG] reason stage failed for {}: {}", schoolId, it.message) }

        // ACT — open interventions + notify owners (idempotent, no spam)
        runCatching { interventionService.actOnSnapshots(schoolId, snapshots) }
            .onFailure { log.warn("[$TAG] act stage failed for {}: {}", schoolId, it.message) }

        return snapshots.size
    }
}

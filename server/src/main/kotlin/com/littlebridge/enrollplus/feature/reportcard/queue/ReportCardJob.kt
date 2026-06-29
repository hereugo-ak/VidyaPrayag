/*
 * File: ReportCardJob.kt
 * Module: feature.reportcard.queue
 *
 * AI Report Card 2.0 — Async batch job queue + scheduled term-close trigger.
 *
 * Mirrors the PewsJobQueue pattern: the POST /report-card/generate endpoint
 * enqueues a job and returns immediately with a job_id. A background worker
 * polls the ai_jobs queue and processes report card generation with bounded
 * concurrency.
 *
 * Additionally, a scheduled trigger checks for term-close windows and
 * auto-enqueues generation jobs for classes that have not yet generated
 * drafts for the current term.
 *
 * Spec: AI_REPORT_CARD_2.0_AGENTIC_REDESIGN.md §7 (async batch processing)
 */
package com.littlebridge.enrollplus.feature.reportcard.queue

import com.littlebridge.enrollplus.db.AiJobsTable
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.ReportCardDraftsTable
import com.littlebridge.enrollplus.db.SchoolsTable
import com.littlebridge.enrollplus.db.StudentsTable
import com.littlebridge.enrollplus.feature.reportcard.assemble.ReportAssemblyService
import com.littlebridge.enrollplus.feature.reportcard.core.ReportCardConfig
import com.littlebridge.enrollplus.feature.reportcard.core.ReportCardKillSwitch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

object ReportCardJob {
    private val log = LoggerFactory.getLogger("ReportCardJob")
    private const val TAG = "ReportCardJob"
    private const val FEATURE_TAG = "report_card_batch"
    private const val POLL_INTERVAL_MS = 3000L
    private const val MAX_CONCURRENT_JOBS = 2
    private const val SCHEDULE_INTERVAL_MS = 3600_000L // 1 hour

    @Volatile
    private var workerRunning = false

    @Volatile
    private var schedulerRunning = false

    private val assemblyService = ReportAssemblyService()

    // ── Job lifecycle ──────────────────────────────────────────────────────

    /**
     * Enqueue a report card batch generation job.
     * Returns the job ID immediately so the caller can poll for status.
     */
    suspend fun enqueue(
        schoolId: UUID,
        className: String,
        section: String,
        term: String,
        academicYearId: UUID? = null,
        language: String = "hi",
        createdBy: UUID? = null,
    ): UUID {
        val jobId = UUID.randomUUID()
        val now = Instant.now()

        val payload = """{"className":"$className","section":"$section","term":"$term","language":"$language"}""" +
            (academicYearId?.let { ""","academicYearId":"$it"""" } ?: "")

        dbQuery {
            AiJobsTable.insert {
                it[AiJobsTable.id] = jobId
                it[AiJobsTable.schoolId] = schoolId
                it[AiJobsTable.feature] = FEATURE_TAG
                it[AiJobsTable.status] = "queued"
                it[AiJobsTable.totalItems] = 0
                it[AiJobsTable.completedItems] = 0
                it[AiJobsTable.result] = payload
                it[AiJobsTable.createdBy] = createdBy
                it[AiJobsTable.createdAt] = now
                it[AiJobsTable.updatedAt] = now
            }
        }

        log.info("[$TAG] Enqueued job {} for school {} class {}-{}", jobId, schoolId, className, section)
        return jobId
    }

    /**
     * Get the status of a report card job. Returns null if not found.
     */
    suspend fun status(jobId: UUID): JobStatus? = dbQuery {
        AiJobsTable.selectAll().where {
            (AiJobsTable.id eq jobId) and (AiJobsTable.feature eq FEATURE_TAG)
        }.singleOrNull()?.let { row ->
            JobStatus(
                jobId = jobId,
                status = row[AiJobsTable.status],
                totalItems = row[AiJobsTable.totalItems],
                completedItems = row[AiJobsTable.completedItems],
                result = row[AiJobsTable.result],
                createdAt = row[AiJobsTable.createdAt].toString(),
                completedAt = row[AiJobsTable.completedAt]?.toString(),
            )
        }
    }

    // ── Worker ─────────────────────────────────────────────────────────────

    /**
     * Start the background worker that polls for queued report card jobs
     * and processes them. Should be called once at application startup.
     */
    fun startWorker(scope: CoroutineScope) {
        if (workerRunning) {
            log.info("[$TAG] Worker already running — skipping")
            return
        }
        workerRunning = true

        scope.launch {
            log.info("[$TAG] Worker started — polling every {}ms, max {} concurrent",
                POLL_INTERVAL_MS, MAX_CONCURRENT_JOBS)
            while (isActive) {
                runCatching { pollAndProcess(scope) }
                    .onFailure { log.warn("[$TAG] Worker poll failed: {}", it.message) }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private suspend fun pollAndProcess(scope: CoroutineScope) {
        val queuedJobs = dbQuery {
            AiJobsTable.selectAll().where {
                (AiJobsTable.status eq "queued") and (AiJobsTable.feature eq FEATURE_TAG)
            }.orderBy(AiJobsTable.createdAt, SortOrder.ASC)
                .limit(MAX_CONCURRENT_JOBS)
                .toList()
        }

        if (queuedJobs.isEmpty()) return

        val processingCount = dbQuery {
            AiJobsTable.selectAll().where {
                (AiJobsTable.status eq "processing") and (AiJobsTable.feature eq FEATURE_TAG)
            }.count().toInt()
        }

        val availableSlots = MAX_CONCURRENT_JOBS - processingCount
        if (availableSlots <= 0) return

        for (job in queuedJobs.take(availableSlots)) {
            val jobId = job[AiJobsTable.id].value
            val schoolId = job[AiJobsTable.schoolId]
            val payload = job[AiJobsTable.result] ?: "{}"

            // Claim the job
            dbQuery {
                AiJobsTable.update({ AiJobsTable.id eq jobId }) {
                    it[AiJobsTable.status] = "processing"
                    it[AiJobsTable.updatedAt] = Instant.now()
                }
            }

            scope.launch {
                runCatching {
                    log.info("[$TAG] Processing job {} for school {}", jobId, schoolId)
                    val params = parsePayload(payload)
                    val result = assemblyService.generateForClass(
                        schoolId = schoolId,
                        className = params.className,
                        section = params.section,
                        term = params.term,
                        academicYearId = params.academicYearId,
                        language = params.language,
                        createdBy = job[AiJobsTable.createdBy] ?: UUID.randomUUID(),
                    )

                    dbQuery {
                        AiJobsTable.update({ AiJobsTable.id eq jobId }) {
                            it[AiJobsTable.status] = "completed"
                            it[AiJobsTable.totalItems] = result.totalStudents
                            it[AiJobsTable.completedItems] = result.completed
                            it[AiJobsTable.result] = """{"completed":${result.completed},"flagged":${result.flagged},"failed":${result.failed}}"""
                            it[AiJobsTable.updatedAt] = Instant.now()
                            it[AiJobsTable.completedAt] = Instant.now()
                        }
                    }
                    log.info("[$TAG] Job {} completed — {} students processed", jobId, result.completed)
                }.onFailure { e ->
                    log.error("[$TAG] Job {} failed: {}", jobId, e.message)
                    dbQuery {
                        AiJobsTable.update({ AiJobsTable.id eq jobId }) {
                            it[AiJobsTable.status] = "failed"
                            it[AiJobsTable.result] = """{"error":"${e.message?.replace("\"", "\\\"")}"}"""
                            it[AiJobsTable.updatedAt] = Instant.now()
                            it[AiJobsTable.completedAt] = Instant.now()
                        }
                    }
                }
            }
        }
    }

    // ── Scheduled term-close trigger ───────────────────────────────────────

    /**
     * Start the scheduled trigger that checks for term-close windows.
     * If a term window is active and a class has no drafts for that term,
     * auto-enqueue a generation job.
     *
     * Should be called once at application startup.
     */
    fun startScheduler(scope: CoroutineScope) {
        if (schedulerRunning) {
            log.info("[$TAG] Scheduler already running — skipping")
            return
        }
        schedulerRunning = true

        scope.launch {
            log.info("[$TAG] Scheduler started — checking every {}ms", SCHEDULE_INTERVAL_MS)
            while (isActive) {
                runCatching { checkTermCloseAndEnqueue() }
                    .onFailure { log.warn("[$TAG] Scheduler check failed: {}", it.message) }
                delay(SCHEDULE_INTERVAL_MS)
            }
        }
    }

    /**
     * Check all schools for term-close windows and auto-enqueue generation
     * for classes that haven't generated drafts yet.
     */
    private suspend fun checkTermCloseAndEnqueue() {
        if (!ReportCardConfig.enabled) return
        runCatching { ReportCardKillSwitch.require("reportcard") }.getOrElse { return }

        val schools = dbQuery {
            SchoolsTable.selectAll().where { SchoolsTable.isActive eq true }
                .map { it[SchoolsTable.id].value }
        }

        for (schoolId in schools) {
            runCatching {
                // Find unique class/section combos from students table
                val classSections = dbQuery {
                    StudentsTable.selectAll().where {
                        (StudentsTable.schoolId eq schoolId) and
                        (StudentsTable.isActive eq true)
                    }.map { row ->
                        row[StudentsTable.className] to (row[StudentsTable.section] ?: "A")
                    }.distinct()
                }

                for ((className, section) in classSections) {
                    val term = ReportCardConfig.currentTerm ?: continue
                    val existing = dbQuery {
                        ReportCardDraftsTable.selectAll().where {
                            (ReportCardDraftsTable.schoolId eq schoolId) and
                            (ReportCardDraftsTable.className eq className) and
                            (ReportCardDraftsTable.section eq section) and
                            (ReportCardDraftsTable.term eq term)
                        }.count().toInt()
                    }

                    if (existing == 0) {
                        log.info("[$TAG] Auto-enqueuing generation for {}-{} ({})", className, section, term)
                        enqueue(schoolId, className, section, term)
                    }
                }
            }.onFailure { log.warn("[$TAG] School {} check failed: {}", schoolId, it.message) }
        }
    }

    // ── Payload parsing ────────────────────────────────────────────────────

    private data class JobParams(
        val className: String,
        val section: String,
        val term: String,
        val academicYearId: UUID? = null,
        val language: String = "hi",
    )

    private fun parsePayload(payload: String): JobParams {
        val map = payload.removeSurrounding("{", "}")
            .split(",")
            .filter { it.contains(":") }
            .associate {
                val idx = it.indexOf(":")
                val key = it.substring(0, idx).trim().removeSurrounding("\"")
                val value = it.substring(idx + 1).trim().removeSurrounding("\"")
                key to value
            }
        return JobParams(
            className = map["className"] ?: "Unknown",
            section = map["section"] ?: "A",
            term = map["term"] ?: "Term 1",
            academicYearId = map["academicYearId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() },
            language = map["language"] ?: "hi",
        )
    }

    // ── Types ──────────────────────────────────────────────────────────────

    @Serializable
    data class JobStatus(
        @Contextual val jobId: UUID,
        val status: String,
        val totalItems: Int,
        val completedItems: Int,
        val result: String?,
        val createdAt: String,
        val completedAt: String?,
    )
}

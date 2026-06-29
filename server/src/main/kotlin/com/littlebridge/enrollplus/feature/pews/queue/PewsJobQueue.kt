/*
 * File: PewsJobQueue.kt
 * Module: feature.pews.queue
 *
 * PEWS 2.0 — Async job queue for PEWS pipeline runs.
 *
 * Replaces the synchronous POST /pews/run (which blocked for 23.5s) with an
 * async pattern: the endpoint enqueues a job and returns immediately (<1s)
 * with a job_id. A background worker polls the queue and processes jobs with
 * bounded concurrency.
 *
 * Uses the existing ai_jobs table for persistence.
 *
 * Spec: PEWS_2.0_AGENTIC_REDESIGN.md §9 (23.5s → async queue)
 */
package com.littlebridge.enrollplus.feature.pews.queue

import com.littlebridge.enrollplus.db.AiJobsTable
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

object PewsJobQueue {
    private val log = LoggerFactory.getLogger("PewsJobQueue")
    private const val TAG = "PewsJobQueue"

    private const val POLL_INTERVAL_MS = 2000L
    private const val MAX_CONCURRENT_JOBS = 2

    @Volatile
    private var workerRunning = false

    // ── Job lifecycle ──────────────────────────────────────────────────────

    /**
     * Enqueue a PEWS run job for a school. Returns the job ID immediately.
     * The caller can poll GET /pews/run/{jobId} for status.
     */
    suspend fun enqueue(schoolId: UUID, createdBy: UUID? = null): UUID {
        val jobId = UUID.randomUUID()
        val now = Instant.now()

        dbQuery {
            AiJobsTable.insert {
                it[AiJobsTable.id] = jobId
                it[AiJobsTable.schoolId] = schoolId
                it[AiJobsTable.feature] = "pews_run"
                it[AiJobsTable.status] = "queued"
                it[AiJobsTable.totalItems] = 0
                it[AiJobsTable.completedItems] = 0
                it[AiJobsTable.createdBy] = createdBy
                it[AiJobsTable.createdAt] = now
                it[AiJobsTable.updatedAt] = now
            }
        }

        log.info("[$TAG] Enqueued job {} for school {}", jobId, schoolId)
        return jobId
    }

    /**
     * Get the status of a job. Returns null if not found.
     */
    suspend fun status(jobId: UUID): JobStatus? = dbQuery {
        AiJobsTable.selectAll().where { AiJobsTable.id eq jobId }
            .singleOrNull()?.let { row ->
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
     * Start the background worker that polls for queued jobs and processes them.
     * Should be called once at application startup.
     */
    fun startWorker(scope: CoroutineScope, processor: suspend (UUID) -> Int) {
        if (workerRunning) {
            log.info("[$TAG] Worker already running — skipping")
            return
        }
        workerRunning = true

        scope.launch {
            log.info("[$TAG] Worker started — polling every {}ms, max {} concurrent",
                POLL_INTERVAL_MS, MAX_CONCURRENT_JOBS)
            while (isActive) {
                runCatching { pollAndProcess(scope, processor) }
                    .onFailure { log.warn("[$TAG] Worker poll failed: {}", it.message) }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private suspend fun pollAndProcess(scope: CoroutineScope, processor: suspend (UUID) -> Int) {
        // Claim queued jobs (simple claim: mark as processing)
        val queuedJobs = dbQuery {
            AiJobsTable.selectAll().where { AiJobsTable.status eq "queued" }
                .orderBy(AiJobsTable.createdAt, SortOrder.ASC)
                .limit(MAX_CONCURRENT_JOBS)
                .toList()
        }

        if (queuedJobs.isEmpty()) return

        // Count currently processing jobs to respect concurrency limit
        val processingCount = dbQuery {
            AiJobsTable.selectAll().where { AiJobsTable.status eq "processing" }
                .count().toInt()
        }

        val availableSlots = MAX_CONCURRENT_JOBS - processingCount
        if (availableSlots <= 0) return

        for (job in queuedJobs.take(availableSlots)) {
            val jobId = job[AiJobsTable.id].value
            val schoolId = job[AiJobsTable.schoolId]

            // Claim the job
            dbQuery {
                AiJobsTable.update({ AiJobsTable.id eq jobId }) {
                    it[AiJobsTable.status] = "processing"
                    it[AiJobsTable.updatedAt] = Instant.now()
                }
            }

            // Process in a separate coroutine
            scope.launch {
                runCatching {
                    log.info("[$TAG] Processing job {} for school {}", jobId, schoolId)
                    val atRiskCount = processor(schoolId)

                    dbQuery {
                        AiJobsTable.update({ AiJobsTable.id eq jobId }) {
                            it[AiJobsTable.status] = "completed"
                            it[AiJobsTable.totalItems] = atRiskCount
                            it[AiJobsTable.completedItems] = atRiskCount
                            it[AiJobsTable.result] = """{"at_risk":$atRiskCount}"""
                            it[AiJobsTable.updatedAt] = Instant.now()
                            it[AiJobsTable.completedAt] = Instant.now()
                        }
                    }
                    log.info("[$TAG] Job {} completed — {} at-risk", jobId, atRiskCount)
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

    // ── Types ──────────────────────────────────────────────────────────────

    @Serializable
    data class JobStatus(
        @Contextual val jobId: UUID,
        val status: String,        // queued|processing|completed|failed
        val totalItems: Int,
        val completedItems: Int,
        val result: String?,
        val createdAt: String,
        val completedAt: String?,
    )
}

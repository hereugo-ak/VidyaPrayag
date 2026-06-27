/*
 * File: PulseWeeklyJob.kt
 * Module: feature.pulse
 *
 * Scheduled job that generates Parent Pulse records every Sunday at 6 PM IST
 * (12:30 PM UTC). Follows the NotificationScheduler pattern: a long-running
 * coroutine launched at application startup that checks every hour whether
 * it's time to run, making it resilient to server restarts.
 *
 * The job:
 *   1. Checks if today is Sunday AND the current UTC hour is 12 (12:30 PM UTC ≈ 6 PM IST)
 *   2. If so, calls ParentPulseService.batchGenerateAll() for the current week
 *   3. Sends a push notification to each parent who received a pulse
 *
 * The hourly check (instead of a single sleep-until-Sunday) ensures that if
 * the server restarts at any point, it will still fire within the same hour
 * on the target day. A run-guard prevents duplicate runs within the same hour.
 */
package com.littlebridge.enrollplus.feature.pulse

import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.ParentPulsesTable
import com.littlebridge.enrollplus.feature.notifications.Notify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.selectAll
import org.slf4j.LoggerFactory
import java.time.DayOfWeek
import java.time.LocalDate

object PulseWeeklyJob {
    private const val TAG = "PulseWeeklyJob"
    private val log = LoggerFactory.getLogger("PulseWeeklyJob")

    // Sunday 6 PM IST = 12:30 PM UTC. We check at the top of hour 12 UTC.
    private val TARGET_DAY = DayOfWeek.SUNDAY
    private const val TARGET_HOUR_UTC = 12

    private val pulseService = ParentPulseService()

    @Volatile
    private var lastRunDate: LocalDate? = null

    fun start(scope: CoroutineScope) {
        scope.launch {
            log.info("[$TAG] Started — will generate pulses every Sunday at 6 PM IST (hourly check)")
            while (true) {
                delay(60 * 60 * 1000) // 1 hour
                runCatching { checkAndRun() }
                    .onFailure { log.warn("[$TAG] checkAndRun failed: ${it.message}") }
            }
        }
    }

    private suspend fun checkAndRun() {
        val now = java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC)
        val today = now.toLocalDate()

        // Guard: only run on Sundays at the target UTC hour
        if (now.dayOfWeek != TARGET_DAY) return
        if (now.hour != TARGET_HOUR_UTC) return

        // Guard: don't run twice in the same day
        if (lastRunDate == today) return
        lastRunDate = today

        val weekStart = ParentPulseService.currentWeekStart(today)
        log.info("[$TAG] Triggering batch generation for week starting {}", weekStart)

        val count = pulseService.batchGenerateAll(weekStart)
        log.info("[$TAG] Generated {} pulses for week {}", count, weekStart)

        // Send notifications to all parents who received a pulse this week
        sendPulseNotifications(weekStart)
    }

    /**
     * Manually trigger pulse generation + notifications for a given week
     * (defaults to the current week). Used by the super admin dev tools
     * endpoint so developers can test pulse generation without waiting for
     * the Sunday cron.
     */
    suspend fun runNow(weekStart: LocalDate = ParentPulseService.currentWeekStart()): Int {
        log.info("[{}] Manual trigger: batch generation for week starting {}", TAG, weekStart)
        val count = pulseService.batchGenerateAll(weekStart)
        log.info("[{}] Manual trigger: generated {} pulses for week {}", TAG, count, weekStart)
        sendPulseNotifications(weekStart)
        return count
    }

    /**
     * Send a push notification to each parent who has a pulse for this week.
     */
    private suspend fun sendPulseNotifications(weekStart: LocalDate) {
        val pulses = dbQuery {
            ParentPulsesTable.selectAll().where {
                ParentPulsesTable.weekStartDate eq weekStart
            }.orderBy(ParentPulsesTable.createdAt, SortOrder.DESC).toList()
        }

        if (pulses.isEmpty()) return

        // Group by parent to avoid sending multiple notifications to the same parent
        val byParent = pulses.groupBy { it[ParentPulsesTable.parentId] }

        for ((parentId, parentPulses) in byParent) {
            if (parentPulses.isEmpty()) continue
            val firstPulse = parentPulses.first()
            val studentName = firstPulse[ParentPulsesTable.studentName]
            val schoolId = firstPulse[ParentPulsesTable.schoolId]

            runCatching {
                Notify.toUser(
                    userId = parentId,
                    category = "pulse",
                    title = "\uD83D\uDCDA $studentName's weekly pulse is ready",
                    body = "Tap to view this week's summary",
                    schoolId = schoolId,
                    deepLink = "/parent/pulse",
                    refType = "parent_pulse",
                    refId = firstPulse[ParentPulsesTable.id].value.toString(),
                )
            }.onFailure { log.warn("[$TAG] Failed to send notification to parent {}: {}", parentId, it.message) }
        }

        log.info("[$TAG] Sent pulse notifications to {} parents", byParent.size)
    }
}

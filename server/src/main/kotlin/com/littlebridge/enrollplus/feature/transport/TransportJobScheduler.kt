/*
 * File: TransportJobScheduler.kt
 * Module: feature.transport
 *
 * Scheduled jobs for the transport tracking feature, following the
 * NotificationScheduler pattern (long-running coroutine launched at
 * application startup).
 *
 * Jobs:
 *   1. GPS Staleness Check — runs every 5 minutes, checks if any active
 *      vehicle's last GPS ping is older than STALENESS_THRESHOLD_MINUTES.
 *      If so, notifies school admins so they can investigate.
 *   2. Daily Attendance Finalization — runs at 8 PM IST (14:30 UTC), marks
 *      any transport attendance records for today that are missing pickup
 *      or drop status as "missed", and notifies parents.
 */
package com.littlebridge.enrollplus.feature.transport

import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.TransportAssignmentsTable
import com.littlebridge.enrollplus.db.TransportAttendanceTable
import com.littlebridge.enrollplus.db.TransportTrackingTable
import com.littlebridge.enrollplus.db.TransportVehiclesTable
import com.littlebridge.enrollplus.db.StudentsTable
import com.littlebridge.enrollplus.db.ChildrenTable
import com.littlebridge.enrollplus.feature.notifications.Notify
import com.littlebridge.enrollplus.feature.notifications.NotifyRecipients
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

object TransportJobScheduler {
    private const val TAG = "TransportJobScheduler"
    private val log = LoggerFactory.getLogger("TransportJobScheduler")

    private const val STALENESS_THRESHOLD_MINUTES = 10L
    private const val STALENESS_CHECK_INTERVAL_MS = 5 * 60 * 1000L // 5 minutes
    private const val FINALIZATION_CHECK_INTERVAL_MS = 60 * 60 * 1000L // 1 hour
    private const val FINALIZATION_TARGET_HOUR_UTC = 14 // 8 PM IST ≈ 14:30 UTC

    @Volatile
    private var lastFinalizationDate: LocalDate? = null

    fun start(scope: CoroutineScope) {
        // GPS staleness check — every 5 minutes
        scope.launch {
            log.info("[$TAG] GPS staleness checker started — interval ${STALENESS_CHECK_INTERVAL_MS}ms")
            while (true) {
                delay(STALENESS_CHECK_INTERVAL_MS)
                runCatching { checkGpsStaleness() }
                    .onFailure { log.warn("[$TAG] checkGpsStaleness failed: ${it.message}") }
            }
        }

        // Daily attendance finalization — hourly check, fires at 8 PM IST
        scope.launch {
            log.info("[$TAG] Attendance finalizer started — target hour $FINALIZATION_TARGET_HOUR_UTC UTC")
            while (true) {
                delay(FINALIZATION_CHECK_INTERVAL_MS)
                runCatching { checkAndFinalizeAttendance() }
                    .onFailure { log.warn("[$TAG] checkAndFinalizeAttendance failed: ${it.message}") }
            }
        }
    }

    // ── GPS Staleness Check ────────────────────────────────────────────────

    /**
     * For every active vehicle with a route assignment, check the latest
     * GPS ping. If it's older than [STALENESS_THRESHOLD_MINUTES], notify
     * the school admins so they can follow up with the driver.
     */
    private suspend fun checkGpsStaleness() {
        val now = Instant.now()
        val threshold = now.minus(STALENESS_THRESHOLD_MINUTES, ChronoUnit.MINUTES)

        // Get all active vehicles that have a route assigned
        val vehicles = dbQuery {
            TransportVehiclesTable.selectAll()
                .where {
                    (TransportVehiclesTable.isActive eq true) and
                    (TransportVehiclesTable.routeId.isNotNull())
                }
                .toList()
        }

        if (vehicles.isEmpty()) return

        val staleVehicles = mutableListOf<Triple<UUID, UUID, String>>() // (vehicleId, schoolId, busNumber)

        for (vehicle in vehicles) {
            val vehicleId = vehicle[TransportVehiclesTable.id].value
            val schoolId = vehicle[TransportVehiclesTable.schoolId]
            val busNumber = vehicle[TransportVehiclesTable.busNumber]

            val lastPing = dbQuery {
                TransportTrackingTable.selectAll()
                    .where { TransportTrackingTable.vehicleId eq vehicleId }
                    .orderBy(TransportTrackingTable.recordedAt, SortOrder.DESC)
                    .limit(1)
                    .singleOrNull()
            }

            if (lastPing == null) {
                // No GPS data at all — consider stale
                staleVehicles.add(Triple(vehicleId, schoolId, busNumber))
            } else {
                val recordedAt = lastPing[TransportTrackingTable.recordedAt]
                if (recordedAt.isBefore(threshold)) {
                    val minutesAgo = ChronoUnit.MINUTES.between(recordedAt, now)
                    staleVehicles.add(Triple(vehicleId, schoolId, "$busNumber (${minutesAgo}m ago)"))
                }
            }
        }

        if (staleVehicles.isEmpty()) return

        // Group by school and notify admins
        val bySchool = staleVehicles.groupBy { it.second }
        for ((schoolId, stale) in bySchool) {
            val adminIds = NotifyRecipients.adminsInSchool(schoolId)
            if (adminIds.isEmpty()) continue

            val busList = stale.joinToString(", ") { it.third }
            Notify.toUsers(
                userIds = adminIds,
                category = "transport",
                title = "GPS signal stale",
                body = "Bus GPS not updating: $busList. Please contact the driver.",
                schoolId = schoolId,
                deepLink = "/school/transport",
                refType = "transport_staleness",
                refId = schoolId.toString(),
            )
        }

        log.info("[$TAG] checkGpsStaleness: flagged {} stale vehicle(s) across {} school(s)", staleVehicles.size, bySchool.size)
    }

    // ── Daily Attendance Finalization ──────────────────────────────────────

    /**
     * Runs at 8 PM IST (14:30 UTC). For today's date, finds all active
     * transport assignments, and for each student that has NO attendance
     * record (or a record with null pickup/drop status), marks them as
     * "missed" and notifies the parent.
     */
    private suspend fun checkAndFinalizeAttendance() {
        val nowUtc = ZonedDateTime.now(ZoneOffset.UTC)
        val today = nowUtc.toLocalDate()

        // Guard: only run at the target UTC hour
        if (nowUtc.hour != FINALIZATION_TARGET_HOUR_UTC) return

        // Guard: don't run twice on the same day
        if (lastFinalizationDate == today) return
        lastFinalizationDate = today

        log.info("[$TAG] Finalizing transport attendance for {}", today)

        val todayDate = today.toString()
        val now = Instant.now()
        var finalizedCount = 0

        // Get all active assignments grouped by school
        val assignments = dbQuery {
            TransportAssignmentsTable.selectAll()
                .where { TransportAssignmentsTable.isActive eq true }
                .toList()
        }

        if (assignments.isEmpty()) {
            log.info("[$TAG] No active transport assignments found")
            return
        }

        // Group by school for notification batching
        val bySchool = assignments.groupBy { it[TransportAssignmentsTable.schoolId] }

        for ((schoolId, schoolAssignments) in bySchool) {
            val missedPickupParents = mutableListOf<UUID>()
            val missedDropParents = mutableListOf<UUID>()

            for (assignment in schoolAssignments) {
                val studentId = assignment[TransportAssignmentsTable.studentId]
                val routeId = assignment[TransportAssignmentsTable.routeId]

                // Check if an attendance record exists for today
                val existing = dbQuery {
                    TransportAttendanceTable.selectAll()
                        .where {
                            (TransportAttendanceTable.schoolId eq schoolId) and
                            (TransportAttendanceTable.studentId eq studentId) and
                            (TransportAttendanceTable.date eq today)
                        }
                        .singleOrNull()
                }

                if (existing == null) {
                    // No attendance record at all — create one with both missed
                    dbQuery {
                        TransportAttendanceTable.insert {
                            it[TransportAttendanceTable.schoolId] = schoolId
                            it[TransportAttendanceTable.studentId] = studentId
                            it[TransportAttendanceTable.routeId] = routeId
                            it[TransportAttendanceTable.date] = today
                            it[pickupStatus] = "missed"
                            it[dropStatus] = "missed"
                            it[createdAt] = now
                        }
                    }
                    finalizedCount++

                    // Resolve parent for notification
                    val parentIds = resolveParentIds(schoolId, studentId)
                    missedPickupParents.addAll(parentIds)
                    missedDropParents.addAll(parentIds)
                } else {
                    // Record exists — fill in any null statuses
                    val pickupStatus = existing[TransportAttendanceTable.pickupStatus]
                    val dropStatus = existing[TransportAttendanceTable.dropStatus]

                    if (pickupStatus == null) {
                        dbQuery {
                            TransportAttendanceTable.update({
                                TransportAttendanceTable.id eq existing[TransportAttendanceTable.id]
                            }) {
                                it[TransportAttendanceTable.pickupStatus] = "missed"
                            }
                        }
                        finalizedCount++
                        val parentIds = resolveParentIds(schoolId, studentId)
                        missedPickupParents.addAll(parentIds)
                    }

                    if (dropStatus == null) {
                        dbQuery {
                            TransportAttendanceTable.update({
                                TransportAttendanceTable.id eq existing[TransportAttendanceTable.id]
                            }) {
                                it[TransportAttendanceTable.dropStatus] = "missed"
                            }
                        }
                        finalizedCount++
                        val parentIds = resolveParentIds(schoolId, studentId)
                        missedDropParents.addAll(parentIds)
                    }
                }
            }

            // Notify parents of missed pickup
            if (missedPickupParents.isNotEmpty()) {
                val distinctParents = missedPickupParents.distinct()
                Notify.toUsers(
                    userIds = distinctParents,
                    category = "transport",
                    title = "Missed bus pickup",
                    body = "Your child was not marked as picked up on the bus today.",
                    schoolId = schoolId,
                    deepLink = "/parent/transport",
                    refType = "transport_attendance",
                    refId = todayDate,
                )
            }

            // Notify parents of missed drop
            if (missedDropParents.isNotEmpty()) {
                val distinctParents = missedDropParents.distinct()
                Notify.toUsers(
                    userIds = distinctParents,
                    category = "transport",
                    title = "Missed bus drop",
                    body = "Your child was not marked as dropped off by the bus today.",
                    schoolId = schoolId,
                    deepLink = "/parent/transport",
                    refType = "transport_attendance",
                    refId = todayDate,
                )
            }
        }

        log.info("[$TAG] Finalized {} attendance record(s) for {}", finalizedCount, today)
    }

    /**
     * Resolve parent app_users.id for a student by joining through
     * StudentsTable → studentCode → ChildrenTable → parentId.
     */
    private suspend fun resolveParentIds(schoolId: UUID, studentId: UUID): List<UUID> {
        val student = dbQuery {
            StudentsTable.selectAll()
                .where { StudentsTable.id eq studentId }
                .singleOrNull()
        } ?: return emptyList()

        val studentCode = student[StudentsTable.studentCode] ?: return emptyList()

        return dbQuery {
            ChildrenTable.selectAll()
                .where {
                    (ChildrenTable.studentCode eq studentCode) and
                    (ChildrenTable.isActive eq true)
                }
                .map { it[ChildrenTable.parentId] }
                .distinct()
        }
    }
}

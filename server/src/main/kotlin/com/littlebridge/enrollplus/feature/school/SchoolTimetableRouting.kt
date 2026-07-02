/*
 * File: SchoolTimetableRouting.kt
 * Module: feature.school
 *
 * Endpoint: GET /api/v1/school/timetable        (JWT, school-scoped)
 *           optional ?class=<class_name>        (server-side pre-filter)
 *
 * WHY THIS EXISTS
 *   The web admin "Command Center" calendar must render EVERY class's weekly
 *   schedule (all teachers, all subjects), but the only existing read of
 *   `teacher_periods` is TEACHER-scoped (TeacherRouting.kt filters on
 *   teacher_id = the caller). An admin has no way to see the whole school's
 *   timetable. This route closes that gap with a purely additive, read-only,
 *   school-scoped read assembled from REAL rows only.
 *
 *   The timetable is a RECURRING WEEKLY PATTERN: teacher_periods is keyed by
 *   weekday (1=Mon … 7=Sun, matching java.time.DayOfWeek.value), not by a
 *   calendar date. The client paints that pattern onto the dates of whatever
 *   week/month it is viewing. Date-specific overrides (holidays, exams) come
 *   from the separate /calendar endpoint and are layered on the client.
 *
 *   When a school hasn't entered a timetable the response is an honest empty
 *   payload (weekdays: [], classes: []) — the calendar then renders its
 *   designed empty state. Nothing is fabricated.
 *
 * REAL-TIME STRATEGY (documented here AND in the web hook useTimetable):
 *   SLOW (300s SWR poll, focus-revalidate). Timetables change on the order of
 *   terms, not minutes; second-by-second metrics (present-now, unread) ride the
 *   separate LIVE hooks. No websocket — the Ktor backend publishes no Supabase
 *   realtime channel to the web client (see ARCHITECTURE §Real-time).
 *
 * SOURCE: teacher_periods (school-scoped) left-joined to app_users for the
 *   teacher display name. weekday/start_time/position drive the ordering.
 */
package com.littlebridge.enrollplus.feature.school

import com.littlebridge.enrollplus.core.created
import com.littlebridge.enrollplus.core.fail
import com.littlebridge.enrollplus.core.ok
import com.littlebridge.enrollplus.core.requireSchoolAdmin
import com.littlebridge.enrollplus.core.requireSchoolContext
import com.littlebridge.enrollplus.db.AppUsersTable
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.TeacherPeriodsTable
import com.littlebridge.enrollplus.db.TeacherSubjectAssignmentsTable
import com.littlebridge.enrollplus.feature.notifications.Notify
import com.littlebridge.enrollplus.feature.notifications.NotifyRecipients
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

// T-101: teacher_periods.start_time/end_time are now typed `time` (LocalTime).
// Format back to the "HH:mm" wire contract this endpoint's DTOs expect.
private val TT_HHMM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

// ───────────────────────────────── DTOs ──────────────────────────────────────

@Serializable
data class TimetablePeriodDto(
    val id: String,
    @SerialName("start_time") val startTime: String,   // "HH:mm"
    @SerialName("end_time") val endTime: String,        // "HH:mm"
    @SerialName("class_name") val className: String,
    val section: String,
    val subject: String,
    val room: String,
    @SerialName("teacher_id") val teacherId: String,
    @SerialName("teacher_name") val teacherName: String // "" when unassigned/unknown
)

@Serializable
data class TimetableWeekdayDto(
    val weekday: Int,                                   // 1=Mon … 7=Sun
    val periods: List<TimetablePeriodDto>
)

@Serializable
data class TimetableDto(
    val weekdays: List<TimetableWeekdayDto>,
    val classes: List<String>                           // distinct class_name in view, sorted
)

@Serializable
data class CreatePeriodRequest(
    @SerialName("assignment_id") val assignmentId: String,
    val weekday: Int,                                   // 1=Mon … 7=Sun
    @SerialName("start_time") val startTime: String,    // "HH:mm"
    @SerialName("end_time") val endTime: String,        // "HH:mm"
    val room: String = "",
    @SerialName("valid_from") val validFrom: String? = null,  // YYYY-MM-DD
    @SerialName("valid_to") val validTo: String? = null,
)

@Serializable
data class UpdatePeriodRequest(
    val weekday: Int? = null,
    @SerialName("start_time") val startTime: String? = null,
    @SerialName("end_time") val endTime: String? = null,
    val room: String? = null,
    @SerialName("is_active") val isActive: Boolean? = null,
    @SerialName("valid_from") val validFrom: String? = null,
    @SerialName("valid_to") val validTo: String? = null,
)

@Serializable
data class PeriodDetailDto(
    val id: String,
    @SerialName("teacher_id") val teacherId: String,
    @SerialName("assignment_id") val assignmentId: String? = null,
    val weekday: Int,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    @SerialName("class_name") val className: String,
    val section: String,
    val subject: String,
    val room: String,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("valid_from") val validFrom: String? = null,
    @SerialName("valid_to") val validTo: String? = null,
)

private fun parseTime(hhmm: String): LocalTime? =
    runCatching { LocalTime.parse(hhmm, TT_HHMM) }.getOrNull()

private fun parseUuid(s: String?): UUID? =
    s?.let { runCatching { UUID.fromString(it) }.getOrNull() }

fun Route.schoolTimetableRouting() {
    authenticate("jwt") {
        get("/api/v1/school/timetable") {
            val ctx = call.requireSchoolContext() ?: return@get
            val schoolId = ctx.schoolId
            val classFilter = call.request.queryParameters["class"]?.trim()?.takeIf { it.isNotBlank() }

            val payload = dbQuery {
                // Teacher display names for this school (id → full name).
                val teacherNames = AppUsersTable.selectAll()
                    .where { AppUsersTable.schoolId eq schoolId }
                    .associate { it[AppUsersTable.id].value to it[AppUsersTable.fullName] }

                val rows = TeacherPeriodsTable.selectAll()
                    .where {
                        if (classFilter != null) {
                            (TeacherPeriodsTable.schoolId eq schoolId) and
                                (TeacherPeriodsTable.className eq classFilter)
                        } else {
                            TeacherPeriodsTable.schoolId eq schoolId
                        }
                    }
                    .map { r ->
                        val tId = r[TeacherPeriodsTable.teacherId]
                        TimetablePeriodDto(
                            id = r[TeacherPeriodsTable.id].value.toString(),
                            startTime = r[TeacherPeriodsTable.startTime].format(TT_HHMM),
                            endTime = r[TeacherPeriodsTable.endTime].format(TT_HHMM),
                            className = r[TeacherPeriodsTable.className],
                            section = r[TeacherPeriodsTable.section],
                            subject = r[TeacherPeriodsTable.subject],
                            room = r[TeacherPeriodsTable.room],
                            teacherId = tId.toString(),
                            teacherName = teacherNames[tId] ?: "",
                        ) to r[TeacherPeriodsTable.weekday]
                    }

                // Group by weekday; sort periods by start time then a stable id
                // (position isn't selected into the DTO but start_time is the
                // operationally meaningful order for a calendar column).
                val byWeekday = rows.groupBy { it.second }
                val weekdays = byWeekday
                    .map { (weekday, list) ->
                        TimetableWeekdayDto(
                            weekday = weekday,
                            periods = list.map { it.first }
                                .sortedWith(compareBy({ it.startTime }, { it.endTime }, { it.id }))
                        )
                    }
                    .sortedBy { it.weekday }

                val classes = rows.map { it.first.className }.distinct().sorted()

                TimetableDto(weekdays = weekdays, classes = classes)
            }

            call.ok(payload, message = "School timetable fetched")
        }

        // ── POST /api/v1/school/timetable/periods — admin creates a period ──────
        post("/api/v1/school/timetable/periods") {
            val ctx = call.requireSchoolAdmin() ?: return@post
            val req = call.receive<CreatePeriodRequest>()

            if (req.weekday !in 1..7) {
                call.fail("weekday must be 1-7", HttpStatusCode.BadRequest)
                return@post
            }
            val start = parseTime(req.startTime)
            val end = parseTime(req.endTime)
            if (start == null || end == null) {
                call.fail("start_time and end_time must be HH:mm", HttpStatusCode.BadRequest)
                return@post
            }
            if (!start.isBefore(end)) {
                call.fail("start_time must be before end_time", HttpStatusCode.BadRequest)
                return@post
            }

            val assignmentUuid = parseUuid(req.assignmentId)
            if (assignmentUuid == null) {
                call.fail("Valid assignment_id is required", HttpStatusCode.BadRequest)
                return@post
            }

            val period = dbQuery {
                val asg = TeacherSubjectAssignmentsTable.selectAll().where {
                    (TeacherSubjectAssignmentsTable.id eq assignmentUuid) and
                        (TeacherSubjectAssignmentsTable.schoolId eq ctx.schoolId) and
                        (TeacherSubjectAssignmentsTable.isActive eq true)
                }.firstOrNull() ?: return@dbQuery null

                val teacherId = asg[TeacherSubjectAssignmentsTable.teacherId]
                    ?: return@dbQuery null
                val className = asg[TeacherSubjectAssignmentsTable.className]
                val section = asg[TeacherSubjectAssignmentsTable.section]
                val subject = asg[TeacherSubjectAssignmentsTable.subject]

                // Double-booking check: same teacher, same weekday, same start time
                val conflict = TeacherPeriodsTable.selectAll().where {
                    (TeacherPeriodsTable.schoolId eq ctx.schoolId) and
                        (TeacherPeriodsTable.teacherId eq teacherId) and
                        (TeacherPeriodsTable.weekday eq req.weekday) and
                        (TeacherPeriodsTable.startTime eq start) and
                        (TeacherPeriodsTable.isActive eq true)
                }.firstOrNull()
                if (conflict != null) return@dbQuery null

                val newId = UUID.randomUUID()
                TeacherPeriodsTable.insert {
                    it[TeacherPeriodsTable.id] = newId
                    it[TeacherPeriodsTable.schoolId] = ctx.schoolId
                    it[TeacherPeriodsTable.teacherId] = teacherId
                    it[TeacherPeriodsTable.weekday] = req.weekday
                    it[TeacherPeriodsTable.startTime] = start
                    it[TeacherPeriodsTable.endTime] = end
                    it[TeacherPeriodsTable.assignmentId] = assignmentUuid
                    it[TeacherPeriodsTable.className] = className
                    it[TeacherPeriodsTable.section] = section
                    it[TeacherPeriodsTable.subject] = subject
                    it[TeacherPeriodsTable.room] = req.room
                    it[TeacherPeriodsTable.isActive] = true
                    it[TeacherPeriodsTable.createdAt] = Instant.now()
                }

                PeriodDetailDto(
                    id = newId.toString(),
                    teacherId = teacherId.toString(),
                    assignmentId = assignmentUuid.toString(),
                    weekday = req.weekday,
                    startTime = start.format(TT_HHMM),
                    endTime = end.format(TT_HHMM),
                    className = className,
                    section = section,
                    subject = subject,
                    room = req.room,
                    isActive = true,
                )
            }

            if (period == null) {
                call.fail(
                    "Could not create period — assignment not found or teacher double-booked",
                    HttpStatusCode.Conflict,
                    "PERIOD_CONFLICT"
                )
                return@post
            }

            // Notify the teacher about the new period
            runCatching {
                Notify.toUser(
                    userId = parseUuid(period.teacherId)!!,
                    category = "timetable",
                    title = "New period assigned",
                    body = "${period.subject} — ${period.className}-${period.section} on day $req.weekday at ${period.startTime}",
                    schoolId = ctx.schoolId,
                    refType = "teacher_period",
                    refId = period.id,
                )
            }

            call.created(period, message = "Period created")
        }

        // ── PUT /api/v1/school/timetable/periods/{id} — admin updates a period ──
        put("/api/v1/school/timetable/periods/{id}") {
            val ctx = call.requireSchoolAdmin() ?: return@put
            val periodId = parseUuid(call.parameters["id"]) ?: run {
                call.fail("Invalid period id", HttpStatusCode.BadRequest)
                return@put
            }
            val req = call.receive<UpdatePeriodRequest>()

            val updated = dbQuery {
                val existing = TeacherPeriodsTable.selectAll().where {
                    (TeacherPeriodsTable.id eq periodId) and
                        (TeacherPeriodsTable.schoolId eq ctx.schoolId)
                }.firstOrNull() ?: return@dbQuery null

                val newStart = req.startTime?.let { parseTime(it) } ?: existing[TeacherPeriodsTable.startTime]
                val newEnd = req.endTime?.let { parseTime(it) } ?: existing[TeacherPeriodsTable.endTime]
                if (!newStart.isBefore(newEnd)) return@dbQuery null

                TeacherPeriodsTable.update({
                    (TeacherPeriodsTable.id eq periodId) and
                        (TeacherPeriodsTable.schoolId eq ctx.schoolId)
                }) {
                    req.weekday?.let { v -> it[TeacherPeriodsTable.weekday] = v }
                    req.startTime?.let { v -> it[TeacherPeriodsTable.startTime] = newStart }
                    req.endTime?.let { v -> it[TeacherPeriodsTable.endTime] = newEnd }
                    req.room?.let { v -> it[TeacherPeriodsTable.room] = v }
                    req.isActive?.let { v -> it[TeacherPeriodsTable.isActive] = v }
                }

                TeacherPeriodsTable.selectAll().where {
                    TeacherPeriodsTable.id eq periodId
                }.first().let { r ->
                    PeriodDetailDto(
                        id = r[TeacherPeriodsTable.id].value.toString(),
                        teacherId = r[TeacherPeriodsTable.teacherId].toString(),
                        assignmentId = r[TeacherPeriodsTable.assignmentId]?.toString(),
                        weekday = r[TeacherPeriodsTable.weekday],
                        startTime = r[TeacherPeriodsTable.startTime].format(TT_HHMM),
                        endTime = r[TeacherPeriodsTable.endTime].format(TT_HHMM),
                        className = r[TeacherPeriodsTable.className],
                        section = r[TeacherPeriodsTable.section],
                        subject = r[TeacherPeriodsTable.subject],
                        room = r[TeacherPeriodsTable.room],
                        isActive = r[TeacherPeriodsTable.isActive],
                    )
                }
            }

            if (updated == null) {
                call.fail("Period not found or invalid time range", HttpStatusCode.NotFound)
                return@put
            }
            call.ok(updated, message = "Period updated")
        }

        // ── DELETE /api/v1/school/timetable/periods/{id} — admin deletes a period ─
        delete("/api/v1/school/timetable/periods/{id}") {
            val ctx = call.requireSchoolAdmin() ?: return@delete
            val periodId = parseUuid(call.parameters["id"]) ?: run {
                call.fail("Invalid period id", HttpStatusCode.BadRequest)
                return@delete
            }

            val deleted = dbQuery {
                val existing = TeacherPeriodsTable.selectAll().where {
                    (TeacherPeriodsTable.id eq periodId) and
                        (TeacherPeriodsTable.schoolId eq ctx.schoolId)
                }.firstOrNull() ?: return@dbQuery null

                val teacherId = existing[TeacherPeriodsTable.teacherId]
                val className = existing[TeacherPeriodsTable.className]
                val subject = existing[TeacherPeriodsTable.subject]

                TeacherPeriodsTable.deleteWhere {
                    (TeacherPeriodsTable.id eq periodId) and
                        (TeacherPeriodsTable.schoolId eq ctx.schoolId)
                }

                Triple(teacherId, className, subject)
            }

            if (deleted == null) {
                call.fail("Period not found", HttpStatusCode.NotFound)
                return@delete
            }

            // Notify the teacher
            runCatching {
                Notify.toUser(
                    userId = deleted.first,
                    category = "timetable",
                    title = "Period removed",
                    body = "${deleted.third} — ${deleted.second} period has been removed from the timetable",
                    schoolId = ctx.schoolId,
                    refType = "teacher_period",
                    refId = periodId.toString(),
                )
            }

            call.ok(mapOf("id" to periodId.toString()), message = "Period deleted")
        }
    }
}

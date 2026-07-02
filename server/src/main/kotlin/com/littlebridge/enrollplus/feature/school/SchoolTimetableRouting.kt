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
    @SerialName("teacher_id") val teacherId: String,
    @SerialName("class_name") val className: String,
    val section: String = "A",
    val subject: String,
    val weekday: Int,                                   // 1=Mon … 7=Sun
    @SerialName("start_time") val startTime: String,    // "HH:mm"
    @SerialName("end_time") val endTime: String,        // "HH:mm"
    val room: String = "",
    @SerialName("valid_from") val validFrom: String? = null,  // YYYY-MM-DD
    @SerialName("valid_to") val validTo: String? = null,
)

@Serializable
data class BulkPeriodItem(
    @SerialName("teacher_id") val teacherId: String,
    @SerialName("class_name") val className: String,
    val section: String = "A",
    val subject: String,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    val room: String = "",
)

@Serializable
data class BulkCreatePeriodsRequest(
    val weekday: Int,
    val periods: List<BulkPeriodItem>,
)

@Serializable
data class BulkCreatePeriodsResponse(
    val created: List<PeriodDetailDto> = emptyList(),
    val errors: List<String> = emptyList(),
    @SerialName("created_count") val createdCount: Int = 0,
    @SerialName("error_count") val errorCount: Int = 0,
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
data class CopySectionRequest(
    @SerialName("class_name") val className: String,
    @SerialName("from_section") val fromSection: String,
    @SerialName("to_section") val toSection: String,
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

            val teacherUuid = parseUuid(req.teacherId)
            if (teacherUuid == null) {
                call.fail("Valid teacher_id is required", HttpStatusCode.BadRequest)
                return@post
            }

            val period = dbQuery {
                // Validate teacher belongs to this school
                val teacherRow = AppUsersTable.selectAll().where {
                    (AppUsersTable.id eq teacherUuid) and
                        (AppUsersTable.schoolId eq ctx.schoolId) and
                        (AppUsersTable.role eq "teacher")
                }.firstOrNull() ?: return@dbQuery null

                // Find or create assignment — must match the ux_tsa_unique constraint columns
                val teacherName = teacherRow[AppUsersTable.fullName]
                val existingAsg = TeacherSubjectAssignmentsTable.selectAll().where {
                    (TeacherSubjectAssignmentsTable.schoolId eq ctx.schoolId) and
                        (TeacherSubjectAssignmentsTable.className eq req.className) and
                        (TeacherSubjectAssignmentsTable.section eq req.section) and
                        (TeacherSubjectAssignmentsTable.subject eq req.subject) and
                        (TeacherSubjectAssignmentsTable.teacherName eq teacherName)
                }.firstOrNull()

                val assignmentUuid = if (existingAsg != null) {
                    existingAsg[TeacherSubjectAssignmentsTable.id].value
                } else {
                    // Create the assignment if it doesn't exist
                    val newAsgId = UUID.randomUUID()
                    TeacherSubjectAssignmentsTable.insert {
                        it[TeacherSubjectAssignmentsTable.id] = newAsgId
                        it[TeacherSubjectAssignmentsTable.schoolId] = ctx.schoolId
                        it[TeacherSubjectAssignmentsTable.className] = req.className
                        it[TeacherSubjectAssignmentsTable.section] = req.section
                        it[TeacherSubjectAssignmentsTable.subject] = req.subject
                        it[TeacherSubjectAssignmentsTable.teacherId] = teacherUuid
                        it[TeacherSubjectAssignmentsTable.teacherName] = teacherName
                        it[TeacherSubjectAssignmentsTable.isActive] = true
                        it[TeacherSubjectAssignmentsTable.createdAt] = Instant.now()
                        it[TeacherSubjectAssignmentsTable.updatedAt] = Instant.now()
                    }
                    newAsgId
                }

                // Double-booking check: same teacher, same weekday, same start time
                val conflict = TeacherPeriodsTable.selectAll().where {
                    (TeacherPeriodsTable.schoolId eq ctx.schoolId) and
                        (TeacherPeriodsTable.teacherId eq teacherUuid) and
                        (TeacherPeriodsTable.weekday eq req.weekday) and
                        (TeacherPeriodsTable.startTime eq start) and
                        (TeacherPeriodsTable.isActive eq true)
                }.firstOrNull()
                if (conflict != null) return@dbQuery null

                val newId = UUID.randomUUID()
                TeacherPeriodsTable.insert {
                    it[TeacherPeriodsTable.id] = newId
                    it[TeacherPeriodsTable.schoolId] = ctx.schoolId
                    it[TeacherPeriodsTable.teacherId] = teacherUuid
                    it[TeacherPeriodsTable.weekday] = req.weekday
                    it[TeacherPeriodsTable.startTime] = start
                    it[TeacherPeriodsTable.endTime] = end
                    it[TeacherPeriodsTable.assignmentId] = assignmentUuid
                    it[TeacherPeriodsTable.className] = req.className
                    it[TeacherPeriodsTable.section] = req.section
                    it[TeacherPeriodsTable.subject] = req.subject
                    it[TeacherPeriodsTable.room] = req.room
                    it[TeacherPeriodsTable.isActive] = true
                    it[TeacherPeriodsTable.createdAt] = Instant.now()
                }

                PeriodDetailDto(
                    id = newId.toString(),
                    teacherId = teacherUuid.toString(),
                    assignmentId = assignmentUuid.toString(),
                    weekday = req.weekday,
                    startTime = start.format(TT_HHMM),
                    endTime = end.format(TT_HHMM),
                    className = req.className,
                    section = req.section,
                    subject = req.subject,
                    room = req.room,
                    isActive = true,
                )
            }

            if (period == null) {
                call.fail(
                    "Could not create period — teacher not found or double-booked",
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
                    body = "${period.subject} — ${period.className}-${period.section} on day ${period.weekday} at ${period.startTime}",
                    schoolId = ctx.schoolId,
                    refType = "teacher_period",
                    refId = period.id,
                )
            }

            call.created(period, message = "Period created")
        }

        // ── POST /api/v1/school/timetable/periods/bulk — admin creates many periods for one day ──
        post("/api/v1/school/timetable/periods/bulk") {
            val ctx = call.requireSchoolAdmin() ?: return@post
            val req = call.receive<BulkCreatePeriodsRequest>()

            if (req.weekday !in 1..7) {
                call.fail("weekday must be 1-7", HttpStatusCode.BadRequest)
                return@post
            }
            if (req.periods.isEmpty()) {
                call.fail("At least one period is required", HttpStatusCode.BadRequest)
                return@post
            }

            val result = dbQuery {
                val created = mutableListOf<PeriodDetailDto>()
                val errors = mutableListOf<String>()

                req.periods.forEach { item ->
                    val start = parseTime(item.startTime)
                    val end = parseTime(item.endTime)
                    if (start == null || end == null || !start.isBefore(end)) {
                        errors.add("Invalid time range: ${item.startTime}-${item.endTime} for ${item.subject}")
                        return@forEach
                    }

                    val teacherUuid = parseUuid(item.teacherId)
                    if (teacherUuid == null) {
                        errors.add("Invalid teacher_id for ${item.subject}")
                        return@forEach
                    }

                    val teacherRow = AppUsersTable.selectAll().where {
                        (AppUsersTable.id eq teacherUuid) and
                            (AppUsersTable.schoolId eq ctx.schoolId) and
                            (AppUsersTable.role eq "teacher")
                    }.firstOrNull()
                    if (teacherRow == null) {
                        errors.add("Teacher not found for ${item.subject}")
                        return@forEach
                    }

                    // Find or create assignment — must match the ux_tsa_unique constraint columns
                    val teacherName = teacherRow[AppUsersTable.fullName]
                    val existingAsg = TeacherSubjectAssignmentsTable.selectAll().where {
                        (TeacherSubjectAssignmentsTable.schoolId eq ctx.schoolId) and
                            (TeacherSubjectAssignmentsTable.className eq item.className) and
                            (TeacherSubjectAssignmentsTable.section eq item.section) and
                            (TeacherSubjectAssignmentsTable.subject eq item.subject) and
                            (TeacherSubjectAssignmentsTable.teacherName eq teacherName)
                    }.firstOrNull()

                    val assignmentUuid = if (existingAsg != null) {
                        existingAsg[TeacherSubjectAssignmentsTable.id].value
                    } else {
                        val newAsgId = UUID.randomUUID()
                        TeacherSubjectAssignmentsTable.insert {
                            it[TeacherSubjectAssignmentsTable.id] = newAsgId
                            it[TeacherSubjectAssignmentsTable.schoolId] = ctx.schoolId
                            it[TeacherSubjectAssignmentsTable.className] = item.className
                            it[TeacherSubjectAssignmentsTable.section] = item.section
                            it[TeacherSubjectAssignmentsTable.subject] = item.subject
                            it[TeacherSubjectAssignmentsTable.teacherId] = teacherUuid
                            it[TeacherSubjectAssignmentsTable.teacherName] = teacherName
                            it[TeacherSubjectAssignmentsTable.isActive] = true
                            it[TeacherSubjectAssignmentsTable.createdAt] = Instant.now()
                            it[TeacherSubjectAssignmentsTable.updatedAt] = Instant.now()
                        }
                        newAsgId
                    }

                    // Double-booking check
                    val conflict = TeacherPeriodsTable.selectAll().where {
                        (TeacherPeriodsTable.schoolId eq ctx.schoolId) and
                            (TeacherPeriodsTable.teacherId eq teacherUuid) and
                            (TeacherPeriodsTable.weekday eq req.weekday) and
                            (TeacherPeriodsTable.startTime eq start) and
                            (TeacherPeriodsTable.isActive eq true)
                    }.firstOrNull()
                    if (conflict != null) {
                        errors.add("Double-booked: ${item.subject} at ${item.startTime}")
                        return@forEach
                    }

                    val newId = UUID.randomUUID()
                    TeacherPeriodsTable.insert {
                        it[TeacherPeriodsTable.id] = newId
                        it[TeacherPeriodsTable.schoolId] = ctx.schoolId
                        it[TeacherPeriodsTable.teacherId] = teacherUuid
                        it[TeacherPeriodsTable.weekday] = req.weekday
                        it[TeacherPeriodsTable.startTime] = start
                        it[TeacherPeriodsTable.endTime] = end
                        it[TeacherPeriodsTable.assignmentId] = assignmentUuid
                        it[TeacherPeriodsTable.className] = item.className
                        it[TeacherPeriodsTable.section] = item.section
                        it[TeacherPeriodsTable.subject] = item.subject
                        it[TeacherPeriodsTable.room] = item.room
                        it[TeacherPeriodsTable.isActive] = true
                        it[TeacherPeriodsTable.createdAt] = Instant.now()
                    }

                    created += PeriodDetailDto(
                        id = newId.toString(),
                        teacherId = teacherUuid.toString(),
                        assignmentId = assignmentUuid.toString(),
                        weekday = req.weekday,
                        startTime = start.format(TT_HHMM),
                        endTime = end.format(TT_HHMM),
                        className = item.className,
                        section = item.section,
                        subject = item.subject,
                        room = item.room,
                        isActive = true,
                    )
                }

                BulkCreatePeriodsResponse(
                    created = created,
                    errors = errors,
                    createdCount = created.size,
                    errorCount = errors.size,
                )
            }

            call.created(result, message = "Bulk periods processed: ${result.createdCount} created, ${result.errorCount} errors")
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

        // ── POST /api/v1/school/timetable/periods/copy-section — copy all periods from one section to another ──
        post("/api/v1/school/timetable/periods/copy-section") {
            val ctx = call.requireSchoolAdmin() ?: return@post
            val req = call.receive<CopySectionRequest>()

            if (req.className.isBlank() || req.fromSection.isBlank() || req.toSection.isBlank()) {
                call.fail("class_name, from_section, and to_section are required", HttpStatusCode.BadRequest)
                return@post
            }
            if (req.fromSection == req.toSection) {
                call.fail("from_section and to_section must be different", HttpStatusCode.BadRequest)
                return@post
            }

            val result = dbQuery {
                val created = mutableListOf<PeriodDetailDto>()
                val errors = mutableListOf<String>()

                // Fetch all source periods (all weekdays) for the source section
                val sourcePeriods = TeacherPeriodsTable.selectAll().where {
                    (TeacherPeriodsTable.schoolId eq ctx.schoolId) and
                        (TeacherPeriodsTable.className eq req.className) and
                        (TeacherPeriodsTable.section eq req.fromSection) and
                        (TeacherPeriodsTable.isActive eq true)
                }.orderBy(TeacherPeriodsTable.weekday to org.jetbrains.exposed.sql.SortOrder.ASC)
                 .orderBy(TeacherPeriodsTable.startTime to org.jetbrains.exposed.sql.SortOrder.ASC)
                 .toList()

                if (sourcePeriods.isEmpty()) {
                    errors.add("No active periods found for ${req.className} section ${req.fromSection}")
                    return@dbQuery BulkCreatePeriodsResponse(created = created, errors = errors)
                }

                sourcePeriods.forEach { src ->
                    val srcTeacherId = src[TeacherPeriodsTable.teacherId]
                    val srcWeekday = src[TeacherPeriodsTable.weekday]
                    val srcStart = src[TeacherPeriodsTable.startTime]
                    val srcEnd = src[TeacherPeriodsTable.endTime]
                    val srcSubject = src[TeacherPeriodsTable.subject]
                    val srcRoom = src[TeacherPeriodsTable.room]

                    // Check if target already has a period at this slot (same teacher, weekday, start time)
                    val dupConflict = TeacherPeriodsTable.selectAll().where {
                        (TeacherPeriodsTable.schoolId eq ctx.schoolId) and
                            (TeacherPeriodsTable.teacherId eq srcTeacherId) and
                            (TeacherPeriodsTable.weekday eq srcWeekday) and
                            (TeacherPeriodsTable.startTime eq srcStart) and
                            (TeacherPeriodsTable.className eq req.className) and
                            (TeacherPeriodsTable.section eq req.toSection) and
                            (TeacherPeriodsTable.isActive eq true)
                    }.firstOrNull()
                    if (dupConflict != null) {
                        errors.add("Skipped: ${srcSubject} on day $srcWeekday at ${srcStart.format(TT_HHMM)} already exists in section ${req.toSection}")
                        return@forEach
                    }

                    // Find or create assignment for the target section (same teacher, same subject, new section)
                    val teacherRow = AppUsersTable.selectAll().where {
                        (AppUsersTable.id eq srcTeacherId) and
                            (AppUsersTable.schoolId eq ctx.schoolId)
                    }.firstOrNull()
                    val teacherName = teacherRow?.get(AppUsersTable.fullName) ?: ""

                    val existingAsg = TeacherSubjectAssignmentsTable.selectAll().where {
                        (TeacherSubjectAssignmentsTable.schoolId eq ctx.schoolId) and
                            (TeacherSubjectAssignmentsTable.className eq req.className) and
                            (TeacherSubjectAssignmentsTable.section eq req.toSection) and
                            (TeacherSubjectAssignmentsTable.subject eq srcSubject) and
                            (TeacherSubjectAssignmentsTable.teacherName eq teacherName)
                    }.firstOrNull()

                    val assignmentUuid = if (existingAsg != null) {
                        existingAsg[TeacherSubjectAssignmentsTable.id].value
                    } else {
                        val newAsgId = UUID.randomUUID()
                        TeacherSubjectAssignmentsTable.insert {
                            it[TeacherSubjectAssignmentsTable.id] = newAsgId
                            it[TeacherSubjectAssignmentsTable.schoolId] = ctx.schoolId
                            it[TeacherSubjectAssignmentsTable.className] = req.className
                            it[TeacherSubjectAssignmentsTable.section] = req.toSection
                            it[TeacherSubjectAssignmentsTable.subject] = srcSubject
                            it[TeacherSubjectAssignmentsTable.teacherId] = srcTeacherId
                            it[TeacherSubjectAssignmentsTable.teacherName] = teacherName
                            it[TeacherSubjectAssignmentsTable.isActive] = true
                            it[TeacherSubjectAssignmentsTable.createdAt] = Instant.now()
                            it[TeacherSubjectAssignmentsTable.updatedAt] = Instant.now()
                        }
                        newAsgId
                    }

                    val newId = UUID.randomUUID()
                    TeacherPeriodsTable.insert {
                        it[TeacherPeriodsTable.id] = newId
                        it[TeacherPeriodsTable.schoolId] = ctx.schoolId
                        it[TeacherPeriodsTable.teacherId] = srcTeacherId
                        it[TeacherPeriodsTable.weekday] = srcWeekday
                        it[TeacherPeriodsTable.startTime] = srcStart
                        it[TeacherPeriodsTable.endTime] = srcEnd
                        it[TeacherPeriodsTable.assignmentId] = assignmentUuid
                        it[TeacherPeriodsTable.className] = req.className
                        it[TeacherPeriodsTable.section] = req.toSection
                        it[TeacherPeriodsTable.subject] = srcSubject
                        it[TeacherPeriodsTable.room] = srcRoom
                        it[TeacherPeriodsTable.isActive] = true
                        it[TeacherPeriodsTable.createdAt] = Instant.now()
                    }

                    created += PeriodDetailDto(
                        id = newId.toString(),
                        teacherId = srcTeacherId.toString(),
                        assignmentId = assignmentUuid.toString(),
                        weekday = srcWeekday,
                        startTime = srcStart.format(TT_HHMM),
                        endTime = srcEnd.format(TT_HHMM),
                        className = req.className,
                        section = req.toSection,
                        subject = srcSubject,
                        room = srcRoom,
                        isActive = true,
                    )
                }

                BulkCreatePeriodsResponse(
                    created = created,
                    errors = errors,
                    createdCount = created.size,
                    errorCount = errors.size,
                )
            }

            call.created(result, message = "Copied ${result.createdCount} periods from ${req.fromSection} to ${req.toSection}${if (result.errorCount > 0) ", ${result.errorCount} skipped" else ""}")
        }
    }
}

/*
 * File: PeriodExceptionRouting.kt
 * Module: feature.school
 *
 * Period exceptions: one-off overrides to the recurring weekly timetable for a
 * specific date. Supports CANCELLED, RESCHEDULED, ROOM_CHANGE, SUBSTITUTION, EXTRA.
 *
 * Admin endpoints (school-scoped):
 *   GET    /api/v1/school/timetable/exceptions?date=YYYY-MM-DD
 *   POST   /api/v1/school/timetable/exceptions
 *   DELETE /api/v1/school/timetable/exceptions/{id}
 *
 * Teacher endpoints (scoped to own periods):
 *   GET    /api/v1/teacher/timetable/exceptions?date=YYYY-MM-DD
 *   POST   /api/v1/teacher/timetable/exceptions
 */
package com.littlebridge.enrollplus.feature.school

import com.littlebridge.enrollplus.core.TeacherContext
import com.littlebridge.enrollplus.core.created
import com.littlebridge.enrollplus.core.fail
import com.littlebridge.enrollplus.core.ok
import com.littlebridge.enrollplus.core.requireSchoolAdmin
import com.littlebridge.enrollplus.core.requireSchoolContext
import com.littlebridge.enrollplus.core.requireTeacherContext
import com.littlebridge.enrollplus.db.AppUsersTable
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.PeriodExceptionsTable
import com.littlebridge.enrollplus.db.TeacherPeriodsTable
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
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

private val EXC_HHMM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Serializable
data class PeriodExceptionDto(
    val id: String,
    @SerialName("period_id") val periodId: String? = null,
    val date: String,
    val kind: String,
    @SerialName("new_start") val newStart: String? = null,
    @SerialName("new_end") val newEnd: String? = null,
    @SerialName("new_room") val newRoom: String? = null,
    @SerialName("substitute_teacher_id") val substituteTeacherId: String? = null,
    @SerialName("substitute_teacher_name") val substituteTeacherName: String? = null,
    @SerialName("assignment_id") val assignmentId: String? = null,
    val note: String = "",
    @SerialName("class_name") val className: String = "",
    val section: String = "",
    val subject: String = "",
)

@Serializable
data class CreateExceptionRequest(
    @SerialName("period_id") val periodId: String? = null,
    val date: String,
    val kind: String,
    @SerialName("new_start") val newStart: String? = null,
    @SerialName("new_end") val newEnd: String? = null,
    @SerialName("new_room") val newRoom: String? = null,
    @SerialName("substitute_teacher_id") val substituteTeacherId: String? = null,
    @SerialName("assignment_id") val assignmentId: String? = null,
    val note: String = "",
)

@Serializable
data class PeriodExceptionListResponse(
    val exceptions: List<PeriodExceptionDto>
)

private fun parseUuid(s: String?): UUID? =
    s?.let { runCatching { UUID.fromString(it) }.getOrNull() }

private fun parseTime(hhmm: String?): LocalTime? =
    hhmm?.let { runCatching { LocalTime.parse(it, EXC_HHMM) }.getOrNull() }

private fun parseDate(iso: String): LocalDate? =
    runCatching { LocalDate.parse(iso) }.getOrNull()

private fun org.jetbrains.exposed.sql.ResultRow.toExceptionDto(
    substituteNames: Map<UUID, String>
): PeriodExceptionDto {
    val subId = this[PeriodExceptionsTable.substituteTeacherId]
    return PeriodExceptionDto(
        id = this[PeriodExceptionsTable.id].value.toString(),
        periodId = this[PeriodExceptionsTable.periodId]?.toString(),
        date = this[PeriodExceptionsTable.date].toString(),
        kind = this[PeriodExceptionsTable.kind],
        newStart = this[PeriodExceptionsTable.newStart]?.format(EXC_HHMM),
        newEnd = this[PeriodExceptionsTable.newEnd]?.format(EXC_HHMM),
        newRoom = this[PeriodExceptionsTable.newRoom],
        substituteTeacherId = subId?.toString(),
        substituteTeacherName = subId?.let { substituteNames[it] },
        assignmentId = this[PeriodExceptionsTable.assignmentId]?.toString(),
        note = this[PeriodExceptionsTable.note],
        className = this[PeriodExceptionsTable.className],
        section = this[PeriodExceptionsTable.section],
        subject = this[PeriodExceptionsTable.subject],
    )
}

fun Route.periodExceptionRouting() {
    authenticate("jwt") {

        // ═══════════════ Admin endpoints ═══════════════

        get("/api/v1/school/timetable/exceptions") {
            val ctx = call.requireSchoolContext() ?: return@get
            val dateStr = call.request.queryParameters["date"]
            val date = dateStr?.let { parseDate(it) }

            val list = dbQuery {
                val rows = PeriodExceptionsTable.selectAll().where {
                    (PeriodExceptionsTable.schoolId eq ctx.schoolId) and
                        (if (date != null) PeriodExceptionsTable.date eq date else PeriodExceptionsTable.date.isNotNull())
                }.toList()

                val subIds = rows.mapNotNull { it[PeriodExceptionsTable.substituteTeacherId] }.distinct()
                val subNames = if (subIds.isEmpty()) emptyMap()
                else AppUsersTable.selectAll().where { AppUsersTable.id inList subIds.map { org.jetbrains.exposed.dao.id.EntityID(it, AppUsersTable) } }
                    .associate { it[AppUsersTable.id].value to it[AppUsersTable.fullName] }

                rows.map { it.toExceptionDto(subNames) }
            }
            call.ok(PeriodExceptionListResponse(list), message = "Exceptions fetched")
        }

        post("/api/v1/school/timetable/exceptions") {
            val ctx = call.requireSchoolAdmin() ?: return@post
            val req = call.receive<CreateExceptionRequest>()

            val kind = req.kind.uppercase()
            if (kind !in listOf("CANCELLED", "RESCHEDULED", "ROOM_CHANGE", "SUBSTITUTION", "EXTRA")) {
                call.fail("Invalid kind: $kind", HttpStatusCode.BadRequest)
                return@post
            }
            val date = parseDate(req.date) ?: run {
                call.fail("Invalid date (expected YYYY-MM-DD)", HttpStatusCode.BadRequest)
                return@post
            }

            val periodUuid = parseUuid(req.periodId)
            val assignmentUuid = parseUuid(req.assignmentId)
            val subUuid = parseUuid(req.substituteTeacherId)

            // For non-EXTRA, period_id is required
            if (kind != "EXTRA" && periodUuid == null) {
                call.fail("period_id is required for $kind exceptions", HttpStatusCode.BadRequest)
                return@post
            }

            val exception = dbQuery {
                var className = ""
                var section = ""
                var subject = ""

                if (periodUuid != null) {
                    val period = TeacherPeriodsTable.selectAll().where {
                        (TeacherPeriodsTable.id eq periodUuid) and
                            (TeacherPeriodsTable.schoolId eq ctx.schoolId)
                    }.firstOrNull() ?: return@dbQuery null
                    className = period[TeacherPeriodsTable.className]
                    section = period[TeacherPeriodsTable.section]
                    subject = period[TeacherPeriodsTable.subject]
                }

                val newId = UUID.randomUUID()
                PeriodExceptionsTable.insert {
                    it[PeriodExceptionsTable.id] = newId
                    it[PeriodExceptionsTable.schoolId] = ctx.schoolId
                    it[PeriodExceptionsTable.periodId] = periodUuid
                    it[PeriodExceptionsTable.date] = date
                    it[PeriodExceptionsTable.kind] = kind
                    it[PeriodExceptionsTable.newStart] = parseTime(req.newStart)
                    it[PeriodExceptionsTable.newEnd] = parseTime(req.newEnd)
                    it[PeriodExceptionsTable.newRoom] = req.newRoom
                    it[PeriodExceptionsTable.substituteTeacherId] = subUuid
                    it[PeriodExceptionsTable.assignmentId] = assignmentUuid
                    it[PeriodExceptionsTable.note] = req.note
                    it[PeriodExceptionsTable.createdAt] = Instant.now()
                    it[PeriodExceptionsTable.updatedAt] = Instant.now()
                }

                val subNames = if (subUuid != null) {
                    AppUsersTable.selectAll().where { AppUsersTable.id eq subUuid }
                        .firstOrNull()?.let { mapOf(subUuid to it[AppUsersTable.fullName]) } ?: emptyMap()
                } else emptyMap()

                PeriodExceptionsTable.selectAll().where { PeriodExceptionsTable.id eq newId }
                    .first().toExceptionDto(subNames)
            }

            if (exception == null) {
                call.fail("Period not found in your school", HttpStatusCode.NotFound)
                return@post
            }

            // Notify parents of the class about the exception
            runCatching {
                val parentIds = NotifyRecipients.parentsOfClass(ctx.schoolId, exception.className)
                val msg = when (kind) {
                    "CANCELLED" -> "${exception.subject} (${exception.className}-${exception.section}) on ${exception.date} has been cancelled"
                    "RESCHEDULED" -> "${exception.subject} (${exception.className}-${exception.section}) on ${exception.date} has been rescheduled to ${exception.newStart}"
                    "ROOM_CHANGE" -> "${exception.subject} (${exception.className}-${exception.section}) on ${exception.date} room changed to ${exception.newRoom}"
                    "SUBSTITUTION" -> "${exception.subject} (${exception.className}-${exception.section}) on ${exception.date} has a substitute teacher"
                    "EXTRA" -> "Extra ${exception.subject} (${exception.className}-${exception.section}) added on ${exception.date} at ${exception.newStart}"
                    else -> "Timetable update for ${exception.className}-${exception.section} on ${exception.date}"
                }
                Notify.toUsers(
                    userIds = parentIds,
                    category = "timetable",
                    title = "Timetable update",
                    body = msg,
                    schoolId = ctx.schoolId,
                    refType = "period_exception",
                    refId = exception.id,
                )
            }

            call.created(exception, message = "Exception created")
        }

        delete("/api/v1/school/timetable/exceptions/{id}") {
            val ctx = call.requireSchoolAdmin() ?: return@delete
            val id = parseUuid(call.parameters["id"]) ?: run {
                call.fail("Invalid exception id", HttpStatusCode.BadRequest)
                return@delete
            }
            val deleted = dbQuery {
                PeriodExceptionsTable.deleteWhere {
                    (PeriodExceptionsTable.id eq id) and
                        (PeriodExceptionsTable.schoolId eq ctx.schoolId)
                }
            }
            if (deleted == 0) {
                call.fail("Exception not found", HttpStatusCode.NotFound)
                return@delete
            }
            call.ok(mapOf("id" to id.toString()), message = "Exception deleted")
        }

        // ═══════════════ Teacher endpoints ═══════════════

        get("/api/v1/teacher/timetable/exceptions") {
            val ctx = call.requireTeacherContext() ?: return@get
            val dateStr = call.request.queryParameters["date"]
            val date = dateStr?.let { parseDate(it) }

            val list = dbQuery {
                val myPeriodIds = TeacherPeriodsTable.selectAll().where {
                    (TeacherPeriodsTable.schoolId eq ctx.schoolId) and
                        (TeacherPeriodsTable.teacherId eq ctx.userId)
                }.map { it[TeacherPeriodsTable.id].value }.toSet()

                val rows = PeriodExceptionsTable.selectAll().where {
                    (PeriodExceptionsTable.schoolId eq ctx.schoolId) and
                        (if (date != null) PeriodExceptionsTable.date eq date else PeriodExceptionsTable.date.isNotNull())
                }.filter { row ->
                    val pid = row[PeriodExceptionsTable.periodId]
                    pid != null && pid in myPeriodIds
                }

                val subIds = rows.mapNotNull { it[PeriodExceptionsTable.substituteTeacherId] }.distinct()
                val subNames = if (subIds.isEmpty()) emptyMap()
                else AppUsersTable.selectAll().where { AppUsersTable.id inList subIds.map { org.jetbrains.exposed.dao.id.EntityID(it, AppUsersTable) } }
                    .associate { it[AppUsersTable.id].value to it[AppUsersTable.fullName] }

                rows.map { it.toExceptionDto(subNames) }
            }
            call.ok(PeriodExceptionListResponse(list), message = "Your exceptions fetched")
        }

        post("/api/v1/teacher/timetable/exceptions") {
            val ctx = call.requireTeacherContext() ?: return@post
            val req = call.receive<CreateExceptionRequest>()

            val kind = req.kind.uppercase()
            if (kind !in listOf("CANCELLED", "RESCHEDULED", "ROOM_CHANGE", "SUBSTITUTION", "EXTRA")) {
                call.fail("Invalid kind: $kind", HttpStatusCode.BadRequest)
                return@post
            }
            val date = parseDate(req.date) ?: run {
                call.fail("Invalid date (expected YYYY-MM-DD)", HttpStatusCode.BadRequest)
                return@post
            }

            val periodUuid = parseUuid(req.periodId)
            if (periodUuid == null && kind != "EXTRA") {
                call.fail("period_id is required for $kind exceptions", HttpStatusCode.BadRequest)
                return@post
            }

            val exception = dbQuery {
                if (periodUuid != null) {
                    val period = TeacherPeriodsTable.selectAll().where {
                        (TeacherPeriodsTable.id eq periodUuid) and
                            (TeacherPeriodsTable.schoolId eq ctx.schoolId) and
                            (TeacherPeriodsTable.teacherId eq ctx.userId)
                    }.firstOrNull() ?: return@dbQuery null
                }

                val newId = UUID.randomUUID()
                PeriodExceptionsTable.insert {
                    it[PeriodExceptionsTable.id] = newId
                    it[PeriodExceptionsTable.schoolId] = ctx.schoolId
                    it[PeriodExceptionsTable.periodId] = periodUuid
                    it[PeriodExceptionsTable.date] = date
                    it[PeriodExceptionsTable.kind] = kind
                    it[PeriodExceptionsTable.newStart] = parseTime(req.newStart)
                    it[PeriodExceptionsTable.newEnd] = parseTime(req.newEnd)
                    it[PeriodExceptionsTable.newRoom] = req.newRoom
                    it[PeriodExceptionsTable.substituteTeacherId] = parseUuid(req.substituteTeacherId)
                    it[PeriodExceptionsTable.assignmentId] = parseUuid(req.assignmentId)
                    it[PeriodExceptionsTable.note] = req.note
                    it[PeriodExceptionsTable.createdAt] = Instant.now()
                    it[PeriodExceptionsTable.updatedAt] = Instant.now()
                }

                PeriodExceptionsTable.selectAll().where { PeriodExceptionsTable.id eq newId }
                    .first().toExceptionDto(emptyMap())
            }

            if (exception == null) {
                call.fail("Period not found or not owned by you", HttpStatusCode.NotFound)
                return@post
            }

            // Notify admins about the teacher-created exception
            runCatching {
                val adminIds = NotifyRecipients.adminsInSchool(ctx.schoolId)
                Notify.toUsers(
                    userIds = adminIds,
                    category = "timetable",
                    title = "Timetable exception by ${ctx.fullName}",
                    body = "${kind} for ${exception.className}-${exception.section} ${exception.subject} on ${exception.date}",
                    schoolId = ctx.schoolId,
                    actorId = ctx.userId,
                    refType = "period_exception",
                    refId = exception.id,
                )
            }

            call.created(exception, message = "Exception created")
        }
    }
}

/*
 * File: TimetableChangeRequestRouting.kt
 * Module: feature.school
 *
 * Teacher → admin approval workflow for timetable changes.
 *
 * Teacher endpoints:
 *   POST   /api/v1/teacher/timetable-requests        — submit a request
 *   GET    /api/v1/teacher/timetable-requests         — list own requests
 *
 * Admin endpoints:
 *   GET    /api/v1/school/timetable-requests          — list pending/all
 *   POST   /api/v1/school/timetable-requests/{id}/approve
 *   POST   /api/v1/school/timetable-requests/{id}/reject
 */
package com.littlebridge.enrollplus.feature.school

import com.littlebridge.enrollplus.core.created
import com.littlebridge.enrollplus.core.fail
import com.littlebridge.enrollplus.core.ok
import com.littlebridge.enrollplus.core.requireSchoolAdmin
import com.littlebridge.enrollplus.core.requireSchoolContext
import com.littlebridge.enrollplus.core.requireTeacherContext
import com.littlebridge.enrollplus.db.AppUsersTable
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.TeacherPeriodsTable
import com.littlebridge.enrollplus.db.TeacherSubjectAssignmentsTable
import com.littlebridge.enrollplus.db.TimetableChangeRequestsTable
import com.littlebridge.enrollplus.feature.notifications.Notify
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

private val TCR_HHMM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Serializable
data class TimetableChangeRequestDto(
    val id: String,
    @SerialName("teacher_id") val teacherId: String,
    @SerialName("teacher_name") val teacherName: String = "",
    @SerialName("assignment_id") val assignmentId: String? = null,
    @SerialName("period_id") val periodId: String? = null,
    val kind: String,
    val weekday: Int,
    @SerialName("start_time") val startTime: String? = null,
    @SerialName("end_time") val endTime: String? = null,
    val room: String = "",
    val reason: String = "",
    val status: String,
    @SerialName("admin_note") val adminNote: String = "",
    @SerialName("reviewed_by") val reviewedBy: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("reviewed_at") val reviewedAt: String? = null,
    @SerialName("class_name") val className: String = "",
    val section: String = "",
    val subject: String = "",
)

@Serializable
data class CreateChangeRequestRequest(
    @SerialName("assignment_id") val assignmentId: String? = null,
    @SerialName("period_id") val periodId: String? = null,
    val kind: String,
    val weekday: Int,
    @SerialName("start_time") val startTime: String? = null,
    @SerialName("end_time") val endTime: String? = null,
    val room: String = "",
    val reason: String = "",
)

@Serializable
data class ReviewRequest(
    @SerialName("admin_note") val adminNote: String = "",
)

@Serializable
data class ChangeRequestListResponse(
    val requests: List<TimetableChangeRequestDto>
)

private fun parseUuid(s: String?): UUID? =
    s?.let { runCatching { UUID.fromString(it) }.getOrNull() }

private fun parseTime(hhmm: String?): LocalTime? =
    hhmm?.let { runCatching { LocalTime.parse(it, TCR_HHMM) }.getOrNull() }

private fun org.jetbrains.exposed.sql.ResultRow.toRequestDto(
    teacherName: String
): TimetableChangeRequestDto = TimetableChangeRequestDto(
    id = this[TimetableChangeRequestsTable.id].value.toString(),
    teacherId = this[TimetableChangeRequestsTable.teacherId].toString(),
    teacherName = teacherName,
    assignmentId = this[TimetableChangeRequestsTable.assignmentId]?.toString(),
    periodId = this[TimetableChangeRequestsTable.periodId]?.toString(),
    kind = this[TimetableChangeRequestsTable.kind],
    weekday = this[TimetableChangeRequestsTable.weekday],
    startTime = this[TimetableChangeRequestsTable.startTime]?.format(TCR_HHMM),
    endTime = this[TimetableChangeRequestsTable.endTime]?.format(TCR_HHMM),
    room = this[TimetableChangeRequestsTable.room],
    reason = this[TimetableChangeRequestsTable.reason],
    status = this[TimetableChangeRequestsTable.status],
    adminNote = this[TimetableChangeRequestsTable.adminNote],
    reviewedBy = this[TimetableChangeRequestsTable.reviewedBy]?.toString(),
    createdAt = this[TimetableChangeRequestsTable.createdAt].toString(),
    reviewedAt = this[TimetableChangeRequestsTable.reviewedAt]?.toString(),
    className = this[TimetableChangeRequestsTable.className],
    section = this[TimetableChangeRequestsTable.section],
    subject = this[TimetableChangeRequestsTable.subject],
)

fun Route.timetableChangeRequestRouting() {
    authenticate("jwt") {

        // ═══════════════ Teacher endpoints ═══════════════

        post("/api/v1/teacher/timetable-requests") {
            val ctx = call.requireTeacherContext() ?: return@post
            val req = call.receive<CreateChangeRequestRequest>()

            val kind = req.kind.uppercase()
            if (kind !in listOf("NEW_PERIOD", "UPDATE_PERIOD", "DELETE_PERIOD")) {
                call.fail("Invalid kind: $kind", HttpStatusCode.BadRequest)
                return@post
            }
            if (req.weekday !in 1..7) {
                call.fail("weekday must be 1-7", HttpStatusCode.BadRequest)
                return@post
            }

            val assignmentUuid = parseUuid(req.assignmentId)
            val periodUuid = parseUuid(req.periodId)

            if (kind == "NEW_PERIOD" && assignmentUuid == null) {
                call.fail("assignment_id is required for NEW_PERIOD", HttpStatusCode.BadRequest)
                return@post
            }
            if (kind in listOf("UPDATE_PERIOD", "DELETE_PERIOD") && periodUuid == null) {
                call.fail("period_id is required for $kind", HttpStatusCode.BadRequest)
                return@post
            }

            val start = parseTime(req.startTime)
            val end = parseTime(req.endTime)
            if (kind != "DELETE_PERIOD") {
                if (start == null || end == null) {
                    call.fail("start_time and end_time are required", HttpStatusCode.BadRequest)
                    return@post
                }
                if (!start.isBefore(end)) {
                    call.fail("start_time must be before end_time", HttpStatusCode.BadRequest)
                    return@post
                }
            }

            val request = dbQuery {
                var className = ""
                var section = ""
                var subject = ""

                if (assignmentUuid != null) {
                    val asg = TeacherSubjectAssignmentsTable.selectAll().where {
                        (TeacherSubjectAssignmentsTable.id eq assignmentUuid) and
                            (TeacherSubjectAssignmentsTable.schoolId eq ctx.schoolId) and
                            (TeacherSubjectAssignmentsTable.isActive eq true)
                    }.firstOrNull()
                    if (asg != null) {
                        className = asg[TeacherSubjectAssignmentsTable.className]
                        section = asg[TeacherSubjectAssignmentsTable.section]
                        subject = asg[TeacherSubjectAssignmentsTable.subject]
                    }
                }

                if (periodUuid != null) {
                    val period = TeacherPeriodsTable.selectAll().where {
                        (TeacherPeriodsTable.id eq periodUuid) and
                            (TeacherPeriodsTable.schoolId eq ctx.schoolId) and
                            (TeacherPeriodsTable.teacherId eq ctx.userId)
                    }.firstOrNull() ?: return@dbQuery null
                    if (className.isEmpty()) {
                        className = period[TeacherPeriodsTable.className]
                        section = period[TeacherPeriodsTable.section]
                        subject = period[TeacherPeriodsTable.subject]
                    }
                }

                val newId = UUID.randomUUID()
                TimetableChangeRequestsTable.insert {
                    it[TimetableChangeRequestsTable.id] = newId
                    it[TimetableChangeRequestsTable.schoolId] = ctx.schoolId
                    it[TimetableChangeRequestsTable.teacherId] = ctx.userId
                    it[TimetableChangeRequestsTable.assignmentId] = assignmentUuid
                    it[TimetableChangeRequestsTable.periodId] = periodUuid
                    it[TimetableChangeRequestsTable.kind] = kind
                    it[TimetableChangeRequestsTable.weekday] = req.weekday
                    it[TimetableChangeRequestsTable.startTime] = start
                    it[TimetableChangeRequestsTable.endTime] = end
                    it[TimetableChangeRequestsTable.room] = req.room
                    it[TimetableChangeRequestsTable.reason] = req.reason
                    it[TimetableChangeRequestsTable.status] = "PENDING"
                    it[TimetableChangeRequestsTable.createdAt] = Instant.now()
                    it[TimetableChangeRequestsTable.className] = className
                    it[TimetableChangeRequestsTable.section] = section
                    it[TimetableChangeRequestsTable.subject] = subject
                }

                TimetableChangeRequestsTable.selectAll().where {
                    TimetableChangeRequestsTable.id eq newId
                }.first().toRequestDto(ctx.fullName)
            }

            if (request == null) {
                call.fail("Period not found or not owned by you", HttpStatusCode.NotFound)
                return@post
            }

            // Notify admins about the new request
            runCatching {
                val adminIds = com.littlebridge.enrollplus.feature.notifications.NotifyRecipients.adminsInSchool(ctx.schoolId)
                Notify.toUsers(
                    userIds = adminIds,
                    category = "timetable",
                    title = "Timetable change request",
                    body = "${ctx.fullName} requested $kind for ${request.className}-${request.section} ${request.subject}",
                    schoolId = ctx.schoolId,
                    actorId = ctx.userId,
                    refType = "timetable_change_request",
                    refId = request.id,
                )
            }

            call.created(request, message = "Change request submitted")
        }

        get("/api/v1/teacher/timetable-requests") {
            val ctx = call.requireTeacherContext() ?: return@get

            val list = dbQuery {
                TimetableChangeRequestsTable.selectAll().where {
                    (TimetableChangeRequestsTable.schoolId eq ctx.schoolId) and
                        (TimetableChangeRequestsTable.teacherId eq ctx.userId)
                }.orderBy(TimetableChangeRequestsTable.createdAt, org.jetbrains.exposed.sql.SortOrder.DESC)
                    .map { it.toRequestDto(ctx.fullName) }
            }
            call.ok(ChangeRequestListResponse(list), message = "Your change requests fetched")
        }

        // ═══════════════ Admin endpoints ═══════════════

        get("/api/v1/school/timetable-requests") {
            val ctx = call.requireSchoolContext() ?: return@get
            val statusFilter = call.request.queryParameters["status"]?.uppercase()

            val list = dbQuery {
                val baseQuery = TimetableChangeRequestsTable.selectAll().where {
                    TimetableChangeRequestsTable.schoolId eq ctx.schoolId
                }
                val rows = if (statusFilter != null) {
                    baseQuery.andWhere {
                        TimetableChangeRequestsTable.status eq statusFilter
                    }.toList()
                } else {
                    baseQuery.toList()
                }

                val teacherIds = rows.map { it[TimetableChangeRequestsTable.teacherId] }.distinct()
                val teacherNames = if (teacherIds.isEmpty()) emptyMap()
                else AppUsersTable.selectAll().where { AppUsersTable.id inList teacherIds.map { org.jetbrains.exposed.dao.id.EntityID(it, AppUsersTable) } }
                    .associate { it[AppUsersTable.id].value to it[AppUsersTable.fullName] }

                rows.sortedByDescending { it[TimetableChangeRequestsTable.createdAt] }
                    .map { row -> row.toRequestDto(teacherNames[row[TimetableChangeRequestsTable.teacherId]] ?: "") }
            }
            call.ok(ChangeRequestListResponse(list), message = "Change requests fetched")
        }

        post("/api/v1/school/timetable-requests/{id}/approve") {
            val ctx = call.requireSchoolAdmin() ?: return@post
            val reqId = parseUuid(call.parameters["id"]) ?: run {
                call.fail("Invalid request id", HttpStatusCode.BadRequest)
                return@post
            }
            val req = call.receive<ReviewRequest>()

            val result = dbQuery {
                val tcr = TimetableChangeRequestsTable.selectAll().where {
                    (TimetableChangeRequestsTable.id eq reqId) and
                        (TimetableChangeRequestsTable.schoolId eq ctx.schoolId) and
                        (TimetableChangeRequestsTable.status eq "PENDING")
                }.firstOrNull() ?: return@dbQuery null

                val kind = tcr[TimetableChangeRequestsTable.kind]
                val teacherId = tcr[TimetableChangeRequestsTable.teacherId]
                val assignmentId = tcr[TimetableChangeRequestsTable.assignmentId]
                val periodId = tcr[TimetableChangeRequestsTable.periodId]
                val weekday = tcr[TimetableChangeRequestsTable.weekday]
                val startTime = tcr[TimetableChangeRequestsTable.startTime]
                val endTime = tcr[TimetableChangeRequestsTable.endTime]
                val room = tcr[TimetableChangeRequestsTable.room]
                val className = tcr[TimetableChangeRequestsTable.className]
                val section = tcr[TimetableChangeRequestsTable.section]
                val subject = tcr[TimetableChangeRequestsTable.subject]

                when (kind) {
                    "NEW_PERIOD" -> {
                        if (assignmentId != null && startTime != null && endTime != null) {
                            val asg = TeacherSubjectAssignmentsTable.selectAll().where {
                                (TeacherSubjectAssignmentsTable.id eq assignmentId) and
                                    (TeacherSubjectAssignmentsTable.isActive eq true)
                            }.firstOrNull()
                            if (asg != null) {
                                val tid = asg[TeacherSubjectAssignmentsTable.teacherId]
                                if (tid != null) {
                                    TeacherPeriodsTable.insert {
                                        it[TeacherPeriodsTable.id] = UUID.randomUUID()
                                        it[TeacherPeriodsTable.schoolId] = ctx.schoolId
                                        it[TeacherPeriodsTable.teacherId] = tid
                                        it[TeacherPeriodsTable.weekday] = weekday
                                        it[TeacherPeriodsTable.startTime] = startTime
                                        it[TeacherPeriodsTable.endTime] = endTime
                                        it[TeacherPeriodsTable.assignmentId] = assignmentId
                                        it[TeacherPeriodsTable.className] = className
                                        it[TeacherPeriodsTable.section] = section
                                        it[TeacherPeriodsTable.subject] = subject
                                        it[TeacherPeriodsTable.room] = room
                                        it[TeacherPeriodsTable.isActive] = true
                                        it[TeacherPeriodsTable.createdAt] = Instant.now()
                                    }
                                }
                            }
                        }
                    }
                    "UPDATE_PERIOD" -> {
                        if (periodId != null && startTime != null && endTime != null) {
                            TeacherPeriodsTable.update({
                                TeacherPeriodsTable.id eq periodId
                            }) {
                                it[TeacherPeriodsTable.weekday] = weekday
                                it[TeacherPeriodsTable.startTime] = startTime
                                it[TeacherPeriodsTable.endTime] = endTime
                                it[TeacherPeriodsTable.room] = room
                            }
                        }
                    }
                    "DELETE_PERIOD" -> {
                        if (periodId != null) {
                            TeacherPeriodsTable.update({
                                TeacherPeriodsTable.id eq periodId
                            }) {
                                it[TeacherPeriodsTable.isActive] = false
                            }
                        }
                    }
                }

                TimetableChangeRequestsTable.update({
                    TimetableChangeRequestsTable.id eq reqId
                }) {
                    it[TimetableChangeRequestsTable.status] = "APPROVED"
                    it[TimetableChangeRequestsTable.adminNote] = req.adminNote
                    it[TimetableChangeRequestsTable.reviewedBy] = ctx.userId
                    it[TimetableChangeRequestsTable.reviewedAt] = Instant.now()
                }

                teacherId
            }

            if (result == null) {
                call.fail("Request not found or already reviewed", HttpStatusCode.NotFound)
                return@post
            }

            // Notify the teacher
            runCatching {
                Notify.toUser(
                    userId = result,
                    category = "timetable",
                    title = "Timetable request approved",
                    body = "Your timetable change request has been approved",
                    schoolId = ctx.schoolId,
                    refType = "timetable_change_request",
                    refId = reqId.toString(),
                )
            }

            call.ok(mapOf("id" to reqId.toString(), "status" to "APPROVED"), message = "Request approved")
        }

        post("/api/v1/school/timetable-requests/{id}/reject") {
            val ctx = call.requireSchoolAdmin() ?: return@post
            val reqId = parseUuid(call.parameters["id"]) ?: run {
                call.fail("Invalid request id", HttpStatusCode.BadRequest)
                return@post
            }
            val req = call.receive<ReviewRequest>()

            val teacherId = dbQuery {
                val tcr = TimetableChangeRequestsTable.selectAll().where {
                    (TimetableChangeRequestsTable.id eq reqId) and
                        (TimetableChangeRequestsTable.schoolId eq ctx.schoolId) and
                        (TimetableChangeRequestsTable.status eq "PENDING")
                }.firstOrNull() ?: return@dbQuery null

                val tid = tcr[TimetableChangeRequestsTable.teacherId]

                TimetableChangeRequestsTable.update({
                    TimetableChangeRequestsTable.id eq reqId
                }) {
                    it[TimetableChangeRequestsTable.status] = "REJECTED"
                    it[TimetableChangeRequestsTable.adminNote] = req.adminNote
                    it[TimetableChangeRequestsTable.reviewedBy] = ctx.userId
                    it[TimetableChangeRequestsTable.reviewedAt] = Instant.now()
                }

                tid
            }

            if (teacherId == null) {
                call.fail("Request not found or already reviewed", HttpStatusCode.NotFound)
                return@post
            }

            // Notify the teacher
            runCatching {
                Notify.toUser(
                    userId = teacherId,
                    category = "timetable",
                    title = "Timetable request rejected",
                    body = if (req.adminNote.isNotBlank()) req.adminNote else "Your timetable change request has been rejected",
                    schoolId = ctx.schoolId,
                    refType = "timetable_change_request",
                    refId = reqId.toString(),
                )
            }

            call.ok(mapOf("id" to reqId.toString(), "status" to "REJECTED"), message = "Request rejected")
        }
    }
}

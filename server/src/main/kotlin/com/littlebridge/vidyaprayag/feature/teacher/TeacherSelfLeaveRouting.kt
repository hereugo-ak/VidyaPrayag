/*
 * File: TeacherSelfLeaveRouting.kt
 * Module: feature.teacher
 *
 * T-602a (Doc 04 §5.14) — the teacher's OWN leave workflow (apply + status list).
 *
 *   POST /api/v1/teacher/leave   — a teacher applies for their own leave.
 *   GET  /api/v1/teacher/leave   — the teacher's own submitted requests + status.
 *
 * This is DISTINCT from TeacherLeaveRouting.kt, which is the teacher's APPROVAL
 * inbox for STUDENT leave routed to their classes (requester_role="student").
 * Here the teacher is the APPLICANT, so every row is written with
 * requester_role="teacher" and routed to the school's admins for a decision
 * (school_admin / admin already see + decide teacher-type rows via the school
 * LeaveRequestsRouting endpoint — `GET /school/leave-requests?type=teacher`).
 *
 * DELETE-don't-patch: a NEW file, not a patch of the approval-inbox routing —
 * the apply/list concern is its own surface (mirrors the parent split of
 * ParentLeaveRouting [apply] vs the admin/teacher decision endpoints).
 *
 * MULTI-TENANCY: school_id is taken from the JWT-derived TeacherContext, never
 * from the body; a teacher can only ever list / create their OWN requests.
 */
package com.littlebridge.vidyaprayag.feature.teacher

import com.littlebridge.vidyaprayag.core.created
import com.littlebridge.vidyaprayag.core.fail
import com.littlebridge.vidyaprayag.core.ok
import com.littlebridge.vidyaprayag.core.requireTeacherContext
import com.littlebridge.vidyaprayag.db.AppUsersTable
import com.littlebridge.vidyaprayag.db.DatabaseFactory.dbQuery
import com.littlebridge.vidyaprayag.db.LeaveRequestsTable
import com.littlebridge.vidyaprayag.feature.notifications.Notify
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

// ── DTOs (mirror the shared TeacherModels.kt field-for-field) ─────────────────

@Serializable
data class CreateTeacherLeaveDto(
    @SerialName("date_from") val dateFrom: String, // YYYY-MM-DD
    @SerialName("date_to") val dateTo: String,      // YYYY-MM-DD
    val reason: String,
    @SerialName("image_url") val imageUrl: String? = null,
)

@Serializable
data class TeacherSelfLeaveDto(
    val id: String,
    @SerialName("date_from") val dateFrom: String,
    @SerialName("date_to") val dateTo: String,
    val reason: String,
    @SerialName("image_url") val imageUrl: String? = null,
    val status: String, // Pending | Approved | Rejected
)

@Serializable
data class TeacherSelfLeaveListResponse(
    @SerialName("pending_count") val pendingCount: Int,
    val requests: List<TeacherSelfLeaveDto> = emptyList(),
)

private fun org.jetbrains.exposed.sql.ResultRow.toTeacherSelfLeaveDto() = TeacherSelfLeaveDto(
    id = this[LeaveRequestsTable.id].value.toString(),
    dateFrom = this[LeaveRequestsTable.dateFrom],
    dateTo = this[LeaveRequestsTable.dateTo],
    reason = this[LeaveRequestsTable.reason],
    imageUrl = this[LeaveRequestsTable.imageUrl],
    status = this[LeaveRequestsTable.status],
)

/** Validates a YYYY-MM-DD pair: both parseable and from <= to. */
private fun validateTeacherLeaveDates(from: String, to: String): String? {
    val a = runCatching { LocalDate.parse(from) }.getOrNull()
        ?: return "date_from must be a valid YYYY-MM-DD date"
    val b = runCatching { LocalDate.parse(to) }.getOrNull()
        ?: return "date_to must be a valid YYYY-MM-DD date"
    if (b.isBefore(a)) return "date_to cannot be before date_from"
    return null
}

fun Route.teacherSelfLeaveRouting() {
    authenticate("jwt") {
        route("/api/v1/teacher/leave") {

            // -------- LIST own requests --------
            get {
                val ctx = call.requireTeacherContext() ?: return@get
                val statusParam = call.request.queryParameters["status"]?.trim()

                // A teacher's OWN leave rows: school-scoped, requester_role="teacher",
                // requester_id == me. requester_id is the canonical applicant key;
                // teacher_id on these rows is left NULL (it means "routed-to teacher"
                // for the student workflow, which doesn't apply when the teacher is
                // the applicant).
                val mine = dbQuery {
                    LeaveRequestsTable.selectAll()
                        .where {
                            (LeaveRequestsTable.schoolId eq ctx.schoolId) and
                                (LeaveRequestsTable.requesterRole eq "teacher") and
                                (LeaveRequestsTable.requesterId eq ctx.userId)
                        }
                        .orderBy(LeaveRequestsTable.createdAt, SortOrder.DESC)
                        .toList()
                }

                val filtered = mine.filter { row ->
                    statusParam.isNullOrBlank() || row[LeaveRequestsTable.status].equals(statusParam, true)
                }
                val pendingCount = mine.count { it[LeaveRequestsTable.status].equals("Pending", true) }

                call.ok(
                    TeacherSelfLeaveListResponse(
                        pendingCount = pendingCount,
                        requests = filtered.map { it.toTeacherSelfLeaveDto() },
                    ),
                    message = "Leave requests loaded",
                )
            }

            // -------- APPLY (my own leave) --------
            post {
                val ctx = call.requireTeacherContext() ?: return@post
                val req = call.receive<CreateTeacherLeaveDto>()
                validateTeacherLeaveDates(req.dateFrom, req.dateTo)?.let { call.fail(it); return@post }
                if (req.reason.isBlank()) { call.fail("reason is required"); return@post }

                val newId = UUID.randomUUID()
                val now = Instant.now()
                dbQuery {
                    LeaveRequestsTable.insert {
                        it[LeaveRequestsTable.id] = newId
                        it[schoolId] = ctx.schoolId
                        it[requesterId] = ctx.userId
                        it[requesterName] = ctx.fullName
                        it[requesterRole] = "teacher"
                        it[dateFrom] = req.dateFrom
                        it[dateTo] = req.dateTo
                        it[reason] = req.reason
                        it[imageUrl] = req.imageUrl
                        it[status] = "Pending"
                        // class/section/teacher/child/parent are STUDENT-workflow fields —
                        // left NULL for a teacher's own leave (the applicant is staff, not
                        // tied to a class roster). requesterId is the applicant key.
                        it[createdAt] = now
                        it[updatedAt] = now
                    }
                }

                // Route to the school's admins for a decision (they see + decide
                // teacher-type rows via GET /school/leave-requests?type=teacher).
                val adminIds: List<UUID> = dbQuery {
                    AppUsersTable.selectAll().where {
                        (AppUsersTable.schoolId eq ctx.schoolId) and
                            ((AppUsersTable.role eq "school_admin") or (AppUsersTable.role eq "admin"))
                    }.map { it[AppUsersTable.id].value }.distinct()
                }
                if (adminIds.isNotEmpty()) {
                    Notify.toUsers(
                        userIds = adminIds,
                        category = "leave",
                        title = "New staff leave request",
                        body = "${ctx.fullName} applied for leave (${req.dateFrom} → ${req.dateTo}).",
                        schoolId = ctx.schoolId,
                        actorId = ctx.userId,
                        deepLink = "school/leave-requests",
                        refType = "leave_request",
                        refId = newId.toString(),
                    )
                }

                call.created(
                    TeacherSelfLeaveDto(
                        id = newId.toString(),
                        dateFrom = req.dateFrom,
                        dateTo = req.dateTo,
                        reason = req.reason,
                        imageUrl = req.imageUrl,
                        status = "Pending",
                    ),
                    message = "Leave request submitted",
                )
            }
        }
    }
}

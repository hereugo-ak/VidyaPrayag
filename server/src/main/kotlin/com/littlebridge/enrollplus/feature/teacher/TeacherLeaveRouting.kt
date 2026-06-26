/*
 * File: TeacherLeaveRouting.kt
 * Module: feature.teacher
 *
 * RA-44 — the TEACHER leg of the cross-role leave workflow.
 *
 *   GET   /api/v1/teacher/leave-requests?status=Pending|Approved|Rejected
 *   PATCH /api/v1/teacher/leave-requests/{id}   { "status": "Approved"|"Rejected" }
 *
 * A teacher sees leave requests routed to THEIR classes only — either directly
 * addressed (leave_requests.teacher_id == me) or for a (class, section) the
 * teacher is assigned to (teacher_subject_assignments). school_admin / admin
 * see every request in their school (they stand in for any teacher). Deciding
 * a request notifies the applicant parent. An admin can still override later
 * via the school leave-requests endpoint (LeaveRequestsRouting).
 *
 * MULTI-TENANCY: every query is scoped to ctx.schoolId (derived from the JWT in
 * requireTeacherContext) and a teacher can only act on requests inside the set
 * of classes they own — never another teacher's or another school's.
 */
package com.littlebridge.vidyaprayag.feature.teacher

import com.littlebridge.vidyaprayag.core.fail
import com.littlebridge.vidyaprayag.core.ok
import com.littlebridge.vidyaprayag.core.okMessage
import com.littlebridge.vidyaprayag.core.requireTeacherContext
import com.littlebridge.vidyaprayag.core.teacherAssignmentsFor
import com.littlebridge.vidyaprayag.db.AttendanceRecordsTable
import com.littlebridge.vidyaprayag.db.ChildrenTable
import com.littlebridge.vidyaprayag.db.DatabaseFactory.dbQuery
import com.littlebridge.vidyaprayag.db.LeaveRequestsTable
import com.littlebridge.vidyaprayag.db.StudentsTable
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
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

// ── DTOs ─────────────────────────────────────────────────────────────────────

@Serializable
data class TeacherLeaveDto(
    val id: String,
    @SerialName("student_name") val studentName: String,
    @SerialName("class_name") val className: String? = null,
    val section: String? = null,
    @SerialName("date_from") val dateFrom: String,
    @SerialName("date_to") val dateTo: String,
    val reason: String,
    @SerialName("image_url") val imageUrl: String? = null,
    val status: String,
)

@Serializable
data class TeacherLeaveListResponse(
    @SerialName("pending_count") val pendingCount: Int,
    val requests: List<TeacherLeaveDto> = emptyList(),
)

@Serializable
data class TeacherLeaveDecisionDto(val status: String)

// ── T-204 constants (Doc 06 §3.5) ─────────────────────────────────────────────
// Approving a student leave WRITES `leave` attendance for the covered dates so
// downstream readers that query attendance_records directly (admin dashboards,
// parent academics aggregates) see the leave — closing B-LV-2 from the write
// direction (the load path already reflects it via approvedLeaveStudentIds, T-203).
private const val LEAVE_ATT_STATUS = "leave"
private const val LEAVE_ATT_SOURCE = "leave_auto"
private const val ATT_TYPE_STUDENT = "student"
// Safety cap: a single approval expands to at most this many day-rows. A normal
// student leave is a few days; this only guards against a pathological multi-month
// range producing thousands of rows in one transaction. DEVIATION (flagged): the
// spec doesn't name a cap; chosen for write-amplification safety, generous enough
// that real leaves (days/weeks) are never truncated.
private const val LEAVE_WRITE_MAX_DAYS = 62

private fun org.jetbrains.exposed.sql.ResultRow.toTeacherLeaveDto() = TeacherLeaveDto(
    id = this[LeaveRequestsTable.id].value.toString(),
    studentName = this[LeaveRequestsTable.requesterName],
    className = this[LeaveRequestsTable.className],
    section = this[LeaveRequestsTable.section],
    dateFrom = this[LeaveRequestsTable.dateFrom],
    dateTo = this[LeaveRequestsTable.dateTo],
    reason = this[LeaveRequestsTable.reason],
    imageUrl = this[LeaveRequestsTable.imageUrl],
    status = this[LeaveRequestsTable.status],
)

fun Route.teacherLeaveRouting() {
    authenticate("jwt") {
        route("/api/v1/teacher/leave-requests") {

            // -------- LIST (routed to my classes) --------
            get {
                val ctx = call.requireTeacherContext() ?: return@get
                val statusParam = call.request.queryParameters["status"]?.trim()

                // The (class, section) pairs this teacher owns — used to match
                // requests that were routed by class rather than a specific id.
                val owned = teacherAssignmentsFor(ctx)
                val ownedPairs = owned.map { it.className to it.section }.toSet()
                val privileged = ctx.role == "school_admin" || ctx.role == "admin"

                val all = dbQuery {
                    LeaveRequestsTable.selectAll()
                        .where {
                            (LeaveRequestsTable.schoolId eq ctx.schoolId) and
                                (LeaveRequestsTable.requesterRole eq "student")
                        }
                        .orderBy(LeaveRequestsTable.createdAt, SortOrder.DESC)
                        .toList()
                }

                val mine = all.filter { row ->
                    if (privileged) return@filter true
                    val direct = row[LeaveRequestsTable.teacherId] == ctx.userId
                    val byClass = (row[LeaveRequestsTable.className] to row[LeaveRequestsTable.section]) in ownedPairs
                    direct || byClass
                }

                val filtered = mine.filter { row ->
                    statusParam == null || row[LeaveRequestsTable.status].equals(statusParam, true)
                }
                val pendingCount = mine.count { it[LeaveRequestsTable.status].equals("Pending", true) }

                call.ok(
                    TeacherLeaveListResponse(
                        pendingCount = pendingCount,
                        requests = filtered.map { it.toTeacherLeaveDto() },
                    ),
                    message = "Leave requests loaded",
                )
            }

            // -------- DECIDE (approve / reject) --------
            patch("/{id}") {
                val ctx = call.requireTeacherContext() ?: return@patch
                val id = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() } ?: run {
                    call.fail("Invalid id", HttpStatusCode.BadRequest, "BAD_ID"); return@patch
                }
                val body = call.receive<TeacherLeaveDecisionDto>()
                val normalized = when (body.status.trim().lowercase()) {
                    "approved" -> "Approved"
                    "rejected" -> "Rejected"
                    else -> { call.fail("status must be Approved|Rejected"); return@patch }
                }

                val owned = teacherAssignmentsFor(ctx)
                val ownedPairs = owned.map { it.className to it.section }.toSet()
                val privileged = ctx.role == "school_admin" || ctx.role == "admin"

                // Read the row (school-scoped) and verify it is routed to this teacher
                // before mutating — a teacher may only decide requests for their classes.
                val decided = dbQuery {
                    val row = LeaveRequestsTable.selectAll().where {
                        (LeaveRequestsTable.id eq id) and (LeaveRequestsTable.schoolId eq ctx.schoolId)
                    }.singleOrNull() ?: return@dbQuery LeaveDecideResult.NotFound

                    val direct = row[LeaveRequestsTable.teacherId] == ctx.userId
                    val byClass = (row[LeaveRequestsTable.className] to row[LeaveRequestsTable.section]) in ownedPairs
                    if (!privileged && !direct && !byClass) return@dbQuery LeaveDecideResult.Forbidden

                    val now = Instant.now()
                    LeaveRequestsTable.update({
                        (LeaveRequestsTable.id eq id) and (LeaveRequestsTable.schoolId eq ctx.schoolId)
                    }) {
                        it[status] = normalized
                        it[actionedBy] = ctx.userId
                        it[actionedAt] = now
                        it[updatedAt] = now
                    }

                    // T-204 (Doc 06 §3.5, B-LV-2): approving a student leave WRITES
                    // day-level `leave` attendance for the covered dates, atomically
                    // with the status flip (same txn). Rejections write nothing.
                    if (normalized == "Approved") {
                        writeLeaveMarksOnApproval(
                            schoolId = ctx.schoolId,
                            childId = row[LeaveRequestsTable.childId],
                            dateFromIso = row[LeaveRequestsTable.dateFrom],
                            dateToIso = row[LeaveRequestsTable.dateTo],
                            markedBy = ctx.userId,
                            now = now,
                        )
                    }

                    LeaveDecideResult.Ok(
                        parentId = row[LeaveRequestsTable.parentId],
                        requesterName = row[LeaveRequestsTable.requesterName],
                    )
                }

                when (decided) {
                    LeaveDecideResult.NotFound ->
                        call.fail("Leave request not found", HttpStatusCode.NotFound, "NOT_FOUND")
                    LeaveDecideResult.Forbidden ->
                        call.fail("This leave request is not assigned to your classes", HttpStatusCode.Forbidden, "FORBIDDEN")
                    is LeaveDecideResult.Ok -> {
                        // RA-44 + RA-41: tell the applicant parent the outcome.
                        val verb = if (normalized == "Approved") "approved" else "rejected"
                        decided.parentId?.let { pid ->
                            Notify.toUser(
                                userId = pid,
                                category = "leave",
                                title = "Leave request $verb",
                                body = "${decided.requesterName}'s leave request was $verb by ${ctx.fullName}.",
                                schoolId = ctx.schoolId,
                                actorId = ctx.userId,
                                deepLink = "parent/leave",
                                refType = "leave_request",
                                refId = id.toString(),
                            )
                        }
                        call.okMessage("Leave request $verb")
                    }
                }
            }
        }
    }
}

/** Result of a teacher leave decision, so notify happens outside the txn. */
private sealed interface LeaveDecideResult {
    data object NotFound : LeaveDecideResult
    data object Forbidden : LeaveDecideResult
    data class Ok(val parentId: UUID?, val requesterName: String) : LeaveDecideResult
}

/**
 * T-204 (Doc 06 §3.5) — on leave APPROVAL, write day-level `leave` attendance
 * for every date the leave covers, closing B-LV-2 from the write direction.
 *
 * MUST be called from inside an already-open Exposed transaction (it is invoked
 * within the same `dbQuery {}` that flips the request to Approved, so the
 * status flip and the attendance writes commit atomically).
 *
 * Identity: `leave_requests.child_id` is a FK to `children.id` (the parent-side
 * child row), NOT `students.id`. Resolve child_id → children.student_code →
 * students.id (student_code is globally unique; still school-scoped for safety),
 * exactly as the T-203 loader does — otherwise the rows key to nothing.
 *
 * Shape of each written row: (school, date, type='student', student_id,
 * assignment_id = NULL, status='leave', source='leave_auto', marked_by=actioner).
 * We deliberately write a DAY-LEVEL row with a NULL assignment_id rather than one
 * per class period: a leave is a whole-day fact, not tied to a single teacher's
 * period, and a per-assignment fan-out would collide with the per-period marks a
 * teacher takes via T-203. Postgres treats NULLs as distinct in the typed unique
 * index, so we upsert MANUALLY (keyed on assignment_id IS NULL) to stay idempotent
 * across re-approvals.
 *
 * Returns the number of dates written/updated (0 if the child can't be resolved
 * to a student or the range is empty/invalid — never throws on resolution misses;
 * the leave approval itself must still succeed).
 */
private fun writeLeaveMarksOnApproval(
    schoolId: UUID,
    childId: UUID?,
    dateFromIso: String,
    dateToIso: String,
    markedBy: UUID,
    now: Instant,
): Int {
    if (childId == null) return 0

    // child_id → children.student_code (must be linked).
    val studentCode = ChildrenTable.selectAll().where {
        (ChildrenTable.id eq childId)
    }.firstOrNull()
        ?.get(ChildrenTable.studentCode)
        ?.takeIf { it.isNotBlank() } ?: return 0

    // children.student_code → students.id (school-scoped defense-in-depth).
    val studentId = StudentsTable.selectAll().where {
        (StudentsTable.schoolId eq schoolId) and (StudentsTable.studentCode eq studentCode)
    }.firstOrNull()
        ?.get(StudentsTable.id)?.value ?: return 0

    val from = runCatching { LocalDate.parse(dateFromIso) }.getOrNull() ?: return 0
    val to = runCatching { LocalDate.parse(dateToIso) }.getOrNull() ?: return 0
    if (to.isBefore(from)) return 0

    var written = 0
    var d = from
    var guard = 0
    while (!d.isAfter(to) && guard < LEAVE_WRITE_MAX_DAYS) {
        // Manual upsert on the day-level key (assignment_id IS NULL). We can't lean
        // on the unique index because NULL assignment_id is distinct in Postgres.
        val existing = AttendanceRecordsTable.selectAll().where {
            (AttendanceRecordsTable.schoolId eq schoolId) and
                (AttendanceRecordsTable.date eq d) and
                (AttendanceRecordsTable.type eq ATT_TYPE_STUDENT) and
                (AttendanceRecordsTable.studentId eq studentId) and
                (AttendanceRecordsTable.assignmentId.isNull())
        }.firstOrNull()

        if (existing != null) {
            // Don't clobber a manual mark a teacher already made for this day-level
            // row (e.g. student on approved leave who actually attended). Only refresh
            // rows that are themselves auto-derived from leave.
            val curSource = existing[AttendanceRecordsTable.attSource]
            if (curSource == LEAVE_ATT_SOURCE) {
                AttendanceRecordsTable.update({
                    (AttendanceRecordsTable.schoolId eq schoolId) and
                        (AttendanceRecordsTable.date eq d) and
                        (AttendanceRecordsTable.type eq ATT_TYPE_STUDENT) and
                        (AttendanceRecordsTable.studentId eq studentId) and
                        (AttendanceRecordsTable.assignmentId.isNull())
                }) {
                    it[status] = LEAVE_ATT_STATUS
                    it[attSource] = LEAVE_ATT_SOURCE
                    it[AttendanceRecordsTable.markedBy] = markedBy
                    it[markedAt] = now
                }
                written++
            }
        } else {
            AttendanceRecordsTable.insert {
                it[id] = UUID.randomUUID()
                it[AttendanceRecordsTable.schoolId] = schoolId
                it[date] = d
                it[type] = ATT_TYPE_STUDENT
                it[AttendanceRecordsTable.studentId] = studentId
                // assignment_id / enrollment_id left NULL — this is a day-level leave fact.
                it[status] = LEAVE_ATT_STATUS
                it[attSource] = LEAVE_ATT_SOURCE
                it[AttendanceRecordsTable.markedBy] = markedBy
                it[markedAt] = now
                it[createdAt] = now
            }
            written++
        }
        d = d.plusDays(1)
        guard++
    }
    return written
}

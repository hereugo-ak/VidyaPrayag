/*
 * File: TeacherAttendanceRouting.kt
 * Module: feature.teacher
 *
 * T-203 (Doc 06 §3.8) — the TYPED, SCOPED student-attendance plane that replaces
 * the legacy packed-`grade` attendance writer in TeacherRoutingTasks.kt. Every
 * read and write is keyed by an authorizing teacher_subject_assignment id
 * (requireOwnedAssignment), so scope is PROVABLE (Doc 05 binding), not parsed
 * from a "<class>-<section>" string.
 *
 * T-205: this plane now OWNS the canonical /api/v1/teacher/attendance path — the
 * legacy packed-`grade` GET/POST handler in TeacherRoutingTasks.kt was deleted
 * (DELETE-don't-patch), so the prior `-typed` suffix is retired. Absent/late saves
 * fan out parent alerts (RA-41), preserved from the deleted handler.
 *
 * Routes (JWT-guarded, scoped via core/TeacherAccess):
 *   GET  /api/v1/teacher/attendance?assignmentId=…&date=YYYY-MM-DD
 *        → AttendanceLoadDto: the enrollment roster (active on `date`),
 *          approved-leave students pre-marked `leave`/`leave_auto` (§3.5),
 *          existing saved marks merged for EDIT (alreadyMarked + last-marked
 *          audit, E3), holiday/cancelled flags (E1/E2) and the back-date window.
 *   POST /api/v1/teacher/attendance
 *        body AttendanceSaveRequest { assignmentId, date, marks:[{studentId,status}] }
 *        → AttendanceSaveResultDto { saved, date }; UPSERTS on
 *          (school, date, type='student', student_id, assignment_id), stamping
 *          marked_by=me, marked_at=now, source per origin. No publish side
 *          effects (contrast B-MK-1) — attendance just saves.
 *
 * Scoping is enforced at THREE levels (the constitution): the SQL only ever
 * touches the owned assignment's enrollment roster (query), the response only
 * carries that roster (API), and the screen reaches this pre-scoped (UI, T-205).
 *
 * Honesty: an unconfigured class (assignment with no class_id / no enrollments)
 * yields an empty roster + the honest empty state — never a fabricated list.
 *
 * DTOs are defined server-side (the :server module does NOT depend on :shared)
 * and mirror shared/.../teacher/domain/model/TeacherModels.kt field-for-field.
 */
package com.littlebridge.vidyaprayag.feature.teacher

import com.littlebridge.vidyaprayag.core.TeacherContext
import com.littlebridge.vidyaprayag.core.enrollmentsFor
import com.littlebridge.vidyaprayag.core.fail
import com.littlebridge.vidyaprayag.core.ok
import com.littlebridge.vidyaprayag.core.requireOwnedAssignment
import com.littlebridge.vidyaprayag.core.requireTeacherContext
import com.littlebridge.vidyaprayag.db.AppUsersTable
import com.littlebridge.vidyaprayag.db.AttendanceRecordsTable
import com.littlebridge.vidyaprayag.db.CalendarEventsTable
import com.littlebridge.vidyaprayag.db.ChildrenTable
import com.littlebridge.vidyaprayag.db.DatabaseFactory.dbQuery
import com.littlebridge.vidyaprayag.db.LeaveRequestsTable
import com.littlebridge.vidyaprayag.db.StudentsTable
import com.littlebridge.vidyaprayag.feature.calendar.EventStatus
import com.littlebridge.vidyaprayag.feature.calendar.EventType
import com.littlebridge.vidyaprayag.feature.notifications.Notify
import com.littlebridge.vidyaprayag.feature.notifications.NotifyRecipients
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.receive
import io.ktor.server.routing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

// ─────────────────────────────────────────────────────────────────────────────
// Server-side DTOs — mirror shared/.../teacher/domain/model/TeacherModels.kt
// (AttendanceLoadDto / AttendanceStudentDto / AttendanceSaveRequest /
// AttendanceSaveMarkDto / AttendanceSaveResultDto) field-for-field.
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class AttendanceStudentDto(
    @SerialName("student_id") val studentId: String,
    val name: String,
    @SerialName("roll_no") val rollNo: String = "",
    val status: String = "present",
    val source: String? = null,
    @SerialName("enrollment_id") val enrollmentId: String? = null,
)

@Serializable
data class AttendanceLoadDto(
    @SerialName("assignment_id") val assignmentId: String,
    val date: String,
    val scope: String = "",
    @SerialName("class_name") val className: String = "",
    val section: String = "",
    val subject: String = "",
    val students: List<AttendanceStudentDto> = emptyList(),
    @SerialName("already_marked") val alreadyMarked: Boolean = false,
    @SerialName("last_marked_by") val lastMarkedBy: String? = null,
    @SerialName("last_marked_at") val lastMarkedAt: String? = null,
    @SerialName("leave_defaults") val leaveDefaults: List<String> = emptyList(),
    @SerialName("is_holiday") val isHoliday: Boolean = false,
    @SerialName("holiday_name") val holidayName: String? = null,
    @SerialName("is_cancelled") val isCancelled: Boolean = false,
    @SerialName("back_date_window_days") val backDateWindowDays: Int = ATT_BACK_DATE_WINDOW_DAYS,
)

@Serializable
data class AttendanceSaveMarkDto(
    @SerialName("student_id") val studentId: String,
    val status: String,
)

@Serializable
data class AttendanceSaveRequest(
    @SerialName("assignment_id") val assignmentId: String,
    val date: String,
    val marks: List<AttendanceSaveMarkDto> = emptyList(),
)

@Serializable
data class AttendanceSaveResultDto(
    val saved: Int = 0,
    val date: String,
)

// Valid student attendance states (Doc 06 §3 — adds `leave`, D-ATT-1).
private val VALID_ATTENDANCE = setOf("present", "absent", "late", "leave")

// E9: back-dating beyond this window is blocked ("Contact admin to mark older
// than N days"). Future dates are blocked outright. A single source of truth so
// the GET (advertised window) and POST (enforced window) can never disagree.
const val ATT_BACK_DATE_WINDOW_DAYS = 7

private const val LEAVE_STATUS_APPROVED = "Approved"
private const val SOURCE_LEAVE_AUTO = "leave_auto"
private const val SOURCE_MANUAL = "manual"

/**
 * Student ids (as `students.id`) on an APPROVED leave covering [date] for this
 * school. leave_requests stores dates as varchar "YYYY-MM-DD" and the student by
 * `child_id` (the children/students linkage) — we match on child_id when present
 * and fall back to nothing else (no fragile name match). Returns the set so the
 * loader can pre-default those students to `leave` (§3.5).
 */
private fun approvedLeaveStudentIds(schoolId: UUID, date: LocalDate): Set<UUID> {
    val iso = date.toString()

    // STEP 1 — collect children.id for approved student-leave rows that cover `date`.
    // NOTE: leave_requests.child_id is a FK to children.id (the PARENT-side child row),
    // NOT students.id. We must resolve children.id -> children.student_code -> students.id,
    // otherwise the returned set can never intersect the roster (which keys by students.id).
    // dateFrom/dateTo are zero-padded varchar(12) ISO dates, so lexicographic compare
    // is chronologically correct.
    val childIds: Set<UUID> = LeaveRequestsTable.selectAll().where {
        (LeaveRequestsTable.schoolId eq schoolId) and
            (LeaveRequestsTable.requesterRole eq "student") and
            (LeaveRequestsTable.status eq LEAVE_STATUS_APPROVED)
    }.mapNotNull { row ->
        val from = row[LeaveRequestsTable.dateFrom]
        val to = row[LeaveRequestsTable.dateTo]
        val covers = from <= iso && iso <= to
        if (covers) row[LeaveRequestsTable.childId] else null
    }.toSet()

    if (childIds.isEmpty()) return emptySet()

    // STEP 2 — children.id -> children.student_code (drop rows without a linked code).
    val studentCodes: Set<String> = ChildrenTable.selectAll().where {
        ChildrenTable.id inList childIds.toList()
    }.mapNotNull { it[ChildrenTable.studentCode]?.takeIf { code -> code.isNotBlank() } }
        .toSet()

    if (studentCodes.isEmpty()) return emptySet()

    // STEP 3 — children.student_code -> students.id. student_code is globally unique,
    // but we still scope by schoolId for defense-in-depth (cross-tenant safety).
    return StudentsTable.selectAll().where {
        (StudentsTable.schoolId eq schoolId) and
            (StudentsTable.studentCode inList studentCodes.toList())
    }.map { it[StudentsTable.id].value }
        .toSet()
}

fun Route.teacherAttendanceRouting() {
    authenticate("jwt") {
        route("/api/v1/teacher") {

            // ── GET /attendance?assignmentId=…&date=YYYY-MM-DD ────────────────
            // T-205: converged to the canonical `/attendance` path. The legacy
            // packed-`grade` handler in TeacherRoutingTasks.kt is now DELETED, so
            // there is no method+path collision and this typed plane owns it.
            get("/attendance") {
                val ctx = call.requireTeacherContext() ?: return@get
                val assignmentParam = call.request.queryParameters["assignmentId"]
                    ?: call.request.queryParameters["assignment_id"]
                val assignment = call.requireOwnedAssignment(ctx, assignmentParam) ?: return@get
                val date = call.request.queryParameters["date"]?.takeIf { it.isNotBlank() }
                    ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                    ?: LocalDate.now()

                // Roster from enrollments active for this assignment's class+section
                // (E4/E5 honored by enrollmentsFor — withdrawn/transferred excluded).
                val roster = enrollmentsFor(assignment)

                val data = dbQuery {
                    // Holiday flag (Doc 05 / E1) — a published HOLIDAY covering `date`.
                    val holiday = CalendarEventsTable.selectAll().where {
                        (CalendarEventsTable.schoolId eq ctx.schoolId) and
                            (CalendarEventsTable.isActive eq true) and
                            (CalendarEventsTable.status eq EventStatus.PUBLISHED) and
                            (CalendarEventsTable.type eq EventType.HOLIDAY) and
                            (CalendarEventsTable.startDate lessEq date) and
                            (CalendarEventsTable.endDate greaterEq date)
                    }.firstOrNull()

                    // Existing saved marks for THIS (school, date, assignment) — the
                    // edit path (E3). Keyed by student_id. Also collect last-marked
                    // audit (most recent marked_at + the marker's name).
                    val existingRows = AttendanceRecordsTable.selectAll().where {
                        (AttendanceRecordsTable.schoolId eq ctx.schoolId) and
                            (AttendanceRecordsTable.date eq date) and
                            (AttendanceRecordsTable.type eq "student") and
                            (AttendanceRecordsTable.assignmentId eq assignment.assignmentId)
                    }.toList()
                    val savedByStudent = existingRows
                        .mapNotNull { r -> r[AttendanceRecordsTable.studentId]?.let { it to r } }
                        .toMap()
                    val alreadyMarked = existingRows.isNotEmpty()

                    val lastRow = existingRows.maxByOrNull {
                        it[AttendanceRecordsTable.markedAt] ?: Instant.EPOCH
                    }
                    val lastMarkedAt = lastRow?.get(AttendanceRecordsTable.markedAt)?.toString()
                    val lastMarkedBy = lastRow?.get(AttendanceRecordsTable.markedBy)?.let { uid ->
                        AppUsersTable.selectAll().where { AppUsersTable.id eq uid }
                            .firstOrNull()?.get(AppUsersTable.fullName)
                    }

                    // Approved-leave pre-defaults (§3.5) — only applied when there's
                    // no saved mark yet (a saved value always wins on edit).
                    val onLeave = approvedLeaveStudentIds(ctx.schoolId, date)

                    val students = roster.map { s ->
                        val saved = savedByStudent[s.studentId]
                        val savedStatus = saved?.get(AttendanceRecordsTable.status)
                        val savedSource = saved?.get(AttendanceRecordsTable.source)
                        val isLeaveDefault = saved == null && s.studentId in onLeave
                        AttendanceStudentDto(
                            studentId = s.studentId.toString(),
                            name = s.fullName,
                            rollNo = s.rollNumber?.toString() ?: "",
                            status = savedStatus ?: if (isLeaveDefault) "leave" else "present",
                            source = savedSource ?: if (isLeaveDefault) SOURCE_LEAVE_AUTO else null,
                            enrollmentId = s.enrollmentId.toString(),
                        )
                    }
                    val leaveDefaults = roster
                        .filter { it.studentId in onLeave && savedByStudent[it.studentId] == null }
                        .map { it.studentId.toString() }

                    val scopeLabel = listOfNotNull(
                        "${assignment.className}-${assignment.section}".takeIf { assignment.className.isNotBlank() },
                        assignment.subject.takeIf { it.isNotBlank() },
                    ).joinToString(" · ")

                    AttendanceLoadDto(
                        assignmentId = assignment.assignmentId.toString(),
                        date = date.toString(),
                        scope = scopeLabel,
                        className = assignment.className,
                        section = assignment.section,
                        subject = assignment.subject,
                        students = students,
                        alreadyMarked = alreadyMarked,
                        lastMarkedBy = lastMarkedBy,
                        lastMarkedAt = lastMarkedAt,
                        leaveDefaults = leaveDefaults,
                        isHoliday = holiday != null,
                        holidayName = holiday?.get(CalendarEventsTable.title),
                        // Cancelled is a per-PERIOD concept (period_exceptions); the
                        // attendance plane is per-assignment+date, so we don't assert
                        // it here (the Today card already strikes cancelled periods,
                        // and never raises an unmarked obligation for them — T-107).
                        isCancelled = false,
                        backDateWindowDays = ATT_BACK_DATE_WINDOW_DAYS,
                    )
                }
                call.ok(data, message = "Attendance loaded")
            }

            // ── POST /attendance — upsert scoped marks ────────────────────────
            // T-205: converged to the canonical `/attendance` path (legacy handler deleted).
            post("/attendance") {
                val ctx = call.requireTeacherContext() ?: return@post
                val req = runCatching { call.receive<AttendanceSaveRequest>() }.getOrNull()
                if (req == null) {
                    call.fail("Invalid request body", HttpStatusCode.BadRequest, "BAD_REQUEST")
                    return@post
                }
                val assignment = call.requireOwnedAssignment(ctx, req.assignmentId) ?: return@post
                val date = runCatching { LocalDate.parse(req.date) }.getOrNull() ?: run {
                    call.fail("A valid date (YYYY-MM-DD) is required", HttpStatusCode.BadRequest, "BAD_DATE")
                    return@post
                }

                // E9: window guard. Future dates blocked; back-dating limited.
                val today = LocalDate.now()
                if (date.isAfter(today)) {
                    call.fail("Cannot mark attendance for a future date", HttpStatusCode.BadRequest, "FUTURE_DATE")
                    return@post
                }
                if (date.isBefore(today.minusDays(ATT_BACK_DATE_WINDOW_DAYS.toLong()))) {
                    call.fail(
                        "Contact admin to mark attendance older than $ATT_BACK_DATE_WINDOW_DAYS days",
                        HttpStatusCode.BadRequest,
                        "BACK_DATE_BLOCKED",
                    )
                    return@post
                }

                // Validate the marks against the typed roster (only enrolled students
                // can be marked) and the valid state space.
                val rosterById = enrollmentsFor(assignment).associateBy { it.studentId }
                val now = Instant.now()
                // Collect students newly flagged absent/late so we can alert their
                // parents after the transaction commits (preserves the legacy
                // RA-41 alert behaviour the deleted handler had). Keyed by
                // student_code, since NotifyRecipients.parentsOfStudent expects it.
                val flaggedCodes = mutableListOf<Pair<String, String>>() // (studentCode, status)
                val saved = dbQuery {
                    var count = 0
                    for (m in req.marks) {
                        val status = m.status.trim().lowercase()
                        if (status !in VALID_ATTENDANCE) continue // skip invalid silently-safe
                        val sid = runCatching { UUID.fromString(m.studentId) }.getOrNull() ?: continue
                        val enrolled = rosterById[sid] ?: continue // not in this class → ignore
                        if (status == "absent" || status == "late") {
                            flaggedCodes += enrolled.studentCode to status
                        }
                        // Upsert on (school, date, type, student, assignment).
                        val existing = AttendanceRecordsTable.selectAll().where {
                            (AttendanceRecordsTable.schoolId eq ctx.schoolId) and
                                (AttendanceRecordsTable.date eq date) and
                                (AttendanceRecordsTable.type eq "student") and
                                (AttendanceRecordsTable.studentId eq sid) and
                                (AttendanceRecordsTable.assignmentId eq assignment.assignmentId)
                        }.firstOrNull()
                        if (existing != null) {
                            AttendanceRecordsTable.update({
                                (AttendanceRecordsTable.schoolId eq ctx.schoolId) and
                                    (AttendanceRecordsTable.date eq date) and
                                    (AttendanceRecordsTable.type eq "student") and
                                    (AttendanceRecordsTable.studentId eq sid) and
                                    (AttendanceRecordsTable.assignmentId eq assignment.assignmentId)
                            }) {
                                it[AttendanceRecordsTable.status] = status
                                it[AttendanceRecordsTable.source] = SOURCE_MANUAL
                                it[AttendanceRecordsTable.markedBy] = ctx.userId
                                it[AttendanceRecordsTable.markedAt] = now
                                it[AttendanceRecordsTable.enrollmentId] = enrolled.enrollmentId
                            }
                        } else {
                            AttendanceRecordsTable.insert {
                                it[id] = UUID.randomUUID()
                                it[schoolId] = ctx.schoolId
                                it[AttendanceRecordsTable.date] = date
                                it[type] = "student"
                                it[studentId] = sid
                                it[enrollmentId] = enrolled.enrollmentId
                                it[assignmentId] = assignment.assignmentId
                                it[AttendanceRecordsTable.status] = status
                                it[source] = SOURCE_MANUAL
                                it[markedBy] = ctx.userId
                                it[markedAt] = now
                                it[createdAt] = now
                            }
                        }
                        count++
                    }
                    count
                }

                // RA-41 (preserved from the deleted legacy handler): alert each
                // affected parent when their child is absent/late. Recipients are
                // resolved per student_code within this school (multi-tenant safe).
                val scopeLabel = listOfNotNull(
                    "${assignment.className}-${assignment.section}".takeIf { assignment.className.isNotBlank() },
                    assignment.subject.takeIf { it.isNotBlank() },
                ).joinToString(" · ")
                for ((code, status) in flaggedCodes) {
                    val parents = NotifyRecipients.parentsOfStudent(ctx.schoolId, code)
                    if (parents.isNotEmpty()) {
                        val verb = if (status == "absent") "marked absent" else "marked late"
                        val context = if (scopeLabel.isNotBlank()) " in $scopeLabel" else ""
                        Notify.toUsers(
                            userIds = parents,
                            category = "attendance",
                            title = "Attendance update",
                            body = "Your child was $verb$context on ${date}.",
                            schoolId = ctx.schoolId,
                            actorId = ctx.userId,
                            deepLink = "parent/academics/attendance",
                            refType = "attendance",
                            refId = code,
                        )
                    }
                }

                call.ok(
                    AttendanceSaveResultDto(saved = saved, date = date.toString()),
                    message = "Attendance saved",
                )
            }
        }
    }
}

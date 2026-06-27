/*
 * File: TeacherStudentRouting.kt
 * Module: feature.teacher
 *
 * PHASE 5 — CLASSES, the student-profile drill-down (Doc 09 §4). The scoped
 * read-only profile a teacher reaches from a class roster row (T-505 renders it).
 *
 * Defects closed: B-PROF-1, B-PROF-2 (no scoped student profile existed),
 * F-PROF-3 (no drill-down UI — landed in T-505).
 *
 * THE AUTHORIZATION RULE (Doc 09 §4, the constitution's scope law): the server
 * verifies the caller actually TEACHES this student — at least one of the
 * teacher's owned assignments must match an active enrollment of the student
 * (class_id + section). Otherwise 403 (no cross-class snooping). This is the
 * query-level + API-level enforcement; the UI only ever links here from an owned
 * roster row (third level).
 *
 * Parent contact is privacy-gated: a CLASS TEACHER of the student's class sees
 * name+phone; a subject-only teacher sees no direct contact (Doc 09 §4.5 — "not
 * blanket-exposed"). A future per-school policy can widen this without a contract
 * change (the field is already optional/nullable).
 *
 * PATH (canonical since T-504): `/api/v1/teacher/students/{id}`. It was staged
 * under `/students-v2/{id}` until T-504 deleted the legacy looping /classes
 * handler; the whole typed classes plane now owns its canonical paths.
 *
 * Reuses the `internal` helpers in TeacherClassesRouting.kt (same package):
 * scopedAssessmentsInTxn, computeFlags, ClassFlags, StudentAttendanceWindow,
 * StudentMarkPoint — so the profile's flags can never drift from the roster's.
 *
 * DTOs mirror shared/.../teacher/domain/model/TeacherModels.kt field-for-field.
 */
package com.littlebridge.enrollplus.feature.teacher

import com.littlebridge.enrollplus.core.TeacherContext
import com.littlebridge.enrollplus.core.fail
import com.littlebridge.enrollplus.core.ok
import com.littlebridge.enrollplus.core.teacherAssignmentsFor
import com.littlebridge.enrollplus.db.AppUsersTable
import com.littlebridge.enrollplus.db.AssessmentMarksTable
import com.littlebridge.enrollplus.db.AssessmentsTable
import com.littlebridge.enrollplus.db.AttendanceRecordsTable
import com.littlebridge.enrollplus.db.ChildrenTable
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.EnrollmentsTable
import com.littlebridge.enrollplus.db.StudentsTable
import com.littlebridge.enrollplus.core.requireTeacherContext
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import java.time.LocalDate
import java.util.UUID

fun Route.teacherStudentRouting() {
    authenticate("jwt") {
        route("/api/v1/teacher") {

            // ── GET /students/{studentId} — scoped student profile (T-503) ──────
            get("/students/{studentId}") {
                val ctx = call.requireTeacherContext() ?: return@get
                val studentId = call.parameters["studentId"]
                    ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run {
                        call.fail("A valid student id is required", HttpStatusCode.BadRequest, "BAD_STUDENT_ID")
                        return@get
                    }

                // All assignments the caller owns (school_admin/admin → whole school).
                val assignments = teacherAssignmentsFor(ctx)

                val profile = dbQuery {
                    // 1) Student exists in this school?
                    val student = StudentsTable.selectAll().where {
                        (StudentsTable.id eq studentId) and (StudentsTable.schoolId eq ctx.schoolId)
                    }.singleOrNull() ?: return@dbQuery ProfileResult.NotFound

                    // 2) Active enrollments of this student.
                    val enrollments = EnrollmentsTable.selectAll().where {
                        (EnrollmentsTable.studentId eq studentId) and
                            (EnrollmentsTable.status eq "active")
                    }.toList()

                    // 3) Authorization: at least one owned assignment must match an
                    //    active enrollment (class_id + section). Admins pass via the
                    //    teacherAssignmentsFor whole-school list.
                    val matching = assignments.filter { a ->
                        a.classId != null && enrollments.any { e ->
                            e[EnrollmentsTable.classId] == a.classId &&
                                e[EnrollmentsTable.section] == a.section
                        }
                    }
                    val isPrivileged = ctx.role == "school_admin" || ctx.role == "admin"
                    if (matching.isEmpty() && !isPrivileged) {
                        return@dbQuery ProfileResult.Forbidden
                    }

                    // The enrollment we anchor display to (the matched one, else the
                    // first active — privileged path).
                    val anchorEnrollment = enrollments.firstOrNull { e ->
                        matching.any { a -> a.classId == e[EnrollmentsTable.classId] && a.section == e[EnrollmentsTable.section] }
                    } ?: enrollments.firstOrNull()

                    val className = student[StudentsTable.className]
                    val section = anchorEnrollment?.get(EnrollmentsTable.section)
                        ?: student[StudentsTable.section]
                    val roll = anchorEnrollment?.get(EnrollmentsTable.rollNumber)
                        ?: student[StudentsTable.rollNumber].toIntOrNull()

                    // 4) Attendance — across the assignments the caller can see for
                    //    this student (their own subjects). One batched query.
                    val visibleAssignmentIds = matching.map { it.assignmentId }
                    val today = LocalDate.now()
                    val monthStart = today.minusDays(30)

                    val attRows = AttendanceRecordsTable.selectAll().where {
                        (AttendanceRecordsTable.schoolId eq ctx.schoolId) and
                            (AttendanceRecordsTable.type eq "student") and
                            (AttendanceRecordsTable.studentId eq studentId) and
                            (AttendanceRecordsTable.date greaterEq monthStart)
                    }.toList()
                        .filter {
                            // privileged sees all; a subject teacher sees only marks
                            // under an assignment they own for this student.
                            isPrivileged || it[AttendanceRecordsTable.assignmentId] == null ||
                                it[AttendanceRecordsTable.assignmentId] in visibleAssignmentIds
                        }
                        .sortedByDescending { it[AttendanceRecordsTable.date] }

                    val attTotal = attRows.size
                    val attPresent = attRows.count {
                        it[AttendanceRecordsTable.status].equals("present", ignoreCase = true)
                    }
                    val attRate = if (attTotal == 0) null else attPresent.toDouble() / attTotal
                    val recentDays = attRows.take(10).map {
                        StudentAttendanceDayDto(
                            date = it[AttendanceRecordsTable.date].toString(),
                            status = it[AttendanceRecordsTable.status].lowercase(),
                        )
                    }
                    // Trend: compare the present-rate of the most-recent 5 vs the prior 5.
                    val trend = attendanceTrend(attRows.map { it[AttendanceRecordsTable.status].lowercase() })

                    // 5) Performance — published marks for the student across the
                    //    teacher's scoped assessments (their subject(s) for this class).
                    val performance = mutableListOf<StudentPerformanceDto>()
                    // markPoint paired with its assessment exam_date (nullable) so we
                    // can sort newest-first robustly (not by fragile value equality).
                    val datedMarks = mutableListOf<Pair<LocalDate?, StudentMarkPoint>>()
                    val scopeAssignments = if (isPrivileged && matching.isEmpty()) {
                        // privileged with no direct assignment: use any assignment in the
                        // student's class to scope display (still their school).
                        assignments.filter { a -> a.classId != null && enrollments.any { it[EnrollmentsTable.classId] == a.classId } }
                    } else matching

                    for (a in scopeAssignments) {
                        val assessments = scopedAssessmentsInTxn(ctx, a, publishedOnly = true)
                        if (assessments.isEmpty()) continue
                        val byId = assessments.associateBy { it[AssessmentsTable.id].value }
                        val marks = AssessmentMarksTable.selectAll().where {
                            (AssessmentMarksTable.assessmentId inList byId.keys.toList()) and
                                (AssessmentMarksTable.studentRef eq studentId)
                        }.toList()
                        for (m in marks) {
                            val asg = byId[m[AssessmentMarksTable.assessmentId]] ?: continue
                            val isAbsent = m[AssessmentMarksTable.isAbsent]
                            val marksVal = m[AssessmentMarksTable.marks]
                            performance += StudentPerformanceDto(
                                assessmentId = asg[AssessmentsTable.id].value.toString(),
                                assessmentName = asg[AssessmentsTable.name],
                                subject = asg[AssessmentsTable.subject],
                                marks = if (isAbsent) null else marksVal,
                                max = asg[AssessmentsTable.maxMarks],
                                isAbsent = isAbsent,
                                date = asg[AssessmentsTable.examDate]?.toString(),
                            )
                            if (!isAbsent && marksVal != null) {
                                datedMarks += asg[AssessmentsTable.examDate] to StudentMarkPoint(
                                    marks = marksVal,
                                    max = asg[AssessmentsTable.maxMarks],
                                    pass = asg[AssessmentsTable.passMarks],
                                )
                            }
                        }
                    }
                    performance.sortByDescending { it.date ?: "" }
                    // markPoints newest-first (by exam_date) for flag computation
                    val orderedMarks = datedMarks
                        .sortedByDescending { it.first ?: LocalDate.MIN }
                        .map { it.second }

                    // 6) Flags — same computeFlags as the roster (no drift).
                    val window = if (attTotal == 0) null else StudentAttendanceWindow(
                        monthPresent = attPresent,
                        monthTotal = attTotal,
                        recentStatuses = attRows.map { it[AttendanceRecordsTable.status].lowercase() },
                    )
                    val flags = computeFlags(window, orderedMarks)

                    // 7) Parent contact — gated to a CLASS TEACHER of this class
                    //    (privacy, Doc 09 §4.5). Resolve students.student_code →
                    //    children.student_code → children.parent_id → app_users.
                    val isClassTeacherOfStudent = matching.any { it.isClassTeacher }
                    val parentContact = if (isClassTeacherOfStudent || isPrivileged) {
                        resolveParentContact(student[StudentsTable.studentCode], ctx.schoolId)
                    } else null

                    ProfileResult.Ok(
                        StudentProfileData(
                            studentId = studentId.toString(),
                            name = student[StudentsTable.fullName],
                            roll = roll,
                            photoUrl = student[StudentsTable.profilePhotoUrl],
                            className = className,
                            section = section,
                            attendance = StudentAttendanceDto(
                                rate = attRate,
                                recent = recentDays,
                                trend = trend,
                            ),
                            performance = performance,
                            flags = flags,
                            parentContact = parentContact,
                        )
                    )
                }

                when (profile) {
                    ProfileResult.NotFound ->
                        call.fail("Student not found in your school", HttpStatusCode.NotFound, "STUDENT_NOT_FOUND")
                    ProfileResult.Forbidden ->
                        call.fail("You do not teach this student", HttpStatusCode.Forbidden, "NOT_YOUR_STUDENT")
                    is ProfileResult.Ok ->
                        call.ok(profile.data, message = "Student profile loaded")
                }
            }
        }
    }
}

private sealed interface ProfileResult {
    data object NotFound : ProfileResult
    data object Forbidden : ProfileResult
    data class Ok(val data: StudentProfileData) : ProfileResult
}

/**
 * Attendance trend from a newest-first status list: present-rate of the most
 * recent 5 sessions vs the prior 5. >+10pp improving, <-10pp declining, else flat;
 * "none" when there isn't enough history.
 */
private fun attendanceTrend(statusesNewestFirst: List<String>): String {
    if (statusesNewestFirst.size < 4) return if (statusesNewestFirst.isEmpty()) "none" else "flat"
    val recent = statusesNewestFirst.take(5)
    val prior = statusesNewestFirst.drop(5).take(5)
    if (prior.isEmpty()) return "flat"
    fun rate(l: List<String>) = l.count { it == "present" }.toDouble() / l.size
    val delta = rate(recent) - rate(prior)
    return when {
        delta > 0.10 -> "improving"
        delta < -0.10 -> "declining"
        else -> "flat"
    }
}

/**
 * Resolve a student's parent contact via the link chain
 * students.student_code → children.student_code → children.parent_id → app_users.
 * Returns null when no linked parent exists (honest empty, not fabricated).
 */
private fun resolveParentContact(studentCode: String, schoolId: UUID): ParentContactDto? {
    // Match by student_code (unique to a student). children.school_id is optional/
    // nullable, so prefer a same-school link but fall back to any code match rather
    // than miss a legitimately-linked parent whose row predates the school backfill.
    val candidates = ChildrenTable.selectAll().where {
        ChildrenTable.studentCode eq studentCode
    }.toList()
    if (candidates.isEmpty()) return null
    val childRow = candidates.firstOrNull { it[ChildrenTable.schoolId] == schoolId }
        ?: candidates.first()
    val parentId = childRow[ChildrenTable.parentId]
    val parent = AppUsersTable.selectAll().where { AppUsersTable.id eq parentId }.singleOrNull()
        ?: return null
    val phone = parent[AppUsersTable.phone]?.takeIf { it.isNotBlank() }
    val name = parent[AppUsersTable.fullName].takeIf { it.isNotBlank() }
    if (name == null && phone == null) return null
    return ParentContactDto(name = name, phone = phone)
}

// ─────────────────────────────────────────────────────────────────────────────
// T-503 — scoped student-profile DTOs (mirror shared StudentProfile* field-for-
// field; :server does NOT depend on :shared, so these are the wire truth).
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class StudentAttendanceDayDto(
    val date: String,
    val status: String, // present | absent | late | leave
)

@Serializable
data class StudentAttendanceDto(
    val rate: Double? = null,            // 0..1, null = no data
    val recent: List<StudentAttendanceDayDto> = emptyList(),
    val trend: String = "flat",          // improving | declining | flat | none
)

@Serializable
data class StudentPerformanceDto(
    @SerialName("assessment_id") val assessmentId: String,
    @SerialName("assessment_name") val assessmentName: String,
    val subject: String,
    val marks: Double? = null,           // null = absent (is_absent) or not entered
    val max: Int,
    @SerialName("is_absent") val isAbsent: Boolean = false,
    val date: String? = null,
)

@Serializable
data class ParentContactDto(
    val name: String? = null,
    val phone: String? = null,
)

@Serializable
data class StudentProfileData(
    @SerialName("student_id") val studentId: String,
    val name: String,
    val roll: Int? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("class_name") val className: String,
    val section: String,
    val attendance: StudentAttendanceDto,
    val performance: List<StudentPerformanceDto> = emptyList(),
    val flags: List<String> = emptyList(),
    @SerialName("parent_contact") val parentContact: ParentContactDto? = null,
)

/*
 * File: SchoolStudentsRouting.kt
 * Module: feature.school
 *
 * RA-45: admin student roster + student profile + teacher profile detail.
 *
 * ROOT: there was no `GET /school/students` (admin could never see real
 * students), no admin write surface for students, and no detail endpoint for a
 * single student or a single teacher. The People tab showed only the teacher
 * roster; the student roster and both profile screens were entirely missing.
 *
 * Endpoints (JWT + school-scoped; school_id resolved from JWT, never the body):
 *   GET    /api/v1/school/students                list active students in caller's school
 *   POST   /api/v1/school/students                add a student (school-admin)
 *   DELETE /api/v1/school/students/{id}           soft-delete a student (school-admin)
 *   GET    /api/v1/school/students/{id}           full student profile (attendance/marks/leave/fees)
 *   GET    /api/v1/school/teachers/{id}           teacher profile detail (assignments/coverage)
 *
 * Every read/write is constrained to ctx.schoolId, so an admin can only ever
 * see/mutate students + teachers in their OWN school (IDOR-safe). Soft-delete
 * (isActive=false) mirrors the teacher provisioning convention.
 */
package com.littlebridge.vidyaprayag.feature.school

import com.littlebridge.vidyaprayag.core.created
import com.littlebridge.vidyaprayag.core.fail
import com.littlebridge.vidyaprayag.core.ok
import com.littlebridge.vidyaprayag.core.okMessage
import com.littlebridge.vidyaprayag.core.requireSchoolAdmin
import com.littlebridge.vidyaprayag.core.requireSchoolContext
import com.littlebridge.vidyaprayag.db.AppUsersTable
import com.littlebridge.vidyaprayag.db.AssessmentMarksTable
import com.littlebridge.vidyaprayag.db.AssessmentsTable
import com.littlebridge.vidyaprayag.db.AttendanceRecordsTable
import com.littlebridge.vidyaprayag.db.ChildrenTable
import com.littlebridge.vidyaprayag.db.DatabaseFactory.dbQuery
import com.littlebridge.vidyaprayag.db.FeeRecordsTable
import com.littlebridge.vidyaprayag.db.LeaveRequestsTable
import com.littlebridge.vidyaprayag.db.StudentsTable
import com.littlebridge.vidyaprayag.db.TeacherSubjectAssignmentsTable
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

// ───────────────────────────── DTOs ─────────────────────────────

@Serializable
data class StudentDto(
    val id: String,
    @SerialName("student_code") val studentCode: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("class_name") val className: String,
    val section: String,
    @SerialName("roll_number") val rollNumber: String,
    @SerialName("profile_photo_url") val profilePhotoUrl: String? = null
)

@Serializable
data class StudentListResponse(val students: List<StudentDto>)

@Serializable
data class CreateStudentRequest(
    @SerialName("full_name") val fullName: String,
    @SerialName("class_name") val className: String,
    val section: String? = null,
    @SerialName("roll_number") val rollNumber: String,
    @SerialName("student_code") val studentCode: String? = null  // optional; auto-generated when blank
)

@Serializable
data class AttendanceDayDto(val date: String, val status: String)

@Serializable
data class StudentMarkDto(
    val subject: String,
    @SerialName("assessment") val assessmentName: String,
    val marks: Double? = null,
    @SerialName("max_marks") val maxMarks: Int,
    @SerialName("exam_date") val examDate: String? = null
)

@Serializable
data class StudentLeaveDto(
    @SerialName("date_from") val dateFrom: String,
    @SerialName("date_to") val dateTo: String,
    val reason: String,
    val status: String
)

@Serializable
data class StudentFeeDto(
    val title: String,
    val amount: Double,
    val currency: String,
    val status: String,
    @SerialName("due_date") val dueDate: String? = null
)

@Serializable
data class StudentProfileDto(
    val student: StudentDto,
    @SerialName("present_days") val presentDays: Int,
    @SerialName("absent_days") val absentDays: Int,
    @SerialName("late_days") val lateDays: Int,
    @SerialName("attendance_rate") val attendanceRate: Int,
    @SerialName("recent_attendance") val recentAttendance: List<AttendanceDayDto>,
    val marks: List<StudentMarkDto>,
    val leave: List<StudentLeaveDto>,
    val fees: List<StudentFeeDto>
)

@Serializable
data class TeacherProfileAssignmentDto(
    @SerialName("class_name") val className: String,
    val section: String,
    val subject: String
)

@Serializable
data class TeacherProfileDto(
    val id: String,
    val name: String,
    val email: String? = null,
    val phone: String? = null,
    val role: String,
    val assignments: List<TeacherProfileAssignmentDto>,
    @SerialName("class_count") val classCount: Int,
    @SerialName("subject_count") val subjectCount: Int
)

// ─────────────────────────── helpers ────────────────────────────

private fun studentRowToDto(row: org.jetbrains.exposed.sql.ResultRow): StudentDto =
    StudentDto(
        id = row[StudentsTable.id].value.toString(),
        studentCode = row[StudentsTable.studentCode],
        fullName = row[StudentsTable.fullName],
        className = row[StudentsTable.className],
        section = row[StudentsTable.section],
        rollNumber = row[StudentsTable.rollNumber],
        profilePhotoUrl = row[StudentsTable.profilePhotoUrl]
    )

// ─────────────────────────── routing ────────────────────────────

fun Route.schoolStudentsRouting() {
    authenticate("jwt") {

        route("/api/v1/school/students") {

            // ---- roster: active students in the caller's school ----
            get {
                val ctx = call.requireSchoolContext() ?: return@get
                val students = dbQuery {
                    StudentsTable.selectAll()
                        .where { (StudentsTable.schoolId eq ctx.schoolId) and (StudentsTable.isActive eq true) }
                        .orderBy(StudentsTable.className to SortOrder.ASC, StudentsTable.rollNumber to SortOrder.ASC)
                        .map(::studentRowToDto)
                }
                call.ok(StudentListResponse(students), message = "Students fetched")
            }

            // ---- add a student (school-admin) ----
            post {
                val ctx = call.requireSchoolAdmin() ?: return@post
                val req = runCatching { call.receive<CreateStudentRequest>() }.getOrNull()
                    ?: run { call.fail("Invalid body"); return@post }
                if (req.fullName.isBlank() || req.className.isBlank() || req.rollNumber.isBlank()) {
                    call.fail("Name, class and roll number are required.")
                    return@post
                }
                // Auto-generate a unique student_code when not supplied.
                val code = req.studentCode?.takeIf { it.isNotBlank() }
                    ?: "S-${System.currentTimeMillis().toString(36).uppercase()}"

                val dto = dbQuery {
                    val clash = StudentsTable.selectAll()
                        .where { StudentsTable.studentCode eq code }
                        .firstOrNull()
                    if (clash != null) return@dbQuery null  // duplicate code

                    val newId = StudentsTable.insert {
                        it[schoolId] = ctx.schoolId
                        it[studentCode] = code
                        it[fullName] = req.fullName.trim()
                        it[className] = req.className.trim()
                        it[section] = req.section?.takeIf { s -> s.isNotBlank() }?.trim() ?: "A"
                        it[rollNumber] = req.rollNumber.trim()
                        it[isActive] = true
                        it[createdAt] = Instant.now()
                    } get StudentsTable.id

                    StudentsTable.selectAll().where { StudentsTable.id eq newId }.first().let(::studentRowToDto)
                }
                if (dto == null) {
                    call.fail("A student with that code already exists.", HttpStatusCode.Conflict, "STUDENT_CODE_TAKEN")
                    return@post
                }
                call.created(dto, message = "Student added")
            }

            // ---- soft-delete a student (school-admin) ----
            delete("{id}") {
                val ctx = call.requireSchoolAdmin() ?: return@delete
                val id = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid student id"); return@delete }
                val n = dbQuery {
                    StudentsTable.update({
                        (StudentsTable.id eq id) and (StudentsTable.schoolId eq ctx.schoolId)
                    }) { it[isActive] = false }
                }
                if (n == 0) {
                    call.fail("Student not found in your school", HttpStatusCode.NotFound, "STUDENT_NOT_FOUND")
                    return@delete
                }
                call.okMessage("Student removed")
            }

            // ---- full student profile (attendance/marks/leave/fees) ----
            get("{id}") {
                val ctx = call.requireSchoolContext() ?: return@get
                val id = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid student id"); return@get }

                val profile = dbQuery {
                    val row = StudentsTable.selectAll()
                        .where { (StudentsTable.id eq id) and (StudentsTable.schoolId eq ctx.schoolId) }
                        .firstOrNull() ?: return@dbQuery null
                    val student = studentRowToDto(row)
                    val code = student.studentCode

                    // attendance — person_id = student_code, type = student, same school
                    val attRows = AttendanceRecordsTable.selectAll().where {
                        (AttendanceRecordsTable.schoolId eq ctx.schoolId) and
                            (AttendanceRecordsTable.type eq "student") and
                            (AttendanceRecordsTable.personId eq code)
                    }.orderBy(AttendanceRecordsTable.date, SortOrder.DESC).map {
                        AttendanceDayDto(it[AttendanceRecordsTable.date], it[AttendanceRecordsTable.status].lowercase())
                    }
                    val present = attRows.count { it.status == "present" }
                    val absent = attRows.count { it.status == "absent" }
                    val late = attRows.count { it.status == "late" }
                    val total = attRows.size
                    val rate = if (total > 0) (((present + late) * 100) / total) else 0

                    // marks — join assessments (same school) ← assessment_marks (student_code)
                    val marks = AssessmentsTable.selectAll()
                        .where { (AssessmentsTable.schoolId eq ctx.schoolId) and (AssessmentsTable.isActive eq true) }
                        .orderBy(AssessmentsTable.examDate, SortOrder.DESC)
                        .mapNotNull { a ->
                            val aId = a[AssessmentsTable.id].value
                            val mark = AssessmentMarksTable.selectAll().where {
                                (AssessmentMarksTable.assessmentId eq aId) and
                                    (AssessmentMarksTable.studentId eq code)
                            }.firstOrNull()?.get(AssessmentMarksTable.marks)
                            if (mark == null) null else StudentMarkDto(
                                subject = a[AssessmentsTable.subject],
                                assessmentName = a[AssessmentsTable.name],
                                marks = mark,
                                maxMarks = a[AssessmentsTable.maxMarks],
                                examDate = a[AssessmentsTable.examDate]
                            )
                        }

                    // leave — children row links to this student_code; surface its leave history
                    val childId = ChildrenTable.selectAll()
                        .where { (ChildrenTable.schoolId eq ctx.schoolId) and (ChildrenTable.studentCode eq code) }
                        .firstOrNull()?.get(ChildrenTable.id)?.value
                    val leave = if (childId == null) emptyList() else LeaveRequestsTable.selectAll().where {
                        (LeaveRequestsTable.schoolId eq ctx.schoolId) and (LeaveRequestsTable.childId eq childId)
                    }.orderBy(LeaveRequestsTable.dateFrom, SortOrder.DESC).map {
                        StudentLeaveDto(
                            dateFrom = it[LeaveRequestsTable.dateFrom],
                            dateTo = it[LeaveRequestsTable.dateTo],
                            reason = it[LeaveRequestsTable.reason],
                            status = it[LeaveRequestsTable.status]
                        )
                    }

                    // fees — fee_records key off child_id (school-scoped)
                    val fees = if (childId == null) emptyList() else FeeRecordsTable.selectAll().where {
                        (FeeRecordsTable.schoolId eq ctx.schoolId) and (FeeRecordsTable.childId eq childId)
                    }.orderBy(FeeRecordsTable.createdAt, SortOrder.DESC).map {
                        StudentFeeDto(
                            title = it[FeeRecordsTable.title],
                            amount = it[FeeRecordsTable.amount],
                            currency = it[FeeRecordsTable.currency],
                            status = it[FeeRecordsTable.status],
                            dueDate = it[FeeRecordsTable.dueDate]
                        )
                    }

                    StudentProfileDto(
                        student = student,
                        presentDays = present,
                        absentDays = absent,
                        lateDays = late,
                        attendanceRate = rate,
                        recentAttendance = attRows.take(30),
                        marks = marks,
                        leave = leave,
                        fees = fees
                    )
                }
                if (profile == null) {
                    call.fail("Student not found in your school", HttpStatusCode.NotFound, "STUDENT_NOT_FOUND")
                    return@get
                }
                call.ok(profile, message = "Student profile fetched")
            }
        }

        // ---- teacher profile detail (assignments + coverage) ----
        // Lives under the teachers route prefix; the roster GET/POST/DELETE are
        // in TeacherProvisioningRouting, this adds the per-teacher detail (RA-45).
        get("/api/v1/school/teachers/{id}") {
            val ctx = call.requireSchoolContext() ?: return@get
            val id = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                ?: run { call.fail("Invalid teacher id"); return@get }

            val profile = dbQuery {
                val row = AppUsersTable.selectAll().where {
                    (AppUsersTable.id eq id) and
                        (AppUsersTable.schoolId eq ctx.schoolId) and
                        (AppUsersTable.role eq "teacher")
                }.firstOrNull() ?: return@dbQuery null

                val assignments = TeacherSubjectAssignmentsTable.selectAll().where {
                    (TeacherSubjectAssignmentsTable.schoolId eq ctx.schoolId) and
                        (TeacherSubjectAssignmentsTable.teacherId eq id) and
                        (TeacherSubjectAssignmentsTable.isActive eq true)
                }.map {
                    TeacherProfileAssignmentDto(
                        className = it[TeacherSubjectAssignmentsTable.className],
                        section = it[TeacherSubjectAssignmentsTable.section],
                        subject = it[TeacherSubjectAssignmentsTable.subject]
                    )
                }
                TeacherProfileDto(
                    id = row[AppUsersTable.id].value.toString(),
                    name = row[AppUsersTable.fullName],
                    email = row[AppUsersTable.email],
                    phone = row[AppUsersTable.phone],
                    role = row[AppUsersTable.role],
                    assignments = assignments,
                    classCount = assignments.map { it.className to it.section }.distinct().size,
                    subjectCount = assignments.map { it.subject }.distinct().size
                )
            }
            if (profile == null) {
                call.fail("Teacher not found in your school", HttpStatusCode.NotFound, "TEACHER_NOT_FOUND")
                return@get
            }
            call.ok(profile, message = "Teacher profile fetched")
        }
    }
}

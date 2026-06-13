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
 *   POST   /api/v1/school/students/import         bulk import (JSON array OR CSV text, school-admin)
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
import com.littlebridge.vidyaprayag.db.ExamResultsTable
import com.littlebridge.vidyaprayag.db.FeeRecordsTable
import com.littlebridge.vidyaprayag.db.HomeworkSubmissionsTable
import com.littlebridge.vidyaprayag.db.LeaveRequestsTable
import com.littlebridge.vidyaprayag.db.ParentChildLinksTable
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
import org.jetbrains.exposed.sql.deleteWhere
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
data class BulkImportStudentsRequest(
    val students: List<CreateStudentRequest>? = null,
    val csv: String? = null
)

@Serializable
data class BulkImportRowResult(
    val row: Int,
    val success: Boolean,
    @SerialName("student_code") val studentCode: String? = null,
    val error: String? = null
)

@Serializable
data class BulkImportStudentsResponse(
    val total: Int,
    val inserted: Int,
    val failed: Int,
    val results: List<BulkImportRowResult>
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

/**
 * Parse CSV text into CreateStudentRequest rows.
 *
 * Expected header (case-insensitive, order-independent; extra columns ignored):
 *   full_name, class_name, roll_number, section, student_code
 * `section` and `student_code` are optional. Common header aliases
 * (name, class, roll, roll_no) are accepted so a teacher-exported sheet
 * doesn't need exact column names. Quoted fields and embedded commas are
 * handled by a small RFC-4180-style splitter.
 */
private fun parseStudentCsv(csv: String): List<CreateStudentRequest> {
    val lines = csv.split('\n').map { it.trimEnd('\r') }.filter { it.isNotBlank() }
    if (lines.isEmpty()) return emptyList()

    fun splitCsvLine(line: String): List<String> {
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> { sb.append('"'); i++ }
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> { out.add(sb.toString()); sb.setLength(0) }
                else -> sb.append(c)
            }
            i++
        }
        out.add(sb.toString())
        return out.map { it.trim() }
    }

    fun norm(s: String) = s.trim().lowercase().replace(" ", "_")
    val header = splitCsvLine(lines.first()).map(::norm)

    fun indexOfAny(vararg names: String): Int =
        names.firstNotNullOfOrNull { n -> header.indexOf(n).takeIf { it >= 0 } } ?: -1

    val iName = indexOfAny("full_name", "name", "student_name")
    val iClass = indexOfAny("class_name", "class", "grade")
    val iRoll = indexOfAny("roll_number", "roll", "roll_no", "rollno")
    val iSection = indexOfAny("section", "sec")
    val iCode = indexOfAny("student_code", "code", "admission_no", "admission_number")

    fun cell(cols: List<String>, idx: Int): String? =
        if (idx in cols.indices) cols[idx].takeIf { it.isNotBlank() } else null

    return lines.drop(1).mapNotNull { line ->
        val cols = splitCsvLine(line)
        val name = cell(cols, iName)
        val klass = cell(cols, iClass)
        val roll = cell(cols, iRoll)
        // Skip completely empty rows; keep partial rows so the endpoint can
        // report a meaningful per-row validation error.
        if (name == null && klass == null && roll == null) return@mapNotNull null
        CreateStudentRequest(
            fullName = name ?: "",
            className = klass ?: "",
            section = cell(cols, iSection),
            rollNumber = roll ?: "",
            studentCode = cell(cols, iCode)
        )
    }
}

// ─────────────────────────── routing ────────────────────────────

fun Route.schoolStudentsRouting() {
    authenticate("jwt") {

        route("/api/v1/school/students") {

            // ---- roster: active students in the caller's school ----
            // RA-S17: optional `q` (name/roll/code search) and `class` filter,
            // applied server-side in the school-scoped query. Both are case-
            // insensitive substring matches done in-memory after the scoped
            // fetch so the SQL stays Postgres-portable (no ILIKE/lower() drift).
            get {
                val ctx = call.requireSchoolContext() ?: return@get
                val q = call.request.queryParameters["q"]?.trim()?.takeIf { it.isNotBlank() }?.lowercase()
                val classFilter = call.request.queryParameters["class"]?.trim()?.takeIf { it.isNotBlank() }
                val students = dbQuery {
                    StudentsTable.selectAll()
                        .where { (StudentsTable.schoolId eq ctx.schoolId) and (StudentsTable.isActive eq true) }
                        .orderBy(StudentsTable.className to SortOrder.ASC, StudentsTable.rollNumber to SortOrder.ASC)
                        .map(::studentRowToDto)
                }.filter { s ->
                    (classFilter == null || s.className.equals(classFilter, ignoreCase = true)) &&
                        (q == null ||
                            s.fullName.lowercase().contains(q) ||
                            s.rollNumber.lowercase().contains(q) ||
                            s.studentCode.lowercase().contains(q))
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

            // ---- bulk import students (school-admin) ----
            // Accepts EITHER:
            //   a) a JSON array of student objects     -> { "students": [ {...}, {...} ] }
            //   b) raw CSV text                        -> { "csv": "full_name,class_name,roll_number,section,student_code\n..." }
            // Both manual multi-add and CSV upload from the People → Students tab
            // funnel here. Each row is validated independently; the response
            // reports per-row success/failure so a partial CSV still imports the
            // good rows instead of failing the whole batch.
            post("/import") {
                val ctx = call.requireSchoolAdmin() ?: return@post
                val req = runCatching { call.receive<BulkImportStudentsRequest>() }.getOrNull()
                    ?: run { call.fail("Invalid body"); return@post }

                val rows: List<CreateStudentRequest> = when {
                    !req.students.isNullOrEmpty() -> req.students
                    !req.csv.isNullOrBlank() -> parseStudentCsv(req.csv)
                    else -> emptyList()
                }
                if (rows.isEmpty()) {
                    call.fail("No rows to import. Provide `students` array or `csv` text.")
                    return@post
                }

                val results = mutableListOf<BulkImportRowResult>()
                var inserted = 0
                dbQuery {
                    rows.forEachIndexed { index, r ->
                        val rowNo = index + 1
                        if (r.fullName.isBlank() || r.className.isBlank() || r.rollNumber.isBlank()) {
                            results += BulkImportRowResult(rowNo, false, null, "Name, class and roll number are required.")
                            return@forEachIndexed
                        }
                        val code = r.studentCode?.takeIf { it.isNotBlank() }
                            ?: "S-${System.currentTimeMillis().toString(36).uppercase()}-$rowNo"

                        val clash = StudentsTable.selectAll()
                            .where { StudentsTable.studentCode eq code }
                            .firstOrNull()
                        if (clash != null) {
                            results += BulkImportRowResult(rowNo, false, code, "Student code already exists.")
                            return@forEachIndexed
                        }
                        StudentsTable.insert {
                            it[schoolId] = ctx.schoolId
                            it[studentCode] = code
                            it[fullName] = r.fullName.trim()
                            it[className] = r.className.trim()
                            it[section] = r.section?.takeIf { s -> s.isNotBlank() }?.trim() ?: "A"
                            it[rollNumber] = r.rollNumber.trim()
                            it[isActive] = true
                            it[createdAt] = Instant.now()
                        }
                        inserted++
                        results += BulkImportRowResult(rowNo, true, code, null)
                    }
                }
                call.ok(
                    BulkImportStudentsResponse(
                        total = rows.size,
                        inserted = inserted,
                        failed = rows.size - inserted,
                        results = results
                    ),
                    message = "Imported $inserted of ${rows.size} students"
                )
            }

            // ---- HARD-delete a student (school-admin) ----
            // FIX (admin remove leaves Supabase rows behind): this used to flip
            // is_active=false, so "removed" students stayed in the database
            // forever and reappeared in any query that forgot the isActive
            // filter. Admin removal is now a real DELETE. Because the academic
            // tables join on the TEXT student_code (no FK → the DB cannot
            // cascade for us), we explicitly purge the dependents in the same
            // transaction: attendance, exam results, assessment marks, homework
            // submissions and any pending parent link requests. Parent-side
            // `children` rows are deactivated (not deleted — they belong to the
            // parent's account history).
            delete("{id}") {
                val ctx = call.requireSchoolAdmin() ?: return@delete
                val id = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid student id"); return@delete }
                val removed = dbQuery {
                    val row = StudentsTable.selectAll()
                        .where { (StudentsTable.id eq id) and (StudentsTable.schoolId eq ctx.schoolId) }
                        .firstOrNull() ?: return@dbQuery false
                    val code = row[StudentsTable.studentCode]   // globally unique

                    AttendanceRecordsTable.deleteWhere {
                        (AttendanceRecordsTable.schoolId eq ctx.schoolId) and
                            (AttendanceRecordsTable.type eq "student") and
                            (AttendanceRecordsTable.personId eq code)
                    }
                    ExamResultsTable.deleteWhere {
                        (ExamResultsTable.schoolId eq ctx.schoolId) and (ExamResultsTable.studentId eq code)
                    }
                    AssessmentMarksTable.deleteWhere { AssessmentMarksTable.studentId eq code }
                    HomeworkSubmissionsTable.deleteWhere { HomeworkSubmissionsTable.studentId eq code }
                    ParentChildLinksTable.deleteWhere {
                        (ParentChildLinksTable.schoolId eq ctx.schoolId) and
                            (ParentChildLinksTable.studentCode eq code)
                    }
                    // Unlink (deactivate) parent-side children rows pointing at this student.
                    ChildrenTable.update({
                        (ChildrenTable.schoolId eq ctx.schoolId) and (ChildrenTable.studentCode eq code)
                    }) {
                        it[isActive] = false
                        it[updatedAt] = Instant.now()
                    }

                    StudentsTable.deleteWhere {
                        (StudentsTable.id eq id) and (StudentsTable.schoolId eq ctx.schoolId)
                    }
                    true
                }
                if (!removed) {
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

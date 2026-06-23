/*
 * File: TeacherRouting.kt
 * Module: feature.teacher
 *
 * The teacher vertical (master rebuild doc Step 7 / gap G1). Implements the
 * exact contract the KMP client already speaks via
 *   shared/.../feature/teacher/data/remote/TeacherApi.kt
 *   shared/.../feature/teacher/domain/model/TeacherModels.kt
 *
 * Routes (all JWT-guarded + scoped via core/TeacherAccess):
 *   GET   /api/v1/teacher/home
 *   GET   /api/v1/teacher/classes
 *   GET   /api/v1/teacher/profile
 *   GET   /api/v1/teacher/attendance?class_id=&date=
 *   POST  /api/v1/teacher/attendance
 *   GET   /api/v1/teacher/marks?class_id=&exam_id=
 *   POST  /api/v1/teacher/marks
 *   GET   /api/v1/teacher/syllabus?class_id=&subject=
 *   PATCH /api/v1/teacher/syllabus
 *   GET   /api/v1/teacher/homework
 *   POST  /api/v1/teacher/homework
 *
 * DTOs are defined server-side (the :server module does NOT depend on :shared)
 * with @SerialName matching the client field-for-field. Read responses use the
 * { success, data } envelope the client decodes; writes use the canonical
 * { success, message } envelope via call.ok / call.created / call.okMessage.
 *
 * Honesty rule (master doc): when a school hasn't entered data (e.g. no
 * timetable, no syllabus units) we return honest empty lists — never fabricated
 * rows.
 *
 * This file holds the READ surface (home / classes / profile). Attendance,
 * marks, syllabus and homework live in TeacherRoutingTasks.kt.
 */
package com.littlebridge.vidyaprayag.feature.teacher

import com.littlebridge.vidyaprayag.core.ClassNaming
import com.littlebridge.vidyaprayag.core.TeacherContext
import com.littlebridge.vidyaprayag.core.ok
import com.littlebridge.vidyaprayag.core.requireTeacherContext
import com.littlebridge.vidyaprayag.core.teacherAssignmentsFor
import com.littlebridge.vidyaprayag.db.AppUsersTable
import com.littlebridge.vidyaprayag.db.AssessmentMarksTable
import com.littlebridge.vidyaprayag.db.AssessmentsTable
import com.littlebridge.vidyaprayag.db.AttendanceRecordsTable
import com.littlebridge.vidyaprayag.db.DatabaseFactory.dbQuery
import com.littlebridge.vidyaprayag.db.HomeworkTable
import com.littlebridge.vidyaprayag.db.SchoolsTable
import com.littlebridge.vidyaprayag.db.StudentsTable
import com.littlebridge.vidyaprayag.db.SyllabusUnitsTable
import com.littlebridge.vidyaprayag.db.TeacherPeriodsTable
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
import java.time.format.DateTimeFormatter

// ─────────────────────────────────────────────────────────────────────────────
// Server-side DTOs — field names mirror the client (TeacherModels.kt) exactly.
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class TeacherPeriodDto(
    val id: String,
    val time: String,
    @SerialName("class_name") val className: String,
    val subject: String,
    val room: String = "",
    @SerialName("is_current") val isCurrent: Boolean = false,
    val status: String = "upcoming",
)

@Serializable
data class TeacherTaskDto(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val type: String,
    @SerialName("class_name") val className: String = "",
    @SerialName("is_done") val isDone: Boolean = false,
)

@Serializable
data class TeacherHomeData(
    @SerialName("teacher_name") val teacherName: String,
    @SerialName("school_name") val schoolName: String,
    @SerialName("classes_today") val classesToday: Int,
    @SerialName("pending_attendance") val pendingAttendance: Int,
    @SerialName("pending_marks") val pendingMarks: Int,
    @SerialName("homework_due") val homeworkDue: Int,
    @SerialName("today_periods") val todayPeriods: List<TeacherPeriodDto> = emptyList(),
    val tasks: List<TeacherTaskDto> = emptyList(),
)

// T-504: TeacherClassDto / TeacherClassesData (the legacy /classes list shape) were
// DELETED with the legacy handler. The canonical class list is now served by
// TeacherClassSummaryDto / TeacherClassesV2Data in TeacherClassesRouting.

@Serializable
data class TeacherProfileData(
    val id: String,
    val name: String,
    val username: String,
    @SerialName("school_name") val schoolName: String,
    val subjects: List<String> = emptyList(),
    val classes: List<String> = emptyList(),
    @SerialName("photo_url") val photoUrl: String? = null,
    val email: String = "",
    val phone: String = "",
)

private val HHMM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** Today's date in YYYY-MM-DD, matching the varchar(12) date columns. */
internal fun todayIso(): String = LocalDate.now().toString()

/**
 * Count of distinct active students in a class+section, used for student_count /
 * homework totalCount / marks rosters. Reads the read-only students mirror.
 */
internal suspend fun studentCountFor(schoolId: java.util.UUID, className: String, section: String): Int = dbQuery {
    // ROOT FIX (ISSUE 1): match the roster to the teacher's assignment via the
    // ClassNaming key, not raw eq, so "Grade 4"/"4"/"Class IV" + "A"/"a"/"" all
    // resolve to the same class+section.
    StudentsTable.selectAll().where {
        (StudentsTable.schoolId eq schoolId) and (StudentsTable.isActive eq true)
    }.count {
        ClassNaming.sameClassSection(
            it[StudentsTable.className], it[StudentsTable.section], className, section
        )
    }
}

/**
 * ROOT FIX (ISSUE 1): the shared, normalised student-roster query for teacher
 * task screens (attendance, marks). Returns active students in [schoolId] whose
 * (class, section) matches [className]/[section] under [ClassNaming], sorted by
 * roll number. Replaces the per-screen raw `eq` filters that silently produced
 * empty rosters when the stored class label differed only by case/format.
 *
 * Runs inside the caller's transaction — call from `dbQuery { ... }`.
 */
internal fun studentsForAssignment(
    schoolId: java.util.UUID,
    className: String,
    section: String,
    includeInactive: Boolean = false,
): List<org.jetbrains.exposed.sql.ResultRow> =
    StudentsTable.selectAll().where {
        if (includeInactive) StudentsTable.schoolId eq schoolId
        else (StudentsTable.schoolId eq schoolId) and (StudentsTable.isActive eq true)
    }.filter {
        ClassNaming.sameClassSection(
            it[StudentsTable.className], it[StudentsTable.section], className, section
        )
    }.sortedBy { it[StudentsTable.rollNumber] }

// T-504: syllabusProgressFor / avgAttendanceFor were DELETED with the legacy
// /classes handler (their only caller). The rebuilt class plane computes
// attendance rates from typed enrollments + typed date in TeacherClassesRouting,
// not by parsing grade strings or ClassNaming heuristics (B-CLS-2 fix).

fun Route.teacherRouting() {
    authenticate("jwt") {
        route("/api/v1/teacher") {

            // ── GET /home ───────────────────────────────────────────────────
            get("/home") {
                val ctx = call.requireTeacherContext() ?: return@get
                val assignments = teacherAssignmentsFor(ctx)

                val schoolName = dbQuery {
                    SchoolsTable.selectAll().where { SchoolsTable.id eq ctx.schoolId }
                        .singleOrNull()?.get(SchoolsTable.name)
                } ?: ""

                val today = todayIso()
                // T-004: attendance_records.date / homework.due_date are now typed
                // `date` columns — compare against a LocalDate, not the ISO String.
                val todayDate = LocalDate.parse(today)
                val weekday = LocalDate.now().dayOfWeek.value // 1..7

                // Today's periods from the (optional) timetable. Honest empty if none.
                val periods = dbQuery {
                    TeacherPeriodsTable.selectAll().where {
                        (TeacherPeriodsTable.schoolId eq ctx.schoolId) and
                            (TeacherPeriodsTable.teacherId eq ctx.userId) and
                            (TeacherPeriodsTable.weekday eq weekday)
                    }.toList()
                }.sortedBy { it[TeacherPeriodsTable.startTime] }

                val now = java.time.LocalTime.now()
                val periodDtos = periods.map { r ->
                    // T-101: start_time/end_time are now typed `time` (LocalTime),
                    // so compare directly (no string parsing) and format to "HH:mm"
                    // at the wire boundary to preserve the contract.
                    val start = r[TeacherPeriodsTable.startTime]
                    val end = r[TeacherPeriodsTable.endTime]
                    val status = when {
                        !now.isBefore(start) && now.isBefore(end) -> "current"
                        !now.isBefore(end) -> "done"
                        else -> "upcoming"
                    }
                    TeacherPeriodDto(
                        id = r[TeacherPeriodsTable.id].value.toString(),
                        time = "${start.format(HHMM)} - ${end.format(HHMM)}",
                        className = "${r[TeacherPeriodsTable.className]}-${r[TeacherPeriodsTable.section]}",
                        subject = r[TeacherPeriodsTable.subject],
                        room = r[TeacherPeriodsTable.room],
                        isCurrent = status == "current",
                        status = status,
                    )
                }

                // Pending attendance: classes the teacher handles that have NO
                // student attendance row for today.
                var pendingAttendance = 0
                for (a in assignments) {
                    val grade = "${a.className}-${a.section}"
                    val marked = dbQuery {
                        AttendanceRecordsTable.selectAll().where {
                            (AttendanceRecordsTable.schoolId eq ctx.schoolId) and
                                (AttendanceRecordsTable.date eq todayDate) and
                                (AttendanceRecordsTable.type eq "student") and
                                (AttendanceRecordsTable.grade eq grade)
                        }.limit(1).firstOrNull() != null
                    }
                    if (!marked) pendingAttendance++
                }

                // Pending marks: active assessments owned by this teacher that
                // still have at least one student without a score.
                val pendingMarks = dbQuery {
                    val mine = AssessmentsTable.selectAll().where {
                        (AssessmentsTable.schoolId eq ctx.schoolId) and
                            (AssessmentsTable.teacherId eq ctx.userId) and
                            (AssessmentsTable.isActive eq true)
                    }.toList()
                    mine.count { asg ->
                        AssessmentMarksTable.selectAll().where {
                            (AssessmentMarksTable.assessmentId eq asg[AssessmentsTable.id].value) and
                                (AssessmentMarksTable.marks eq null)
                        }.limit(1).firstOrNull() != null
                    }
                }

                // Homework due today or later, authored by this teacher.
                val homeworkDue = dbQuery {
                    HomeworkTable.selectAll().where {
                        (HomeworkTable.schoolId eq ctx.schoolId) and
                            (HomeworkTable.teacherId eq ctx.userId) and
                            (HomeworkTable.isActive eq true) and
                            (HomeworkTable.dueDate greaterEq todayDate)
                    }.count().toInt()
                }

                // Actionable task cards — one per class still needing attendance.
                val tasks = assignments
                    .map { a ->
                        TeacherTaskDto(
                            id = "att-${a.assignmentId}",
                            title = "Mark attendance",
                            subtitle = "${a.className}-${a.section} · ${a.subject}",
                            type = "attendance",
                            className = "${a.className}-${a.section}",
                            isDone = false,
                        )
                    }

                call.ok(
                    TeacherHomeData(
                        teacherName = ctx.fullName,
                        schoolName = schoolName,
                        classesToday = periodDtos.size.takeIf { it > 0 } ?: assignments.map { "${it.className}-${it.section}" }.distinct().size,
                        pendingAttendance = pendingAttendance,
                        pendingMarks = pendingMarks,
                        homeworkDue = homeworkDue,
                        todayPeriods = periodDtos,
                        tasks = tasks,
                    ),
                    message = "Teacher home loaded",
                )
            }

            // ── GET /classes — DELETED (T-504) ────────────────────────────────
            // The legacy looping list (N+1 per class via studentCountFor /
            // syllabusProgressFor / avgAttendanceFor, hardcoded isClassTeacher=false,
            // grade-string attendance parsing) is GONE. The canonical
            // GET /api/v1/teacher/classes[/{id}] now resolves via the single
            // aggregated query set in TeacherClassesRouting (converged from the
            // staged /classes-v2 paths). Closes B-CLS-1/2/3 + F-CLS-5.

            // ── GET /profile ──────────────────────────────────────────────────
            get("/profile") {
                val ctx = call.requireTeacherContext() ?: return@get
                val assignments = teacherAssignmentsFor(ctx)

                val userRow = dbQuery {
                    AppUsersTable.selectAll().where { AppUsersTable.id eq ctx.userId }.singleOrNull()
                }
                val schoolName = dbQuery {
                    SchoolsTable.selectAll().where { SchoolsTable.id eq ctx.schoolId }
                        .singleOrNull()?.get(SchoolsTable.name)
                } ?: ""

                val subjects = assignments.map { it.subject }.distinct().sorted()
                val classes = assignments.map { "${it.className}-${it.section}" }.distinct().sorted()

                call.ok(
                    TeacherProfileData(
                        id = ctx.userId.toString(),
                        name = ctx.fullName,
                        username = userRow?.get(AppUsersTable.email)
                            ?: userRow?.get(AppUsersTable.phone)
                            ?: "",
                        schoolName = schoolName,
                        subjects = subjects,
                        classes = classes,
                        photoUrl = userRow?.get(AppUsersTable.profilePicUrl),
                        email = userRow?.get(AppUsersTable.email) ?: "",
                        phone = userRow?.get(AppUsersTable.phone) ?: "",
                    ),
                    message = "Profile loaded",
                )
            }

            // Task endpoints (attendance / marks / syllabus / homework).
            teacherTaskRoutes()
        }
    }
}

// avgAttendanceFor — DELETED (T-504). See note above teacherRouting().

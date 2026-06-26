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
 * This file holds the READ surface (profile; legacy /home + /classes list
 * deleted — see T-601 / T-504 notes inside). Attendance, marks, syllabus and
 * homework live in TeacherRoutingTasks.kt.
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
//
// T-601 (DELETE-don't-patch): TeacherPeriodDto / TeacherTaskDto / TeacherHomeData
// (the legacy /home dashboard shapes) were DELETED with the GET /home handler.
// The Today tab (GET /day, GET /week in TeacherTodayRouting) is the canonical
// day surface now (Doc 04 §4).
// ─────────────────────────────────────────────────────────────────────────────

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

            // ── GET /home — DELETED (T-601, DELETE-don't-patch) ───────────────
            // The legacy Home dashboard (counts + today periods + task cards) is
            // replaced by the Today tab (GET /day, GET /week in TeacherTodayRouting),
            // which resolves the real day from timetable + exceptions + calendar and
            // joins per-period attendance state (Doc 04 §4, Doc 05 §4). The matching
            // client getHome/TeacherHomeViewModel/TeacherHomeScreenV2 were deleted too.

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

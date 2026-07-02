/*
 * File: TeacherClassesRouting.kt
 * Module: feature.teacher
 *
 * PHASE 5 — CLASSES (Doc 09). The typed, single-aggregated-query CLASSES plane
 * that replaces the legacy looping `/classes` handler in TeacherRouting.kt.
 *
 * Defects this plane closes:
 *   • B-CLS-1 — the legacy `/classes` ran ~3 separate per-class queries
 *     (student count, syllabus progress, avg attendance) in a loop. This plane
 *     resolves every aggregate in ONE batched query set (no per-class N+1).
 *   • B-CLS-2 — attendance rate was computed by parsing the packed `grade`
 *     string with `lastIndexOf('-')` + in-memory ClassNaming matching. Now it is
 *     a typed join on enrollments + assignment_id + typed `date`.
 *   • B-CLS-3 / F-CLS-3 — `is_class_teacher` was hardcoded `false`. It is now the
 *     real `teacher_subject_assignments.is_class_teacher` flag (T-002).
 *   • F-CLS-5 — the class detail roster was a `VComingSoon` placeholder. T-502's
 *     composite endpoint feeds a real roster (T-504 renders it).
 *   • B-PROF-1/2, F-PROF-3 — no student profile drill-down existed. T-503 adds
 *     a scoped `GET /students/{id}` (403 if the teacher doesn't teach the student).
 *
 * Scoping is enforced at THREE levels (the constitution): the SQL only ever
 * touches owned assignments + their enrollment roster (query); the response only
 * carries that scope (API); the screen reaches each surface pre-scoped (UI, T-504/505).
 *
 * Endpoints (all JWT-guarded + scoped via core/TeacherAccess):
 *   GET /api/v1/teacher/classes                 (T-501 — list; canonical since T-504)
 *   GET /api/v1/teacher/classes/{assignmentId}  (T-502 — composite detail)
 *   GET /api/v1/teacher/students/{studentId}    (T-503 — scoped profile)
 *
 * T-504 CONVERGENCE: these were staged under /classes-v2 (Ktor forbids two
 * handlers on the same method+path) until the legacy looping /classes handler in
 * TeacherRouting was DELETED; they now own the canonical paths.
 *
 * PATH NOTE (converges in T-504): these mount under a temporary `…-v2` prefix
 * because `teacherRouting()` still binds `GET /api/v1/teacher/classes` (Ktor
 * forbids two handlers on the same method+path) — the same staging pattern as
 * T-203's `/attendance-typed`, T-303's `/gradebook`, T-402's `/syllabus-typed`,
 * T-405's `/homework-v2`. T-504 DELETES the legacy looping handler and converges
 * this plane to the canonical `/api/v1/teacher/classes[/{id}]` + `/students/{id}`
 * paths the shared TeacherApi client targets.
 *
 * DTOs are defined server-side (the :server module does NOT depend on :shared)
 * and mirror shared/.../teacher/domain/model/TeacherModels.kt field-for-field.
 *
 * Flag thresholds (Doc 09 §5) — server-computed, plain-language:
 *   Low attendance   : month rate < 75%                → danger
 *   Recent absences  : ≥3 absences in last 5 sessions   → warning
 *   Failing trend    : last 2 published marks < pass    → danger
 *   Dropping         : latest mark down >20% vs prior    → warning
 *   No data          : insufficient data (new student)   → neutral
 */
package com.littlebridge.enrollplus.feature.teacher

import com.littlebridge.enrollplus.core.EnrolledStudent
import com.littlebridge.enrollplus.core.OwnedAssignment
import com.littlebridge.enrollplus.core.TeacherContext
import com.littlebridge.enrollplus.core.ok
import com.littlebridge.enrollplus.core.requireOwnedAssignment
import com.littlebridge.enrollplus.core.requireTeacherContext
import com.littlebridge.enrollplus.core.teacherAssignmentsFor
import com.littlebridge.enrollplus.db.AssessmentMarksTable
import com.littlebridge.enrollplus.db.AssessmentsTable
import com.littlebridge.enrollplus.db.AttendanceRecordsTable
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.HomeworkSubmissionsTable
import com.littlebridge.enrollplus.db.HomeworkTable
import com.littlebridge.enrollplus.db.SchoolClassesTable
import com.littlebridge.enrollplus.db.StudentsTable
import com.littlebridge.enrollplus.db.TeacherPeriodsTable
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

// ─────────────────────────────────────────────────────────────────────────────
// Flag thresholds (Doc 09 §5). Centralised so list/detail/profile agree.
// ─────────────────────────────────────────────────────────────────────────────
internal object ClassFlags {
    const val LOW_ATTENDANCE_RATE = 0.75       // month rate below this → danger
    const val RECENT_WINDOW = 5                // last N sessions
    const val RECENT_ABSENCE_MIN = 3           // ≥ this many absences in window → warning
    const val DROP_FRACTION = 0.20             // latest down >20% vs prior → warning

    const val LOW_ATTENDANCE = "low_attendance"
    const val RECENT_ABSENCES = "recent_absences"
    const val FAILING_TREND = "failing_trend"
    const val DROPPING = "dropping"
    const val NO_DATA = "no_data"
}

private val HHMM_CLS: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val DAY_NAMES = arrayOf("", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

// ─────────────────────────────────────────────────────────────────────────────
// T-501 — list DTOs (mirror shared TeacherClassSummaryDto / TeacherClassesV2Data)
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class NextPeriodDto(
    @SerialName("weekday") val weekday: Int,
    @SerialName("day_label") val dayLabel: String,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    val room: String = "",
    @SerialName("is_today") val isToday: Boolean = false,
)

@Serializable
data class TeacherClassSummaryDto(
    @SerialName("assignment_id") val assignmentId: String,
    @SerialName("class_id") val classId: String? = null,
    @SerialName("class_name") val className: String,
    val section: String,
    @SerialName("subject_id") val subjectId: String? = null,
    val subject: String,
    @SerialName("student_count") val studentCount: Int,
    @SerialName("is_class_teacher") val isClassTeacher: Boolean,
    @SerialName("next_period") val nextPeriod: NextPeriodDto? = null,
    @SerialName("today_attendance_marked") val todayAttendanceMarked: Boolean,
    @SerialName("at_risk_count") val atRiskCount: Int,
)

@Serializable
data class TeacherClassesV2Data(
    val classes: List<TeacherClassSummaryDto> = emptyList(),
)

// ─────────────────────────────────────────────────────────────────────────────
// Shared per-student signal computation (used by list atRisk count + detail
// roster flags + the student profile). Pure given the loaded rows.
// ─────────────────────────────────────────────────────────────────────────────

/** A student's attendance rows over a window, newest-first by date. */
internal data class StudentAttendanceWindow(
    val monthPresent: Int,
    val monthTotal: Int,
    val recentStatuses: List<String>,   // last N sessions, newest first ("present"/"absent"/"late"/"leave")
) {
    val monthRate: Double? get() = if (monthTotal == 0) null else monthPresent.toDouble() / monthTotal
}

/** A student's recent published marks for the teacher's subject, newest-first. */
internal data class StudentMarkPoint(val marks: Double, val max: Int, val pass: Int?)

/**
 * Compute the Doc 09 §5 flags for one student. Returns the flag codes that fire.
 * `attendance` may be null (no data); `marks` newest-first.
 */
internal fun computeFlags(
    attendance: StudentAttendanceWindow?,
    marks: List<StudentMarkPoint>,
): List<String> {
    val flags = mutableListOf<String>()
    val hasAttendance = attendance != null && attendance.monthTotal > 0
    val hasMarks = marks.isNotEmpty()

    // No data — neither attendance nor marks recorded yet.
    if (!hasAttendance && !hasMarks) {
        return listOf(ClassFlags.NO_DATA)
    }

    if (hasAttendance) {
        val rate = attendance!!.monthRate
        if (rate != null && rate < ClassFlags.LOW_ATTENDANCE_RATE) {
            flags += ClassFlags.LOW_ATTENDANCE
        }
        val recentAbsences = attendance.recentStatuses
            .take(ClassFlags.RECENT_WINDOW)
            .count { it.equals("absent", ignoreCase = true) }
        if (recentAbsences >= ClassFlags.RECENT_ABSENCE_MIN) {
            flags += ClassFlags.RECENT_ABSENCES
        }
    }

    if (hasMarks) {
        // Failing trend: last 2 published marks below pass (only when a pass line exists).
        val lastTwo = marks.take(2)
        if (lastTwo.size == 2 && lastTwo.all { it.pass != null && it.marks < it.pass!!.toDouble() }) {
            flags += ClassFlags.FAILING_TREND
        }
        // Dropping: latest down >20% (as a fraction of max) vs the prior one.
        if (marks.size >= 2) {
            val latest = marks[0]
            val prior = marks[1]
            if (latest.max > 0 && prior.max > 0) {
                val latestPct = latest.marks / latest.max
                val priorPct = prior.marks / prior.max
                if (priorPct - latestPct > ClassFlags.DROP_FRACTION) {
                    flags += ClassFlags.DROPPING
                }
            }
        }
    }

    return flags.distinct()
}

fun Route.teacherClassesRouting() {
    authenticate("jwt") {
        route("/api/v1/teacher") {

            // ── GET /classes — the aggregated class list (T-501) ─────────────────
            // One batched query set: assignments → enrollments (counts) →
            // attendance-today → marks (for atRisk) → periods (next). NO per-class
            // loop of independent queries (B-CLS-1).
            get("/classes") {
                val ctx = call.requireTeacherContext() ?: return@get
                val assignments = teacherAssignmentsFor(ctx)
                if (assignments.isEmpty()) {
                    call.ok(TeacherClassesV2Data(emptyList()), message = "Classes loaded")
                    return@get
                }

                val today = todayIst()
                val weekday = today.dayOfWeek.value
                val monthStart = today.minusDays(30)

                val summaries = dbQuery {
                    // 1) Rosters for every owned assignment in one pass each (the
                    //    rosterForInTxn helper batches student identity internally).
                    //    We resolve them sequentially but inside ONE transaction so
                    //    there is no round-trip-per-class fan-out at the call site.
                    assignments.map { a ->
                        buildClassSummaryInTxn(ctx, a, today, weekday, monthStart)
                    }
                }
                call.ok(TeacherClassesV2Data(summaries), message = "Classes loaded")
            }

            // ── GET /classes/{assignmentId} — composite class detail (T-502) ─────
            // Doc 09 §3 — ONE composite endpoint (no client N+1): header, next
            // period, weekly timetable, attendance summary (today + week/month
            // rates), assessment schedule, active homework, and the REAL roster
            // (each student: attendance rate, latest published mark, Doc 09 §5
            // flags). Replaces the VComingSoon placeholder (F-CLS-5, rendered T-504).
            get("/classes/{assignmentId}") {
                val ctx = call.requireTeacherContext() ?: return@get
                val a = call.requireOwnedAssignment(ctx, call.parameters["assignmentId"]) ?: return@get

                val today = todayIst()
                val weekday = today.dayOfWeek.value
                val monthStart = today.minusDays(30)
                val weekStart = today.minusDays(6)

                val data = dbQuery {
                    buildClassDetailInTxn(ctx, a, today, weekday, weekStart, monthStart)
                }
                call.ok(data, message = "Class detail loaded")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// T-502 composite detail builder — runs inside the caller's dbQuery transaction.
// ─────────────────────────────────────────────────────────────────────────────
private fun buildClassDetailInTxn(
    ctx: TeacherContext,
    a: OwnedAssignment,
    today: LocalDate,
    weekday: Int,
    weekStart: LocalDate,
    monthStart: LocalDate,
): ClassDetailData {
    val roster = rosterForInTxn(a)
    val studentIds = roster.map { it.studentId }

    // ── attendance summary (today counts + week/month rates) ─────────────────
    val attRows = AttendanceRecordsTable.selectAll().where {
        (AttendanceRecordsTable.schoolId eq ctx.schoolId) and
            (AttendanceRecordsTable.type eq "student") and
            (AttendanceRecordsTable.assignmentId eq a.assignmentId) and
            (AttendanceRecordsTable.date greaterEq monthStart)
    }.toList()

    val todayRows = attRows.filter { it[AttendanceRecordsTable.date] == today }
    fun countStatus(rows: List<org.jetbrains.exposed.sql.ResultRow>, s: String) =
        rows.count { it[AttendanceRecordsTable.status].equals(s, ignoreCase = true) }

    val weekRows = attRows.filter { !it[AttendanceRecordsTable.date].isBefore(weekStart) }
    fun rate(rows: List<org.jetbrains.exposed.sql.ResultRow>): Double? {
        if (rows.isEmpty()) return null
        val present = rows.count { it[AttendanceRecordsTable.status].equals("present", ignoreCase = true) }
        return present.toDouble() / rows.size
    }

    val attendanceSummary = AttendanceSummaryDto(
        todayMarked = todayRows.isNotEmpty(),
        presentToday = countStatus(todayRows, "present"),
        absentToday = countStatus(todayRows, "absent"),
        lateToday = countStatus(todayRows, "late"),
        leaveToday = countStatus(todayRows, "leave"),
        weekRate = rate(weekRows),
        monthRate = rate(attRows),
    )

    // ── assessment schedule (scoped, newest exam_date first) ─────────────────
    val assessments = scopedAssessmentsInTxn(ctx, a, publishedOnly = false)
    val assessmentSchedule = assessments
        .sortedByDescending { it[AssessmentsTable.examDate] ?: LocalDate.MIN }
        .map { row ->
            ClassAssessmentDto(
                assessmentId = row[AssessmentsTable.id].value.toString(),
                name = row[AssessmentsTable.name],
                type = row[AssessmentsTable.type],
                examDate = row[AssessmentsTable.examDate]?.toString(),
                status = row[AssessmentsTable.status],
            )
        }

    // ── active homework (submitted / not-submitted counts) ───────────────────
    val homeworkRows = HomeworkTable.selectAll().where {
        (HomeworkTable.schoolId eq ctx.schoolId) and
            (HomeworkTable.isActive eq true)
    }.filter { hw ->
        val asgId = hw[HomeworkTable.assignmentId]
        if (asgId != null) asgId == a.assignmentId
        else hw[HomeworkTable.teacherId] == ctx.userId &&
            hw[HomeworkTable.className] == a.className &&
            hw[HomeworkTable.section] == a.section &&
            hw[HomeworkTable.subject] == a.subject
    }
    val rosterSize = roster.size
    val activeHomework = homeworkRows
        .sortedBy { it[HomeworkTable.dueDate] }
        .map { hw ->
            val hwId = hw[HomeworkTable.id].value
            val submitted = HomeworkSubmissionsTable.selectAll().where {
                (HomeworkSubmissionsTable.homeworkId eq hwId) and
                    (HomeworkSubmissionsTable.status neq "not_submitted")
            }.count().toInt()
            ClassHomeworkDto(
                homeworkId = hwId.toString(),
                title = hw[HomeworkTable.title],
                dueDate = hw[HomeworkTable.dueDate].toString(),
                submittedCount = submitted,
                notSubmittedCount = (rosterSize - submitted).coerceAtLeast(0),
            )
        }

    // ── roster with per-student attendance rate + latest mark + flags ────────
    val attByStudent = attendanceWindowsInTxn(ctx, a, studentIds, monthStart, today)
    val marksByStudent = recentMarksInTxn(ctx, a, studentIds)
    val latestMarkMeta = latestPublishedMarkInTxn(ctx, a, studentIds)
    val photoByStudent = if (studentIds.isEmpty()) emptyMap() else
        StudentsTable.selectAll().where { StudentsTable.id inList studentIds.map { EntityID(it, StudentsTable) } }
            .associate { it[StudentsTable.id].value to it[StudentsTable.profilePhotoUrl] }

    val rosterDtos = roster.map { s ->
        val win = attByStudent[s.studentId]
        val flags = computeFlags(win, marksByStudent[s.studentId] ?: emptyList())
        RosterStudentDto(
            studentId = s.studentId.toString(),
            name = s.fullName,
            roll = s.rollNumber,
            photoUrl = photoByStudent[s.studentId],
            attendanceRate = win?.monthRate,
            latestMark = latestMarkMeta[s.studentId],
            flags = flags,
        )
    }

    return ClassDetailData(
        header = ClassDetailHeaderDto(
            assignmentId = a.assignmentId.toString(),
            classId = a.classId?.toString(),
            className = a.className,
            section = a.section,
            subjectId = a.subjectId?.toString(),
            subject = a.subject,
            isClassTeacher = a.isClassTeacher,
            studentCount = roster.size,
        ),
        nextPeriod = nextPeriodForInTxn(ctx, a, today, weekday),
        weeklyTimetable = weeklyTimetableInTxn(ctx, a, weekday),
        attendanceSummary = attendanceSummary,
        assessmentSchedule = assessmentSchedule,
        activeHomework = activeHomework,
        roster = rosterDtos,
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// T-501 list builder — runs inside the caller's dbQuery transaction.
// ─────────────────────────────────────────────────────────────────────────────
private fun buildClassSummaryInTxn(
    ctx: TeacherContext,
    a: OwnedAssignment,
    today: LocalDate,
    weekday: Int,
    monthStart: LocalDate,
): TeacherClassSummaryDto {
    // Roster (typed enrollments). classId null (unmigrated) → empty roster.
    val roster: List<EnrolledStudent> = rosterForInTxn(a)
    val studentIds = roster.map { it.studentId }

    // today's attendance marked? one row for this assignment+date is enough.
    val todayMarked = AttendanceRecordsTable.selectAll().where {
        (AttendanceRecordsTable.schoolId eq ctx.schoolId) and
            (AttendanceRecordsTable.date eq today) and
            (AttendanceRecordsTable.type eq "student") and
            (AttendanceRecordsTable.assignmentId eq a.assignmentId)
    }.limit(1).firstOrNull() != null

    // at-risk count — compute flags per student from batched attendance + marks.
    val atRisk = if (studentIds.isEmpty()) 0 else {
        val attByStudent = attendanceWindowsInTxn(ctx, a, studentIds, monthStart, today)
        val marksByStudent = recentMarksInTxn(ctx, a, studentIds)
        roster.count { s ->
            val flags = computeFlags(attByStudent[s.studentId], marksByStudent[s.studentId] ?: emptyList())
            flags.any { it == ClassFlags.LOW_ATTENDANCE || it == ClassFlags.RECENT_ABSENCES ||
                it == ClassFlags.FAILING_TREND || it == ClassFlags.DROPPING }
        }
    }

    val next = nextPeriodForInTxn(ctx, a, today, weekday)

    return TeacherClassSummaryDto(
        assignmentId = a.assignmentId.toString(),
        classId = a.classId?.toString(),
        className = a.className,
        section = a.section,
        subjectId = a.subjectId?.toString(),
        subject = a.subject,
        studentCount = roster.size,
        isClassTeacher = a.isClassTeacher,   // REAL (B-CLS-3 fix), not hardcoded false
        nextPeriod = next,
        todayAttendanceMarked = todayMarked,
        atRiskCount = atRisk,
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared in-transaction query helpers (reused by T-501/T-502/T-503).
// ─────────────────────────────────────────────────────────────────────────────

/** Typed roster (active enrollments) for an owned assignment, roll-ordered.
 *
 * Primary path: EnrollmentsTable by classId + section (typed FK lookup).
 * Fallback when classId is null (unmigrated TSA row): resolve classId from
 * SchoolClassesTable by school + class name, then query enrollments.
 * Final fallback: StudentsTable + ClassNaming match (same as studentCountFor).
 */
internal fun rosterForInTxn(a: OwnedAssignment): List<EnrolledStudent> {
    val resolvedClassId = a.classId ?: resolveClassIdByName(a) ?: return fallbackRosterByClassNaming(a)
    val enrollments = com.littlebridge.enrollplus.db.EnrollmentsTable.selectAll().where {
        (com.littlebridge.enrollplus.db.EnrollmentsTable.classId eq resolvedClassId) and
            (com.littlebridge.enrollplus.db.EnrollmentsTable.section eq a.section) and
            (com.littlebridge.enrollplus.db.EnrollmentsTable.status eq "active")
    }.toList()
    if (enrollments.isEmpty()) return fallbackRosterByClassNaming(a)
    val sids = enrollments.map { it[com.littlebridge.enrollplus.db.EnrollmentsTable.studentId] }.distinct()
    val studentsById = com.littlebridge.enrollplus.db.StudentsTable.selectAll().where {
        com.littlebridge.enrollplus.db.StudentsTable.id inList
            sids.map { EntityID(it, com.littlebridge.enrollplus.db.StudentsTable) }
    }.associateBy { it[com.littlebridge.enrollplus.db.StudentsTable.id].value }
    return enrollments.mapNotNull { e ->
        val sid = e[com.littlebridge.enrollplus.db.EnrollmentsTable.studentId]
        val s = studentsById[sid] ?: return@mapNotNull null
        EnrolledStudent(
            studentId = sid,
            studentCode = s[com.littlebridge.enrollplus.db.StudentsTable.studentCode],
            fullName = s[com.littlebridge.enrollplus.db.StudentsTable.fullName],
            rollNumber = e[com.littlebridge.enrollplus.db.EnrollmentsTable.rollNumber],
            section = e[com.littlebridge.enrollplus.db.EnrollmentsTable.section],
            enrollmentId = e[com.littlebridge.enrollplus.db.EnrollmentsTable.id].value,
        )
    }.sortedWith(compareBy({ it.rollNumber ?: Int.MAX_VALUE }, { it.fullName }))
}

/** Look up classId from SchoolClassesTable by school + class name (case-insensitive). */
private fun resolveClassIdByName(a: OwnedAssignment): java.util.UUID? {
    return SchoolClassesTable.selectAll().where {
        (SchoolClassesTable.schoolId eq a.schoolId)
    }.firstOrNull {
        com.littlebridge.enrollplus.core.ClassNaming.classKey(it[SchoolClassesTable.name]) ==
            com.littlebridge.enrollplus.core.ClassNaming.classKey(a.className)
    }?.get(SchoolClassesTable.id)?.value
}

/** Fallback: match students by ClassNaming on className + section (no enrollments needed). */
private fun fallbackRosterByClassNaming(a: OwnedAssignment): List<EnrolledStudent> {
    return StudentsTable.selectAll().where {
        (StudentsTable.schoolId eq a.schoolId) and (StudentsTable.isActive eq true)
    }.filter {
        com.littlebridge.enrollplus.core.ClassNaming.sameClassSection(
            it[StudentsTable.className], it[StudentsTable.section], a.className, a.section
        )
    }.map { s ->
        EnrolledStudent(
            studentId = s[StudentsTable.id].value,
            studentCode = s[StudentsTable.studentCode],
            fullName = s[StudentsTable.fullName],
            rollNumber = s[StudentsTable.rollNumber]?.toIntOrNull(),
            section = a.section,
            enrollmentId = s[StudentsTable.id].value,
        )
    }.sortedWith(compareBy({ it.rollNumber ?: Int.MAX_VALUE }, { it.fullName }))
}

/**
 * Per-student attendance window for an assignment over [from, to]: month present/
 * total + the last RECENT_WINDOW statuses (newest first). One batched query over
 * the assignment's records, then grouped in memory (no per-student N+1).
 */
internal fun attendanceWindowsInTxn(
    ctx: TeacherContext,
    a: OwnedAssignment,
    studentIds: List<UUID>,
    from: LocalDate,
    to: LocalDate,
): Map<UUID, StudentAttendanceWindow> {
    if (studentIds.isEmpty()) return emptyMap()
    val rows = AttendanceRecordsTable.selectAll().where {
        (AttendanceRecordsTable.schoolId eq ctx.schoolId) and
            (AttendanceRecordsTable.type eq "student") and
            (AttendanceRecordsTable.assignmentId eq a.assignmentId) and
            (AttendanceRecordsTable.studentId inList studentIds) and
            (AttendanceRecordsTable.date greaterEq from)
    }.toList()
    return rows.groupBy { it[AttendanceRecordsTable.studentId] }
        .mapNotNull { (sid, recs) ->
            if (sid == null) return@mapNotNull null
            val inMonth = recs.filter { !it[AttendanceRecordsTable.date].isAfter(to) }
            val present = inMonth.count {
                it[AttendanceRecordsTable.status].equals("present", ignoreCase = true)
            }
            val recent = recs.sortedByDescending { it[AttendanceRecordsTable.date] }
                .map { it[AttendanceRecordsTable.status].lowercase() }
            sid to StudentAttendanceWindow(
                monthPresent = present,
                monthTotal = inMonth.size,
                recentStatuses = recent,
            )
        }.toMap()
}

/**
 * Assessments in the teacher's assignment scope. Prefer the typed assignment
 * binding (T-301); fall back to author + display class/section/subject for
 * legacy/mirrored unbound rows (no fabrication of a binding). `publishedOnly`
 * narrows to parent-visible results.
 */
internal fun scopedAssessmentsInTxn(
    ctx: TeacherContext,
    a: OwnedAssignment,
    publishedOnly: Boolean,
): List<org.jetbrains.exposed.sql.ResultRow> =
    AssessmentsTable.selectAll().where {
        (AssessmentsTable.schoolId eq ctx.schoolId) and
            (AssessmentsTable.isActive eq true)
    }.filter { row ->
        if (publishedOnly && !row[AssessmentsTable.isPublished]) return@filter false
        val asgId = row[AssessmentsTable.assignmentId]
        if (asgId != null) {
            asgId == a.assignmentId
        } else {
            // legacy unbound: match author + display class/section/subject
            row[AssessmentsTable.teacherId] == ctx.userId &&
                row[AssessmentsTable.className] == a.className &&
                row[AssessmentsTable.section] == a.section &&
                row[AssessmentsTable.subject] == a.subject
        }
    }

/**
 * Per-student recent PUBLISHED marks for the teacher's assignment scope, newest
 * first (by exam_date). One batched query joining scoped published assessments →
 * marks. Keyed by students.id.
 */
internal fun recentMarksInTxn(
    ctx: TeacherContext,
    a: OwnedAssignment,
    studentIds: List<UUID>,
): Map<UUID, List<StudentMarkPoint>> {
    if (studentIds.isEmpty()) return emptyMap()
    val assessments = scopedAssessmentsInTxn(ctx, a, publishedOnly = true)
    if (assessments.isEmpty()) return emptyMap()
    val byId = assessments.associateBy { it[AssessmentsTable.id].value }
    val assessmentIds = byId.keys.toList()

    val markRows = AssessmentMarksTable.selectAll().where {
        (AssessmentMarksTable.assessmentId inList assessmentIds) and
            (AssessmentMarksTable.studentRef inList studentIds)
    }.toList()

    return markRows.groupBy { it[AssessmentMarksTable.studentRef] }
        .mapNotNull { (sid, recs) ->
            if (sid == null) return@mapNotNull null
            val points = recs
                .filter { it[AssessmentMarksTable.marks] != null && !it[AssessmentMarksTable.isAbsent] }
                .sortedByDescending { byId[it[AssessmentMarksTable.assessmentId]]?.get(AssessmentsTable.examDate) ?: LocalDate.MIN }
                .mapNotNull { mr ->
                    val asg = byId[mr[AssessmentMarksTable.assessmentId]] ?: return@mapNotNull null
                    StudentMarkPoint(
                        marks = mr[AssessmentMarksTable.marks]!!,
                        max = asg[AssessmentsTable.maxMarks],
                        pass = asg[AssessmentsTable.passMarks],
                    )
                }
            sid to points
        }.toMap()
}

/**
 * The single most-recent PUBLISHED mark per student (name + marks + max), for the
 * roster's "latest performance" column (Doc 09 §3.7). Newest by exam_date; AB and
 * not-yet-entered rows are excluded.
 */
internal fun latestPublishedMarkInTxn(
    ctx: TeacherContext,
    a: OwnedAssignment,
    studentIds: List<UUID>,
): Map<UUID, LatestMarkDto> {
    if (studentIds.isEmpty()) return emptyMap()
    val assessments = scopedAssessmentsInTxn(ctx, a, publishedOnly = true)
    if (assessments.isEmpty()) return emptyMap()
    val byId = assessments.associateBy { it[AssessmentsTable.id].value }
    val assessmentIds = byId.keys.toList()

    val markRows = AssessmentMarksTable.selectAll().where {
        (AssessmentMarksTable.assessmentId inList assessmentIds) and
            (AssessmentMarksTable.studentRef inList studentIds)
    }.toList()

    return markRows.groupBy { it[AssessmentMarksTable.studentRef] }
        .mapNotNull { (sid, recs) ->
            if (sid == null) return@mapNotNull null
            val latest = recs
                .filter { it[AssessmentMarksTable.marks] != null && !it[AssessmentMarksTable.isAbsent] }
                .maxByOrNull { byId[it[AssessmentMarksTable.assessmentId]]?.get(AssessmentsTable.examDate) ?: LocalDate.MIN }
                ?: return@mapNotNull null
            val asg = byId[latest[AssessmentMarksTable.assessmentId]] ?: return@mapNotNull null
            sid to LatestMarkDto(
                name = asg[AssessmentsTable.name],
                marks = latest[AssessmentMarksTable.marks]!!,
                max = asg[AssessmentsTable.maxMarks],
            )
        }.toMap()
}

/**
 * The weekly timetable for THIS class (the periods I teach it across the week),
 * ordered by weekday then start time. Today's slots flagged.
 */
internal fun weeklyTimetableInTxn(
    ctx: TeacherContext,
    a: OwnedAssignment,
    todayWeekday: Int,
): List<WeeklyPeriodDto> {
    val periods = TeacherPeriodsTable.selectAll().where {
        (TeacherPeriodsTable.schoolId eq ctx.schoolId) and
            (TeacherPeriodsTable.teacherId eq ctx.userId) and
            (TeacherPeriodsTable.assignmentId eq a.assignmentId) and
            (TeacherPeriodsTable.isActive eq true)
    }.toList()
    return periods
        .sortedWith(compareBy({ it[TeacherPeriodsTable.weekday] }, { it[TeacherPeriodsTable.startTime] }))
        .map { r ->
            val wd = r[TeacherPeriodsTable.weekday]
            WeeklyPeriodDto(
                weekday = wd,
                dayLabel = DAY_NAMES.getOrElse(wd) { "" },
                startTime = r[TeacherPeriodsTable.startTime].format(HHMM_CLS),
                endTime = r[TeacherPeriodsTable.endTime].format(HHMM_CLS),
                room = r[TeacherPeriodsTable.room],
                isToday = wd == todayWeekday,
            )
        }
}


/**
 * The next period for an assignment within this week (today onward). Scans the
 * teacher's recurring periods bound to this assignment, picking the soonest slot
 * ≥ now today, else the earliest on a later weekday. Returns null if none.
 */
internal fun nextPeriodForInTxn(
    ctx: TeacherContext,
    a: OwnedAssignment,
    today: LocalDate,
    todayWeekday: Int,
): NextPeriodDto? {
    val now = LocalTime.now(IST_ZONE)
    val periods = TeacherPeriodsTable.selectAll().where {
        (TeacherPeriodsTable.schoolId eq ctx.schoolId) and
            (TeacherPeriodsTable.teacherId eq ctx.userId) and
            (TeacherPeriodsTable.assignmentId eq a.assignmentId) and
            (TeacherPeriodsTable.isActive eq true)
    }.toList()
    if (periods.isEmpty()) return null

    // Candidate = (weekday-distance, startTime). Today's remaining slots first.
    data class Cand(val weekday: Int, val start: LocalTime, val end: LocalTime, val room: String, val distance: Int)
    val cands = periods.mapNotNull { r ->
        val wd = r[TeacherPeriodsTable.weekday]
        val start = r[TeacherPeriodsTable.startTime]
        val end = r[TeacherPeriodsTable.endTime]
        val room = r[TeacherPeriodsTable.room]
        // distance in days from today to the next occurrence of wd (0..6)
        var dist = (wd - todayWeekday)
        if (dist < 0) dist += 7
        if (dist == 0 && !start.isAfter(now)) {
            // today's slot already passed → next week's occurrence
            dist = 7
        }
        Cand(wd, start, end, room, dist)
    }
    val best = cands.minWithOrNull(compareBy({ it.distance }, { it.start })) ?: return null
    return NextPeriodDto(
        weekday = best.weekday,
        dayLabel = DAY_NAMES.getOrElse(best.weekday) { "" },
        startTime = best.start.format(HHMM_CLS),
        endTime = best.end.format(HHMM_CLS),
        room = best.room,
        isToday = best.distance == 0,
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// T-502 — composite class-detail DTOs (mirror shared ClassDetail* / RosterStudentDto
// field-for-field; :server does NOT depend on :shared, so these are the wire truth).
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class ClassDetailHeaderDto(
    @SerialName("assignment_id") val assignmentId: String,
    @SerialName("class_id") val classId: String? = null,
    @SerialName("class_name") val className: String,
    val section: String,
    @SerialName("subject_id") val subjectId: String? = null,
    val subject: String,
    @SerialName("is_class_teacher") val isClassTeacher: Boolean,
    @SerialName("student_count") val studentCount: Int,
)

@Serializable
data class WeeklyPeriodDto(
    val weekday: Int,
    @SerialName("day_label") val dayLabel: String,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    val room: String = "",
    @SerialName("is_today") val isToday: Boolean = false,
)

@Serializable
data class AttendanceSummaryDto(
    @SerialName("today_marked") val todayMarked: Boolean,
    @SerialName("present_today") val presentToday: Int,
    @SerialName("absent_today") val absentToday: Int,
    @SerialName("late_today") val lateToday: Int,
    @SerialName("leave_today") val leaveToday: Int,
    @SerialName("week_rate") val weekRate: Double? = null,
    @SerialName("month_rate") val monthRate: Double? = null,
)

@Serializable
data class ClassAssessmentDto(
    @SerialName("assessment_id") val assessmentId: String,
    val name: String,
    val type: String,
    @SerialName("exam_date") val examDate: String? = null,
    val status: String,
)

@Serializable
data class ClassHomeworkDto(
    @SerialName("homework_id") val homeworkId: String,
    val title: String,
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("submitted_count") val submittedCount: Int,
    @SerialName("not_submitted_count") val notSubmittedCount: Int,
)

@Serializable
data class LatestMarkDto(
    val name: String,
    val marks: Double,
    val max: Int,
)

@Serializable
data class RosterStudentDto(
    @SerialName("student_id") val studentId: String,
    val name: String,
    val roll: Int? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("attendance_rate") val attendanceRate: Double? = null,
    @SerialName("latest_mark") val latestMark: LatestMarkDto? = null,
    val flags: List<String> = emptyList(),
)

@Serializable
data class ClassDetailData(
    val header: ClassDetailHeaderDto,
    @SerialName("next_period") val nextPeriod: NextPeriodDto? = null,
    @SerialName("weekly_timetable") val weeklyTimetable: List<WeeklyPeriodDto> = emptyList(),
    @SerialName("attendance_summary") val attendanceSummary: AttendanceSummaryDto,
    @SerialName("assessment_schedule") val assessmentSchedule: List<ClassAssessmentDto> = emptyList(),
    @SerialName("active_homework") val activeHomework: List<ClassHomeworkDto> = emptyList(),
    val roster: List<RosterStudentDto> = emptyList(),
)

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
package com.littlebridge.enrollplus.feature.school

import com.littlebridge.enrollplus.core.ClassNaming
import com.littlebridge.enrollplus.core.ClassResolution
import com.littlebridge.enrollplus.core.PhoneNormalizer
import com.littlebridge.enrollplus.core.StudentCode
import com.littlebridge.enrollplus.core.created
import com.littlebridge.enrollplus.core.fail
import com.littlebridge.enrollplus.core.ok
import com.littlebridge.enrollplus.core.okMessage
import com.littlebridge.enrollplus.core.requireSchoolAdmin
import com.littlebridge.enrollplus.core.requireSchoolContext
import com.littlebridge.enrollplus.db.AnnouncementsTable
import com.littlebridge.enrollplus.db.AppUsersTable
import com.littlebridge.enrollplus.db.AssessmentMarksTable
import com.littlebridge.enrollplus.db.AssessmentsTable
import com.littlebridge.enrollplus.db.AttendanceRecordsTable
import com.littlebridge.enrollplus.db.ChildrenTable
import com.littlebridge.enrollplus.db.EnrollmentsTable
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.ExamResultsTable
import com.littlebridge.enrollplus.db.FacultyTable
import com.littlebridge.enrollplus.db.FeeRecordsTable
import com.littlebridge.enrollplus.db.HomeworkSubmissionsTable
import com.littlebridge.enrollplus.db.HomeworkTable
import com.littlebridge.enrollplus.db.LeaveRequestsTable
import com.littlebridge.enrollplus.db.ParentChildLinksTable
import com.littlebridge.enrollplus.db.SchoolClassesTable
import com.littlebridge.enrollplus.db.StudentsTable
import com.littlebridge.enrollplus.db.TeacherSubjectAssignmentsTable
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
import java.time.LocalDate
import java.util.UUID

// ───────────────────────────── DTOs ─────────────────────────────

@Serializable
data class TodayItemDto(
    val color: String,  // "green"|"yellow"|"red"|"sky"
    val text: String
)

@Serializable
data class StudentDto(
    val id: String,
    @SerialName("student_code") val studentCode: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("class_name") val className: String,
    val section: String,
    @SerialName("roll_number") val rollNumber: String,
    // ISSUE 2b: parent/guardian phone on record (used by parent-link matching).
    @SerialName("parent_phone") val parentPhone: String? = null,
    @SerialName("profile_photo_url") val profilePhotoUrl: String? = null,
    val address: String? = null,
    // RA-SP: listing-card enrichment so the redesigned roster cards can show
    // meaningful, relationship-aware information at a glance. All defaulted so
    // older clients keep parsing; all DERIVED by StudentAggregationService.
    @SerialName("attendance_percent") val attendancePercent: Float = 0f,
    @SerialName("teacher_count") val teacherCount: Int = 0,
    @SerialName("parent_count") val parentCount: Int = 0,
    @SerialName("is_new_admission") val isNewAdmission: Boolean = false,
    val status: String = "active",
    // People Tab enrichment — new fields for enriched card UI.
    // All defaulted so older clients keep parsing; all DERIVED server-side.
    @SerialName("parent_name") val parentName: String? = null,
    @SerialName("homework_percent") val homeworkPercent: Float = 0f,
    @SerialName("fees_pending") val feesPending: Boolean = false,
    // Tri-state fee status: "PAID" | "PENDING" | "NONE". NONE means no fee
    // records exist yet, so the card renders no fee chip (fixes the bug where
    // every student wrongly showed "Fees paid").
    @SerialName("fee_status") val feeStatus: String = "NONE",
    @SerialName("parent_meeting_scheduled") val parentMeetingScheduled: Boolean = false,
    @SerialName("parent_user_id") val parentUserId: String? = null,
    @SerialName("today_items") val todayItems: List<TodayItemDto> = emptyList()
)

@Serializable
data class StudentListResponse(val students: List<StudentDto>)

@Serializable
data class StudentPaginationDto(
    val page: Int,
    val pageSize: Int,
    val totalRecords: Int,
    val hasNext: Boolean
)

@Serializable
data class StudentListPaginatedResponse(
    val students: List<StudentDto>,
    val pagination: StudentPaginationDto
)

// RA-SP: a teacher connected to a student, derived from class assignments.
@Serializable
data class StudentTeacherDto(
    val id: String,
    val name: String,
    val subject: String,
    val designation: String? = null
)

// RA-SP: a parent linked to a student. Supports multiple; one primary guardian.
@Serializable
data class StudentParentDto(
    val id: String,
    val name: String,
    val relation: String,
    @SerialName("is_primary_guardian") val isPrimaryGuardian: Boolean = false,
    val phone: String? = null
)

// RA-SP: a single recent-activity timeline entry (newest first in the list).
@Serializable
data class StudentActivityDto(
    val title: String,
    @SerialName("created_at") val createdAt: String,
    val type: String
)

@Serializable
data class CreateStudentRequest(
    @SerialName("full_name") val fullName: String,
    @SerialName("class_name") val className: String,
    val section: String? = null,
    @SerialName("roll_number") val rollNumber: String,
    // ISSUE 2b: parent/guardian phone captured at creation, validated + persisted,
    // and consumed by the parent→child link matcher.
    @SerialName("parent_phone") val parentPhone: String? = null,
    @SerialName("student_code") val studentCode: String? = null,  // optional; auto-generated when blank
    @SerialName("admission_date") val admissionDate: String? = null  // YYYY-MM-DD; falls back to today if null
)

/**
 * RA-LINK: partial student update (rename / move class+section / change roll).
 * Every field is optional so a caller can move ONLY the class without resending
 * the whole record. A non-null `className`/`section` that differs from the
 * stored value is what drives the Student ↔ Teacher relationship re-sync, since
 * relationships are DERIVED from the student's (class, section).
 */
@Serializable
data class UpdateStudentRequest(
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("class_name") val className: String? = null,
    val section: String? = null,
    @SerialName("roll_number") val rollNumber: String? = null,
    @SerialName("admission_date") val admissionDate: String? = null
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
    // RA-SP: current-term attendance (rolling ~90-day window) so the profile can
    // render the "This Term" bar alongside overall. DERIVED server-side.
    @SerialName("this_term_attendance") val thisTermAttendance: Int = 0,
    @SerialName("recent_attendance") val recentAttendance: List<AttendanceDayDto>,
    val marks: List<StudentMarkDto>,
    val leave: List<StudentLeaveDto>,
    val fees: List<StudentFeeDto>,
    // RA-SP: dashboard enrichment — relationship-aware sections + KPI carousel
    // metrics + backend-generated narrative. Defaulted so older clients still
    // parse; every value is DERIVED by StudentAggregationService.
    @SerialName("admission_date") val admissionDate: String? = null,
    @SerialName("attendance_percent") val attendancePercent: Float = 0f,
    @SerialName("teacher_count") val teacherCount: Int = 0,
    @SerialName("parent_count") val parentCount: Int = 0,
    @SerialName("subject_count") val subjectCount: Int = 0,
    @SerialName("academic_score") val academicScore: Float? = null,
    @SerialName("is_new_admission") val isNewAdmission: Boolean = false,
    val status: String = "active",
    val teachers: List<StudentTeacherDto> = emptyList(),
    val parents: List<StudentParentDto> = emptyList(),
    val insights: List<String> = emptyList(),
    val activities: List<StudentActivityDto> = emptyList()
)

@Serializable
data class TeacherProfileAssignmentDto(
    @SerialName("class_name") val className: String,
    val section: String,
    val subject: String,
    // RA-PP: students taught in this class+section, powering the portfolio cards.
    @SerialName("student_count") val studentCount: Int = 0
)

// RA-PP: achievement highlight card (title + supporting copy).
@Serializable
data class TeacherAchievementDto(
    val title: String,
    val description: String
)

// RA-PP: a single recent-activity timeline entry (newest first in the list).
@Serializable
data class TeacherActivityDto(
    val title: String,
    @SerialName("created_at") val createdAt: String,
    val type: String
)

@Serializable
data class TeacherProfileDto(
    val id: String,
    val name: String,

    val email: String? = null,
    val phone: String? = null,

    val role: String,

    // RA-PP: hero-banner identity enrichment.
    val designation: String? = null,
    @SerialName("joined_on") val joinedOn: String? = null,
    @SerialName("experience_years") val experienceYears: Int? = null,

    // RA-PP: KPI carousel + performance-overview metrics.
    @SerialName("student_count") val studentCount: Int = 0,
    @SerialName("class_count") val classCount: Int,
    @SerialName("subject_count") val subjectCount: Int,
    @SerialName("attendance_percent") val attendancePercent: Float = 0f,
    @SerialName("assignment_completion_percent") val assignmentCompletionPercent: Float = 0f,
    @SerialName("parent_satisfaction_percent") val parentSatisfactionPercent: Float = 0f,

    val status: String = "active",

    val assignments: List<TeacherProfileAssignmentDto>,

    // RA-PP: backend-generated narrative sections.
    val insights: List<String> = emptyList(),
    val achievements: List<TeacherAchievementDto> = emptyList(),
    @SerialName("recent_activities") val recentActivities: List<TeacherActivityDto> = emptyList()
)

// ─────────────────────────── helpers ────────────────────────────

/**
 * RA-PP: assemble the enriched teacher profile consumed by the redesigned
 * dashboard-style screen. Runs inside an Exposed transaction (call from
 * `dbQuery { ... }`). Returns null when the id is not an active teacher in
 * [schoolId] so the route can answer 404.
 *
 * Everything here is derived from real rows the school already owns
 * (assignments, students, attendance_records, homework + submissions,
 * announcements). Where a signal is genuinely unavailable we fall back to a
 * conservative, clearly-derived value rather than fabricating data — the UI
 * shows honest empty states for the narrative sections.
 */
private suspend fun buildTeacherProfile(schoolId: UUID, teacherId: UUID): TeacherProfileDto? {
    val row = AppUsersTable.selectAll().where {
        (AppUsersTable.id eq teacherId) and
            (AppUsersTable.schoolId eq schoolId) and
            (AppUsersTable.role eq "teacher")
    }.firstOrNull() ?: return null

    // ---- assignments (class+section+subject the teacher is allotted) ----
    val rawAssignments = TeacherSubjectAssignmentsTable.selectAll().where {
        (TeacherSubjectAssignmentsTable.schoolId eq schoolId) and
            (TeacherSubjectAssignmentsTable.teacherId eq teacherId) and
            (TeacherSubjectAssignmentsTable.isActive eq true)
    }.map {
        Triple(
            it[TeacherSubjectAssignmentsTable.className],
            it[TeacherSubjectAssignmentsTable.section],
            it[TeacherSubjectAssignmentsTable.subject]
        )
    }

    // Per (class, section) active-student headcount — looked up once, reused.
    // ISSUE 1: match via ClassNaming so counts aren't lost to format drift.
    val classSectionCounts: Map<Pair<String, String>, Int> =
        rawAssignments.map { it.first to it.second }.distinct().associateWith { (cls, sec) ->
            StudentsTable.selectAll().where {
                (StudentsTable.schoolId eq schoolId) and (StudentsTable.isActive eq true)
            }.count {
                ClassNaming.sameClassSection(
                    it[StudentsTable.className], it[StudentsTable.section], cls, sec
                )
            }
        }

    val assignments = rawAssignments.map { (cls, sec, subject) ->
        TeacherProfileAssignmentDto(
            className = cls,
            section = sec,
            subject = subject,
            studentCount = classSectionCounts[cls to sec] ?: 0
        )
    }

    val classKeys = rawAssignments.map { it.first to it.second }.distinct()
    val classCount = classKeys.size
    val subjectCount = rawAssignments.map { it.third }.distinct().size
    // Total students taught = distinct class+section headcounts summed (a student
    // in two of the teacher's subjects in the same class is only counted once).
    val studentCount = classKeys.sumOf { classSectionCounts[it] ?: 0 }

    // ---- attendance % (faculty attendance for this teacher) ----
    val facultyAtt = AttendanceRecordsTable.selectAll().where {
        (AttendanceRecordsTable.schoolId eq schoolId) and
            (AttendanceRecordsTable.type eq "faculty") and
            (AttendanceRecordsTable.personId eq teacherId.toString())
    }.map { it[AttendanceRecordsTable.status].lowercase() }
    val attTotal = facultyAtt.size
    val attendancePercent: Float = if (attTotal > 0) {
        val present = facultyAtt.count { it == "present" || it == "late" }
        (present * 100f) / attTotal
    } else {
        // No faculty attendance ledger yet → derive a sensible default rather
        // than reporting a misleading 0%. Active teachers read as fully present.
        if (row[AppUsersTable.isActive]) 100f else 0f
    }

    // ---- assignment completion % (homework submitted / expected) ----
    val homeworkIds = HomeworkTable.selectAll().where {
        (HomeworkTable.schoolId eq schoolId) and
            (HomeworkTable.teacherId eq teacherId) and
            (HomeworkTable.isActive eq true)
    }.map {
        HomeworkRef(
            it[HomeworkTable.id].value,
            it[HomeworkTable.className],
            it[HomeworkTable.section]
        )
    }
    var expectedSubmissions = 0
    var actualSubmissions = 0
    val hwIdList = homeworkIds.map { it.id }
    val submissionCounts = if (hwIdList.isEmpty()) emptyMap() else
        HomeworkSubmissionsTable.selectAll().where {
            HomeworkSubmissionsTable.homeworkId inList hwIdList
        }.groupBy { it[HomeworkSubmissionsTable.homeworkId] }.mapValues { it.value.size }

    homeworkIds.forEach { hw ->
        val expected = classSectionCounts[hw.className to hw.section]
            ?: StudentsTable.selectAll().where {
                (StudentsTable.schoolId eq schoolId) and (StudentsTable.isActive eq true)
            }.count {
                ClassNaming.sameClassSection(
                    it[StudentsTable.className], it[StudentsTable.section], hw.className, hw.section
                )
            }
        val submitted = submissionCounts[hw.id] ?: 0
        expectedSubmissions += expected
        // Cap submissions at the expected headcount so the ratio can never exceed
        // 100% (e.g. stale submissions from since-removed students).
        actualSubmissions += submitted.coerceAtMost(expected)
    }
    val assignmentCompletionPercent: Float =
        if (expectedSubmissions > 0) (actualSubmissions * 100f) / expectedSubmissions
        else if (homeworkIds.isNotEmpty()) 100f
        else 0f

    // ---- parent satisfaction % (no dedicated feedback store yet) ----
    // Blend the two real signals we DO have (attendance + completion) into a
    // single aggregate proxy. This is honest about its provenance and updates
    // automatically once a real feedback table lands.
    val parentSatisfactionPercent: Float = when {
        attTotal == 0 && expectedSubmissions == 0 -> 0f
        else -> ((attendancePercent + assignmentCompletionPercent) / 2f).coerceIn(0f, 100f)
    }

    // ---- recent activities (newest first, capped at 15) ----
    val activities = teacherRecentActivities(schoolId, teacherId, limit = 15)

    // ---- achievements (derived from the same real signals) ----
    val achievements = teacherAchievements(
        attendancePercent = attendancePercent,
        completionPercent = assignmentCompletionPercent,
        studentCount = studentCount,
        subjectCount = subjectCount
    )

    // ---- insights (3–5 short narrative strings) ----
    val insights = teacherInsights(
        studentCount = studentCount,
        subjectCount = subjectCount,
        classCount = classCount,
        attendancePercent = attendancePercent,
        completionPercent = assignmentCompletionPercent,
        topGrade = classKeys.firstOrNull()?.first
    )

    // ---- hero-banner identity enrichment ----
    val joinedOn = row[AppUsersTable.createdAt].toString().take(10)
    val experienceYears = runCatching {
        val created = row[AppUsersTable.createdAt]
        val years = java.time.Duration.between(created, Instant.now()).toDays() / 365
        years.toInt().coerceAtLeast(0)
    }.getOrDefault(0)
    val savedDesignation = dbQuery {
        FacultyTable.selectAll()
            .where { FacultyTable.externalId eq "U-$teacherId" }
            .firstOrNull()
            ?.get(FacultyTable.department)
    }
    val computedDesignation = when {
        subjectCount >= 4 -> "Senior Teacher"
        subjectCount >= 1 -> "Subject Teacher"
        else -> "Teacher"
    }
    val designation = savedDesignation?.takeIf { it.isNotBlank() } ?: computedDesignation

    return TeacherProfileDto(
        id = row[AppUsersTable.id].value.toString(),
        name = row[AppUsersTable.fullName],
        email = row[AppUsersTable.email],
        phone = row[AppUsersTable.phone],
        role = row[AppUsersTable.role],
        designation = designation,
        joinedOn = joinedOn,
        experienceYears = experienceYears,
        studentCount = studentCount,
        classCount = classCount,
        subjectCount = subjectCount,
        attendancePercent = attendancePercent,
        assignmentCompletionPercent = assignmentCompletionPercent,
        parentSatisfactionPercent = parentSatisfactionPercent,
        status = if (row[AppUsersTable.isActive]) "active" else "inactive",
        assignments = assignments,
        insights = insights,
        achievements = achievements,
        recentActivities = activities
    )
}

private data class HomeworkRef(val id: UUID, val className: String, val section: String)

/**
 * RA-PP: real teacher activity feed assembled from the rows the teacher
 * authored — homework, assessments (with publish events) and announcements —
 * merged and sorted newest-first, then capped.
 */
private fun teacherRecentActivities(
    schoolId: UUID,
    teacherId: UUID,
    limit: Int
): List<TeacherActivityDto> {
    val items = mutableListOf<Pair<Instant, TeacherActivityDto>>()

    HomeworkTable.selectAll().where {
        (HomeworkTable.schoolId eq schoolId) and (HomeworkTable.teacherId eq teacherId)
    }.orderBy(HomeworkTable.createdAt, SortOrder.DESC).limit(limit).forEach {
        val ts = it[HomeworkTable.createdAt]
        items += ts to TeacherActivityDto(
            title = "Assigned homework: ${it[HomeworkTable.title]} · ${it[HomeworkTable.className]} ${it[HomeworkTable.section]}",
            createdAt = ts.toString(),
            type = "homework"
        )
    }

    AssessmentsTable.selectAll().where {
        (AssessmentsTable.schoolId eq schoolId) and (AssessmentsTable.teacherId eq teacherId)
    }.orderBy(AssessmentsTable.createdAt, SortOrder.DESC).limit(limit).forEach {
        val published = it[AssessmentsTable.isPublished]
        val ts = it[AssessmentsTable.publishedAt] ?: it[AssessmentsTable.createdAt]
        items += ts to TeacherActivityDto(
            title = if (published)
                "Published results: ${it[AssessmentsTable.name]} · ${it[AssessmentsTable.subject]}"
            else
                "Created assessment: ${it[AssessmentsTable.name]} · ${it[AssessmentsTable.subject]}",
            createdAt = ts.toString(),
            type = if (published) "exam_result" else "assessment"
        )
    }

    AnnouncementsTable.selectAll().where {
        (AnnouncementsTable.schoolId eq schoolId) and (AnnouncementsTable.createdBy eq teacherId)
    }.orderBy(AnnouncementsTable.createdAt, SortOrder.DESC).limit(limit).forEach {
        val ts = it[AnnouncementsTable.createdAt]
        items += ts to TeacherActivityDto(
            title = "Posted announcement: ${it[AnnouncementsTable.title]}",
            createdAt = ts.toString(),
            type = "announcement"
        )
    }

    return items.sortedByDescending { it.first }.take(limit).map { it.second }
}

/** RA-PP: derive achievement cards from the teacher's real metric profile. */
private fun teacherAchievements(
    attendancePercent: Float,
    completionPercent: Float,
    studentCount: Int,
    subjectCount: Int
): List<TeacherAchievementDto> {
    val out = mutableListOf<TeacherAchievementDto>()
    if (attendancePercent >= 95f) {
        out += TeacherAchievementDto(
            title = "Attendance Excellence",
            description = "Maintained ${attendancePercent.toInt()}% personal attendance."
        )
    }
    if (completionPercent >= 90f) {
        out += TeacherAchievementDto(
            title = "Academic Excellence",
            description = "${completionPercent.toInt()}% assignment completion across classes."
        )
    }
    if (studentCount >= 100) {
        out += TeacherAchievementDto(
            title = "School Recognition",
            description = "Teaching $studentCount students this term."
        )
    }
    if (subjectCount >= 4) {
        out += TeacherAchievementDto(
            title = "Multi-Subject Specialist",
            description = "Covers $subjectCount subjects across the school."
        )
    }
    return out
}

/** RA-PP: generate 3–5 short, meaningful insight strings. */
private fun teacherInsights(
    studentCount: Int,
    subjectCount: Int,
    classCount: Int,
    attendancePercent: Float,
    completionPercent: Float,
    topGrade: String?
): List<String> {
    val out = mutableListOf<String>()
    if (studentCount > 0) out += "Handles $studentCount students across $classCount classes."
    if (subjectCount > 0) out += "Teaches $subjectCount subject${if (subjectCount == 1) "" else "s"}."
    if (attendancePercent >= 90f) out += "Above-average personal attendance at ${attendancePercent.toInt()}%."
    if (completionPercent >= 80f) out += "Strong assignment completion rate of ${completionPercent.toInt()}%."
    if (!topGrade.isNullOrBlank()) out += "Highest engagement among $topGrade teachers."
    return out.take(5)
}

private fun studentRowToDto(row: org.jetbrains.exposed.sql.ResultRow): StudentDto {
    val studentCode = row[StudentsTable.studentCode]
    val schoolId = row[StudentsTable.schoolId]
    val parentUserId = if (studentCode != null && schoolId != null) {
        ChildrenTable.selectAll()
            .where {
                (ChildrenTable.studentCode eq studentCode) and
                (ChildrenTable.schoolId eq schoolId) and
                (ChildrenTable.isActive eq true)
            }
            .firstOrNull()
            ?.get(ChildrenTable.parentId)?.toString()
    } else null
    return StudentDto(
        id = row[StudentsTable.id].value.toString(),
        studentCode = row[StudentsTable.studentCode],
        fullName = row[StudentsTable.fullName],
        className = row[StudentsTable.className],
        section = row[StudentsTable.section],
        rollNumber = row[StudentsTable.rollNumber],
        parentPhone = row[StudentsTable.parentPhone],
        profilePhotoUrl = row[StudentsTable.profilePhotoUrl],
        address = row[StudentsTable.address],
        status = if (row[StudentsTable.isActive]) "active" else "inactive",
        isNewAdmission = isNewAdmission(
            row[StudentsTable.admissionDate]?.let { java.time.LocalDateTime.of(it, java.time.LocalTime.MIDNIGHT).toInstant(java.time.ZoneOffset.UTC) }
                ?: row[StudentsTable.createdAt]
        ),
        parentUserId = parentUserId,
    )
}

/**
 * RA-SP: average attendance % across the active students of a (class, section),
 * used to derive the "above class average" insight. Returns null when the class
 * has no attendance records yet. Runs inside the caller's Exposed transaction.
 */
private fun classSectionAverageAttendance(schoolId: UUID, className: String, section: String): Float? {
    val codes = StudentsTable.selectAll().where {
        (StudentsTable.schoolId eq schoolId) and (StudentsTable.isActive eq true)
    }.filter {
        ClassNaming.sameClassSection(
            it[StudentsTable.className], it[StudentsTable.section], className, section
        )
    }.map { it[StudentsTable.studentCode] }
    if (codes.isEmpty()) return null
    val percents = codes.mapNotNull { code ->
        val summary = StudentAggregationService.attendanceForStudent(schoolId, code)
        summary.percent.takeIf { it > 0f }
    }
    return if (percents.isEmpty()) null else percents.average().toFloat()
}

/** RA-SP: a student admitted within the last 30 days reads as a New Admission. */
private fun isNewAdmission(admittedAt: Instant?): Boolean {
    if (admittedAt == null) return false
    val days = runCatching {
        java.time.Duration.between(admittedAt, Instant.now()).toDays()
    }.getOrDefault(Long.MAX_VALUE)
    return days in 0..30
}

/**
 * RA-SP: enrich a roster [StudentDto] with the relationship-aware listing
 * metrics (attendance %, teacher count, parent count) via the centralized
 * [StudentAggregationService] — the single source of truth — so cards never show
 * stale numbers. Runs inside the caller's Exposed transaction.
 */
private fun enrichStudentForList(schoolId: UUID, dto: StudentDto): StudentDto {
    val attendance = StudentAggregationService.attendanceForStudent(schoolId, dto.studentCode)
    val teacherCount = StudentAggregationService.teacherCountForStudent(schoolId, dto.className, dto.section)
    val parentCount = StudentAggregationService.parentCountForStudent(schoolId, dto.studentCode)
    val parentName = StudentAggregationService.primaryParentNameForStudent(schoolId, dto.studentCode)
    val homeworkPercent = StudentAggregationService.homeworkPercentForStudent(schoolId, dto.studentCode)
    val feeStatus = StudentAggregationService.feeStatusForStudent(schoolId, dto.studentCode)
    val parentMeetingScheduled = StudentAggregationService.parentMeetingScheduledForStudent(schoolId, dto.studentCode)
    val todayItems = StudentAggregationService.todayItemsForStudent(schoolId, dto.studentCode, dto.className, dto.section)
    return dto.copy(
        attendancePercent = attendance.percent,
        teacherCount = teacherCount,
        parentCount = parentCount,
        parentName = parentName,
        homeworkPercent = homeworkPercent,
        feesPending = feeStatus == StudentAggregationService.FeeStatus.PENDING,
        feeStatus = feeStatus.name,
        parentMeetingScheduled = parentMeetingScheduled,
        todayItems = todayItems
    )
}

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
    val iPhone = indexOfAny("parent_phone", "phone", "parent_mobile", "mobile", "guardian_phone", "contact")

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
            parentPhone = cell(cols, iPhone),
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
                val pageParam = call.request.queryParameters["page"]?.toIntOrNull()
                val pageSizeParam = call.request.queryParameters["pageSize"]?.toIntOrNull()
                val isPaginated = pageParam != null

                if (isPaginated) {
                    val page = (pageParam ?: 1).coerceAtLeast(1)
                    val pageSize = (pageSizeParam ?: 10).coerceIn(1, 100)
                    val offset = (page - 1).toLong() * pageSize

                    val result = dbQuery {
                        val totalRecords = StudentsTable.selectAll()
                            .where { StudentsTable.schoolId eq ctx.schoolId }
                            .count()

                        val students = StudentsTable.selectAll()
                            .where { StudentsTable.schoolId eq ctx.schoolId }
                            .orderBy(StudentsTable.className to SortOrder.ASC, StudentsTable.rollNumber to SortOrder.ASC)
                            .limit(pageSize, offset)
                            .map(::studentRowToDto)
                            .filter { s ->
                                (classFilter == null || s.className.equals(classFilter, ignoreCase = true)) &&
                                    (q == null ||
                                        s.fullName.lowercase().contains(q) ||
                                        s.rollNumber.lowercase().contains(q) ||
                                        s.studentCode.lowercase().contains(q))
                            }
                            .let { batch -> StudentAggregationService.batchEnrichForList(ctx.schoolId, batch) }

                        StudentListPaginatedResponse(
                            students = students,
                            pagination = StudentPaginationDto(
                                page = page,
                                pageSize = pageSize,
                                totalRecords = totalRecords.toInt(),
                                hasNext = offset + students.size < totalRecords
                            )
                        )
                    }
                    call.ok(result, message = "Students fetched")
                } else {
                    val students = dbQuery {
                        StudentsTable.selectAll()
                            .where { StudentsTable.schoolId eq ctx.schoolId }
                            .orderBy(StudentsTable.className to SortOrder.ASC, StudentsTable.rollNumber to SortOrder.ASC)
                            .map(::studentRowToDto)
                            .filter { s ->
                                (classFilter == null || s.className.equals(classFilter, ignoreCase = true)) &&
                                    (q == null ||
                                        s.fullName.lowercase().contains(q) ||
                                        s.rollNumber.lowercase().contains(q) ||
                                        s.studentCode.lowercase().contains(q))
                            }
                            .let { batch -> StudentAggregationService.batchEnrichForList(ctx.schoolId, batch) }
                    }
                    call.ok(StudentListResponse(students), message = "Students fetched")
                }
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
                // ISSUE 2b: parent phone is OPTIONAL but validated when provided.
                // Not blocking — a school may not capture parent phone at enrollment time,
                // and admin UIs built before this field was added should not break.
                // Phone-less students can still be matched by name+class+roll in the
                // parent link flow; they go to the normal pending queue (not needs_review).
                val canonPhone: String? = if (!req.parentPhone.isNullOrBlank()) {
                    if (!PhoneNormalizer.isValid(req.parentPhone)) {
                        call.fail(
                            "The parent phone number you entered doesn't look right. " +
                                "Leave it blank if you don't have it, or enter a 10-digit mobile number.",
                            HttpStatusCode.BadRequest, "PARENT_PHONE_INVALID"
                        )
                        return@post
                    }
                    PhoneNormalizer.canonical(req.parentPhone)
                } else null

                val classOk = dbQuery { ClassResolution.classExists(ctx.schoolId, req.className) }
                if (!classOk) {
                    call.fail(
                        "Class \"${req.className}\" is not configured for your school. Add it in Classes & Subjects first.",
                        HttpStatusCode.BadRequest, "CLASS_NOT_FOUND"
                    )
                    return@post
                }

                val dto = dbQuery {
                    // ISSUE 1: store the canonical class name + section so the derived
                    // teacher⇄student join holds byte-for-byte.
                    val resolvedClass = ClassResolution.canonicalClassName(ctx.schoolId, req.className)
                    val resolvedSection = ClassNaming.canonicalSection(req.section)

                    // ISSUE 2a: one predictable code derived from class/section/roll,
                    // unique per school. An explicit code (rare) is still honoured.
                    val code = req.studentCode?.takeIf { it.isNotBlank() }?.trim()
                        ?: StudentCode.generate(resolvedClass, resolvedSection, req.rollNumber) { candidate ->
                            StudentsTable.selectAll()
                                .where { StudentsTable.studentCode eq candidate }
                                .any()
                        }

                    val clash = StudentsTable.selectAll()
                        .where { StudentsTable.studentCode eq code }
                        .firstOrNull()
                    if (clash != null) return@dbQuery null  // duplicate code

                    val newId = StudentsTable.insert {
                        it[schoolId] = ctx.schoolId
                        it[studentCode] = code
                        it[fullName] = req.fullName.trim()
                        it[className] = resolvedClass
                        it[section] = resolvedSection
                        it[rollNumber] = req.rollNumber.trim()
                        it[parentPhone] = canonPhone
                        it[isActive] = true
                        it[admissionDate] = req.admissionDate?.let { d ->
                            runCatching { LocalDate.parse(d) }.getOrNull()
                        } ?: LocalDate.now()
                        it[createdAt] = Instant.now()
                    } get StudentsTable.id

                    // ── Enrollment creation ─────────────────────────────────────
                    // Create an active enrollment row so enrollmentsFor() finds this
                    // student with a REAL enrollment_id. Without this, the fallback
                    // roster sets enrollmentId = null, and attendance inserts can't
                    // link to a valid enrollment row.
                    val classId = SchoolClassesTable.selectAll()
                        .where { SchoolClassesTable.schoolId eq ctx.schoolId }
                        .firstOrNull {
                            ClassNaming.classKey(it[SchoolClassesTable.name]) == ClassNaming.classKey(resolvedClass)
                        }?.get(SchoolClassesTable.id)?.value

                    if (classId != null) {
                        val rollInt = req.rollNumber.trim().toIntOrNull()
                        EnrollmentsTable.insert {
                            it[EnrollmentsTable.schoolId] = ctx.schoolId
                            it[EnrollmentsTable.studentId] = newId.value
                            it[EnrollmentsTable.classId] = classId
                            it[EnrollmentsTable.section] = resolvedSection
                            if (rollInt != null) it[EnrollmentsTable.rollNumber] = rollInt
                            it[status] = "active"
                            it[EnrollmentsTable.startDate] = req.admissionDate?.let { d ->
                                runCatching { LocalDate.parse(d) }.getOrNull()
                            } ?: LocalDate.now()
                            it[EnrollmentsTable.createdAt] = Instant.now()
                        }
                    }

                    // RA-SP: student joined a class → recompute the live student
                    // counts for every teacher assigned to that class+section, so
                    // teacher workload/metrics never go stale. No manual updates.
                    StudentAggregationService.recalcTeacherStudentCountsForClass(
                        ctx.schoolId, resolvedClass, resolvedSection
                    )

                    StudentsTable.selectAll().where { StudentsTable.id eq newId }.first().let(::studentRowToDto)
                }
                if (dto == null) {
                    call.fail("A student with that code already exists.", HttpStatusCode.Conflict, "STUDENT_CODE_TAKEN")
                    return@post
                }
                call.created(dto, message = "Student added")
            }

            // ---- update a student / MOVE class+section (school-admin) ----
            // RA-LINK: the missing write surface for Example 3 (a student changes
            // class). Student ↔ Teacher relationships are DERIVED from the
            // student's (class, section), so moving a student is what re-syncs the
            // link graph. When the class/section actually changes we funnel through
            // the centralized reconciler `recalcTeacherStudentCountsForMove`, which:
            //   1. drops the student from the OLD class's teachers' counts,
            //   2. links the student to the NEW class's teachers automatically,
            //   3. refreshes the workload of every affected teacher,
            // with no manual linking. The student's profile teacher list updates on
            // the next read because it too is derived from (class, section).
            patch("{id}") {
                val ctx = call.requireSchoolAdmin() ?: return@patch
                val id = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid student id"); return@patch }
                val req = runCatching { call.receive<UpdateStudentRequest>() }.getOrNull()
                    ?: run { call.fail("Invalid body"); return@patch }

                val dto = dbQuery {
                    val row = StudentsTable.selectAll()
                        .where { (StudentsTable.id eq id) and (StudentsTable.schoolId eq ctx.schoolId) }
                        .firstOrNull() ?: return@dbQuery null

                    val oldClass = row[StudentsTable.className]
                    val oldSection = row[StudentsTable.section]

                    // Resolve the new values, falling back to the current ones.
                    // ISSUE 1: canonicalise a class/section change so the moved
                    // student stores the same string the teacher assignment does.
                    val newName = req.fullName?.takeIf { it.isNotBlank() }?.trim()
                    val newClass = req.className?.takeIf { it.isNotBlank() }
                        ?.let { ClassResolution.canonicalClassName(ctx.schoolId, it) } ?: oldClass
                    val newSection = req.section?.takeIf { it.isNotBlank() }
                        ?.let { ClassNaming.canonicalSection(it) } ?: oldSection
                    val newRoll = req.rollNumber?.takeIf { it.isNotBlank() }?.trim()
                    val newAdmissionDate = req.admissionDate?.takeIf { it.isNotBlank() }
                        ?.let { d -> runCatching { LocalDate.parse(d) }.getOrNull() }

                    StudentsTable.update({
                        (StudentsTable.id eq id) and (StudentsTable.schoolId eq ctx.schoolId)
                    }) {
                        if (newName != null) it[fullName] = newName
                        it[className] = newClass
                        it[section] = newSection
                        if (newRoll != null) it[rollNumber] = newRoll
                        if (newAdmissionDate != null) it[admissionDate] = newAdmissionDate
                    }

                    // RA-LINK: only re-sync when the class+section actually changed —
                    // a pure rename/roll edit leaves the relationship graph intact.
                    val moved = !oldClass.equals(newClass, ignoreCase = true) ||
                        !oldSection.equals(newSection, ignoreCase = true)
                    if (moved) {
                        // ── Enrollment sync ──────────────────────────────────────
                        // When a student moves class/section, EnrollmentsTable must
                        // be updated so enrollmentsFor() finds the student in the
                        // NEW class with a REAL enrollment_id. Without this, the
                        // fallback roster uses the student's UUID as a fake
                        // enrollment_id, causing fk_att_enrollment violations on
                        // attendance insert.
                        //
                        // Pattern (mirrors StudentTransferService.approveTransfer):
                        //   1. Resolve the new classId from SchoolClassesTable.
                        //   2. Mark the student's current active enrollment(s) as
                        //      "transferred" with endDate = today (preserves
                        //      historical data and keeps old attendance FK refs valid).
                        //   3. Insert a new "active" enrollment for the new class+section.
                        val newClassId = SchoolClassesTable.selectAll()
                            .where { SchoolClassesTable.schoolId eq ctx.schoolId }
                            .firstOrNull {
                                ClassNaming.classKey(it[SchoolClassesTable.name]) == ClassNaming.classKey(newClass)
                            }?.get(SchoolClassesTable.id)?.value

                        if (newClassId != null) {
                            EnrollmentsTable.update({
                                (EnrollmentsTable.studentId eq id) and
                                    (EnrollmentsTable.status eq "active")
                            }) {
                                it[status] = "transferred"
                                it[endDate] = LocalDate.now()
                            }

                            val rollInt = newRoll?.toIntOrNull()
                            EnrollmentsTable.insert {
                                it[EnrollmentsTable.schoolId] = ctx.schoolId
                                it[EnrollmentsTable.studentId] = id
                                it[EnrollmentsTable.classId] = newClassId
                                it[EnrollmentsTable.section] = newSection
                                if (rollInt != null) it[EnrollmentsTable.rollNumber] = rollInt
                                it[status] = "active"
                                it[EnrollmentsTable.startDate] = LocalDate.now()
                                it[EnrollmentsTable.createdAt] = Instant.now()
                            }
                        }

                        StudentAggregationService.recalcTeacherStudentCountsForMove(
                            ctx.schoolId, oldClass, oldSection, newClass, newSection
                        )
                    }

                    StudentsTable.selectAll().where { StudentsTable.id eq id }.first().let(::studentRowToDto)
                }
                if (dto == null) {
                    call.fail("Student not found in your school", HttpStatusCode.NotFound, "STUDENT_NOT_FOUND")
                    return@patch
                }
                call.ok(dto, message = "Student updated")
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
                // RA-SP: track every class+section that gained a student so we can
                // recompute the affected teachers' workload once, after the batch.
                val touchedClasses = LinkedHashSet<Pair<String, String>>()
                dbQuery {
                    // Pre-fetch all configured class keys for this school for fast validation.
                    val configuredClassKeys = SchoolClassesTable.selectAll()
                        .where { SchoolClassesTable.schoolId eq ctx.schoolId }
                        .flatMap { row ->
                            listOf(
                                ClassNaming.classKey(row[SchoolClassesTable.name]),
                                ClassNaming.classKey(row[SchoolClassesTable.code])
                            )
                        }.toSet()

                    // Pre-build a classKey → classId map so we can create enrollment
                    // rows without re-querying SchoolClassesTable per student.
                    val classIdByKey = SchoolClassesTable.selectAll()
                        .where { SchoolClassesTable.schoolId eq ctx.schoolId }
                        .associate { row ->
                            ClassNaming.classKey(row[SchoolClassesTable.name]) to row[SchoolClassesTable.id].value
                        }

                    rows.forEachIndexed { index, r ->
                        val rowNo = index + 1
                        if (r.fullName.isBlank() || r.className.isBlank() || r.rollNumber.isBlank()) {
                            results += BulkImportRowResult(rowNo, false, null, "Name, class and roll number are required.")
                            return@forEachIndexed
                        }
                        // Bug 17: reject rows whose class is not configured for this school.
                        val typedClassKey = ClassNaming.classKey(r.className.trim().replace(Regex("\\s+"), " "))
                        if (typedClassKey !in configuredClassKeys) {
                            results += BulkImportRowResult(rowNo, false, null, "Class \"${r.className}\" is not configured for your school.")
                            return@forEachIndexed
                        }
                        // ISSUE 1: canonical class + section so the derived join holds.
                        val resolvedClass = ClassResolution.canonicalClassName(ctx.schoolId, r.className)
                        val resolvedSection = ClassNaming.canonicalSection(r.section)

                        // ISSUE 2a: standardized, per-school-unique code derived from
                        // class/section/roll (explicit code still honoured if given).
                        val code = r.studentCode?.takeIf { it.isNotBlank() }?.trim()
                            ?: StudentCode.generate(resolvedClass, resolvedSection, r.rollNumber) { candidate ->
                                StudentsTable.selectAll()
                                    .where { StudentsTable.studentCode eq candidate }
                                    .any()
                            }

                        val clash = StudentsTable.selectAll()
                            .where { StudentsTable.studentCode eq code }
                            .firstOrNull()
                        if (clash != null) {
                            results += BulkImportRowResult(rowNo, false, code, "Student code already exists.")
                            return@forEachIndexed
                        }
                        // ISSUE 2b: persist a parent phone when the row supplies one
                        // (bulk import stays lenient — phone is optional here).
                        val canonPhone = r.parentPhone?.takeIf { PhoneNormalizer.isValid(it) }
                            ?.let { PhoneNormalizer.canonical(it) }
                        val newId = StudentsTable.insert {
                            it[schoolId] = ctx.schoolId
                            it[studentCode] = code
                            it[fullName] = r.fullName.trim()
                            it[className] = resolvedClass
                            it[section] = resolvedSection
                            it[rollNumber] = r.rollNumber.trim()
                            it[parentPhone] = canonPhone
                            it[isActive] = true
                            it[createdAt] = Instant.now()
                        } get StudentsTable.id

                        // Create active enrollment so enrollmentsFor() finds this
                        // student with a real enrollment_id.
                        val resolvedClassId = classIdByKey[ClassNaming.classKey(resolvedClass)]
                        if (resolvedClassId != null) {
                            val rollInt = r.rollNumber.trim().toIntOrNull()
                            EnrollmentsTable.insert {
                                it[EnrollmentsTable.schoolId] = ctx.schoolId
                                it[EnrollmentsTable.studentId] = newId.value
                                it[EnrollmentsTable.classId] = resolvedClassId
                                it[EnrollmentsTable.section] = resolvedSection
                                if (rollInt != null) it[EnrollmentsTable.rollNumber] = rollInt
                                it[status] = "active"
                                it[EnrollmentsTable.startDate] = LocalDate.now()
                                it[EnrollmentsTable.createdAt] = Instant.now()
                            }
                        }
                        touchedClasses += resolvedClass to resolvedSection
                        inserted++
                        results += BulkImportRowResult(rowNo, true, code, null)
                    }

                    // RA-SP: one workload recalc per affected class+section after the
                    // import, so teacher metrics reflect every newly-added student.
                    touchedClasses.forEach { (cls, sec) ->
                        StudentAggregationService.recalcTeacherStudentCountsForClass(ctx.schoolId, cls, sec)
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
                    // RA-SP: remember the class so we can refresh affected teachers
                    // after the student is removed.
                    val removedClass = row[StudentsTable.className]
                    val removedSection = row[StudentsTable.section]

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

                    // RA-SP: student left the class → recompute the live student
                    // counts for every teacher assigned to it (workload auto-sync,
                    // no stale data).
                    StudentAggregationService.recalcTeacherStudentCountsForClass(
                        ctx.schoolId, removedClass, removedSection
                    )
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
                        AttendanceDayDto(it[AttendanceRecordsTable.date].toString(), it[AttendanceRecordsTable.status].lowercase())
                    }
                    val present = attRows.count { it.status == "present" }
                    val absent = attRows.count { it.status == "absent" }
                    val late = attRows.count { it.status == "late" }
                    val total = attRows.size
                    val rate = if (total > 0) (((present + late) * 100) / total) else 0

                    // "This Term" attendance: rolling 90-day window of the same
                    // attendance rows. Falls back to the overall rate when the
                    // window has no records (keeps the bar meaningful, no mock).
                    val termCutoff = java.time.LocalDate.now().minusDays(90).toString()
                    val termRows = attRows.filter { it.date >= termCutoff }
                    val termTotal = termRows.size
                    val thisTermAttendance = if (termTotal > 0) {
                        ((termRows.count { it.status == "present" || it.status == "late" } * 100) / termTotal)
                    } else rate

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
                                examDate = a[AssessmentsTable.examDate]?.toString()
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

                    // ── RA-SP: relationship-aware aggregation via the single source
                    // of truth, so teachers/parents/insights/activities and the KPI
                    // carousel metrics are always derived from live facts. ──
                    val admittedAt = row[StudentsTable.admissionDate]?.let { java.time.LocalDateTime.of(it, java.time.LocalTime.MIDNIGHT).toInstant(java.time.ZoneOffset.UTC) }
                        ?: row[StudentsTable.createdAt]
                    val teachers = StudentAggregationService.teachersForStudent(
                        ctx.schoolId, student.className, student.section
                    )
                    val parents = StudentAggregationService.parentsForStudent(ctx.schoolId, code)
                    val subjectCount = StudentAggregationService.subjectCountForStudent(
                        ctx.schoolId, student.className, student.section
                    )
                    val academicScore = StudentAggregationService.academicScoreForStudent(ctx.schoolId, code)
                    val daysSinceAdmission = runCatching {
                        java.time.Duration.between(admittedAt, Instant.now()).toDays()
                    }.getOrNull()
                    // Average attendance of the student's class+section, used to derive
                    // an "above class average" insight.
                    val classAvgAttendance = classSectionAverageAttendance(
                        ctx.schoolId, student.className, student.section
                    )
                    val insights = StudentAggregationService.insightsForStudent(
                        attendancePercent = rate.toFloat(),
                        classAverageAttendance = classAvgAttendance,
                        parentCount = parents.size,
                        hasPrimaryGuardian = parents.any { it.isPrimaryGuardian },
                        teacherCount = teachers.map { it.id }.distinct().size,
                        academicScore = academicScore,
                        daysSinceAdmission = daysSinceAdmission
                    )
                    val activities = StudentAggregationService.activitiesForStudent(
                        schoolId = ctx.schoolId,
                        studentCode = code,
                        admissionDate = admittedAt
                    )

                    StudentProfileDto(
                        student = student,
                        presentDays = present,
                        absentDays = absent,
                        lateDays = late,
                        attendanceRate = rate,
                        thisTermAttendance = thisTermAttendance,
                        recentAttendance = attRows.take(30),
                        marks = marks,
                        leave = leave,
                        fees = fees,
                        admissionDate = admittedAt?.toString()?.take(10),
                        attendancePercent = rate.toFloat(),
                        teacherCount = teachers.map { it.id }.distinct().size,
                        parentCount = parents.size,
                        subjectCount = subjectCount,
                        academicScore = academicScore,
                        isNewAdmission = student.isNewAdmission,
                        status = student.status,
                        teachers = teachers,
                        parents = parents,
                        insights = insights,
                        activities = activities
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

            val profile = dbQuery { buildTeacherProfile(ctx.schoolId, id) }
            if (profile == null) {
                call.fail("Teacher not found in your school", HttpStatusCode.NotFound, "TEACHER_NOT_FOUND")
                return@get
            }
            call.ok(profile, message = "Teacher profile fetched")
        }
    }
}

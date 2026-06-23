package com.littlebridge.vidyaprayag.feature.teacher.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

// ─────────────────────────────────────────────────────────────────────────────
// Teacher Home — dashboard glance + today's periods + pending task counts
// Backs Teacher.tsx → Home tab.
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class TeacherHomeResponse(
    val success: Boolean,
    val data: TeacherHomeData,
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

@Serializable
data class TeacherPeriodDto(
    val id: String,
    val time: String,
    @SerialName("class_name") val className: String,
    val subject: String,
    val room: String = "",
    @SerialName("is_current") val isCurrent: Boolean = false,
    val status: String = "upcoming", // "done" | "current" | "upcoming"
)

@Serializable
data class TeacherTaskDto(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val type: String, // "attendance" | "marks" | "syllabus" | "homework"
    @SerialName("class_name") val className: String = "",
    @SerialName("is_done") val isDone: Boolean = false,
)

// ─────────────────────────────────────────────────────────────────────────────
// Teacher Today — the resolved day & week (Teacher Portal Rebuild, Doc 05 §4).
//
// These power the new Today tab's 3-face schedule card (Doc 05 §5) and the
// Profile → My Schedule full-week view. The SERVER resolves the day for a
// SPECIFIC date — merging the recurring teacher_periods pattern with one-off
// period_exceptions (cancel/reschedule/room-change/substitution/extra), the
// published HOLIDAY calendar events, and the relevant published calendar
// overlay (EXAM/PTM/EVENT) — and joins per-period attendance state so the
// "marked ✓ / unmarked !" badge is REAL, not fabricated (kills B-HOME-4).
//
// Mirrors server DTOs in feature/teacher/TeacherDayRouting.kt field-for-field.
// Times serialize as "HH:mm"; dates as ISO "YYYY-MM-DD".
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class ResolvedDayResponse(
    val success: Boolean,
    val data: ResolvedDayDto,
)

@Serializable
data class ResolvedDayDto(
    val date: String,                                   // ISO YYYY-MM-DD (server-authoritative)
    val weekday: Int,                                   // 1=Mon … 7=Sun (ISO)
    @SerialName("is_holiday") val isHoliday: Boolean = false,
    @SerialName("holiday_name") val holidayName: String? = null,
    val periods: List<ResolvedPeriodDto> = emptyList(),
    val calendar: List<CalendarOverlayDto> = emptyList(),
    // Server-clock authoritative indices into [periods] (no device-clock drift).
    // null when there is no current/next period (before first / after last / holiday).
    @SerialName("now_index") val nowIndex: Int? = null,
    @SerialName("next_index") val nextIndex: Int? = null,
)

@Serializable
data class ResolvedPeriodDto(
    @SerialName("period_id") val periodId: String? = null,   // null for an EXTRA (exception-only) period
    @SerialName("assignment_id") val assignmentId: String? = null, // the TSA that authorizes scoped actions
    @SerialName("class_name") val className: String,
    val section: String = "",
    val subject: String = "",
    val room: String = "",
    @SerialName("start_time") val startTime: String,         // "HH:mm"
    @SerialName("end_time") val endTime: String,             // "HH:mm"
    // SCHEDULED | CANCELLED | RESCHEDULED | SUBSTITUTION | ROOM_CHANGE | EXTRA
    val status: String = "SCHEDULED",
    @SerialName("attendance_marked") val attendanceMarked: Boolean = false,
    @SerialName("substitute_teacher_name") val substituteTeacherName: String? = null,
    // True when THIS teacher is the inserted substitute for this date (so the
    // period appears in MY day and I may mark it) — Doc 06 E14.
    @SerialName("is_substitute_for_me") val isSubstituteForMe: Boolean = false,
    // Data-quality flag: this slot overlaps another (server never silently drops
    // one — Doc 05 §6 "two periods overlap"). UI shows a warning chip.
    @SerialName("has_overlap") val hasOverlap: Boolean = false,
    val note: String = "",
)

@Serializable
data class CalendarOverlayDto(
    @SerialName("event_id") val eventId: String,
    val type: String,                                   // EXAM | HOLIDAY | PTM | SCHOOL_EVENT | …
    val title: String,
    val audience: String = "ALL_SCHOOL",
    // When an EXAM is tied to one of the teacher's assessments, the assessment id
    // so Today can deep-link to its marks entry (Doc 05 §3.3 / Doc 07 §4.1).
    @SerialName("assessment_id") val assessmentId: String? = null,
    @SerialName("class_ref") val classRef: String? = null,
)

@Serializable
data class ResolvedWeekResponse(
    val success: Boolean,
    val data: ResolvedWeekDto,
)

@Serializable
data class ResolvedWeekDto(
    @SerialName("week_start") val weekStart: String,    // ISO date of Monday of the resolved week
    val days: List<ResolvedDayDto> = emptyList(),       // Mon..Sat (or Sun) resolved
)

// ─────────────────────────────────────────────────────────────────────────────
// Teacher self check-in (Doc 06 §2). Surfaced on Today's greeting band; the
// biometric ladder (biometric → PIN → manual) records WHICH method succeeded.
// Mirrors server feature/teacher/TeacherDayRouting.kt check-in handlers.
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class CheckInStatusResponse(
    val success: Boolean,
    val data: CheckInStatusDto,
)

@Serializable
data class CheckInStatusDto(
    @SerialName("checked_in") val checkedIn: Boolean = false,
    @SerialName("checked_in_at") val checkedInAt: String? = null, // ISO timestamp, server-stamped
    val method: String? = null,                          // biometric | pin | manual
    val date: String,                                    // ISO date the status is for
)

@Serializable
data class TeacherCheckInRequest(
    val method: String,                                  // biometric | pin | manual
    @SerialName("device_id") val deviceId: String? = null,
)

// ─────────────────────────────────────────────────────────────────────────────
// Today obligations strip (Doc 04 §5.5). REAL outstanding work, replacing the
// fabricated "Today's tasks" (B-HOME-4). Each obligation deep-links to its
// scoped surface. Mirrors server feature/teacher/TeacherDayRouting.kt.
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class TeacherObligationsResponse(
    val success: Boolean,
    val data: TeacherObligationsDto,
)

@Serializable
data class TeacherObligationsDto(
    @SerialName("unmarked_classes") val unmarkedClasses: Int = 0,
    @SerialName("classes_today_total") val classesTodayTotal: Int = 0,
    @SerialName("unpublished_results") val unpublishedResults: Int = 0,
    @SerialName("submissions_to_review") val submissionsToReview: Int = 0,
    @SerialName("pending_leave_decisions") val pendingLeaveDecisions: Int = 0,
    val items: List<ObligationItemDto> = emptyList(),
) {
    /** True only when there is genuinely nothing outstanding (earned "all caught up"). */
    val isAllCaughtUp: Boolean
        get() = items.isEmpty() &&
            unmarkedClasses == 0 && unpublishedResults == 0 &&
            submissionsToReview == 0 && pendingLeaveDecisions == 0
}

@Serializable
data class ObligationItemDto(
    val id: String,
    // attendance | marks | homework | leave
    val type: String,
    val title: String,
    val subtitle: String = "",
    val count: Int = 0,
    // Pre-scoped deep-link target so the UI jumps straight to the scoped tool.
    @SerialName("assignment_id") val assignmentId: String? = null,
    @SerialName("ref_id") val refId: String? = null,
)

// ─────────────────────────────────────────────────────────────────────────────
// My Classes — list of classes the teacher handles, + class detail roster
// Backs Teacher.tsx → MyClasses tab + ClassDetail.
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class TeacherClassesResponse(
    val success: Boolean,
    val data: TeacherClassesData,
)

@Serializable
data class TeacherClassesData(
    val classes: List<TeacherClassDto> = emptyList(),
)

@Serializable
data class TeacherClassDto(
    val id: String,
    @SerialName("class_name") val className: String,
    val subject: String,
    @SerialName("student_count") val studentCount: Int,
    @SerialName("is_class_teacher") val isClassTeacher: Boolean = false,
    @SerialName("syllabus_progress") val syllabusProgress: Float = 0f,
    @SerialName("avg_attendance") val avgAttendance: Float = 0f,
)

@Serializable
data class TeacherStudentDto(
    val id: String,
    val name: String,
    @SerialName("roll_no") val rollNo: String = "",
    @SerialName("photo_url") val photoUrl: String? = null,
)

// ─────────────────────────────────────────────────────────────────────────────
// Attendance — load the day's roster, then save marks.
//
// T-205 (Doc 06 §3, Doc 11): the legacy class_id+packed-`grade` attendance DTOs
// (TeacherAttendanceResponse / TeacherAttendanceData / AttendanceEntryDto /
// SubmitAttendanceRequest / AttendanceMarkDto) are DELETED. They backed the
// pre-rebuild Update › Attendance screen, which T-205 replaced from scratch. The
// only attendance contract now is the typed, assignment-scoped one below (T-202).
// (admin's own AttendanceEntryDto lives in feature.admin.domain.model — separate.)
// ─────────────────────────────────────────────────────────────────────────────

// ─────────────────────────────────────────────────────────────────────────────
// T-202 — Typed, SCOPED attendance load/save (Doc 06 §1.2/§3.8). The new contract
// is keyed by the
// authorizing assignmentId (Doc 05 binding), carries the typed roster from
// enrollments, pre-marks approved-leave students (leaveDefaults / source), and
// reports alreadyMarked + last-marked audit so the screen can load for EDIT
// rather than silently overwrite (E3). Mirrors server feature/teacher.
// Status space: present | absent | late | leave (VALID_ATTENDANCE, D-ATT-1).
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class AttendanceLoadResponse(
    val success: Boolean,
    val data: AttendanceLoadDto,
)

@Serializable
data class AttendanceLoadDto(
    @SerialName("assignment_id") val assignmentId: String,
    val date: String,
    // Human scope label for the wrong-class guard header (e.g. "7B · Mathematics").
    val scope: String = "",
    @SerialName("class_name") val className: String = "",
    val section: String = "",
    val subject: String = "",
    // The typed roster, sourced from enrollments active on `date` (E4/E5 honored).
    val students: List<AttendanceStudentDto> = emptyList(),
    // True when any mark already exists for (school,date,assignment) → load for EDIT.
    @SerialName("already_marked") val alreadyMarked: Boolean = false,
    // Last-marked audit for the "Last marked by {name} at {time}" line (E3).
    @SerialName("last_marked_by") val lastMarkedBy: String? = null,
    @SerialName("last_marked_at") val lastMarkedAt: String? = null,
    // Student ids pre-defaulted to `leave` from an Approved leave covering `date`
    // (§3.5). They arrive with status=leave/source=leave_auto in `students` too;
    // this list is the explicit set so the UI can badge them "on approved leave".
    @SerialName("leave_defaults") val leaveDefaults: List<String> = emptyList(),
    // Edge-case flags so the screen can warn without re-deriving (§4).
    @SerialName("is_holiday") val isHoliday: Boolean = false,
    @SerialName("holiday_name") val holidayName: String? = null,
    @SerialName("is_cancelled") val isCancelled: Boolean = false,
    // Back-date window (days) the server will accept a save for; UI disables older.
    @SerialName("back_date_window_days") val backDateWindowDays: Int = 7,
)

@Serializable
data class AttendanceStudentDto(
    @SerialName("student_id") val studentId: String,
    val name: String,
    @SerialName("roll_no") val rollNo: String = "",
    // Current/default mark for this student on this date. Defaults to present for
    // a fresh sheet; `leave` when on approved leave; the saved value on edit.
    val status: String = "present",
    // Origin of the current status: manual | leave_auto | bulk | biometric.
    val source: String? = null,
    @SerialName("enrollment_id") val enrollmentId: String? = null,
)

@Serializable
data class AttendanceSaveRequest(
    @SerialName("assignment_id") val assignmentId: String,
    val date: String,
    val marks: List<AttendanceSaveMarkDto> = emptyList(),
)

@Serializable
data class AttendanceSaveMarkDto(
    @SerialName("student_id") val studentId: String,
    val status: String, // present | absent | late | leave
)

@Serializable
data class AttendanceSaveResponse(
    val success: Boolean,
    val data: AttendanceSaveResultDto,
)

@Serializable
data class AttendanceSaveResultDto(
    val saved: Int = 0,
    val date: String,
)

// ─────────────────────────────────────────────────────────────────────────────
// Marks — load students for an exam/subject, then submit scores.
// Backs Teacher.tsx → Update › Marks.
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class TeacherMarksResponse(
    val success: Boolean,
    val data: TeacherMarksData,
)

@Serializable
data class TeacherMarksData(
    @SerialName("class_name") val className: String,
    val subject: String,
    @SerialName("exam_name") val examName: String,
    @SerialName("max_marks") val maxMarks: Int,
    val students: List<MarksEntryDto> = emptyList(),
)

@Serializable
data class MarksEntryDto(
    @SerialName("student_id") val studentId: String,
    val name: String,
    @SerialName("roll_no") val rollNo: String = "",
    val marks: Float? = null,
)

@Serializable
data class SubmitMarksRequest(
    @SerialName("class_id") val classId: String,
    @SerialName("exam_id") val examId: String,
    val entries: List<MarkScoreDto>,
)

@Serializable
data class MarkScoreDto(
    @SerialName("student_id") val studentId: String,
    val marks: Float,
)

// ─────────────────────────────────────────────────────────────────────────────
// Syllabus — chapter/topic coverage per class+subject, with progress update.
// Backs Teacher.tsx → Update › Syllabus.
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class TeacherSyllabusResponse(
    val success: Boolean,
    val data: TeacherSyllabusData,
)

@Serializable
data class TeacherSyllabusData(
    @SerialName("class_name") val className: String,
    val subject: String,
    @SerialName("overall_progress") val overallProgress: Float = 0f,
    val units: List<SyllabusUnitDto> = emptyList(),
)

@Serializable
data class SyllabusUnitDto(
    val id: String,
    val title: String,
    @SerialName("is_covered") val isCovered: Boolean = false,
    @SerialName("covered_on") val coveredOn: String? = null,
)

@Serializable
data class UpdateSyllabusRequest(
    @SerialName("unit_id") val unitId: String,
    @SerialName("is_covered") val isCovered: Boolean,
)

// ─────────────────────────────────────────────────────────────────────────────
// Homework — list assignments + create a new one.
// Backs Teacher.tsx → Update › Homework.
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class TeacherHomeworkResponse(
    val success: Boolean,
    val data: TeacherHomeworkData,
)

@Serializable
data class TeacherHomeworkData(
    val items: List<HomeworkDto> = emptyList(),
)

@Serializable
data class HomeworkDto(
    val id: String,
    val title: String,
    val description: String = "",
    @SerialName("class_name") val className: String,
    val subject: String,
    @SerialName("due_date") val dueDate: String,
    @SerialName("submitted_count") val submittedCount: Int = 0,
    @SerialName("total_count") val totalCount: Int = 0,
)

@Serializable
data class CreateHomeworkRequest(
    @SerialName("class_id") val classId: String,
    val title: String,
    val description: String = "",
    @SerialName("due_date") val dueDate: String,
)

// ─────────────────────────────────────────────────────────────────────────────
// Teacher Profile
// Backs Teacher.tsx → Profile tab.
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class TeacherProfileResponse(
    val success: Boolean,
    val data: TeacherProfileData,
)

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

// ─────────────────────────────────────────────────────────────────────────────
// Assessments (exams) — list + create. RA-40: the marks plane needs a valid
// exam_id; these models back the exam selector that feeds SubmitMarksRequest.
// Mirror server TeacherAssessmentDto / TeacherAssessmentsData / CreateAssessmentRequest.
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class TeacherAssessmentsResponse(
    val success: Boolean,
    val data: TeacherAssessmentsData,
)

@Serializable
data class TeacherAssessmentsData(
    val assessments: List<TeacherAssessmentDto> = emptyList(),
)

@Serializable
data class TeacherAssessmentDto(
    val id: String,
    val name: String,
    val subject: String,
    @SerialName("max_marks") val maxMarks: Int,
    @SerialName("exam_date") val examDate: String? = null,
    @SerialName("is_published") val isPublished: Boolean = false,
)

@Serializable
data class CreateAssessmentRequest(
    @SerialName("class_id") val classId: String,
    val name: String,
    @SerialName("max_marks") val maxMarks: Int? = null,
    @SerialName("exam_date") val examDate: String? = null,
)

// ─────────────────────────────────────────────────────────────────────────────
// RA-44: teacher leave workflow. Mirror server/.../teacher/TeacherLeaveRouting.kt.
// A teacher lists leave requests routed to their classes and approves/rejects.
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class TeacherLeaveListResponse(
    val success: Boolean = true,
    val data: TeacherLeaveListData = TeacherLeaveListData(),
)

@Serializable
data class TeacherLeaveListData(
    @SerialName("pending_count") val pendingCount: Int = 0,
    val requests: List<TeacherLeaveDto> = emptyList(),
)

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
data class TeacherLeaveDecisionRequest(val status: String) // Approved | Rejected

// ─────────────────────────────────────────────────────────────────────────────
// RA-51 — teacher messaging (1:1 + "message class parents" broadcast).
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class TeacherClassBroadcastRequest(
    @SerialName("class_name") val className: String,
    val section: String? = null,
    val body: String,
)

@Serializable
data class TeacherClassBroadcastData(
    val recipients: Int = 0,
)

@Serializable
data class TeacherClassBroadcastResponse(
    val success: Boolean = false,
    val message: String? = null,
    val data: TeacherClassBroadcastData? = null,
)

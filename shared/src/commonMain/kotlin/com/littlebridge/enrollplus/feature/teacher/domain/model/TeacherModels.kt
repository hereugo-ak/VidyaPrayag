package com.littlebridge.enrollplus.feature.teacher.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

// ─────────────────────────────────────────────────────────────────────────────
// Teacher Home — DELETED (T-601, DELETE-don't-patch).
// The legacy Home dashboard (TeacherHomeResponse / TeacherHomeData /
// TeacherPeriodDto / TeacherTaskDto) is replaced by the Today tab (Doc 04 §4),
// whose resolved day/week DTOs live below. The GET /teacher/home server handler
// and TeacherHomeViewModel were deleted alongside.
// ─────────────────────────────────────────────────────────────────────────────

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
    // Lesson plan for this period's assignment on this day (LESSON_PLANNING_SPEC
    // §7 — Today tab integration). Null when no plan exists; status when one does.
    @SerialName("lesson_plan_id") val lessonPlanId: String? = null,
    @SerialName("lesson_plan_status") val lessonPlanStatus: String? = null, // planned | completed | skipped
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

// T-504: the legacy TeacherClassesResponse / TeacherClassesData / TeacherClassDto
// (the old looping /classes list shape) were DELETED. The canonical class list is
// TeacherClassesV2Response / TeacherClassSummaryDto (see PHASE 5 CLASSES below).

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
// Marks — the canonical gradebook contract (below).
//
// T-305 (DELETE-don't-patch): the legacy free-text marks DTOs
// (TeacherMarksResponse / TeacherMarksData / MarksEntryDto / SubmitMarksRequest /
// MarkScoreDto) that backed the deleted force-publishing `/marks` flow are GONE —
// replaced by the scope-bound, lifecycle-aware models below in the SAME commit
// that removed their consumers (TeacherApi/TeacherRepository(+Impl) + the screen).
// ─────────────────────────────────────────────────────────────────────────────

// ═════════════════════════════════════════════════════════════════════════════
// T-302/T-305 — ASSESSMENT + MARKS LIFECYCLE (the canonical gradebook contract)
// Doc 07 §1.3 (canonical model), §2 (draft→…→published machine, the B-MK-1 fix),
// §3 (scoped create), §5 (validated entry), §6 (history/comparison).
//
// These models REPLACE the legacy free-text marks contract (now DELETED, T-305).
// Everything here is
// scope-bound to the authorizing `assignment_id` (X-1 / D-ASMT-6) — never a
// free-text class/section/subject.
//
// Wire endpoints (Doc 07 §2):
//   POST   /api/v1/teacher/assessments               → create (returns draft)
//   GET    /api/v1/teacher/assessments?assignmentId=&status=
//   GET    /api/v1/teacher/assessments/{id}/marks     → roster + existing marks
//   PUT    /api/v1/teacher/assessments/{id}/marks     → SAVE only (no publish)
//   POST   /api/v1/teacher/assessments/{id}/publish   → explicit publish (+notify)
//   POST   /api/v1/teacher/assessments/{id}/unpublish
//   GET    /api/v1/teacher/assessments/history?assignmentId=
// ═════════════════════════════════════════════════════════════════════════════

/**
 * The five lifecycle states (Doc 07 §2). Save NEVER advances to `published`;
 * only the explicit publish endpoint does (the B-MK-1 fix). `archived` is a
 * soft-retire. Kept as string constants (not an enum) so an unknown server value
 * can't crash deserialization — the UI treats anything unrecognised as `draft`.
 */
object AssessmentStatus {
    const val DRAFT = "draft"
    const val SCHEDULED = "scheduled"
    const val MARKS_PENDING = "marks_pending"
    const val PUBLISHED = "published"
    const val ARCHIVED = "archived"
    val ALL = listOf(DRAFT, SCHEDULED, MARKS_PENDING, PUBLISHED, ARCHIVED)
}

/** The assessment types (Doc 07 §1.3 / D-ASMT-4). */
object AssessmentType {
    const val SCHEDULED = "scheduled"
    const val SURPRISE = "surprise"
    const val ASSIGNMENT = "assignment"
    const val PROJECT = "project"
    const val EXAM = "exam"
    val ALL = listOf(SCHEDULED, SURPRISE, ASSIGNMENT, PROJECT, EXAM)
}

// ── Assessment summary (list rows + create echo) ─────────────────────────────

@Serializable
data class AssessmentListResponse(
    val success: Boolean = true,
    val data: AssessmentListData = AssessmentListData(),
)

@Serializable
data class AssessmentListData(
    val assessments: List<AssessmentDto> = emptyList(),
)

/**
 * One assessment — the scope-bound, typed, lifecycle-aware shape (Doc 07 §1.3).
 * `className`/`section`/`subject` are JOINED for display (X-4: never the source
 * of truth); the truth is `assignmentId`/`classId`/`subjectId`.
 *
 * `enteredCount`/`rosterCount` let the list show "entered 31/38" without a second
 * round-trip; `passMarks` drives below-pass danger-ink coloring (null → disabled).
 */
@Serializable
data class AssessmentDto(
    val id: String,
    @SerialName("assignment_id") val assignmentId: String? = null,
    @SerialName("class_id") val classId: String? = null,
    @SerialName("subject_id") val subjectId: String? = null,
    // Display-only (joined). Carried so a list row needs no extra lookups.
    @SerialName("class_name") val className: String = "",
    val section: String = "",
    val subject: String = "",
    val name: String,
    // scheduled | surprise | assignment | project | exam  (AssessmentType)
    val type: String = AssessmentType.SCHEDULED,
    @SerialName("max_marks") val maxMarks: Int = 100,
    @SerialName("pass_marks") val passMarks: Int? = null,
    @SerialName("exam_date") val examDate: String? = null,    // typed `date` → "YYYY-MM-DD"
    @SerialName("calendar_event_id") val calendarEventId: String? = null,
    // draft | scheduled | marks_pending | published | archived  (AssessmentStatus)
    val status: String = AssessmentStatus.DRAFT,
    @SerialName("published_at") val publishedAt: String? = null,
    // Entry progress for the list ("entered k/n"); server-computed.
    @SerialName("entered_count") val enteredCount: Int = 0,
    @SerialName("roster_count") val rosterCount: Int = 0,
) {
    val isPublished: Boolean get() = status == AssessmentStatus.PUBLISHED
    val isDraft: Boolean get() = status == AssessmentStatus.DRAFT
    /** Pass/fail coloring only makes sense when a pass line is set (M11). */
    val hasPassLine: Boolean get() = passMarks != null
}

// ── Create assessment (scoped) — Doc 07 §3 ───────────────────────────────────

/**
 * Create payload. Scope arrives PRE-FILLED from the launch context (class detail
 * or Gradebook create-with-scope) — never re-picked (F-SHELL-3). `assignmentId`
 * is the authorization the server validates via `requireOwnedAssignment`.
 * Validation (max>0, 0≤pass≤max, name non-empty) is echoed server-side (§3).
 */
@Serializable
data class CreateAssessmentRequestV2(
    @SerialName("assignment_id") val assignmentId: String,
    val name: String,
    val type: String = AssessmentType.SCHEDULED,
    @SerialName("max_marks") val maxMarks: Int,
    @SerialName("pass_marks") val passMarks: Int? = null,
    @SerialName("exam_date") val examDate: String? = null,    // "YYYY-MM-DD"; surprise → today
    // Optional: tie to / create a calendar EXAM event (Doc 07 §4.1, D-ASMT-5).
    @SerialName("calendar_event_id") val calendarEventId: String? = null,
    @SerialName("link_to_calendar") val linkToCalendar: Boolean = false,
)

@Serializable
data class AssessmentCreateResponse(
    val success: Boolean = true,
    val message: String? = null,
    val data: AssessmentDto? = null,
)

// ── Load roster + existing marks — Doc 07 §5 (GET …/{id}/marks) ──────────────

@Serializable
data class MarksLoadResponse(
    val success: Boolean = true,
    val data: MarksLoadDto,
)

/**
 * The marks-entry payload: the assessment header + the roster (from enrollments,
 * roll-ordered) with any already-entered values restored (§5.3 — reopening a
 * `marks_pending` assessment is NOT a blank slate; contrast attendance E3).
 * New students added after creation appear with an empty mark (M4); transferred-
 * out students are absent from the enrollment roster (M5).
 */
@Serializable
data class MarksLoadDto(
    val assessment: AssessmentDto,
    val students: List<MarkEntryDto> = emptyList(),
    // Convenience aggregates so the sticky header needs no client recompute.
    @SerialName("entered_count") val enteredCount: Int = 0,
    @SerialName("roster_count") val rosterCount: Int = 0,
)

/**
 * One student's entry row. `studentId` is the typed FK identity (students.id,
 * D-ASMT-3); `name`/`rollNo` are joined for display. `isAbsent` (AB) is a state
 * distinct from a real 0 (§5.2) and is excluded from averages server-side.
 */
@Serializable
data class MarkEntryDto(
    @SerialName("student_id") val studentId: String,
    val name: String,
    @SerialName("roll_no") val rollNo: String = "",
    val marks: Float? = null,           // null = not yet entered
    @SerialName("is_absent") val isAbsent: Boolean = false,
    val remark: String? = null,
)

// ── Save marks (no publish) — Doc 07 §2/§5 (PUT …/{id}/marks) ────────────────

/**
 * SAVE payload — writes `assessment_marks` and keeps status `marks_pending`
 * (or `draft`). Carries NO publish flag: there is no way to publish from a save
 * (the structural B-MK-1 fix). Per-entry max/≥0 validation is enforced both in
 * the grid and server-side (§5.2).
 */
@Serializable
data class MarksSaveRequest(
    val entries: List<MarkSaveEntryDto> = emptyList(),
)

@Serializable
data class MarkSaveEntryDto(
    @SerialName("student_id") val studentId: String,
    // null when absent or cleared; server clamps to [0, max_marks] and rejects >max.
    val marks: Float? = null,
    @SerialName("is_absent") val isAbsent: Boolean = false,
    val remark: String? = null,
)

@Serializable
data class MarksSaveResponse(
    val success: Boolean = true,
    val message: String? = null,
    val data: MarksSaveResultDto = MarksSaveResultDto(),
)

@Serializable
data class MarksSaveResultDto(
    val saved: Int = 0,
    // Echo the (unchanged) lifecycle status so the UI proves "saved, NOT published".
    val status: String = AssessmentStatus.MARKS_PENDING,
    @SerialName("entered_count") val enteredCount: Int = 0,
    @SerialName("roster_count") val rosterCount: Int = 0,
)

// ── Publish / Unpublish — Doc 07 §2 (the ONLY paths that notify parents) ─────

/**
 * Publish confirmation echo. The UI confirm dialog names the parent-notify count
 * BEFORE this is called ("Publish to {n} parents?"); the response confirms how
 * many were actually notified.
 */
@Serializable
data class PublishResponse(
    val success: Boolean = true,
    val message: String? = null,
    val data: PublishResultDto = PublishResultDto(),
)

@Serializable
data class PublishResultDto(
    val status: String = AssessmentStatus.PUBLISHED,
    @SerialName("published_at") val publishedAt: String? = null,
    // How many parents were notified on publish (0 on unpublish).
    @SerialName("parents_notified") val parentsNotified: Int = 0,
)

// ── History & comparison — Doc 07 §6 (server-aggregated, no client N+1) ──────

@Serializable
data class AssessmentHistoryResponse(
    val success: Boolean = true,
    val data: AssessmentHistoryDto = AssessmentHistoryDto(),
)

/**
 * Server-computed trends for an assignment (Doc 07 §6). Everything here is
 * aggregated server-side (contrast the B-CLS N+1 bugs). `timeline` is the
 * per-assessment class average over the term; `distribution` is the latest
 * (or a selected) assessment's histogram; both feed the Gradebook history view
 * (T-306). Empty → the screen shows "Not enough data yet".
 */
@Serializable
data class AssessmentHistoryDto(
    @SerialName("assignment_id") val assignmentId: String? = null,
    @SerialName("class_name") val className: String = "",
    val subject: String = "",
    // Class average per published assessment, oldest→newest (the timeline/sparkline).
    val timeline: List<AssessmentTrendPointDto> = emptyList(),
    // Histogram buckets for one assessment (difficulty gauge).
    val distribution: List<MarkBucketDto> = emptyList(),
) {
    val hasData: Boolean get() = timeline.isNotEmpty()
}

@Serializable
data class AssessmentTrendPointDto(
    @SerialName("assessment_id") val assessmentId: String,
    val name: String,
    @SerialName("exam_date") val examDate: String? = null,
    @SerialName("max_marks") val maxMarks: Int = 100,
    // Class average over present (non-absent) students; null when nobody entered.
    val average: Float? = null,
    @SerialName("pass_rate") val passRate: Float? = null,   // 0..1, null when no pass line
    @SerialName("entered_count") val enteredCount: Int = 0,
    @SerialName("roster_count") val rosterCount: Int = 0,
)

/** One histogram bucket: "label" e.g. "0–24%", count of students. */
@Serializable
data class MarkBucketDto(
    val label: String,
    val count: Int = 0,
)

// ─────────────────────────────────────────────────────────────────────────────
// Syllabus — chapter/topic coverage scoped to a teacher allocation.
// Backs Teacher portal → Planner › Syllabus.
//
// T-403: the legacy flat DTOs (TeacherSyllabusResponse / TeacherSyllabusData /
// SyllabusUnitDto / UpdateSyllabusRequest) were DELETED here. They backed the
// old class+subject GET/PATCH /syllabus contract, which has been fully replaced
// by the typed, hierarchical, assignment-scoped plane below (T-402/T-403). Their
// only consumers (legacy TeacherApi.getSyllabus/updateSyllabus, the repository
// overrides, and the old ViewModel path) were removed in the same commit.
// ─────────────────────────────────────────────────────────────────────────────

// ── Typed, scoped syllabus — T-402 (Doc 08 §1.2/§2/§3) ───────────────────────
// The template/progress-split contract: a unit list is HIERARCHICAL (chapters
// carry topics), each row carrying its own per-section coverage state. The
// one-tap toggle (PATCH /syllabus/progress) flips is_covered + stamps a typed
// covered_on. Reached PRE-SCOPED by assignmentId (X-1) — no shared picker.

@Serializable
data class SyllabusLoadResponse(
    val success: Boolean = true,
    val data: SyllabusLoadDto = SyllabusLoadDto(),
)

@Serializable
data class SyllabusLoadDto(
    @SerialName("assignment_id") val assignmentId: String? = null,
    @SerialName("class_name") val className: String = "",
    val section: String = "",
    val subject: String = "",
    // Flat, ordered list; each node carries depth + parentId so the screen can
    // render the chapter ▸ topic hierarchy without a second call.
    val units: List<SyllabusNodeDto> = emptyList(),
    @SerialName("covered_count") val coveredCount: Int = 0,
    @SerialName("total_count") val totalCount: Int = 0,
) {
    /** 0..1; 0 when nothing to cover yet (honest, not NaN). */
    val progress: Float get() = if (totalCount == 0) 0f else coveredCount.toFloat() / totalCount
    val hasUnits: Boolean get() = units.isNotEmpty()
}

@Serializable
data class SyllabusNodeDto(
    val id: String,
    @SerialName("parent_id") val parentId: String? = null,
    val title: String,
    val position: Int = 0,
    // 0 = chapter (top-level), 1 = topic. The hierarchy is at most 2 deep (Doc 08).
    val depth: Int = 0,
    @SerialName("is_chapter") val isChapter: Boolean = false,
    @SerialName("is_covered") val isCovered: Boolean = false,
    @SerialName("covered_on") val coveredOn: String? = null,
    val note: String? = null,
)

/** Create a unit (chapter or topic). parentId null → chapter. Fixes B-SYL-1. */
@Serializable
data class CreateSyllabusUnitRequest(
    @SerialName("assignment_id") val assignmentId: String,
    val title: String,
    @SerialName("parent_id") val parentId: String? = null,
)

/** Rename / reorder a unit (edit-mode, deliberate). */
@Serializable
data class UpdateSyllabusUnitRequest(
    val title: String? = null,
    val position: Int? = null,
)

/** The one-tap toggle — idempotent, optimistic; stamps typed covered_on=today. */
@Serializable
data class ToggleSyllabusProgressRequest(
    @SerialName("assignment_id") val assignmentId: String,
    @SerialName("unit_id") val unitId: String,
    @SerialName("is_covered") val isCovered: Boolean,
    // Optional explicit date (edit-mode "covered on a past date", Doc 08 §4);
    // blank/absent → server stamps today.
    @SerialName("covered_on") val coveredOn: String? = null,
    val note: String? = null,
)

@Serializable
data class SyllabusUnitMutationResponse(
    val success: Boolean = true,
    val data: SyllabusNodeDto? = null,
)

// ─────────────────────────────────────────────────────────────────────────────
// Homework — LEGACY list + create REMOVED (T-406, DELETE-don't-patch).
// The old TeacherHomeworkResponse / TeacherHomeworkData / HomeworkDto /
// CreateHomeworkRequest pair (which backed the dead-Assign-button screen, F-HW-1)
// is GONE — replaced field-for-field by the typed lifecycle DTOs below
// (HomeworkListResponse / AssignHomeworkRequest / HomeworkBoardResponse / …).
// ─────────────────────────────────────────────────────────────────────────────

// ─────────────────────────────────────────────────────────────────────────────
// T-405/T-406 (Doc 08 Part B) — the canonical, TYPED HOMEWORK LIFECYCLE.
//   • assign (title/desc/due-date(+time)/allow-late, optional attachments) — the
//     real fix for the dead Assign button (F-HW-1/B-HW-1).
//   • a submissions BOARD that is roster-joined so even NOT-SUBMITTED students
//     appear (B-HW-3/H7), with status columns + counts.
//   • teacher EXTENSION (whole-class or single-student, the "she was sick" case).
//   • the no-submit-past-due rule lives server-side on the student path (D-HW-4).
// Reached PRE-SCOPED by assignmentId (X-1) — no shared picker.
// ─────────────────────────────────────────────────────────────────────────────

/** Homework submission lifecycle states (Doc 08 §5.3). */
object HomeworkSubmissionStatus {
    const val SUBMITTED = "submitted"
    const val LATE = "late"
    const val GRADED = "graded"
    const val NOT_SUBMITTED = "not_submitted"
}

@Serializable
data class HomeworkAttachmentDto(
    val id: String,
    val url: String,
    val filename: String = "",
    val mime: String = "",
    @SerialName("size_bytes") val sizeBytes: Long = 0,
)

/** One homework row in the active list, with scope + live submission ratio. */
@Serializable
data class HomeworkItemDto(
    val id: String,
    @SerialName("assignment_id") val assignmentId: String? = null,
    val title: String,
    val description: String = "",
    @SerialName("class_name") val className: String = "",
    val section: String = "",
    val subject: String = "",
    @SerialName("due_date") val dueDate: String,
    @SerialName("due_time") val dueTime: String? = null,
    @SerialName("allow_late") val allowLate: Boolean = false,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("is_past_due") val isPastDue: Boolean = false,
    @SerialName("submitted_count") val submittedCount: Int = 0,
    @SerialName("late_count") val lateCount: Int = 0,
    @SerialName("graded_count") val gradedCount: Int = 0,
    @SerialName("not_submitted_count") val notSubmittedCount: Int = 0,
    @SerialName("total_count") val totalCount: Int = 0,
    val attachments: List<HomeworkAttachmentDto> = emptyList(),
) {
    /** Submitted+late+graded over total; honest 0 when no roster. */
    val turnedInCount: Int get() = submittedCount + lateCount + gradedCount
}

@Serializable
data class HomeworkListResponse(
    val success: Boolean = true,
    val data: HomeworkListData = HomeworkListData(),
)

@Serializable
data class HomeworkListData(
    val items: List<HomeworkItemDto> = emptyList(),
)

/** Assign homework — scope is pre-filled (assignmentId), no shared picker. */
@Serializable
data class AssignHomeworkRequest(
    @SerialName("assignment_id") val assignmentId: String,
    val title: String,
    val description: String = "",
    @SerialName("due_date") val dueDate: String,
    @SerialName("due_time") val dueTime: String? = null,
    @SerialName("allow_late") val allowLate: Boolean = false,
    // Optional attachments already uploaded (url/filename/mime/size); attach by
    // reference so the assign succeeds even if an upload is retried (H6).
    val attachments: List<AssignHomeworkAttachmentDto> = emptyList(),
)

@Serializable
data class AssignHomeworkAttachmentDto(
    val url: String,
    val filename: String = "",
    val mime: String = "",
    @SerialName("size_bytes") val sizeBytes: Long = 0,
)

@Serializable
data class AssignHomeworkResponse(
    val success: Boolean = true,
    val data: HomeworkItemDto? = null,
)

/** One row on the submissions board — every roster student, submitted or not. */
@Serializable
data class HomeworkSubmissionRowDto(
    @SerialName("student_id") val studentId: String,
    @SerialName("student_code") val studentCode: String = "",
    val name: String,
    @SerialName("roll_no") val rollNo: Int? = null,
    // submitted | late | graded | not_submitted
    val status: String = HomeworkSubmissionStatus.NOT_SUBMITTED,
    @SerialName("submitted_at") val submittedAt: String? = null,
    val grade: String? = null,
    @SerialName("has_extension") val hasExtension: Boolean = false,
    @SerialName("extended_to") val extendedTo: String? = null,
)

@Serializable
data class HomeworkBoardResponse(
    val success: Boolean = true,
    val data: HomeworkBoardData = HomeworkBoardData(),
)

@Serializable
data class HomeworkBoardData(
    @SerialName("homework_id") val homeworkId: String = "",
    val title: String = "",
    @SerialName("class_name") val className: String = "",
    val section: String = "",
    val subject: String = "",
    @SerialName("due_date") val dueDate: String = "",
    @SerialName("due_time") val dueTime: String? = null,
    @SerialName("allow_late") val allowLate: Boolean = false,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("is_past_due") val isPastDue: Boolean = false,
    val rows: List<HomeworkSubmissionRowDto> = emptyList(),
    @SerialName("submitted_count") val submittedCount: Int = 0,
    @SerialName("late_count") val lateCount: Int = 0,
    @SerialName("graded_count") val gradedCount: Int = 0,
    @SerialName("not_submitted_count") val notSubmittedCount: Int = 0,
    @SerialName("total_count") val totalCount: Int = 0,
)

/** Grant an extension: studentId null = whole class; else that one student (H4). */
@Serializable
data class GrantExtensionRequest(
    @SerialName("assignment_id") val assignmentId: String,
    @SerialName("student_id") val studentId: String? = null,
    @SerialName("new_due_date") val newDueDate: String,
    @SerialName("new_due_time") val newDueTime: String? = null,
    val reason: String? = null,
)

/** Mark a student's submission reviewed/graded from the board. */
@Serializable
data class ReviewSubmissionRequest(
    @SerialName("assignment_id") val assignmentId: String,
    val status: String,                       // graded | submitted | late | not_submitted
    val grade: String? = null,
)

@Serializable
data class HomeworkMutationResponse(
    val success: Boolean = true,
    val message: String = "",
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
// Assessments (exams) — legacy exam-selector contract REMOVED (T-305,
// DELETE-don't-patch). The old free-text TeacherAssessmentsResponse /
// TeacherAssessmentsData / TeacherAssessmentDto / CreateAssessmentRequest backed
// the deleted TeacherExamPicker + force-publishing `/assessments` flow; they are
// GONE in the SAME commit that removed their consumers. The canonical scope-bound
// assessment models live in the "T-302/T-305 — ASSESSMENT + MARKS LIFECYCLE"
// block above (AssessmentDto / CreateAssessmentRequestV2 / …).
// ─────────────────────────────────────────────────────────────────────────────

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
// T-602a (Doc 04 §5.14): the teacher's OWN leave workflow (apply + status list).
// Mirror server/.../teacher/TeacherSelfLeaveRouting.kt. DISTINCT from the
// approval-inbox DTOs above — here the teacher is the APPLICANT, so a row has no
// student/class context, just the teacher's own dates/reason/status.
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class CreateTeacherLeaveRequest(
    @SerialName("date_from") val dateFrom: String, // YYYY-MM-DD
    @SerialName("date_to") val dateTo: String,      // YYYY-MM-DD
    val reason: String,
    @SerialName("image_url") val imageUrl: String? = null,
)

@Serializable
data class TeacherSelfLeaveResponse(
    val success: Boolean = true,
    val data: TeacherSelfLeaveDto? = null,
)

@Serializable
data class TeacherSelfLeaveListResponse(
    val success: Boolean = true,
    val data: TeacherSelfLeaveListData = TeacherSelfLeaveListData(),
)

@Serializable
data class TeacherSelfLeaveListData(
    @SerialName("pending_count") val pendingCount: Int = 0,
    val requests: List<TeacherSelfLeaveDto> = emptyList(),
)

@Serializable
data class TeacherSelfLeaveDto(
    val id: String,
    @SerialName("date_from") val dateFrom: String,
    @SerialName("date_to") val dateTo: String,
    val reason: String,
    @SerialName("image_url") val imageUrl: String? = null,
    val status: String, // Pending | Approved | Rejected
)

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

// ─────────────────────────────────────────────────────────────────────────────
// PHASE 5 — CLASSES (Doc 09). The typed, single-aggregated-query class plane.
// These mirror the server DTOs in feature/teacher/TeacherClassesRouting.kt
// field-for-field. The wire path converges from the staged `…-v2` to the
// canonical `/classes[/{id}]` + `/students/{id}` in T-504.
//
// Flag codes (Doc 09 §5): "low_attendance"/"recent_absences"/"failing_trend"
// (danger), "dropping" (warning), "no_data" (neutral). Plain-language labels +
// severity are resolved on the client so the UI is the single styling authority.
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class NextPeriodDto(
    val weekday: Int,
    @SerialName("day_label") val dayLabel: String,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    val room: String = "",
    @SerialName("is_today") val isToday: Boolean = false,
)

// ── T-501 — class list summary ───────────────────────────────────────────────

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

@Serializable
data class TeacherClassesV2Response(
    val success: Boolean = false,
    val message: String? = null,
    val data: TeacherClassesV2Data? = null,
)

// ── T-502 — composite class detail ───────────────────────────────────────────

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
    @SerialName("week_rate") val weekRate: Double? = null,   // 0..1, null = no data
    @SerialName("month_rate") val monthRate: Double? = null, // 0..1, null = no data
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
    @SerialName("attendance_rate") val attendanceRate: Double? = null, // 0..1, null = no data
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

@Serializable
data class ClassDetailResponse(
    val success: Boolean = false,
    val message: String? = null,
    val data: ClassDetailData? = null,
)

// ── T-503 — scoped student profile ───────────────────────────────────────────

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

@Serializable
data class StudentProfileResponse(
    val success: Boolean = false,
    val message: String? = null,
    val data: StudentProfileData? = null,
)

// ─────────────────────────────────────────────────────────────────────────────
// Lesson Planning (LESSON_PLANNING_SPEC.md — P1-20)
//
// Mirrors server DTOs in feature/teacher/TeacherLessonPlanRouting.kt
// field-for-field (Lp* prefix). Assignment-scoped (X-1 ownership pattern).
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class LessonActivityDto(
    val activity: String,
    @SerialName("duration_min") val durationMin: Int = 15,
)

@Serializable
data class LessonPlanDto(
    val id: String,
    @SerialName("assignment_id") val assignmentId: String,
    @SerialName("class_id") val classId: String = "",
    val section: String = "",
    @SerialName("subject_name") val subjectName: String = "",
    @SerialName("curriculum_unit_id") val curriculumUnitId: String? = null,
    @SerialName("curriculum_unit_title") val curriculumUnitTitle: String? = null,
    val title: String,
    val objectives: List<String> = emptyList(),
    val activities: List<LessonActivityDto> = emptyList(),
    val resources: List<String> = emptyList(),
    @SerialName("assessment_method") val assessmentMethod: String? = null,
    @SerialName("duration_minutes") val durationMinutes: Int = 45,
    @SerialName("homework_id") val homeworkId: String? = null,
    @SerialName("planned_date") val plannedDate: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    val status: String = "planned",
    @SerialName("template_source_id") val templateSourceId: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
)

@Serializable
data class LessonPlanListResponse(
    val success: Boolean = true,
    val message: String? = null,
    val data: List<LessonPlanDto> = emptyList(),
)

@Serializable
data class LessonPlanSingleResponse(
    val success: Boolean = true,
    val message: String? = null,
    val data: LessonPlanDto,
)

@Serializable
data class CreateLessonPlanRequest(
    @SerialName("assignment_id") val assignmentId: String,
    @SerialName("curriculum_unit_id") val curriculumUnitId: String? = null,
    val title: String,
    val objectives: List<String> = emptyList(),
    val activities: List<LessonActivityDto> = emptyList(),
    val resources: List<String> = emptyList(),
    @SerialName("assessment_method") val assessmentMethod: String? = null,
    @SerialName("duration_minutes") val durationMinutes: Int = 45,
    @SerialName("homework_id") val homeworkId: String? = null,
    @SerialName("planned_date") val plannedDate: String? = null,
)

@Serializable
data class UpdateLessonPlanRequest(
    @SerialName("curriculum_unit_id") val curriculumUnitId: String? = null,
    val title: String? = null,
    val objectives: List<String>? = null,
    val activities: List<LessonActivityDto>? = null,
    val resources: List<String>? = null,
    @SerialName("assessment_method") val assessmentMethod: String? = null,
    @SerialName("duration_minutes") val durationMinutes: Int? = null,
    @SerialName("homework_id") val homeworkId: String? = null,
    @SerialName("planned_date") val plannedDate: String? = null,
)

@Serializable
data class LessonCalendarDayDto(
    val date: String,
    val plans: List<LessonPlanDto> = emptyList(),
)

@Serializable
data class LessonCalendarDto(
    val month: String,
    val days: List<LessonCalendarDayDto> = emptyList(),
)

@Serializable
data class LessonCalendarResponse(
    val success: Boolean = true,
    val message: String? = null,
    val data: LessonCalendarDto,
)

// ── Templates ────────────────────────────────────────────────────────────────

@Serializable
data class LessonTemplateDto(
    val id: String,
    @SerialName("assignment_id") val assignmentId: String,
    @SerialName("subject_name") val subjectName: String = "",
    val title: String,
    val objectives: List<String> = emptyList(),
    val activities: List<LessonActivityDto> = emptyList(),
    val resources: List<String> = emptyList(),
    @SerialName("assessment_method") val assessmentMethod: String? = null,
    @SerialName("duration_minutes") val durationMinutes: Int = 45,
    @SerialName("is_shared") val isShared: Boolean = false,
)

@Serializable
data class LessonTemplateListResponse(
    val success: Boolean = true,
    val message: String? = null,
    val data: List<LessonTemplateDto> = emptyList(),
)

@Serializable
data class SaveLessonTemplateRequest(
    @SerialName("assignment_id") val assignmentId: String,
    val title: String,
    val objectives: List<String> = emptyList(),
    val activities: List<LessonActivityDto> = emptyList(),
    val resources: List<String> = emptyList(),
    @SerialName("assessment_method") val assessmentMethod: String? = null,
    @SerialName("duration_minutes") val durationMinutes: Int = 45,
    @SerialName("is_shared") val isShared: Boolean = false,
)

@Serializable
data class InstantiateFromTemplateRequest(
    @SerialName("assignment_id") val assignmentId: String,
    @SerialName("planned_date") val plannedDate: String,
)

// ─────────────────────────────────────────────────────────────────────────────
// Read Receipts — teacher 1:1 messaging (mirrors admin/parent message models).
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class TeacherMessageThreadDto(
    val id: String,
    @SerialName("sender_name") val senderName: String,
    @SerialName("sender_role") val senderRole: String,
    @SerialName("last_message") val lastMessage: String,
    val time: String,
    @SerialName("unread_count") val unreadCount: Int = 0,
    @SerialName("sender_image_url") val senderImageUrl: String? = null,
    @SerialName("icon_name") val iconName: String? = null,
    @SerialName("is_read") val isRead: Boolean = true,
)

@Serializable
data class TeacherMessageThreadsData(val threads: List<TeacherMessageThreadDto> = emptyList())

@Serializable
data class TeacherMessageThreadsResponse(
    val success: Boolean = false,
    val data: TeacherMessageThreadsData = TeacherMessageThreadsData(),
)

@Serializable
data class TeacherMessageDto(
    val id: String,
    val body: String,
    @SerialName("is_mine") val isMine: Boolean = false,
    @SerialName("sender_id") val senderId: String? = null,
    @SerialName("created_at") val createdAt: String,
    val time: String,
    val seq: Int? = null,
    val status: String? = null,
    @SerialName("edited_at") val editedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null,
    @SerialName("reply_to_id") val replyToId: String? = null,
)

@Serializable
data class TeacherThreadMessagesData(
    @SerialName("thread_id") val threadId: String,
    @SerialName("sender_name") val senderName: String,
    val messages: List<TeacherMessageDto> = emptyList(),
    @SerialName("has_more") val hasMore: Boolean = false,
    @SerialName("total_count") val totalCount: Long = 0,
)

@Serializable
data class TeacherThreadMessagesResponse(
    val success: Boolean = false,
    val data: TeacherThreadMessagesData? = null,
)

@Serializable
data class TeacherSendMessageRequest(
    @SerialName("thread_id") val threadId: String? = null,
    @SerialName("recipient_user_id") val recipientUserId: String? = null,
    @SerialName("client_msg_id") val clientMsgId: String? = null,
    @SerialName("reply_to_id") val replyToId: String? = null,
    val body: String,
)

@Serializable
data class TeacherSendMessageData(
    @SerialName("thread_id") val threadId: String,
    @SerialName("message_id") val messageId: String,
    val seq: Int? = null,
    @SerialName("server_timestamp") val serverTimestamp: String? = null,
)

@Serializable
data class TeacherSendMessageResponse(
    val success: Boolean = false,
    val message: String? = null,
    val data: TeacherSendMessageData? = null,
)

@Serializable
data class TeacherUnreadCountDto(
    val success: Boolean = false,
    val data: TeacherUnreadCountData? = null,
)

@Serializable
data class TeacherUnreadCountData(
    @SerialName("unread_count") val unreadCount: Int = 0,
)

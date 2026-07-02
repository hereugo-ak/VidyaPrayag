/*
 * File: TeacherDayRouting.kt
 * Module: feature.teacher
 *
 * T-104 (Doc 05 §4) — the keystone "resolved day / resolved week" endpoints that
 * power the Today tab's 3-face schedule card. This is the single place where the
 * recurring weekly timetable (teacher_periods) is merged, FOR A SPECIFIC DATE,
 * with:
 *   - one-off period_exceptions (CANCELLED / RESCHEDULED / ROOM_CHANGE /
 *     SUBSTITUTION / EXTRA)                                 — Doc 05 §3
 *   - the school's published HOLIDAY events                — calendar_events
 *   - the calendar overlay relevant to this teacher        — VP-CAL
 *   - per-period `attendanceMarked` joined from attendance_records (B-HOME-4)
 *   - server-clock authoritative `nowIndex` / `nextIndex`  — Doc 05 §6 "device
 *     clock wrong"
 *
 * Routes (JWT-guarded, scoped via core/TeacherAccess):
 *   GET /api/v1/teacher/day?date=YYYY-MM-DD   → resolved day (date defaults today)
 *   GET /api/v1/teacher/week[?date=YYYY-MM-DD] → resolved Mon..Sat for date's week
 *
 * Why server-resolved (Doc 05 §4):
 *   1. Authoritative clock & timezone (no device drift between phone and school).
 *   2. Holiday / exception merge in ONE place, not duplicated on every client.
 *   3. `attendanceMarked` is REAL (joined), not fabricated (kills B-HOME-4).
 *   4. `assignmentId` carried through → the Today "Mark attendance" CTA is
 *      pre-authorized (resolves to requireOwnedAssignment without a picker).
 *
 * Performance (Closes B-HOME-1): attendance state for the whole day is fetched
 * with ONE batched query over the day's distinct grade strings — not a per-period
 * round-trip. /week resolves all six days inside ONE transaction.
 *
 * Honesty rule: a holiday yields isHoliday=true + no periods; an unseeded
 * timetable yields an empty period list — never a fabricated card (X-5).
 *
 * DTOs are defined server-side (the :server module does NOT depend on :shared)
 * and mirror shared/.../teacher/domain/model/TeacherModels.kt field-for-field.
 *
 * DEVIATION (flagged for commit, Doc 05 §3.3 / Doc 07 §4.1): the doc suggests an
 * EXAM calendar event deep-links to the teacher's matching assessment via
 * `assessment_id`. The current schema has NO link column between calendar_events
 * (EXAM) and assessments (assessments.examId in the API is the assessment's own
 * UUID, not a calendar event_code). Rather than fabricate a fragile name/date
 * heuristic, `CalendarOverlayDto.assessmentId` is returned null here; wiring the
 * real link is deferred to the Gradebook phase (P3) where the assessment↔exam
 * binding is introduced. The field is preserved in the contract.
 */
package com.littlebridge.enrollplus.feature.teacher

import com.littlebridge.enrollplus.core.TeacherContext
import com.littlebridge.enrollplus.core.fail
import com.littlebridge.enrollplus.core.ok
import com.littlebridge.enrollplus.core.requireTeacherContext
import com.littlebridge.enrollplus.core.teacherAssignmentsFor
import com.littlebridge.enrollplus.db.AppUsersTable
import com.littlebridge.enrollplus.db.AssessmentsTable
import com.littlebridge.enrollplus.db.AttendanceRecordsTable
import com.littlebridge.enrollplus.db.CalendarEventsTable
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.HomeworkSubmissionsTable
import com.littlebridge.enrollplus.db.HomeworkTable
import com.littlebridge.enrollplus.db.LessonPlansTable
import com.littlebridge.enrollplus.db.LeaveRequestsTable
import com.littlebridge.enrollplus.db.PeriodExceptionsTable
import com.littlebridge.enrollplus.db.TeacherPeriodsTable
import com.littlebridge.enrollplus.db.SchoolDayConfigTable
import com.littlebridge.enrollplus.db.SchoolDaySlotsTable
import com.littlebridge.enrollplus.db.SYSTEM_SCHOOL_ID
import com.littlebridge.enrollplus.db.TeacherSubjectAssignmentsTable
import com.littlebridge.enrollplus.db.TeacherCheckInsTable
import com.littlebridge.enrollplus.feature.calendar.EventStatus
import com.littlebridge.enrollplus.feature.calendar.EventType
import com.littlebridge.enrollplus.feature.calendar.decodeStringList
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
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

// ─────────────────────────────────────────────────────────────────────────────
// Server-side DTOs — mirror shared/.../teacher/domain/model/TeacherModels.kt
// (ResolvedDayDto / ResolvedPeriodDto / CalendarOverlayDto / ResolvedWeekDto)
// field-for-field. The :server module does not depend on :shared.
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class ResolvedPeriodDto(
    @SerialName("period_id") val periodId: String? = null,
    @SerialName("assignment_id") val assignmentId: String? = null,
    @SerialName("class_name") val className: String,
    val section: String = "",
    val subject: String = "",
    val room: String = "",
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    val status: String = "SCHEDULED",
    @SerialName("attendance_marked") val attendanceMarked: Boolean = false,
    @SerialName("substitute_teacher_name") val substituteTeacherName: String? = null,
    @SerialName("is_substitute_for_me") val isSubstituteForMe: Boolean = false,
    @SerialName("has_overlap") val hasOverlap: Boolean = false,
    val note: String = "",
    @SerialName("lesson_plan_id") val lessonPlanId: String? = null,
    @SerialName("lesson_plan_status") val lessonPlanStatus: String? = null,
)

@Serializable
data class CalendarOverlayDto(
    @SerialName("event_id") val eventId: String,
    val type: String,
    val title: String,
    val audience: String = "ALL_SCHOOL",
    @SerialName("assessment_id") val assessmentId: String? = null,
    @SerialName("class_ref") val classRef: String? = null,
)

@Serializable
data class BellSlotDto(
    @SerialName("slot_index") val slotIndex: Int,
    @SerialName("slot_type") val slotType: String,
    val label: String,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
)

@Serializable
data class ResolvedDayDto(
    val date: String,
    val weekday: Int,
    @SerialName("is_holiday") val isHoliday: Boolean = false,
    @SerialName("holiday_name") val holidayName: String? = null,
    val periods: List<ResolvedPeriodDto> = emptyList(),
    val calendar: List<CalendarOverlayDto> = emptyList(),
    @SerialName("now_index") val nowIndex: Int? = null,
    @SerialName("next_index") val nextIndex: Int? = null,
    @SerialName("bell_schedule") val bellSchedule: List<BellSlotDto> = emptyList(),
)

@Serializable
data class ResolvedWeekDto(
    @SerialName("week_start") val weekStart: String,
    val days: List<ResolvedDayDto> = emptyList(),
)

// ─────────────────────────────────────────────────────────────────────────────
// T-106b — Teacher self check-in (Doc 06 §2). Server-side mirrors of the shared
// CheckInStatusDto / TeacherCheckInRequest (shared/.../teacher/domain/model/
// TeacherModels.kt). The :server module does not depend on :shared.
// `checked_in_at` is the server-stamped ISO timestamp (authoritative clock —
// Doc 06 §2.4). `method` records which rung of the biometric ladder succeeded.
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class TeacherCheckInRequest(
    val method: String,                              // biometric | pin | manual
    @SerialName("device_id") val deviceId: String? = null,
)

@Serializable
data class CheckInStatusDto(
    @SerialName("checked_in") val checkedIn: Boolean = false,
    @SerialName("checked_in_at") val checkedInAt: String? = null, // ISO timestamp, server-stamped
    val method: String? = null,                      // biometric | pin | manual
    val date: String,                                // ISO date the status is for
)

// ─────────────────────────────────────────────────────────────────────────────
// T-107 — Real obligations (Doc 04 §5.5). Server-side mirrors of the shared
// TeacherObligationsDto / ObligationItemDto (shared/.../teacher/domain/model/
// TeacherModels.kt). The :server module does not depend on :shared. Counts are
// REAL — computed from the current schema — not fabricated (kills B-HOME-4):
//   • unmarked_classes        — today's non-cancelled periods missing attendance
//   • unpublished_results     — my active assessments not yet published
//   • submissions_to_review   — homework submissions still "submitted" (ungraded)
//   • pending_leave_decisions — student leave requests routed to me, Pending
// HONESTY (Doc 04 §5.5): "all caught up" is true only when EVERY count is zero;
// the strip never invents work. Counts that depend on phases not yet built
// (full marks lifecycle T-304, attendance-write semantics T-204) ship from what
// the schema can honestly answer today and are tightened as those phases land.
// Each item carries a pre-scoped deep-link target (assignmentId / refId) so the
// UI jumps straight to the scoped tool, never a shared picker.
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class ObligationItemDto(
    val id: String,
    val type: String,                                // attendance | marks | homework | leave
    val title: String,
    val subtitle: String = "",
    val count: Int = 0,
    @SerialName("assignment_id") val assignmentId: String? = null,
    @SerialName("ref_id") val refId: String? = null,
)

@Serializable
data class TeacherObligationsDto(
    @SerialName("unmarked_classes") val unmarkedClasses: Int = 0,
    @SerialName("classes_today_total") val classesTodayTotal: Int = 0,
    @SerialName("unpublished_results") val unpublishedResults: Int = 0,
    @SerialName("submissions_to_review") val submissionsToReview: Int = 0,
    @SerialName("pending_leave_decisions") val pendingLeaveDecisions: Int = 0,
    val items: List<ObligationItemDto> = emptyList(),
)

private val HHMM_DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

// period_exceptions.kind values (Doc 05 §3 / Tables.kt PeriodExceptionsTable)
// Valid teacher check-in methods (Doc 06 §2 biometric ladder).
private val CHECK_IN_METHODS = setOf("biometric", "pin", "manual")

private const val KIND_CANCELLED = "CANCELLED"
private const val KIND_RESCHEDULED = "RESCHEDULED"
private const val KIND_ROOM_CHANGE = "ROOM_CHANGE"
private const val KIND_SUBSTITUTION = "SUBSTITUTION"
private const val KIND_EXTRA = "EXTRA"

// resolved period status value (mirror shared ResolvedPeriodDto.status default)
private const val STATUS_SCHEDULED = "SCHEDULED"

/**
 * The authoritative display scope of a period, resolved assignment-first
 * (Doc 05 §2.1): className/section/subject come from the bound
 * teacher_subject_assignments row, NOT the period's demoted display columns.
 */
private data class PeriodScope(
    val className: String,
    val section: String,
    val subject: String,
)

/**
 * The core resolver (Doc 05 §4). Pure DB reads + in-memory merge for ONE date.
 * Reused by both /day and /week. All queries run inside the single [dbQuery]
 * block supplied by the caller, so /week resolves six days without six
 * transactions.
 *
 * @param assignmentScopes a per-REQUEST cache (className/section/subject keyed by
 *        assignment id), primed by the caller. Threaded as a parameter — NOT a
 *        shared field — so concurrent requests never race (Ktor handles requests
 *        on a shared dispatcher).
 * @param nowProvider server clock for nowIndex/nextIndex; null for days other
 *        than "today" so a non-today resolved day carries no spurious "now".
 */
private data class BellScheduleResult(
    val slots: List<BellSlotDto>,
    val hasConfig: Boolean,
    val isSchoolDay: Boolean,
)

private fun resolveBellSchedule(schoolId: UUID, weekday: Int): BellScheduleResult {
    fun fetchForSchool(sid: UUID): Pair<List<BellSlotDto>, Boolean>? {
        val configs = SchoolDayConfigTable.selectAll()
            .where {
                (SchoolDayConfigTable.schoolId eq sid) and
                    (SchoolDayConfigTable.isActive eq true)
            }
            .toList()
        if (configs.isEmpty()) return null
        val matching = configs.firstOrNull { r ->
            val days = r[SchoolDayConfigTable.applicableDays]
                .split(",").map { it.trim().toIntOrNull() }.filterNotNull()
            weekday in days
        }
        if (matching == null) return emptyList<BellSlotDto>() to false
        val cid = matching[SchoolDayConfigTable.id].value
        val slots = SchoolDaySlotsTable.selectAll()
            .where { (SchoolDaySlotsTable.configId eq cid) and (SchoolDaySlotsTable.schoolId eq sid) }
            .orderBy(SchoolDaySlotsTable.slotIndex)
            .map { s ->
                BellSlotDto(
                    slotIndex = s[SchoolDaySlotsTable.slotIndex],
                    slotType = s[SchoolDaySlotsTable.slotType],
                    label = s[SchoolDaySlotsTable.label],
                    startTime = s[SchoolDaySlotsTable.startTime].format(HHMM_DAY),
                    endTime = s[SchoolDaySlotsTable.endTime].format(HHMM_DAY),
                )
            }
        return slots to true
    }
    val result = fetchForSchool(schoolId) ?: fetchForSchool(SYSTEM_SCHOOL_ID)
    return if (result != null) {
        BellScheduleResult(slots = result.first, hasConfig = true, isSchoolDay = result.second)
    } else {
        BellScheduleResult(slots = emptyList(), hasConfig = false, isSchoolDay = true)
    }
}

private fun resolveDayInTxn(
    ctx: TeacherContext,
    date: LocalDate,
    ownedAssignmentIds: Set<UUID>,
    assignmentScopes: MutableMap<UUID, PeriodScope>,
    nowProvider: LocalTime?,
): ResolvedDayDto {
    val weekday = date.dayOfWeek.value // 1=Mon … 7=Sun (ISO)

    // ── 1. Holiday / calendar overlay (calendar_events, published, active) ──────
    // A published event whose [start_date, end_date] range contains `date`.
    val calendarRows = CalendarEventsTable.selectAll().where {
        (CalendarEventsTable.schoolId eq ctx.schoolId) and
            (CalendarEventsTable.isActive eq true) and
            (CalendarEventsTable.status eq EventStatus.PUBLISHED) and
            (CalendarEventsTable.startDate lessEq date) and
            (CalendarEventsTable.endDate greaterEq date)
    }.toList()

    val holidayRow = calendarRows.firstOrNull { it[CalendarEventsTable.type] == EventType.HOLIDAY }
    val isHoliday = holidayRow != null
    val holidayName = holidayRow?.get(CalendarEventsTable.title)

    val overlay = calendarRows.map { r ->
        CalendarOverlayDto(
            eventId = r[CalendarEventsTable.eventCode],
            type = r[CalendarEventsTable.type],
            title = r[CalendarEventsTable.title],
            audience = r[CalendarEventsTable.audience],
            // DEVIATION (see file header): no calendar_events↔assessments link in
            // the current schema; deep-link wiring deferred to Gradebook (P3).
            assessmentId = null,
            classRef = decodeStringList(r[CalendarEventsTable.classIds]).firstOrNull(),
        )
    }

    // On a holiday we surface the banner + overlay but no periods (Doc 05 §6,
    // Doc 06 holiday edge case: attendance is NOT solicited).
    if (isHoliday) {
        return ResolvedDayDto(
            date = date.toString(),
            weekday = weekday,
            isHoliday = true,
            holidayName = holidayName,
            periods = emptyList(),
            calendar = overlay,
            nowIndex = null,
            nextIndex = null,
        )
    }

    // ── 1b. School day config — bell schedule + applicable-day check (C-2) ────
    val bellResult = resolveBellSchedule(ctx.schoolId, weekday)
    if (bellResult.hasConfig && !bellResult.isSchoolDay) {
        return ResolvedDayDto(
            date = date.toString(),
            weekday = weekday,
            isHoliday = false,
            holidayName = null,
            periods = emptyList(),
            calendar = overlay,
            nowIndex = null,
            nextIndex = null,
            bellSchedule = emptyList(),
        )
    }

    // ── 2. Recurring periods for this weekday, valid on `date`, active ──────────
    // valid_from/valid_to nullable → an open-ended period is always valid
    // (Doc 05 §6 "Day spans term boundary": valid_from/valid_to filter).
    val periodRows = TeacherPeriodsTable.selectAll().where {
        (TeacherPeriodsTable.schoolId eq ctx.schoolId) and
            (TeacherPeriodsTable.teacherId eq ctx.userId) and
            (TeacherPeriodsTable.weekday eq weekday) and
            (TeacherPeriodsTable.isActive eq true) and
            ((TeacherPeriodsTable.validFrom.isNull()) or (TeacherPeriodsTable.validFrom lessEq date)) and
            ((TeacherPeriodsTable.validTo.isNull()) or (TeacherPeriodsTable.validTo greaterEq date))
    }.toList()

    // ── 3. Exceptions for THIS date (Doc 05 §3) ─────────────────────────────────
    //   a) exceptions referencing one of my recurring periods (CANCELLED /
    //      RESCHEDULED / ROOM_CHANGE / SUBSTITUTION) — keyed by period_id;
    //   b) EXTRA periods (period_id null) that are mine for the date — either
    //      authored against one of my assignments, or a SUBSTITUTION where I am
    //      the substitute teacher (so the period appears in MY day — Doc 06 E14).
    val exceptionRows = PeriodExceptionsTable.selectAll().where {
        (PeriodExceptionsTable.schoolId eq ctx.schoolId) and
            (PeriodExceptionsTable.date eq date)
    }.toList()

    val exceptionByPeriod = exceptionRows
        .filter { it[PeriodExceptionsTable.periodId] != null }
        .associateBy { it[PeriodExceptionsTable.periodId]!! }

    val extraRows = exceptionRows.filter { row ->
        row[PeriodExceptionsTable.periodId] == null && (
            (row[PeriodExceptionsTable.assignmentId]?.let { it in ownedAssignmentIds } == true) ||
                (row[PeriodExceptionsTable.kind] == KIND_SUBSTITUTION &&
                    row[PeriodExceptionsTable.substituteTeacherId] == ctx.userId)
        )
    }

    // ── 4. Prime the assignment-scope cache (assignment-first display) ──────────
    val neededAssignmentIds = buildSet {
        periodRows.forEach { it[TeacherPeriodsTable.assignmentId]?.let(::add) }
        extraRows.forEach { it[PeriodExceptionsTable.assignmentId]?.let(::add) }
    }
    val missing = neededAssignmentIds.filter { it !in assignmentScopes }
    if (missing.isNotEmpty()) {
        TeacherSubjectAssignmentsTable.selectAll().where {
            (TeacherSubjectAssignmentsTable.schoolId eq ctx.schoolId) and
                (TeacherSubjectAssignmentsTable.id inList
                    missing.map { EntityID(it, TeacherSubjectAssignmentsTable) })
        }.forEach { row ->
            assignmentScopes[row[TeacherSubjectAssignmentsTable.id].value] = PeriodScope(
                className = row[TeacherSubjectAssignmentsTable.className],
                section = row[TeacherSubjectAssignmentsTable.section],
                subject = row[TeacherSubjectAssignmentsTable.subject],
            )
        }
    }
    fun scopeFor(assignmentId: UUID?): PeriodScope? = assignmentId?.let { assignmentScopes[it] }

    // ── 5. Substitute display names (one batched lookup) ────────────────────────
    val substituteIds = exceptionRows.mapNotNull { it[PeriodExceptionsTable.substituteTeacherId] }.distinct()
    val substituteNames: Map<UUID, String> = if (substituteIds.isEmpty()) {
        emptyMap()
    } else {
        AppUsersTable.selectAll().where { AppUsersTable.id inList substituteIds.map { EntityID(it, AppUsersTable) } }
            .associate { it[AppUsersTable.id].value to it[AppUsersTable.fullName] }
    }

    // ── 6. Build resolved periods (recurring + exceptions applied) ──────────────
    data class Resolved(
        val periodId: UUID?,
        val assignmentId: UUID?,
        val className: String,
        val section: String,
        val subject: String,
        val room: String,
        val start: LocalTime,
        val end: LocalTime,
        val status: String,
        val substituteName: String?,
        val isSubstituteForMe: Boolean,
        val note: String,
    )

    val resolved = mutableListOf<Resolved>()

    for (r in periodRows) {
        val pid = r[TeacherPeriodsTable.id].value
        val asgId = r[TeacherPeriodsTable.assignmentId]
        val scope = scopeFor(asgId)
        val ex = exceptionByPeriod[pid]

        var start = r[TeacherPeriodsTable.startTime]
        var end = r[TeacherPeriodsTable.endTime]
        var room = r[TeacherPeriodsTable.room]
        var status = STATUS_SCHEDULED
        var substituteName: String? = null
        var isSubForMe = false
        var note = ""

        if (ex != null) {
            note = ex[PeriodExceptionsTable.note]
            when (ex[PeriodExceptionsTable.kind]) {
                KIND_CANCELLED -> status = KIND_CANCELLED
                KIND_RESCHEDULED -> {
                    status = KIND_RESCHEDULED
                    ex[PeriodExceptionsTable.newStart]?.let { start = it }
                    ex[PeriodExceptionsTable.newEnd]?.let { end = it }
                    ex[PeriodExceptionsTable.newRoom]?.let { room = it }
                }
                KIND_ROOM_CHANGE -> {
                    status = KIND_ROOM_CHANGE
                    ex[PeriodExceptionsTable.newRoom]?.let { room = it }
                }
                KIND_SUBSTITUTION -> {
                    status = KIND_SUBSTITUTION
                    val subId = ex[PeriodExceptionsTable.substituteTeacherId]
                    substituteName = subId?.let { substituteNames[it] }
                    isSubForMe = subId == ctx.userId
                }
            }
        }

        resolved += Resolved(
            periodId = pid,
            assignmentId = asgId,
            className = scope?.className ?: r[TeacherPeriodsTable.className],
            section = scope?.section ?: r[TeacherPeriodsTable.section],
            subject = scope?.subject ?: r[TeacherPeriodsTable.subject],
            room = room,
            start = start,
            end = end,
            status = status,
            substituteName = substituteName,
            isSubstituteForMe = isSubForMe,
            note = note,
        )
    }

    // EXTRA / substitution-insert periods (no recurring parent) — Doc 05 §3.
    for (r in extraRows) {
        val asgId = r[PeriodExceptionsTable.assignmentId]
        val scope = scopeFor(asgId)
        val start = r[PeriodExceptionsTable.newStart] ?: continue // an extra needs a time
        val end = r[PeriodExceptionsTable.newEnd] ?: start
        val kind = r[PeriodExceptionsTable.kind]
        val subId = r[PeriodExceptionsTable.substituteTeacherId]
        resolved += Resolved(
            periodId = null,
            assignmentId = asgId,
            className = scope?.className ?: "",
            section = scope?.section ?: "",
            subject = scope?.subject ?: "",
            room = r[PeriodExceptionsTable.newRoom] ?: "",
            start = start,
            end = end,
            status = if (kind == KIND_SUBSTITUTION) KIND_SUBSTITUTION else KIND_EXTRA,
            substituteName = subId?.let { substituteNames[it] },
            isSubstituteForMe = subId == ctx.userId,
            note = r[PeriodExceptionsTable.note],
        )
    }

    // Order by start time (then end time) — the timeline order Today renders.
    resolved.sortWith(compareBy({ it.start }, { it.end }))

    // ── 7. attendanceMarked — ONE batched query (kills B-HOME-1 N+1) ────────────
    // attendance_records.grade is "<className>-<section>" (TeacherRoutingTasks).
    // A class counts as "marked" for the date if ANY student row exists for that
    // grade. CANCELLED periods never solicit attendance, so they aren't marked.
    val gradesNeeded = resolved
        .filter { it.status != KIND_CANCELLED && it.className.isNotBlank() }
        .map { "${it.className}-${it.section}" }
        .toSet()
    val markedGrades: Set<String> = if (gradesNeeded.isEmpty()) {
        emptySet()
    } else {
        AttendanceRecordsTable.selectAll().where {
            (AttendanceRecordsTable.schoolId eq ctx.schoolId) and
                (AttendanceRecordsTable.date eq date) and
                (AttendanceRecordsTable.type eq "student") and
                (AttendanceRecordsTable.grade inList gradesNeeded.toList())
        }.mapNotNull { it[AttendanceRecordsTable.grade] }.toSet()
    }

    // ── 8. Overlap detection (data-quality flag, Doc 05 §6) ─────────────────────
    // Flag any active (non-cancelled) period whose time range overlaps another.
    val active = resolved.withIndex().filter { it.value.status != KIND_CANCELLED }
    val overlapped = HashSet<Int>()
    for (i in active.indices) {
        for (j in i + 1 until active.size) {
            val a = active[i].value
            val b = active[j].value
            if (a.start.isBefore(b.end) && b.start.isBefore(a.end)) {
                overlapped += active[i].index
                overlapped += active[j].index
            }
        }
    }

    // ── 8b. Batch-load lesson plans for this date + owned assignments ──────────
    // (LESSON_PLANNING_SPEC §7 — Today tab integration). One query, not per-period.
    val assignmentIdsOnDay = resolved.mapNotNull { it.assignmentId }.toSet()
    val lessonPlansByAssignment = if (assignmentIdsOnDay.isNotEmpty()) {
        LessonPlansTable.selectAll().where {
            (LessonPlansTable.assignmentId inList assignmentIdsOnDay) and
                (LessonPlansTable.plannedDate eq date) and
                (LessonPlansTable.deletedAt.isNull())
        }.associate { row ->
            row[LessonPlansTable.assignmentId] to (row[LessonPlansTable.id].value.toString() to row[LessonPlansTable.status])
        }
    } else {
        emptyMap()
    }

    val periodDtos = resolved.mapIndexed { idx, p ->
        val grade = "${p.className}-${p.section}"
        val lp = p.assignmentId?.let { lessonPlansByAssignment[it] }
        ResolvedPeriodDto(
            periodId = p.periodId?.toString(),
            assignmentId = p.assignmentId?.toString(),
            className = p.className,
            section = p.section,
            subject = p.subject,
            room = p.room,
            startTime = p.start.format(HHMM_DAY),
            endTime = p.end.format(HHMM_DAY),
            status = p.status,
            attendanceMarked = p.status != KIND_CANCELLED && grade in markedGrades,
            substituteTeacherName = p.substituteName,
            isSubstituteForMe = p.isSubstituteForMe,
            hasOverlap = idx in overlapped,
            note = p.note,
            lessonPlanId = lp?.first,
            lessonPlanStatus = lp?.second,
        )
    }

    // ── 9. Authoritative now / next from the server clock (Doc 05 §6) ───────────
    // Only meaningful for "today" (nowProvider non-null). Cancelled periods are
    // skipped for now/next so a struck-through slot is never "current"/"next".
    var nowIndex: Int? = null
    var nextIndex: Int? = null
    if (nowProvider != null) {
        val now = nowProvider
        resolved.forEachIndexed { idx, p ->
            if (p.status == KIND_CANCELLED) return@forEachIndexed
            if (nowIndex == null && !now.isBefore(p.start) && now.isBefore(p.end)) nowIndex = idx
            if (nextIndex == null && now.isBefore(p.start)) nextIndex = idx
        }
    }

    return ResolvedDayDto(
        date = date.toString(),
        weekday = weekday,
        isHoliday = false,
        holidayName = null,
        periods = periodDtos,
        calendar = overlay,
        nowIndex = nowIndex,
        nextIndex = nextIndex,
        bellSchedule = bellResult.slots,
    )
}

fun Route.teacherDayRouting() {
    authenticate("jwt") {
        route("/api/v1/teacher") {

            // ── GET /day?date=YYYY-MM-DD ──────────────────────────────────────
            get("/day") {
                val ctx = call.requireTeacherContext() ?: return@get
                val date = call.request.queryParameters["date"]?.takeIf { it.isNotBlank() }
                    ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                    ?: todayIst()

                val ownedIds = teacherAssignmentsFor(ctx).map { it.assignmentId }.toSet()
                val isToday = date == todayIst()

                val data = dbQuery {
                    resolveDayInTxn(
                        ctx = ctx,
                        date = date,
                        ownedAssignmentIds = ownedIds,
                        assignmentScopes = HashMap(),
                        nowProvider = if (isToday) LocalTime.now(IST_ZONE) else null,
                    )
                }
                call.ok(data, message = "Resolved day loaded")
            }

            // ── GET /week[?date=YYYY-MM-DD] ───────────────────────────────────
            // Resolved Mon..Sat for the week containing `date` (default this week).
            // Powers Today's Face C and Profile → My Schedule (Doc 04 §5.14).
            get("/week") {
                val ctx = call.requireTeacherContext() ?: return@get
                val anchor = call.request.queryParameters["date"]?.takeIf { it.isNotBlank() }
                    ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                    ?: todayIst()
                // Monday of the anchor's ISO week.
                val weekStart = anchor.minusDays((anchor.dayOfWeek.value - 1).toLong())
                val today = todayIst()

                val ownedIds = teacherAssignmentsFor(ctx).map { it.assignmentId }.toSet()

                val days = dbQuery {
                    val scopes = HashMap<UUID, PeriodScope>() // shared across the 6 days
                    (0..5).map { offset -> // Mon..Sat
                        val d = weekStart.plusDays(offset.toLong())
                        resolveDayInTxn(
                            ctx = ctx,
                            date = d,
                            ownedAssignmentIds = ownedIds,
                            assignmentScopes = scopes,
                            nowProvider = if (d == today) LocalTime.now(IST_ZONE) else null,
                        )
                    }
                }

                call.ok(
                    ResolvedWeekDto(weekStart = weekStart.toString(), days = days),
                    message = "Resolved week loaded",
                )
            }

            // ── POST /checkin — teacher self check-in (Doc 06 §2) ─────────────
            // Idempotent per (school, teacher, date): a second POST for the same
            // day returns the EXISTING row (UNIQUE ux_teacher_checkins_unique),
            // it does not duplicate. `checked_in_at` is SERVER-STAMPED with
            // Instant.now() — the device clock is never trusted (Doc 06 §2.4).
            // `method` records which rung of the biometric ladder succeeded
            // (biometric -> PIN -> manual, always-available fallback).
            post("/checkin") {
                val ctx = call.requireTeacherContext() ?: return@post
                val req = runCatching { call.receive<TeacherCheckInRequest>() }.getOrNull()
                if (req == null) {
                    call.fail("Invalid request body", HttpStatusCode.BadRequest, "BAD_REQUEST")
                    return@post
                }
                val method = req.method.trim().lowercase()
                if (method !in CHECK_IN_METHODS) {
                    call.fail(
                        "method must be one of: biometric, pin, manual",
                        HttpStatusCode.BadRequest,
                        "INVALID_METHOD",
                    )
                    return@post
                }
                val date = todayIst() // server date (IST) — check-in is for "today"

                val data = dbQuery {
                    // Idempotency: return existing row if already checked in today.
                    val existing = TeacherCheckInsTable
                        .selectAll()
                        .where {
                            (TeacherCheckInsTable.schoolId eq ctx.schoolId) and
                            (TeacherCheckInsTable.teacherId eq ctx.userId) and
                            (TeacherCheckInsTable.date eq date)
                        }
                        .singleOrNull()
                    if (existing != null) {
                        CheckInStatusDto(
                            checkedIn = true,
                            checkedInAt = existing[TeacherCheckInsTable.checkedInAt].toString(),
                            method = existing[TeacherCheckInsTable.method],
                            date = existing[TeacherCheckInsTable.date].toString(),
                        )
                    } else {
                        val now = Instant.now() // server clock (authoritative)
                        TeacherCheckInsTable.insert {
                            it[TeacherCheckInsTable.schoolId] = ctx.schoolId
                            it[TeacherCheckInsTable.teacherId] = ctx.userId
                            it[TeacherCheckInsTable.date] = date
                            it[TeacherCheckInsTable.checkedInAt] = now
                            it[TeacherCheckInsTable.method] = method
                            it[TeacherCheckInsTable.deviceId] = req.deviceId
                        }
                        CheckInStatusDto(
                            checkedIn = true,
                            checkedInAt = now.toString(),
                            method = method,
                            date = date.toString(),
                        )
                    }
                }
                call.ok(data, message = "Checked in")
            }

            // ── GET /checkin?date=YYYY-MM-DD — today's check-in status ───────
            // Returns checked_in=false when no row exists for (school, teacher,
            // date); checked_in=true + the server-stamped timestamp + method
            // when it does. Powers the Today-tab check-in band's amber→green
            // pill flip (Doc 04 §5.1).
            get("/checkin") {
                val ctx = call.requireTeacherContext() ?: return@get
                val date = call.request.queryParameters["date"]?.takeIf { it.isNotBlank() }
                    ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                    ?: todayIst()

                val data = dbQuery {
                    val row = TeacherCheckInsTable
                        .selectAll()
                        .where {
                            (TeacherCheckInsTable.schoolId eq ctx.schoolId) and
                            (TeacherCheckInsTable.teacherId eq ctx.userId) and
                            (TeacherCheckInsTable.date eq date)
                        }
                        .singleOrNull()
                    if (row != null) {
                        CheckInStatusDto(
                            checkedIn = true,
                            checkedInAt = row[TeacherCheckInsTable.checkedInAt].toString(),
                            method = row[TeacherCheckInsTable.method],
                            date = row[TeacherCheckInsTable.date].toString(),
                        )
                    } else {
                        CheckInStatusDto(checkedIn = false, date = date.toString())
                    }
                }
                call.ok(data, message = "Check-in status loaded")
            }

            // ── GET /obligations — real "what needs me" strip (Doc 04 §5.5) ───
            // T-107. REAL counts, scoped to this teacher's allocation at the
            // QUERY level (the third scoping level, per the constitution):
            //   • unmarked_classes        — today's non-cancelled, attendance-
            //     bearing periods whose attendance isn't marked yet (reuses the
            //     T-104 resolver so the count exactly matches the schedule card);
            //   • unpublished_results     — my active assessments not published;
            //   • submissions_to_review   — homework submissions still in the
            //     "submitted" state (not yet graded) on homework I authored;
            //   • pending_leave_decisions — student leave requests routed to me
            //     (direct teacher_id OR one of my class/section pairs), Pending.
            // HONESTY: every count is a live aggregate; nothing is fabricated and
            // "all caught up" is left for the client to infer ONLY when all are
            // zero (TeacherObligationsDto carries no fake floor). Items deep-link
            // pre-scoped (assignmentId / refId) so taps land on the scoped tool.
            get("/obligations") {
                val ctx = call.requireTeacherContext() ?: return@get
                val today = todayIst()
                val owned = teacherAssignmentsFor(ctx)
                val ownedIds = owned.map { it.assignmentId }.toSet()
                val ownedPairs = owned.map { it.className to it.section }.toSet()
                val privileged = ctx.role == "school_admin" || ctx.role == "admin"

                val data = dbQuery {
                    // ── 1. Unmarked classes today (reuse the authoritative resolver) ──
                    val day = resolveDayInTxn(
                        ctx = ctx,
                        date = today,
                        ownedAssignmentIds = ownedIds,
                        assignmentScopes = HashMap(),
                        nowProvider = LocalTime.now(IST_ZONE),
                    )
                    // Attendance-bearing periods = scheduled/active, not cancelled,
                    // with a real class scope. A holiday yields no periods → zero.
                    val attendancePeriods = day.periods.filter {
                        it.status != KIND_CANCELLED && it.className.isNotBlank()
                    }
                    val classesTodayTotal = attendancePeriods.size
                    val unmarkedPeriods = attendancePeriods.filter { !it.attendanceMarked }
                    val unmarkedClasses = unmarkedPeriods.size

                    // ── 2. Unpublished results — my active, not-yet-published assessments ──
                    // Scoped by authorship (teacher_id) or, for privileged roles, the
                    // whole school. Matches the marks lifecycle (isPublished, RA-43).
                    val assessmentRows = AssessmentsTable.selectAll().where {
                        if (privileged) {
                            (AssessmentsTable.schoolId eq ctx.schoolId) and
                                (AssessmentsTable.isActive eq true) and
                                (AssessmentsTable.isPublished eq false)
                        } else {
                            (AssessmentsTable.schoolId eq ctx.schoolId) and
                                (AssessmentsTable.teacherId eq ctx.userId) and
                                (AssessmentsTable.isActive eq true) and
                                (AssessmentsTable.isPublished eq false)
                        }
                    }.toList()
                    val unpublishedResults = assessmentRows.size

                    // ── 3. Submissions to review — ungraded submissions on my homework ──
                    // First the homework I authored (or all, if privileged), then the
                    // count of submissions still in the "submitted" (ungraded) state.
                    val homeworkRows = HomeworkTable.selectAll().where {
                        if (privileged) {
                            (HomeworkTable.schoolId eq ctx.schoolId) and (HomeworkTable.isActive eq true)
                        } else {
                            (HomeworkTable.schoolId eq ctx.schoolId) and
                                (HomeworkTable.teacherId eq ctx.userId) and
                                (HomeworkTable.isActive eq true)
                        }
                    }.toList()
                    val homeworkIds = homeworkRows.map { it[HomeworkTable.id].value }
                    val submissionsToReview = if (homeworkIds.isEmpty()) {
                        0
                    } else {
                        HomeworkSubmissionsTable.selectAll().where {
                            (HomeworkSubmissionsTable.homeworkId inList homeworkIds) and
                                (HomeworkSubmissionsTable.status eq "submitted")
                        }.count().toInt()
                    }

                    // ── 4. Pending leave decisions — student requests routed to me ──
                    // Mirrors TeacherLeaveRouting's scope: direct teacher_id match OR
                    // one of my (class, section) pairs; privileged sees the school.
                    val leaveRows = LeaveRequestsTable.selectAll().where {
                        (LeaveRequestsTable.schoolId eq ctx.schoolId) and
                            (LeaveRequestsTable.requesterRole eq "student") and
                            (LeaveRequestsTable.status eq "Pending")
                    }.toList()
                    val pendingLeaveRows = leaveRows.filter { row ->
                        if (privileged) return@filter true
                        val direct = row[LeaveRequestsTable.teacherId] == ctx.userId
                        val byClass =
                            (row[LeaveRequestsTable.className] to row[LeaveRequestsTable.section]) in ownedPairs
                        direct || byClass
                    }
                    val pendingLeaveDecisions = pendingLeaveRows.size

                    // ── 5. Build deep-linkable items (only for non-zero work) ──────
                    val items = buildList {
                        // One attendance item per unmarked class, pre-scoped by the
                        // period's assignmentId so the CTA opens the scoped marker.
                        unmarkedPeriods.forEach { p ->
                            val sectionLabel = if (p.section.isBlank()) p.className else "${p.className}-${p.section}"
                            add(
                                ObligationItemDto(
                                    id = "attendance:${p.assignmentId ?: p.periodId ?: sectionLabel}",
                                    type = "attendance",
                                    title = "Mark attendance — $sectionLabel",
                                    subtitle = listOfNotNull(
                                        p.subject.takeIf { it.isNotBlank() },
                                        "${p.startTime}–${p.endTime}",
                                    ).joinToString(" · "),
                                    count = 1,
                                    assignmentId = p.assignmentId,
                                    refId = p.periodId,
                                ),
                            )
                        }
                        // Unpublished results — one item per assessment, deep-linked
                        // by the assessment id (refId) for the Gradebook publish flow.
                        assessmentRows.forEach { a ->
                            val cls = a[AssessmentsTable.className]
                            val sec = a[AssessmentsTable.section]
                            val clsLabel = if (sec.isBlank()) cls else "$cls-$sec"
                            add(
                                ObligationItemDto(
                                    id = "marks:${a[AssessmentsTable.id].value}",
                                    type = "marks",
                                    title = "Publish results — ${a[AssessmentsTable.name]}",
                                    subtitle = listOfNotNull(
                                        clsLabel.takeIf { it.isNotBlank() },
                                        a[AssessmentsTable.subject].takeIf { it.isNotBlank() },
                                    ).joinToString(" · "),
                                    count = 1,
                                    refId = a[AssessmentsTable.id].value.toString(),
                                ),
                            )
                        }
                        // Submissions to review — a single rolled-up item (per-
                        // homework breakdown is the Gradebook/Homework surface's job).
                        if (submissionsToReview > 0) {
                            add(
                                ObligationItemDto(
                                    id = "homework:review",
                                    type = "homework",
                                    title = "Review homework submissions",
                                    subtitle = "$submissionsToReview awaiting grading",
                                    count = submissionsToReview,
                                ),
                            )
                        }
                        // Pending leave decisions — a single rolled-up item routed to
                        // the teacher's leave inbox.
                        if (pendingLeaveDecisions > 0) {
                            add(
                                ObligationItemDto(
                                    id = "leave:pending",
                                    type = "leave",
                                    title = "Decide leave requests",
                                    subtitle = "$pendingLeaveDecisions pending your decision",
                                    count = pendingLeaveDecisions,
                                ),
                            )
                        }
                    }

                    TeacherObligationsDto(
                        unmarkedClasses = unmarkedClasses,
                        classesTodayTotal = classesTodayTotal,
                        unpublishedResults = unpublishedResults,
                        submissionsToReview = submissionsToReview,
                        pendingLeaveDecisions = pendingLeaveDecisions,
                        items = items,
                    )
                }
                call.ok(data, message = "Obligations loaded")
            }
        }
    }
}

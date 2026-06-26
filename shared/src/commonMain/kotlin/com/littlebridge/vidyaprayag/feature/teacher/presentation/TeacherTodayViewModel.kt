package com.littlebridge.vidyaprayag.feature.teacher.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.core.prefs.PreferenceRepository
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.CalendarOverlayDto
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.ResolvedDayDto
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.ResolvedPeriodDto
import com.littlebridge.vidyaprayag.feature.teacher.domain.repository.TeacherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * TeacherTodayViewModel — backs the new Today tab (T-105) by consuming the
 * server-resolved schedule (T-104 `GET /teacher/day` + `GET /teacher/week`).
 *
 * It deliberately does NO client-side clock math to decide "current period":
 * the server's `nowIndex` / `nextIndex` are authoritative (Doc 05 §6, "device
 * clock wrong"). The device clock is used only by the UI to animate the live
 * "now" line — never to re-rank periods. A per-minute UI tick can call
 * [refreshIfStale] to refetch when the minute rolls over.
 *
 * States map cleanly onto VStateHost: loading / error / empty (no periods AND
 * not a holiday → "timetable not set up"). Holiday is a CONTENT state with a
 * distinct banner — not "empty" — so the honest messages stay distinct
 * (Doc 10 §8 / Doc 05 §5.1).
 */
data class TeacherTodayState(
    val isLoading: Boolean = false,
    val error: String? = null,
    // The signed-in teacher's display name, for the greeting bar.
    val teacherName: String = "",
    // Resolved "today" (the default day the card opens on).
    val day: ResolvedDayUi? = null,
    // Resolved week (Mon–Sat) for Face C; loaded lazily alongside the day.
    val week: List<ResolvedDayUi> = emptyList(),
    val weekStart: String = "",
) {
    /** True only when there is genuinely no schedule to show and it isn't a holiday. */
    val isUnseeded: Boolean
        get() = day != null && !day.isHoliday && day.periods.isEmpty()
}

/** A single resolved day for the UI (mirrors the server ResolvedDayDto). */
data class ResolvedDayUi(
    val date: String,
    val weekday: Int,
    val isHoliday: Boolean,
    val holidayName: String?,
    val periods: List<ResolvedPeriodUi>,
    val calendar: List<CalendarOverlayUi>,
    // Authoritative indices into [periods]; -1 when none (mapped from null).
    val nowIndex: Int,
    val nextIndex: Int,
)

/** A single resolved period for the UI. Carries the pre-authorized assignmentId. */
data class ResolvedPeriodUi(
    val periodId: String?,
    val assignmentId: String?,
    val className: String,
    val section: String,
    val subject: String,
    val room: String,
    val startTime: String,
    val endTime: String,
    val status: String,
    val attendanceMarked: Boolean,
    val substituteTeacherName: String?,
    val isSubstituteForMe: Boolean,
    val hasOverlap: Boolean,
    val note: String,
) {
    val isCancelled: Boolean get() = status == "CANCELLED"
    /** "Class-Section" display label, e.g. "7-B". */
    val classLabel: String
        get() = if (section.isBlank()) className else "$className-$section"
}

data class CalendarOverlayUi(
    val eventId: String,
    val type: String,
    val title: String,
    val audience: String,
    val assessmentId: String?,
    val classRef: String?,
)

class TeacherTodayViewModel(
    private val repository: TeacherRepository,
    private val preferenceRepository: PreferenceRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(TeacherTodayState())
    val state: StateFlow<TeacherTodayState> = _state.asStateFlow()

    /** The ISO date the currently-loaded day represents (for stale detection). */
    private var loadedDate: String? = null

    init {
        load()
    }

    /** (Re)load today's resolved day + this week. */
    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val token = preferenceRepository.getUserToken().first()
            if (token == null) {
                _state.update { it.copy(isLoading = false, error = "Not authenticated") }
                return@launch
            }
            // Greeting name (best-effort; blank falls back in the UI).
            val name = preferenceRepository.getUserName().first().orEmpty()
            if (name.isNotBlank()) _state.update { it.copy(teacherName = name) }

            // Day first (the screen's primary content); week is best-effort so a
            // week failure never blocks the Today verdict.
            when (val dayResult = repository.getDay(token, date = null)) {
                is NetworkResult.Success -> {
                    val ui = dayResult.data.data.toUi()
                    loadedDate = ui.date
                    _state.update { it.copy(isLoading = false, error = null, day = ui) }
                }
                is NetworkResult.Error -> {
                    _state.update { it.copy(isLoading = false, error = dayResult.message) }
                    return@launch
                }
                is NetworkResult.ConnectionError -> {
                    _state.update { it.copy(isLoading = false, error = "Connection error") }
                    return@launch
                }
            }

            when (val weekResult = repository.getWeek(token, date = null)) {
                is NetworkResult.Success -> {
                    val w = weekResult.data.data
                    _state.update {
                        it.copy(week = w.days.map { d -> d.toUi() }, weekStart = w.weekStart)
                    }
                }
                // A week failure is non-fatal — Face C simply shows its empty state.
                is NetworkResult.Error, is NetworkResult.ConnectionError ->
                    _state.update { it.copy(week = emptyList()) }
            }
        }
    }

    /**
     * Refetch when the device's notion of "today" has rolled past the loaded
     * day (e.g. the app sat open overnight). Cheap no-op otherwise. The UI's
     * per-minute tick can call this; it does NOT re-rank periods locally.
     */
    fun refreshIfStale(deviceTodayIso: String) {
        if (loadedDate != null && loadedDate != deviceTodayIso) load()
    }
}

private fun ResolvedDayDto.toUi() = ResolvedDayUi(
    date = date,
    weekday = weekday,
    isHoliday = isHoliday,
    holidayName = holidayName,
    periods = periods.map { it.toUi() },
    calendar = calendar.map { it.toUi() },
    nowIndex = nowIndex ?: -1,
    nextIndex = nextIndex ?: -1,
)

private fun ResolvedPeriodDto.toUi() = ResolvedPeriodUi(
    periodId = periodId,
    assignmentId = assignmentId,
    className = className,
    section = section,
    subject = subject,
    room = room,
    startTime = startTime,
    endTime = endTime,
    status = status,
    attendanceMarked = attendanceMarked,
    substituteTeacherName = substituteTeacherName,
    isSubstituteForMe = isSubstituteForMe,
    hasOverlap = hasOverlap,
    note = note,
)

private fun CalendarOverlayDto.toUi() = CalendarOverlayUi(
    eventId = eventId,
    type = type,
    title = title,
    audience = audience,
    assessmentId = assessmentId,
    classRef = classRef,
)

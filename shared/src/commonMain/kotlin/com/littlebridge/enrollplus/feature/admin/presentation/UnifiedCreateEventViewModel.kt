/*
 * File: UnifiedCreateEventViewModel.kt
 * Module: feature.admin.presentation
 *
 * Drives the unified 3-step Create Event screen that replaces both the
 * 7-step CreateEventScreenV2 wizard and the ComposeAnnouncementDialog.
 *
 * All events are announcements under the hood. The "Post as announcement"
 * toggle controls whether the announcement is visible to parents (ON) or
 * hidden as calendar-only (OFF).
 *
 * Steps:
 *   1. What  — type (Holiday/PTM/Event/Update/Reminder), title, description
 *   2. When  — date, schedule toggle (date + time)
 *   3. Who   — audience selector, "Post as announcement" toggle, publish/schedule
 */
package com.littlebridge.enrollplus.feature.admin.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.enrollplus.core.prefs.PreferenceRepository
import com.littlebridge.enrollplus.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UnifiedEventForm(
    val type: String = "Events",
    val title: String = "",
    val description: String = "",
    val date: String = "",
    val isScheduled: Boolean = false,
    val scheduleDate: String = "",
    val scheduleHour: Int = 9,
    val scheduleMinute: String = "00",
    val audienceType: String = "ALL_SCHOOL",
    val audienceTargets: String = "",
    val postAsAnnouncement: Boolean = true,
)

data class UnifiedCreateEventState(
    val step: Int = 1,
    val form: UnifiedEventForm = UnifiedEventForm(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val created: Boolean = false,
) {
    val totalSteps: Int get() = 3

    val canGoNext: Boolean
        get() = when (step) {
            1 -> form.type.isNotBlank() && form.title.isNotBlank() && form.description.isNotBlank()
            2 -> form.date.isNotBlank()
            3 -> {
                val needsTargets = form.audienceType != "ALL_SCHOOL"
                !needsTargets || form.audienceTargets.split(",").map { it.trim() }
                    .filter { it.isNotBlank() }.isNotEmpty()
            }
            else -> true
        }

    val calendarEligible: Boolean
        get() = form.type.equals("Holidays", ignoreCase = true) ||
            form.type.equals("PTM", ignoreCase = true) ||
            form.type.equals("Events", ignoreCase = true)
}

class UnifiedCreateEventViewModel(
    private val announcementsViewModel: SchoolAnnouncementsViewModel,
    private val preferenceRepository: PreferenceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(UnifiedCreateEventState())
    val state: StateFlow<UnifiedCreateEventState> = _state.asStateFlow()

    private val tag = "UnifiedCreateEventVM"

    fun next() {
        val s = _state.value
        if (s.canGoNext && s.step < s.totalSteps) {
            _state.value = s.copy(step = s.step + 1, errorMessage = null)
        }
    }

    fun back() {
        val s = _state.value
        if (s.step > 1) _state.value = s.copy(step = s.step - 1, errorMessage = null)
    }

    private fun update(block: (UnifiedEventForm) -> UnifiedEventForm) {
        _state.value = _state.value.copy(form = block(_state.value.form))
    }

    fun setType(v: String) = update { it.copy(type = v) }
    fun setTitle(v: String) = update { it.copy(title = v) }
    fun setDescription(v: String) = update { it.copy(description = v) }
    fun setDate(v: String) = update { it.copy(date = v) }
    fun setScheduled(v: Boolean) = update { it.copy(isScheduled = v) }
    fun setScheduleDate(v: String) = update { it.copy(scheduleDate = v) }
    fun setScheduleHour(v: Int) = update { it.copy(scheduleHour = v) }
    fun setScheduleMinute(v: String) = update { it.copy(scheduleMinute = v) }
    fun setAudienceType(v: String) = update { it.copy(audienceType = v) }
    fun setAudienceTargets(v: String) = update { it.copy(audienceTargets = v) }
    fun setPostAsAnnouncement(v: Boolean) = update { it.copy(postAsAnnouncement = v) }

    fun publish() {
        val s = _state.value
        val form = s.form
        if (!s.canGoNext) {
            _state.value = s.copy(errorMessage = "Please complete all required fields.")
            return
        }

        val targetList = form.audienceTargets.split(",")
            .map { it.trim() }.filter { it.isNotBlank() }

        val isCalendarOnly = !form.postAsAnnouncement
        val addToCalendar = s.calendarEligible

        _state.value = s.copy(isSaving = true, errorMessage = null)

        if (form.isScheduled) {
            val scheduledAt = "${form.scheduleDate}T${form.scheduleHour.toString().padStart(2, '0')}:${form.scheduleMinute}:00Z"
            announcementsViewModel.scheduleAnnouncement(
                type = form.type,
                title = form.title,
                description = form.description,
                date = form.date,
                scheduledAt = scheduledAt,
                audienceType = form.audienceType,
                audienceValues = targetList,
                addToCalendar = addToCalendar,
                isCalendarOnly = isCalendarOnly,
                onCreated = {
                    _state.value = _state.value.copy(isSaving = false, created = true)
                }
            )
        } else {
            announcementsViewModel.createAnnouncement(
                type = form.type,
                title = form.title,
                description = form.description,
                date = form.date,
                audienceType = form.audienceType,
                audienceValues = targetList,
                addToCalendar = addToCalendar,
                isCalendarOnly = isCalendarOnly,
                onCreated = {
                    _state.value = _state.value.copy(isSaving = false, created = true)
                }
            )
        }

        // Observe error from announcementsViewModel
        viewModelScope.launch {
            announcementsViewModel.state.collect { annState ->
                if (annState.errorMessage != null && _state.value.isSaving) {
                    _state.value = _state.value.copy(isSaving = false, errorMessage = annState.errorMessage)
                    announcementsViewModel.clearMessages()
                }
            }
        }
    }

    fun reset() {
        _state.value = UnifiedCreateEventState()
    }

    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }
}

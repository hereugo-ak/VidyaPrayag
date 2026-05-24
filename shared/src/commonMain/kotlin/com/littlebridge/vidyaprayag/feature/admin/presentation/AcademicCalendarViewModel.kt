package com.littlebridge.vidyaprayag.feature.admin.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.core.prefs.PreferenceRepository
import com.littlebridge.vidyaprayag.feature.admin.domain.model.CalendarEventDto
import com.littlebridge.vidyaprayag.feature.admin.domain.repository.CalendarRepository
import com.littlebridge.vidyaprayag.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SyllabusTarget(
    val id: String,
    val subject: String,
    val chapter: String,
    val deadline: String,
    val progress: Float,
    val status: String // "TARGET SET", "UPCOMING", "PENDING"
)

data class AcademicCalendarState(
    val currentMonth: String = "",
    val workingDays: Int = 0,
    val holidays: Int = 0,
    val conflicts: Int = 0,
    val calendarEvents: List<CalendarEventDto> = emptyList(),
    val syllabusTargets: List<SyllabusTarget> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class AcademicCalendarViewModel(
    private val calendarRepository: CalendarRepository,
    private val preferenceRepository: PreferenceRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AcademicCalendarState())
    val state: StateFlow<AcademicCalendarState> = _state.asStateFlow()

    init {
        loadCalendar()
    }

    fun loadCalendar(date: String? = null, viewType: String = "month") {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)

            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                AppLogger.d("AcademicCalendarVM", "No auth token; skipping load")
                _state.value = _state.value.copy(isLoading = false)
                return@launch
            }

            when (val result = calendarRepository.getCalendar(token, date, viewType)) {
                is NetworkResult.Success -> {
                    val data = result.data.data
                    val summary = data?.summary
                    _state.value = _state.value.copy(
                        calendarEvents = data?.calendarEvents ?: emptyList(),
                        workingDays = summary?.workingDays ?: 0,
                        holidays = (summary?.publicHolidays ?: 0) + (summary?.schoolHolidays ?: 0),
                        conflicts = 0,   // server doesn't return conflicts count yet
                        isLoading = false
                    )
                }
                is NetworkResult.Error -> {
                    AppLogger.e("AcademicCalendarVM", "getCalendar error: ${result.message}")
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
                is NetworkResult.ConnectionError -> {
                    AppLogger.e("AcademicCalendarVM", "getCalendar connection error")
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = "Connection error. Check your internet."
                    )
                }
            }
        }
    }
}

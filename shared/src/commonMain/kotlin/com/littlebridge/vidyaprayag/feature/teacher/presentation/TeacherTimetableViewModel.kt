package com.littlebridge.vidyaprayag.feature.teacher.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.core.prefs.PreferenceRepository
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.TeacherTimetableEntry
import com.littlebridge.vidyaprayag.feature.teacher.domain.repository.TeacherRepository
import com.littlebridge.vidyaprayag.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class TeacherTimetableState(
    val selectedDay: String = "Mon",
    val entries: List<TeacherTimetableEntry> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class TeacherTimetableViewModel(
    private val repository: TeacherRepository,
    private val prefs: PreferenceRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TeacherTimetableState())
    val state: StateFlow<TeacherTimetableState> = _state.asStateFlow()

    init { selectDay("Mon") }

    fun selectDay(day: String) {
        _state.value = _state.value.copy(selectedDay = day, isLoading = true, error = null)
        viewModelScope.launch {
            val token = prefs.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isLoading = false, error = "Not signed in")
                return@launch
            }
            when (val result = repository.getTimetable(token, day)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(
                        entries = result.data.entries,
                        isLoading = false
                    )
                }
                is NetworkResult.Error -> {
                    AppLogger.e("TeacherTimetableVM", "getTimetable failed: ${result.message}")
                    _state.value = _state.value.copy(isLoading = false, error = result.message)
                }
                is NetworkResult.ConnectionError -> {
                    _state.value = _state.value.copy(isLoading = false, error = "Connection error")
                }
            }
        }
    }

    fun clearError() { _state.value = _state.value.copy(error = null) }
}

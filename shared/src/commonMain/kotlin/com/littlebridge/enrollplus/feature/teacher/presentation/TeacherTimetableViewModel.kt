package com.littlebridge.enrollplus.feature.teacher.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.feature.admin.domain.model.ChangeRequestListResponse
import com.littlebridge.enrollplus.feature.admin.domain.model.CreateChangeRequestRequest
import com.littlebridge.enrollplus.feature.admin.domain.model.TimetableChangeRequestDto
import com.littlebridge.enrollplus.feature.teacher.domain.model.ResolvedDayDto
import com.littlebridge.enrollplus.feature.teacher.domain.model.TeacherClassSummaryDto
import com.littlebridge.enrollplus.feature.teacher.domain.repository.TeacherRepository
import com.littlebridge.enrollplus.core.prefs.PreferenceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TeacherTimetableState(
    val week: List<ResolvedDayDto> = emptyList(),
    val weekStart: String = "",
    val assignments: List<TeacherClassSummaryDto> = emptyList(),
    val changeRequests: List<TimetableChangeRequestDto> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
)

class TeacherTimetableViewModel(
    private val repository: TeacherRepository,
    private val preferenceRepository: PreferenceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TeacherTimetableState())
    val state: StateFlow<TeacherTimetableState> = _state.asStateFlow()

    init {
        loadWeek()
        loadAssignments()
        loadChangeRequests()
    }

    fun loadWeek() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.update { it.copy(isLoading = false, errorMessage = "Not authenticated") }
                return@launch
            }
            when (val result = repository.getWeek(token, date = null)) {
                is NetworkResult.Success -> {
                    val w = result.data.data
                    _state.update {
                        it.copy(isLoading = false, week = w.days, weekStart = w.weekStart)
                    }
                }
                is NetworkResult.Error -> {
                    _state.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
                is NetworkResult.ConnectionError -> {
                    _state.update { it.copy(isLoading = false, errorMessage = "Connection error") }
                }
            }
        }
    }

    fun loadAssignments() {
        viewModelScope.launch {
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) return@launch
            when (val result = repository.listClassesV2(token)) {
                is NetworkResult.Success -> {
                    val classes = result.data.data?.classes ?: emptyList()
                    _state.update { it.copy(assignments = classes) }
                }
                is NetworkResult.Error -> {}
                is NetworkResult.ConnectionError -> {}
            }
        }
    }

    fun loadChangeRequests() {
        viewModelScope.launch {
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) return@launch
            when (val result = repository.getTimetableChangeRequests(token)) {
                is NetworkResult.Success -> {
                    val reqs = result.data.requests
                    _state.update { it.copy(changeRequests = reqs) }
                }
                is NetworkResult.Error -> {}
                is NetworkResult.ConnectionError -> {}
            }
        }
    }

    fun submitChangeRequest(
        kind: String,
        assignmentId: String?,
        periodId: String?,
        weekday: Int,
        startTime: String,
        endTime: String,
        room: String,
        reason: String,
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null, infoMessage = null) }
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.update { it.copy(isSaving = false, errorMessage = "Not authenticated") }
                return@launch
            }
            val request = CreateChangeRequestRequest(
                assignmentId = assignmentId,
                periodId = periodId,
                kind = kind,
                weekday = weekday,
                startTime = startTime,
                endTime = endTime,
                room = room,
                reason = reason,
            )
            when (val result = repository.submitTimetableChangeRequest(token, request)) {
                is NetworkResult.Success -> {
                    _state.update {
                        it.copy(isSaving = false, infoMessage = "Change request submitted for admin review")
                    }
                    loadChangeRequests()
                }
                is NetworkResult.Error -> {
                    _state.update { it.copy(isSaving = false, errorMessage = result.message) }
                }
                is NetworkResult.ConnectionError -> {
                    _state.update { it.copy(isSaving = false, errorMessage = "Connection error") }
                }
            }
        }
    }

    fun clearMessages() {
        _state.update { it.copy(errorMessage = null, infoMessage = null) }
    }
}

/*
 * File: TeacherPewsViewModel.kt
 * Module: feature.pews.presentation
 *
 * Drives the teacher "My at-risk students" screen — own-class scoped
 * (GET /api/v1/teacher/pews/students) plus the teacher's own interventions
 * (GET /api/v1/teacher/pews/interventions) and updating one
 * (PATCH /api/v1/teacher/pews/interventions/{id}).
 *
 * A teacher only ever sees students in classes assigned to them (enforced
 * server-side) and only their own interventions.
 */
package com.littlebridge.enrollplus.feature.pews.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.core.prefs.PreferenceRepository
import com.littlebridge.enrollplus.feature.pews.domain.model.PewsInterventionDto
import com.littlebridge.enrollplus.feature.pews.domain.model.PewsStudentDto
import com.littlebridge.enrollplus.feature.pews.domain.model.UpdateInterventionRequest
import com.littlebridge.enrollplus.feature.pews.domain.repository.PewsRepository
import com.littlebridge.enrollplus.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class TeacherPewsState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val students: List<PewsStudentDto> = emptyList(),
    val interventions: List<PewsInterventionDto> = emptyList(),
    val updatingIds: Set<String> = emptySet(),
    val infoMessage: String? = null,
) {
    val isEmpty: Boolean get() = !isLoading && error == null && students.isEmpty()
}

class TeacherPewsViewModel(
    private val repository: PewsRepository,
    private val preferenceRepository: PreferenceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TeacherPewsState())
    val state: StateFlow<TeacherPewsState> = _state.asStateFlow()

    init { load() }

    private suspend fun token(): String? = preferenceRepository.getUserToken().first()

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val t = token()
            if (t.isNullOrBlank()) {
                _state.value = _state.value.copy(isLoading = false, error = "You are not signed in. Please log in again.")
                return@launch
            }
            when (val r = repository.getTeacherStudents(t)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(isLoading = false, error = null, students = r.data.data.orEmpty())
                    loadInterventions(t)
                }
                is NetworkResult.Error -> {
                    AppLogger.e("TeacherPewsVM", "getTeacherStudents error: ${r.message}")
                    _state.value = _state.value.copy(isLoading = false, error = r.message)
                }
                is NetworkResult.ConnectionError ->
                    _state.value = _state.value.copy(isLoading = false, error = "Connection error. Check your internet.")
            }
        }
    }

    private suspend fun loadInterventions(t: String) {
        when (val r = repository.getTeacherInterventions(t, status = null)) {
            is NetworkResult.Success -> _state.value = _state.value.copy(interventions = r.data.data.orEmpty())
            else -> { /* non-fatal */ }
        }
    }

    fun updateIntervention(
        interventionId: String,
        status: String? = null,
        notes: String? = null,
        outcome: String? = null,
        actionType: String? = null,
    ) {
        viewModelScope.launch {
            val t = token() ?: run {
                _state.value = _state.value.copy(error = "You are not signed in. Please log in again.")
                return@launch
            }
            _state.value = _state.value.copy(updatingIds = _state.value.updatingIds + interventionId)
            val req = UpdateInterventionRequest(status = status, notes = notes, outcome = outcome, actionType = actionType)
            when (val r = repository.updateTeacherIntervention(t, interventionId, req)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(
                        updatingIds = _state.value.updatingIds - interventionId,
                        infoMessage = "Intervention updated",
                    )
                    loadInterventions(t)
                }
                is NetworkResult.Error -> {
                    AppLogger.e("TeacherPewsVM", "updateTeacherIntervention error: ${r.message}")
                    _state.value = _state.value.copy(updatingIds = _state.value.updatingIds - interventionId, error = r.message)
                }
                is NetworkResult.ConnectionError ->
                    _state.value = _state.value.copy(updatingIds = _state.value.updatingIds - interventionId, error = "Connection error. Check your internet.")
            }
        }
    }

    fun clearMessages() {
        _state.value = _state.value.copy(infoMessage = null)
    }
}

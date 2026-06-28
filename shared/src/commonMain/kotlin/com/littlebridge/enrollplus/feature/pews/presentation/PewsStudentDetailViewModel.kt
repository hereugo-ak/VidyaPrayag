/*
 * File: PewsStudentDetailViewModel.kt
 * Module: feature.pews.presentation
 *
 * Drives the school-admin PEWS student detail screen
 * (GET /api/v1/school/pews/student/{code}) — current snapshot + history, the
 * deterministic signal bundle, and the (nullable) AI explanation. Also surfaces
 * the open interventions for this student and lets the admin update one
 * (PATCH .../interventions/{id}).
 *
 * Honesty: the AI narrative/cause/recommendation are shown ONLY when present;
 * when null the screen still shows the real signals — it never fabricates a
 * reason or a recommendation (RA-S10 / LAW 6).
 */
package com.littlebridge.enrollplus.feature.pews.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.core.prefs.PreferenceRepository
import com.littlebridge.enrollplus.feature.pews.domain.model.PewsInterventionDto
import com.littlebridge.enrollplus.feature.pews.domain.model.PewsStudentDetailDto
import com.littlebridge.enrollplus.feature.pews.domain.model.UpdateInterventionRequest
import com.littlebridge.enrollplus.feature.pews.domain.repository.PewsRepository
import com.littlebridge.enrollplus.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class PewsStudentDetailState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val detail: PewsStudentDetailDto? = null,
    // open interventions for this student (filtered client-side from the school list)
    val interventions: List<PewsInterventionDto> = emptyList(),
    val updatingIds: Set<String> = emptySet(),
    val infoMessage: String? = null,
) {
    val isEmpty: Boolean get() = !isLoading && error == null && detail?.current == null
}

class PewsStudentDetailViewModel(
    private val repository: PewsRepository,
    private val preferenceRepository: PreferenceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PewsStudentDetailState())
    val state: StateFlow<PewsStudentDetailState> = _state.asStateFlow()

    private var studentCode: String? = null

    private suspend fun token(): String? = preferenceRepository.getUserToken().first()

    /** Call once with the student code (e.g. from the cohort row tap). */
    fun load(code: String) {
        studentCode = code
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val t = token()
            if (t.isNullOrBlank()) {
                _state.value = _state.value.copy(isLoading = false, error = "You are not signed in. Please log in again.")
                return@launch
            }
            when (val r = repository.getStudent(t, code)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(isLoading = false, error = null, detail = r.data.data)
                    loadInterventionsFor(t, code)
                }
                is NetworkResult.Error -> {
                    AppLogger.e("PewsStudentVM", "getStudent error: ${r.message}")
                    _state.value = _state.value.copy(isLoading = false, error = r.message)
                }
                is NetworkResult.ConnectionError ->
                    _state.value = _state.value.copy(isLoading = false, error = "Connection error. Check your internet.")
            }
        }
    }

    private suspend fun loadInterventionsFor(t: String, code: String) {
        when (val r = repository.getInterventions(t, status = null)) {
            is NetworkResult.Success ->
                _state.value = _state.value.copy(
                    interventions = r.data.data.orEmpty().filter { it.studentCode == code }
                )
            else -> { /* non-fatal: detail still renders without the intervention list */ }
        }
    }

    /** Update an intervention (status / notes / outcome / action type). */
    fun updateIntervention(
        interventionId: String,
        status: String? = null,
        notes: String? = null,
        outcome: String? = null,
        actionType: String? = null,
    ) {
        val code = studentCode ?: return
        viewModelScope.launch {
            val t = token() ?: run {
                _state.value = _state.value.copy(error = "You are not signed in. Please log in again.")
                return@launch
            }
            _state.value = _state.value.copy(updatingIds = _state.value.updatingIds + interventionId)
            val req = UpdateInterventionRequest(status = status, notes = notes, outcome = outcome, actionType = actionType)
            when (val r = repository.updateIntervention(t, interventionId, req)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(
                        updatingIds = _state.value.updatingIds - interventionId,
                        infoMessage = "Intervention updated",
                    )
                    loadInterventionsFor(t, code)
                }
                is NetworkResult.Error -> {
                    AppLogger.e("PewsStudentVM", "updateIntervention error: ${r.message}")
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

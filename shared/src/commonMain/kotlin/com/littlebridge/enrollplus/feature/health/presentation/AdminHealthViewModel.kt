package com.littlebridge.enrollplus.feature.health.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.core.prefs.PreferenceRepository
import com.littlebridge.enrollplus.feature.health.domain.model.*
import com.littlebridge.enrollplus.feature.health.domain.repository.HealthRepository
import com.littlebridge.enrollplus.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class AdminHealthState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val profile: HealthProfileDto? = null,
    val immunizations: List<ImmunizationDto> = emptyList(),
    val incidents: List<HealthIncidentDto> = emptyList(),
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val infoMessage: String? = null,
)

class AdminHealthViewModel(
    private val repository: HealthRepository,
    private val prefs: PreferenceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AdminHealthState())
    val state: StateFlow<AdminHealthState> = _state.asStateFlow()

    private var loadedStudentId: String? = null

    fun load(studentId: String) {
        loadedStudentId = studentId
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val token = prefs.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isLoading = false, error = "You are not signed in. Please log in again.")
                return@launch
            }
            val profileResult = repository.getHealthProfile(token, studentId)
            val immResult = repository.getImmunizations(token, studentId)
            val incidentResult = repository.getIncidents(token, studentId)

            val profile = (profileResult as? NetworkResult.Success)?.data?.data
            val imm = (immResult as? NetworkResult.Success)?.data?.data?.immunizations.orEmpty()
            val inc = (incidentResult as? NetworkResult.Success)?.data?.data?.incidents.orEmpty()

            val error = when {
                profileResult is NetworkResult.Error -> profileResult.message
                profileResult is NetworkResult.ConnectionError -> "Connection error. Check your internet."
                else -> null
            }

            _state.value = _state.value.copy(
                isLoading = false,
                error = error,
                profile = profile,
                immunizations = imm,
                incidents = inc,
            )
        }
    }

    fun saveProfile(studentId: String, request: UpsertHealthProfileRequest) {
        viewModelScope.launch {
            val token = prefs.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(saveError = "You are not signed in.")
                return@launch
            }
            _state.value = _state.value.copy(isSaving = true, saveError = null, infoMessage = null)
            when (val r = repository.upsertHealthProfile(token, studentId, request)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        profile = r.data.data,
                        infoMessage = "Health profile saved",
                    )
                }
                is NetworkResult.Error -> {
                    AppLogger.e("AdminHealthVM", "saveProfile error: ${r.message}")
                    _state.value = _state.value.copy(isSaving = false, saveError = r.message)
                }
                is NetworkResult.ConnectionError -> {
                    _state.value = _state.value.copy(isSaving = false, saveError = "Connection error. Check your internet.")
                }
            }
        }
    }

    fun addImmunization(request: AddImmunizationRequest) {
        viewModelScope.launch {
            val token = prefs.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(saveError = "You are not signed in.")
                return@launch
            }
            _state.value = _state.value.copy(isSaving = true, saveError = null, infoMessage = null)
            when (val r = repository.addImmunization(token, request)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        infoMessage = "Immunization record added",
                    )
                    loadedStudentId?.let { load(it) }
                }
                is NetworkResult.Error -> {
                    _state.value = _state.value.copy(isSaving = false, saveError = r.message)
                }
                is NetworkResult.ConnectionError -> {
                    _state.value = _state.value.copy(isSaving = false, saveError = "Connection error. Check your internet.")
                }
            }
        }
    }

    fun logIncident(request: LogIncidentRequest) {
        viewModelScope.launch {
            val token = prefs.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(saveError = "You are not signed in.")
                return@launch
            }
            _state.value = _state.value.copy(isSaving = true, saveError = null, infoMessage = null)
            when (val r = repository.logIncident(token, request)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        infoMessage = "Health incident logged",
                    )
                    loadedStudentId?.let { load(it) }
                }
                is NetworkResult.Error -> {
                    _state.value = _state.value.copy(isSaving = false, saveError = r.message)
                }
                is NetworkResult.ConnectionError -> {
                    _state.value = _state.value.copy(isSaving = false, saveError = "Connection error. Check your internet.")
                }
            }
        }
    }

    fun markNotified(incidentId: String) {
        viewModelScope.launch {
            val token = prefs.getUserToken().first()
            if (token.isNullOrBlank()) return@launch
            when (val r = repository.markIncidentNotified(token, incidentId)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(
                        infoMessage = "Parent marked as notified",
                        incidents = _state.value.incidents.map {
                            if (it.id == incidentId) it.copy(parentNotified = true) else it
                        },
                    )
                }
                is NetworkResult.Error -> {
                    _state.value = _state.value.copy(saveError = r.message)
                }
                is NetworkResult.ConnectionError -> {
                    _state.value = _state.value.copy(saveError = "Connection error.")
                }
            }
        }
    }

    fun clearMessages() {
        _state.value = _state.value.copy(saveError = null, infoMessage = null)
    }
}

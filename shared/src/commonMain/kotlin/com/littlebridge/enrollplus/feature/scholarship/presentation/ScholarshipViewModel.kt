package com.littlebridge.enrollplus.feature.scholarship.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.core.prefs.PreferenceRepository
import com.littlebridge.enrollplus.feature.scholarship.domain.model.*
import com.littlebridge.enrollplus.feature.scholarship.domain.repository.ScholarshipRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScholarshipScreenState(
    val isLoading: Boolean = false,
    // Admin
    val schemes: List<ScholarshipScheme> = emptyList(),
    val applications: List<ScholarshipApplication> = emptyList(),
    val renewals: List<ScholarshipRenewal> = emptyList(),
    val selectedApplication: ScholarshipApplication? = null,
    // Parent
    val parentScholarships: List<ScholarshipScheme> = emptyList(),
    val parentApplications: List<ScholarshipApplication> = emptyList(),
    val gamification: GamificationData = GamificationData(0, 0, 1, 0, 0, 0.0),
    // Common
    val error: String? = null,
    val infoMessage: String? = null,
)

class ScholarshipViewModel(
    private val repository: ScholarshipRepository,
    private val prefs: PreferenceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ScholarshipScreenState())
    val state: StateFlow<ScholarshipScreenState> = _state.asStateFlow()

    private suspend fun token(): String = prefs.getUserToken().first() ?: ""

    // ── Admin: Schemes ───────────────────────────────────────────────────

    fun loadSchemes(all: Boolean = false) {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.listSchemes(token(), all)) {
                is NetworkResult.Success -> _state.update { it.copy(isLoading = false, schemes = result.data.data ?: emptyList()) }
                is NetworkResult.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isLoading = false, error = "Connection error") }
            }
        }
    }

    fun createScheme(request: CreateSchemeRequest) {
        _state.update { it.copy(isLoading = true, error = null, infoMessage = null) }
        viewModelScope.launch {
            when (val result = repository.createScheme(token(), request)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isLoading = false, infoMessage = "Scheme created") }
                    loadSchemes()
                }
                is NetworkResult.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isLoading = false, error = "Connection error") }
            }
        }
    }

    fun updateScheme(schemeId: String, request: UpdateSchemeRequest) {
        viewModelScope.launch {
            when (val result = repository.updateScheme(token(), schemeId, request)) {
                is NetworkResult.Success -> { _state.update { it.copy(infoMessage = "Scheme updated") }; loadSchemes() }
                is NetworkResult.Error -> _state.update { it.copy(error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(error = "Connection error") }
            }
        }
    }

    fun deleteScheme(schemeId: String) {
        viewModelScope.launch {
            when (val result = repository.deleteScheme(token(), schemeId)) {
                is NetworkResult.Success -> { _state.update { it.copy(infoMessage = "Scheme deactivated") }; loadSchemes() }
                is NetworkResult.Error -> _state.update { it.copy(error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(error = "Connection error") }
            }
        }
    }

    // ── Admin: Applications ──────────────────────────────────────────────

    fun loadApplications(status: String? = null) {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.listApplications(token(), status)) {
                is NetworkResult.Success -> _state.update { it.copy(isLoading = false, applications = result.data.data ?: emptyList()) }
                is NetworkResult.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isLoading = false, error = "Connection error") }
            }
        }
    }

    fun approveApplication(applicationId: String, request: ApproveApplicationRequest) {
        _state.update { it.copy(isLoading = true, error = null, infoMessage = null) }
        viewModelScope.launch {
            when (val result = repository.approveApplication(token(), applicationId, request)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isLoading = false, infoMessage = "Application approved") }
                    loadApplications()
                }
                is NetworkResult.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isLoading = false, error = "Connection error") }
            }
        }
    }

    fun rejectApplication(applicationId: String, request: RejectApplicationRequest) {
        _state.update { it.copy(isLoading = true, error = null, infoMessage = null) }
        viewModelScope.launch {
            when (val result = repository.rejectApplication(token(), applicationId, request)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isLoading = false, infoMessage = "Application rejected") }
                    loadApplications()
                }
                is NetworkResult.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isLoading = false, error = "Connection error") }
            }
        }
    }

    fun disburse(applicationId: String, request: DisburseRequest) {
        _state.update { it.copy(isLoading = true, error = null, infoMessage = null) }
        viewModelScope.launch {
            when (val result = repository.disburse(token(), applicationId, request)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isLoading = false, infoMessage = "Disbursement recorded") }
                    loadApplications()
                }
                is NetworkResult.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isLoading = false, error = "Connection error") }
            }
        }
    }

    // ── Admin: Renewals ──────────────────────────────────────────────────

    fun loadRenewals(status: String? = null) {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.listRenewals(token(), status)) {
                is NetworkResult.Success -> _state.update { it.copy(isLoading = false, renewals = result.data.data ?: emptyList()) }
                is NetworkResult.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isLoading = false, error = "Connection error") }
            }
        }
    }

    fun approveRenewal(renewalId: String, request: ApproveRenewalRequest) {
        viewModelScope.launch {
            when (val result = repository.approveRenewal(token(), renewalId, request)) {
                is NetworkResult.Success -> { _state.update { it.copy(infoMessage = "Renewal approved") }; loadRenewals() }
                is NetworkResult.Error -> _state.update { it.copy(error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(error = "Connection error") }
            }
        }
    }

    fun rejectRenewal(renewalId: String, request: RejectApplicationRequest) {
        viewModelScope.launch {
            when (val result = repository.rejectRenewal(token(), renewalId, request)) {
                is NetworkResult.Success -> { _state.update { it.copy(infoMessage = "Renewal rejected") }; loadRenewals() }
                is NetworkResult.Error -> _state.update { it.copy(error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(error = "Connection error") }
            }
        }
    }

    // ── Parent ───────────────────────────────────────────────────────────

    fun loadParentScholarships() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.getParentScholarships(token())) {
                is NetworkResult.Success -> {
                    val data = result.data.data
                    if (data != null) {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                parentScholarships = data.scholarships,
                                parentApplications = data.applications,
                                gamification = data.gamification,
                            )
                        }
                    } else {
                        _state.update { it.copy(isLoading = false) }
                    }
                }
                is NetworkResult.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isLoading = false, error = "Connection error") }
            }
        }
    }

    fun applyScholarship(request: ApplyScholarshipRequest) {
        _state.update { it.copy(isLoading = true, error = null, infoMessage = null) }
        viewModelScope.launch {
            when (val result = repository.applyScholarship(token(), request)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isLoading = false, infoMessage = "Application submitted!") }
                    loadParentScholarships()
                }
                is NetworkResult.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isLoading = false, error = "Connection error") }
            }
        }
    }

    fun applyRenewal(request: ApplyRenewalRequest) {
        _state.update { it.copy(isLoading = true, error = null, infoMessage = null) }
        viewModelScope.launch {
            when (val result = repository.applyRenewal(token(), request)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isLoading = false, infoMessage = "Renewal submitted!") }
                    loadParentScholarships()
                }
                is NetworkResult.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isLoading = false, error = "Connection error") }
            }
        }
    }

    // ── Common ───────────────────────────────────────────────────────────

    fun clearMessages() {
        _state.update { it.copy(error = null, infoMessage = null) }
    }
}

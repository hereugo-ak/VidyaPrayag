package com.littlebridge.enrollplus.feature.alumni.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.enrollplus.core.model.ApiResponse
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.feature.alumni.domain.model.*
import com.littlebridge.enrollplus.feature.alumni.domain.repository.AlumniRepository
import com.littlebridge.enrollplus.core.prefs.PreferenceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AlumniScreenState(
    val isLoading: Boolean = false,
    val alumni: List<Alumni> = emptyList(),
    val pendingVerifications: List<Alumni> = emptyList(),
    val campaigns: List<AlumniDonationCampaign> = emptyList(),
    val donations: List<AlumniDonation> = emptyList(),
    val mentorships: List<AlumniMentorship> = emptyList(),
    val mentorshipRequests: List<AlumniMentorshipRequest> = emptyList(),
    val analytics: AlumniAnalytics? = null,
    val total: Int = 0,
    val page: Int = 1,
    val error: String? = null,
    val infoMessage: String? = null,
)

class AlumniViewModel(
    private val repository: AlumniRepository,
    private val prefs: PreferenceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AlumniScreenState())
    val state: StateFlow<AlumniScreenState> = _state.asStateFlow()

    private suspend fun token(): String = prefs.getUserToken().first() ?: ""

    fun loadAlumni(
        year: Int? = null,
        profession: String? = null,
        city: String? = null,
        q: String? = null,
        page: Int = 1,
    ) {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.listAlumni(token(), year, profession, city, null, null, q, page)) {
                is NetworkResult.Success -> {
                    val data = result.data.data
                    _state.update {
                        it.copy(
                            isLoading = false,
                            alumni = data?.alumni ?: emptyList(),
                            total = data?.total ?: 0,
                            page = data?.page ?: 1,
                        )
                    }
                }
                is NetworkResult.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isLoading = false, error = "Connection error") }
            }
        }
    }

    fun loadPendingVerifications() {
        viewModelScope.launch {
            when (val result = repository.listPendingVerifications(token())) {
                is NetworkResult.Success -> _state.update { it.copy(pendingVerifications = result.data.data ?: emptyList()) }
                is NetworkResult.Error -> _state.update { it.copy(error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(error = "Connection error") }
            }
        }
    }

    fun verifyAlumni(alumniId: String, action: String) {
        viewModelScope.launch {
            when (val result = repository.verifyAlumni(token(), alumniId, action)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(infoMessage = "Alumni ${action}ed") }
                    loadPendingVerifications()
                }
                is NetworkResult.Error -> _state.update { it.copy(error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(error = "Connection error") }
            }
        }
    }

    fun toggleFeatured(alumniId: String) {
        viewModelScope.launch {
            when (val result = repository.toggleFeatured(token(), alumniId)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(infoMessage = "Featured status toggled") }
                    loadAlumni()
                }
                is NetworkResult.Error -> _state.update { it.copy(error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(error = "Connection error") }
            }
        }
    }

    fun loadCampaigns() {
        viewModelScope.launch {
            when (val result = repository.listCampaigns(token())) {
                is NetworkResult.Success -> _state.update { it.copy(campaigns = result.data.data ?: emptyList()) }
                is NetworkResult.Error -> _state.update { it.copy(error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(error = "Connection error") }
            }
        }
    }

    fun loadDonations(campaignId: String? = null) {
        viewModelScope.launch {
            when (val result = repository.listDonations(token(), campaignId)) {
                is NetworkResult.Success -> _state.update { it.copy(donations = result.data.data ?: emptyList()) }
                is NetworkResult.Error -> _state.update { it.copy(error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(error = "Connection error") }
            }
        }
    }

    fun loadMentorships() {
        viewModelScope.launch {
            when (val result = repository.listMentorships(token())) {
                is NetworkResult.Success -> _state.update { it.copy(mentorships = result.data.data ?: emptyList()) }
                is NetworkResult.Error -> _state.update { it.copy(error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(error = "Connection error") }
            }
        }
    }

    fun loadMentorshipRequests() {
        viewModelScope.launch {
            when (val result = repository.listMentorshipRequests(token())) {
                is NetworkResult.Success -> _state.update { it.copy(mentorshipRequests = result.data.data ?: emptyList()) }
                is NetworkResult.Error -> _state.update { it.copy(error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(error = "Connection error") }
            }
        }
    }

    fun loadAnalytics() {
        viewModelScope.launch {
            when (val result = repository.getAnalyticsOverview(token())) {
                is NetworkResult.Success -> _state.update { it.copy(analytics = result.data.data) }
                is NetworkResult.Error -> _state.update { it.copy(error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(error = "Connection error") }
            }
        }
    }

    fun createAlumni(request: CreateAlumniRequest) {
        _state.update { it.copy(isLoading = true, error = null, infoMessage = null) }
        viewModelScope.launch {
            when (val result = repository.createAlumni(token(), request)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isLoading = false, infoMessage = "Alumni added: ${result.data.data?.name ?: request.name}") }
                    loadAlumni()
                }
                is NetworkResult.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isLoading = false, error = "Connection error") }
            }
        }
    }

    fun bulkImport(rows: List<CreateAlumniRequest>) {
        _state.update { it.copy(isLoading = true, error = null, infoMessage = null) }
        viewModelScope.launch {
            when (val result = repository.bulkImport(token(), rows)) {
                is NetworkResult.Success -> {
                    val r = result.data.data
                    if (r != null && r.failed > 0) {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                infoMessage = "Imported ${r.imported}, failed ${r.failed}",
                                error = if (r.errors.isNotEmpty()) r.errors.joinToString("\n") else null,
                            )
                        }
                    } else {
                        _state.update {
                            it.copy(isLoading = false, infoMessage = "Imported ${r?.imported ?: 0} alumni")
                        }
                    }
                    loadAlumni()
                }
                is NetworkResult.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isLoading = false, error = "Connection error") }
            }
        }
    }

    fun clearMessages() {
        _state.update { it.copy(error = null, infoMessage = null) }
    }
}

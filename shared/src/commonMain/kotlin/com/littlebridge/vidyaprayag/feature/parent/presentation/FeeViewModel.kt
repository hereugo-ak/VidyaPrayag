package com.littlebridge.vidyaprayag.feature.parent.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.core.prefs.PreferenceRepository
import com.littlebridge.vidyaprayag.feature.parent.domain.repository.ParentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FeeAnnouncement(
    val id: String,
    val title: String,
    val time: String,
    val description: String,
    val openRate: String,
    val engagement: String,
    val type: String // "Campaign", "Emergency", "Payment"
)

data class FeeState(
    val totalCollected: String = "$0",
    val collectionProgress: Float = 0f,
    val outstandingFees: String = "$0",
    val overdueCount: Int = 0,
    val announcements: List<FeeAnnouncement> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class FeeViewModel(
    private val repository: ParentRepository,
    private val preferenceRepository: PreferenceRepository
) : ViewModel() {
    private val _state = MutableStateFlow(FeeState())
    val state: StateFlow<FeeState> = _state.asStateFlow()

    init {
        loadFees()
    }

    private fun loadFees() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            preferenceRepository.getUserToken().collect { token ->
                if (token != null) {
                    when (val result = repository.getFees(token)) {
                        is NetworkResult.Success -> {
                            val data = result.data.data
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    totalCollected = data.totalCollected,
                                    collectionProgress = data.collectionProgress,
                                    outstandingFees = data.outstandingFees,
                                    overdueCount = data.overdueCount,
                                    announcements = data.announcements.map { a ->
                                        FeeAnnouncement(a.id, a.title, a.time, a.description, a.openRate, a.engagement, a.type)
                                    }
                                )
                            }
                        }
                        is NetworkResult.Error -> {
                            _state.update { it.copy(isLoading = false, error = result.message) }
                        }
                        is NetworkResult.ConnectionError -> {
                            _state.update { it.copy(isLoading = false, error = "Connection error") }
                        }
                    }
                }
            }
        }
    }
}

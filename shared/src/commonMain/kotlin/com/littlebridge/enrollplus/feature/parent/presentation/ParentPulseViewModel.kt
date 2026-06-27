package com.littlebridge.enrollplus.feature.parent.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.core.prefs.PreferenceRepository
import com.littlebridge.enrollplus.core.state.SelectedChildHolder
import com.littlebridge.enrollplus.feature.parent.domain.model.PulseDto
import com.littlebridge.enrollplus.feature.parent.domain.repository.ParentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ParentPulseState(
    val isLoading: Boolean = false,
    val latestPulse: PulseDto? = null,
    val pulseHistory: List<PulseDto> = emptyList(),
    val selectedChildId: String? = null,
    val showHistory: Boolean = false,
    val error: String? = null,
)

class ParentPulseViewModel(
    private val repository: ParentRepository,
    private val preferenceRepository: PreferenceRepository,
    private val selectedChildHolder: SelectedChildHolder,
) : ViewModel() {
    private val _state = MutableStateFlow(ParentPulseState())
    val state: StateFlow<ParentPulseState> = _state.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            selectedChildHolder.selectedChildId.collect { shared ->
                if (shared != null && shared != _state.value.selectedChildId) {
                    _state.update { it.copy(selectedChildId = shared) }
                    loadLatestPulse(shared)
                }
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            val token = preferenceRepository.getUserToken().first()
            if (token == null) {
                _state.update { it.copy(error = "Not signed in") }
                return@launch
            }
            val childId = selectedChildHolder.selectedChildId.value
                ?: _state.value.selectedChildId
            if (childId != null) {
                _state.update { it.copy(selectedChildId = childId) }
                loadLatestPulse(childId)
            }
        }
    }

    fun loadLatestPulse(childId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val token = preferenceRepository.getUserToken().first()
            if (token == null) {
                _state.update { it.copy(isLoading = false, error = "Not signed in") }
                return@launch
            }
            when (val result = repository.getLatestPulse(token, childId)) {
                is NetworkResult.Success -> {
                    _state.update {
                        it.copy(isLoading = false, latestPulse = result.data.data)
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

    fun loadHistory(childId: String, weeks: Int = 12) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val token = preferenceRepository.getUserToken().first()
            if (token == null) {
                _state.update { it.copy(isLoading = false, error = "Not signed in") }
                return@launch
            }
            when (val result = repository.getPulseHistory(token, childId, weeks)) {
                is NetworkResult.Success -> {
                    _state.update {
                        it.copy(isLoading = false, pulseHistory = result.data.data.pulses)
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

    fun toggleHistory() {
        _state.update { it.copy(showHistory = !it.showHistory) }
    }

    fun selectChild(childId: String) {
        _state.update { it.copy(selectedChildId = childId) }
        selectedChildHolder.select(childId)
        loadLatestPulse(childId)
    }
}

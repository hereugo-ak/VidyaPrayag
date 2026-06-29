package com.littlebridge.enrollplus.feature.tutor.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.core.prefs.PreferenceRepository
import com.littlebridge.enrollplus.core.state.SelectedChildHolder
import com.littlebridge.enrollplus.feature.tutor.domain.model.*
import com.littlebridge.enrollplus.feature.tutor.domain.repository.TutorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ParentProgressState(
    val isLoading: Boolean = false,
    val progressCard: ProgressCardDto? = null,
    val selectedChildId: String? = null,
    val subjectId: String = "",
    val error: String? = null,
)

class ParentProgressViewModel(
    private val repository: TutorRepository,
    private val preferenceRepository: PreferenceRepository,
    private val selectedChildHolder: SelectedChildHolder,
) : ViewModel() {
    private val _state = MutableStateFlow(ParentProgressState())
    val state: StateFlow<ParentProgressState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            selectedChildHolder.selectedChildId.collect { childId ->
                if (childId != null && childId != _state.value.selectedChildId) {
                    _state.update { it.copy(selectedChildId = childId) }
                }
            }
        }
    }

    fun updateSubject(subjectId: String) {
        _state.update { it.copy(subjectId = subjectId) }
        loadProgress()
    }

    fun loadProgress() {
        val childId = _state.value.selectedChildId ?: return
        val subjectId = _state.value.subjectId.ifBlank { return }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val token = preferenceRepository.getUserToken().first() ?: run {
                _state.update { it.copy(isLoading = false, error = "Not signed in") }
                return@launch
            }

            when (val result = repository.getProgressCard(token, childId, subjectId)) {
                is NetworkResult.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            progressCard = result.data.data,
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

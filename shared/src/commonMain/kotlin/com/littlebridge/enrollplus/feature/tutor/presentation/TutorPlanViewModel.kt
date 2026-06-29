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

data class TutorPlanState(
    val isLoading: Boolean = false,
    val planItems: List<PlanItemDto> = emptyList(),
    val learnerBundle: LearnerBundleDto? = null,
    val selectedChildId: String? = null,
    val subjectId: String = "",
    val error: String? = null,
)

class TutorPlanViewModel(
    private val repository: TutorRepository,
    private val preferenceRepository: PreferenceRepository,
    private val selectedChildHolder: SelectedChildHolder,
) : ViewModel() {
    private val _state = MutableStateFlow(TutorPlanState())
    val state: StateFlow<TutorPlanState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            selectedChildHolder.selectedChildId.collect { childId ->
                if (childId != null) {
                    _state.update { it.copy(selectedChildId = childId) }
                }
            }
        }
    }

    fun updateSubject(subjectId: String) {
        _state.update { it.copy(subjectId = subjectId) }
        loadPlan()
    }

    fun loadPlan() {
        val childId = _state.value.selectedChildId ?: return
        val subjectId = _state.value.subjectId.ifBlank { return }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val token = preferenceRepository.getUserToken().first() ?: run {
                _state.update { it.copy(isLoading = false, error = "Not signed in") }
                return@launch
            }

            when (val result = repository.getPlan(token, childId, subjectId)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isLoading = false, planItems = result.data.data ?: emptyList()) }
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

    fun loadLearnerBundle() {
        val childId = _state.value.selectedChildId ?: return
        val subjectId = _state.value.subjectId.ifBlank { return }

        viewModelScope.launch {
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.getLearnerBundle(token, childId, subjectId)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(learnerBundle = result.data.data) }
                }
                else -> Unit
            }
        }
    }
}

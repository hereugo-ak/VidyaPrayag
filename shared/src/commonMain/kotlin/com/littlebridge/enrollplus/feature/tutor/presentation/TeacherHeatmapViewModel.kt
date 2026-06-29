package com.littlebridge.enrollplus.feature.tutor.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.core.prefs.PreferenceRepository
import com.littlebridge.enrollplus.feature.tutor.domain.model.*
import com.littlebridge.enrollplus.feature.tutor.domain.repository.TutorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TeacherHeatmapState(
    val isLoading: Boolean = false,
    val scope: List<TeacherScopeItemDto> = emptyList(),
    val selectedClassId: String? = null,
    val selectedSubjectId: String? = null,
    val heatmap: HeatmapDto? = null,
    val error: String? = null,
)

class TeacherHeatmapViewModel(
    private val repository: TutorRepository,
    private val preferenceRepository: PreferenceRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(TeacherHeatmapState())
    val state: StateFlow<TeacherHeatmapState> = _state.asStateFlow()

    init {
        loadScope()
    }

    fun loadScope() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val token = preferenceRepository.getUserToken().first() ?: run {
                _state.update { it.copy(isLoading = false, error = "Not signed in") }
                return@launch
            }

            when (val result = repository.getTeacherScope(token)) {
                is NetworkResult.Success -> {
                    val scope = result.data.data ?: emptyList()
                    _state.update {
                        it.copy(
                            isLoading = false,
                            scope = scope,
                            selectedClassId = scope.firstOrNull()?.classId,
                            selectedSubjectId = scope.firstOrNull()?.subjectId,
                        )
                    }
                    if (scope.isNotEmpty()) {
                        loadHeatmap(scope.first().classId, scope.first().subjectId)
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

    fun selectScope(classId: String, subjectId: String) {
        _state.update { it.copy(selectedClassId = classId, selectedSubjectId = subjectId) }
        loadHeatmap(classId, subjectId)
    }

    fun loadHeatmap(classId: String, subjectId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val token = preferenceRepository.getUserToken().first() ?: run {
                _state.update { it.copy(isLoading = false, error = "Not signed in") }
                return@launch
            }

            when (val result = repository.getHeatmap(token, classId, subjectId)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isLoading = false, heatmap = result.data.data) }
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

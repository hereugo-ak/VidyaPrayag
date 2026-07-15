package com.littlebridge.vidyaprayag.feature.teacher.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.core.prefs.PreferenceRepository
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.TeacherClass
import com.littlebridge.vidyaprayag.feature.teacher.domain.repository.TeacherRepository
import com.littlebridge.vidyaprayag.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class TeacherClassesState(
    val classes: List<TeacherClass> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class TeacherClassesViewModel(
    private val repository: TeacherRepository,
    private val prefs: PreferenceRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TeacherClassesState())
    val state: StateFlow<TeacherClassesState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val token = prefs.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isLoading = false, error = "Not signed in")
                return@launch
            }
            when (val result = repository.getClasses(token)) {
                is NetworkResult.Success -> {
                    _state.value = TeacherClassesState(classes = result.data.classes, isLoading = false)
                }
                is NetworkResult.Error -> {
                    AppLogger.e("TeacherClassesVM", "getClasses failed: ${result.message}")
                    _state.value = _state.value.copy(isLoading = false, error = result.message)
                }
                is NetworkResult.ConnectionError -> {
                    _state.value = _state.value.copy(isLoading = false, error = "Connection error")
                }
            }
        }
    }

    fun clearError() { _state.value = _state.value.copy(error = null) }
}

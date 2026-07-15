package com.littlebridge.vidyaprayag.feature.teacher.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.core.prefs.PreferenceRepository
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.TeacherHomeResponse
import com.littlebridge.vidyaprayag.feature.teacher.domain.repository.TeacherRepository
import com.littlebridge.vidyaprayag.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class TeacherHomeState(
    val data: TeacherHomeResponse? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

class TeacherHomeViewModel(
    private val repository: TeacherRepository,
    private val prefs: PreferenceRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TeacherHomeState())
    val state: StateFlow<TeacherHomeState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val token = prefs.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isLoading = false, error = "Not signed in")
                return@launch
            }
            when (val result = repository.getHome(token)) {
                is NetworkResult.Success -> _state.value = TeacherHomeState(data = result.data, isLoading = false)
                is NetworkResult.Error -> {
                    AppLogger.e("TeacherHomeVM", "getHome failed: ${result.message}")
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

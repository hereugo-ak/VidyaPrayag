package com.littlebridge.vidyaprayag.feature.teacher.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.core.prefs.PreferenceRepository
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.TeacherProfileResponse
import com.littlebridge.vidyaprayag.feature.teacher.domain.repository.TeacherRepository
import com.littlebridge.vidyaprayag.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class TeacherProfileState(
    val profile: TeacherProfileResponse? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

class TeacherProfileViewModel(
    private val repository: TeacherRepository,
    private val prefs: PreferenceRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TeacherProfileState())
    val state: StateFlow<TeacherProfileState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val token = prefs.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isLoading = false, error = "Not signed in")
                return@launch
            }
            when (val result = repository.getProfile(token)) {
                is NetworkResult.Success -> {
                    _state.value = TeacherProfileState(profile = result.data, isLoading = false)
                }
                is NetworkResult.Error -> {
                    AppLogger.e("TeacherProfileVM", "getProfile failed: ${result.message}")
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

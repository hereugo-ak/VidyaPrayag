/*
 * File: TeacherProfileViewModel.kt
 * Module: feature.admin.presentation
 *
 * RA-45: drives the admin teacher profile screen (classes/subjects/coverage).
 * The id is supplied by the screen via load(id). Three states.
 */
package com.littlebridge.vidyaprayag.feature.admin.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.core.prefs.PreferenceRepository
import com.littlebridge.vidyaprayag.feature.admin.domain.model.TeacherProfileDto
import com.littlebridge.vidyaprayag.feature.admin.domain.repository.StudentsRepository
import com.littlebridge.vidyaprayag.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class TeacherProfileUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val profile: TeacherProfileDto? = null
)

class TeacherProfileViewModel(
    private val repository: StudentsRepository,
    private val preferenceRepository: PreferenceRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TeacherProfileUiState())
    val state: StateFlow<TeacherProfileUiState> = _state.asStateFlow()

    private var lastId: String? = null

    fun load(teacherId: String) {
        lastId = teacherId
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isLoading = false, error = "You are not signed in. Please log in again.")
                return@launch
            }
            when (val r = repository.getTeacherProfile(token, teacherId)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(isLoading = false, error = null, profile = r.data.data)
                }
                is NetworkResult.Error -> {
                    AppLogger.e("TeacherProfileVM", "getTeacherProfile error: ${r.message}")
                    _state.value = _state.value.copy(isLoading = false, error = r.message)
                }
                is NetworkResult.ConnectionError -> {
                    _state.value = _state.value.copy(isLoading = false, error = "Connection error. Check your internet.")
                }
            }
        }
    }

    fun retry() { lastId?.let { load(it) } }
}

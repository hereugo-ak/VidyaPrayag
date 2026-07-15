package com.littlebridge.vidyaprayag.feature.teacher.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.core.prefs.PreferenceRepository
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.CreateHomeworkRequest
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.TeacherHomework
import com.littlebridge.vidyaprayag.feature.teacher.domain.repository.TeacherRepository
import com.littlebridge.vidyaprayag.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class TeacherHomeworkState(
    val homework: List<TeacherHomework> = emptyList(),
    val isLoading: Boolean = true,
    val isCreating: Boolean = false,
    val error: String? = null
)

class TeacherHomeworkViewModel(
    private val repository: TeacherRepository,
    private val prefs: PreferenceRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TeacherHomeworkState())
    val state: StateFlow<TeacherHomeworkState> = _state.asStateFlow()

    init { refresh() }

    fun refresh(className: String? = null) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val token = prefs.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isLoading = false, error = "Not signed in")
                return@launch
            }
            when (val result = repository.getHomework(token, className)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(homework = result.data.homework, isLoading = false)
                }
                is NetworkResult.Error -> {
                    AppLogger.e("TeacherHomeworkVM", "getHomework failed: ${result.message}")
                    _state.value = _state.value.copy(isLoading = false, error = result.message)
                }
                is NetworkResult.ConnectionError -> {
                    _state.value = _state.value.copy(isLoading = false, error = "Connection error")
                }
            }
        }
    }

    fun createHomework(
        title: String,
        subject: String,
        className: String,
        description: String,
        dueDate: String,
        onCreated: () -> Unit
    ) {
        if (title.trim().length < MIN_HOMEWORK_TITLE_LENGTH) {
            _state.value = _state.value.copy(
                error = "Title must be at least $MIN_HOMEWORK_TITLE_LENGTH characters"
            )
            return
        }
        if (subject.isBlank() || className.isBlank()) {
            _state.value = _state.value.copy(error = "Subject and class are required")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isCreating = true, error = null)
            val token = prefs.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isCreating = false, error = "Not signed in")
                return@launch
            }
            val request = CreateHomeworkRequest(
                title = title.trim(),
                subject = subject,
                className = className,
                description = description,
                dueDate = dueDate
            )
            when (val result = repository.createHomework(token, request)) {
                is NetworkResult.Success -> {
                    AppLogger.d("TeacherHomeworkVM", "Created homework ${result.data.homeworkId}")
                    _state.value = _state.value.copy(isCreating = false)
                    onCreated()
                    refresh()
                }
                is NetworkResult.Error -> {
                    AppLogger.e("TeacherHomeworkVM", "createHomework failed: ${result.message}")
                    _state.value = _state.value.copy(isCreating = false, error = result.message)
                }
                is NetworkResult.ConnectionError -> {
                    _state.value = _state.value.copy(isCreating = false, error = "Connection error")
                }
            }
        }
    }

    fun clearError() { _state.value = _state.value.copy(error = null) }

    companion object {
        const val MIN_HOMEWORK_TITLE_LENGTH = 3
    }
}

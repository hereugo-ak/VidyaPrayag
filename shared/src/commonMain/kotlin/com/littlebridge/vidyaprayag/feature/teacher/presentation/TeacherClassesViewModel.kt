package com.littlebridge.vidyaprayag.feature.teacher.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.core.prefs.PreferenceRepository
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.TeacherClassDto
import com.littlebridge.vidyaprayag.feature.teacher.domain.repository.TeacherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TeacherClass(
    val id: String,
    val className: String,
    val subject: String,
    val studentCount: Int,
    val isClassTeacher: Boolean,
    val syllabusProgress: Float,
    val avgAttendance: Float,
)

data class TeacherClassesState(
    val classes: List<TeacherClass> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

class TeacherClassesViewModel(
    private val repository: TeacherRepository,
    private val preferenceRepository: PreferenceRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(TeacherClassesState())
    val state: StateFlow<TeacherClassesState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val token = preferenceRepository.getUserToken().first()
            if (token == null) {
                _state.update { it.copy(isLoading = false, error = "Not authenticated") }
                return@launch
            }
            when (val result = repository.getClasses(token)) {
                is NetworkResult.Success -> {
                    val classes = result.data.data.classes.map { it.toUi() }
                    _state.update { it.copy(isLoading = false, classes = classes) }
                }
                is NetworkResult.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isLoading = false, error = "Connection error") }
            }
        }
    }
}

private fun TeacherClassDto.toUi() = TeacherClass(
    id = id,
    className = className,
    subject = subject,
    studentCount = studentCount,
    isClassTeacher = isClassTeacher,
    syllabusProgress = syllabusProgress,
    avgAttendance = avgAttendance,
)

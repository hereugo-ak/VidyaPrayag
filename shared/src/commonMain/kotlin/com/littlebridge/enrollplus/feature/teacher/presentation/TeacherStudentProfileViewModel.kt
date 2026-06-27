package com.littlebridge.enrollplus.feature.teacher.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.core.prefs.PreferenceRepository
import com.littlebridge.enrollplus.feature.teacher.domain.model.StudentProfileData
import com.littlebridge.enrollplus.feature.teacher.domain.repository.TeacherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * T-505 — the scoped student-profile drill-down VM. Loads `GET /teacher/students/{id}`
 * (server enforces "you must teach this student", else 403 surfaced as a forbidden
 * state). Read-only: attendance, performance, server-computed flags, privacy-gated
 * parent contact.
 */
data class StudentProfileState(
    val studentId: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val forbidden: Boolean = false,   // 403 — teacher doesn't teach this student
    val profile: StudentProfileData? = null,
)

class TeacherStudentProfileViewModel(
    private val repository: TeacherRepository,
    private val preferenceRepository: PreferenceRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(StudentProfileState())
    val state: StateFlow<StudentProfileState> = _state.asStateFlow()

    fun load(studentId: String) {
        _state.update {
            it.copy(studentId = studentId, isLoading = true, error = null, forbidden = false, profile = null)
        }
        viewModelScope.launch {
            val token = preferenceRepository.getUserToken().first()
            if (token == null) {
                _state.update { it.copy(isLoading = false, error = "Not authenticated") }
                return@launch
            }
            when (val r = repository.getStudentProfileV2(token, studentId)) {
                is NetworkResult.Success ->
                    _state.update { it.copy(isLoading = false, profile = r.data.data) }
                is NetworkResult.Error -> {
                    // The scope law: a 403 is a semantic "not your student", not a transient error.
                    val forbidden = r.code == 403 || r.message.contains("teach", ignoreCase = true)
                    _state.update {
                        it.copy(isLoading = false, forbidden = forbidden, error = if (forbidden) null else r.message)
                    }
                }
                is NetworkResult.ConnectionError ->
                    _state.update { it.copy(isLoading = false, error = "Connection error") }
            }
        }
    }

    fun retry() {
        _state.value.studentId?.let { load(it) }
    }
}

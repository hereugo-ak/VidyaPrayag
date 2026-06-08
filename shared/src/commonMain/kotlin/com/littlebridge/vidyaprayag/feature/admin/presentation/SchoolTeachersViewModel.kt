package com.littlebridge.vidyaprayag.feature.admin.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.core.prefs.PreferenceRepository
import com.littlebridge.vidyaprayag.feature.admin.domain.model.CreateTeacherRequest
import com.littlebridge.vidyaprayag.feature.admin.domain.model.TeacherAccountDto
import com.littlebridge.vidyaprayag.feature.admin.domain.repository.TeachersRepository
import com.littlebridge.vidyaprayag.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** A single teacher row for the roster UI. */
data class TeacherRosterItem(
    val id: String,
    val name: String,
    val contact: String,
)

data class SchoolTeachersState(
    val teachers: List<TeacherRosterItem> = emptyList(),
    val isLoading: Boolean = false,
    val isMutating: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
)

/**
 * RA-22: backs the teacher roster on the People tab — list active teachers in
 * the admin's school, add a teacher (writes app_users), and remove (soft-delete)
 * a teacher. Every list mutation re-fetches from the server so the UI never
 * shows a stale local copy after a failed write.
 */
class SchoolTeachersViewModel(
    private val repository: TeachersRepository,
    private val preferenceRepository: PreferenceRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SchoolTeachersState())
    val state: StateFlow<SchoolTeachersState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isLoading = false, errorMessage = "Not signed in")
                return@launch
            }
            when (val result = repository.getTeachers(token)) {
                is NetworkResult.Success -> {
                    val items = result.data.data?.teachers.orEmpty().map { it.toRosterItem() }
                    _state.value = _state.value.copy(teachers = items, isLoading = false)
                }
                is NetworkResult.Error -> {
                    AppLogger.e("SchoolTeachersVM", "getTeachers error: ${result.message}")
                    _state.value = _state.value.copy(isLoading = false, errorMessage = result.message)
                }
                is NetworkResult.ConnectionError -> {
                    _state.value = _state.value.copy(isLoading = false, errorMessage = "Connection error. Check your internet.")
                }
            }
        }
    }

    /**
     * Add a teacher by email (with an initial password) or phone (OTP login).
     * [onAdded] fires only after the server round-trip succeeds, so the screen
     * dismisses its dialog only on success.
     */
    fun addTeacher(
        name: String,
        identifier: String,
        initialPassword: String?,
        onAdded: (() -> Unit)? = null
    ) {
        if (name.isBlank() || identifier.isBlank()) {
            _state.value = _state.value.copy(errorMessage = "Name and email/phone are required.")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isMutating = true, errorMessage = null)
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isMutating = false, errorMessage = "Not signed in")
                return@launch
            }
            val body = CreateTeacherRequest(
                name = name.trim(),
                identifier = identifier.trim(),
                initialPassword = initialPassword?.trim()?.takeIf { it.isNotBlank() }
            )
            when (val r = repository.createTeacher(token, body)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(isMutating = false, infoMessage = "Teacher added")
                    onAdded?.invoke()
                    load()
                }
                is NetworkResult.Error -> {
                    _state.value = _state.value.copy(isMutating = false, errorMessage = r.message)
                }
                is NetworkResult.ConnectionError -> {
                    _state.value = _state.value.copy(isMutating = false, errorMessage = "Connection error. Check your internet.")
                }
            }
        }
    }

    /** Remove (soft-delete) a teacher, then refresh the roster. */
    fun removeTeacher(teacherId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isMutating = true, errorMessage = null)
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isMutating = false, errorMessage = "Not signed in")
                return@launch
            }
            when (val r = repository.deleteTeacher(token, teacherId)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(isMutating = false, infoMessage = "Teacher removed")
                    load()
                }
                is NetworkResult.Error -> {
                    _state.value = _state.value.copy(isMutating = false, errorMessage = r.message)
                }
                is NetworkResult.ConnectionError -> {
                    _state.value = _state.value.copy(isMutating = false, errorMessage = "Connection error. Check your internet.")
                }
            }
        }
    }

    fun clearMessages() {
        _state.value = _state.value.copy(errorMessage = null, infoMessage = null)
    }

    private fun TeacherAccountDto.toRosterItem() = TeacherRosterItem(
        id = id,
        name = name,
        contact = (email ?: phone).orEmpty(),
    )
}

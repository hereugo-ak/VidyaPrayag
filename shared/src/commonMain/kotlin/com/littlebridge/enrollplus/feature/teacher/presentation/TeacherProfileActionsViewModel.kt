package com.littlebridge.enrollplus.feature.teacher.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.core.prefs.PreferenceRepository
import com.littlebridge.enrollplus.feature.auth.domain.repository.AuthRepository
import com.littlebridge.enrollplus.feature.teacher.domain.model.CreateTeacherLeaveRequest
import com.littlebridge.enrollplus.feature.teacher.domain.model.TeacherSelfLeaveDto
import com.littlebridge.enrollplus.feature.teacher.domain.repository.TeacherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * TeacherProfileActionsViewModel (T-602b, Doc 04 §5.14) — backs the actionable
 * parts of the rebuilt teacher Profile that the read-only [TeacherProfileViewModel]
 * (identity) does NOT own:
 *
 *   • The teacher's OWN leave: status list (GET /teacher/leave) + apply
 *     (POST /teacher/leave) — closes the "leave apply" gap.
 *   • Password change — delegates to the shared [AuthRepository.changePassword]
 *     (the RA-54 POST /auth/change-password that already honours the
 *     must_change_password first-login gate). Closes F-PROF-2.
 *   • Theme preference (Warm / Light / Night) read+write via [PreferenceRepository]
 *     so the switch is REAL (the teacher portal reads this to drive its tone).
 *     Closes part of F-PROF-1 (inert setting rows).
 *
 * Separate from the identity VM so the heavy identity load and the leave list
 * refresh independently, and so a failed action (password/leave) never blanks
 * the identity card.
 */
data class TeacherLeaveUiState(
    val requests: List<TeacherSelfLeaveDto> = emptyList(),
    val pendingCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
)

/** Transient result of a one-shot action (apply leave / change password). */
sealed interface ActionResult {
    data object Idle : ActionResult
    data object InFlight : ActionResult
    data class Success(val message: String) : ActionResult
    data class Failure(val message: String) : ActionResult
}

class TeacherProfileActionsViewModel(
    private val repository: TeacherRepository,
    private val preferenceRepository: PreferenceRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _leave = MutableStateFlow(TeacherLeaveUiState())
    val leave: StateFlow<TeacherLeaveUiState> = _leave.asStateFlow()

    private val _apply = MutableStateFlow<ActionResult>(ActionResult.Idle)
    val apply: StateFlow<ActionResult> = _apply.asStateFlow()

    private val _password = MutableStateFlow<ActionResult>(ActionResult.Idle)
    val password: StateFlow<ActionResult> = _password.asStateFlow()

    /** Current theme name (legacy: "WARM" | "LIGHT" | "NIGHT"), upper-cased; defaults WARM for teachers. */
    val themeName: StateFlow<String> = preferenceRepository.getThemeName()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "WARM")

    /** Theme mode ("system" | "light" | "dark" | "custom"); defaults system. */
    val themeMode: StateFlow<String> = preferenceRepository.getThemeMode()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "system")

    /** Custom theme id when mode == "custom"; null otherwise. */
    val customThemeId: StateFlow<String?> = preferenceRepository.getCustomThemeId()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        loadLeave()
    }

    fun loadLeave() {
        viewModelScope.launch {
            _leave.update { it.copy(isLoading = true, error = null) }
            val token = preferenceRepository.getUserToken().first()
            if (token == null) {
                _leave.update { it.copy(isLoading = false, error = "Not authenticated") }
                return@launch
            }
            when (val result = repository.getMyLeave(token)) {
                is NetworkResult.Success -> {
                    val data = result.data.data
                    _leave.update {
                        it.copy(
                            isLoading = false,
                            requests = data.requests,
                            pendingCount = data.pendingCount,
                        )
                    }
                }
                is NetworkResult.Error -> _leave.update { it.copy(isLoading = false, error = result.message) }
                is NetworkResult.ConnectionError -> _leave.update { it.copy(isLoading = false, error = "Connection error") }
            }
        }
    }

    /** Apply for the teacher's own leave. On success, refreshes the list. */
    fun applyLeave(dateFrom: String, dateTo: String, reason: String, imageUrl: String? = null) {
        viewModelScope.launch {
            // Client-side validation mirrors the server (clear, immediate feedback).
            if (dateFrom.isBlank() || dateTo.isBlank()) {
                _apply.value = ActionResult.Failure("Pick both a start and end date"); return@launch
            }
            if (dateTo < dateFrom) {
                _apply.value = ActionResult.Failure("End date can't be before the start date"); return@launch
            }
            if (reason.isBlank()) {
                _apply.value = ActionResult.Failure("Add a reason for your leave"); return@launch
            }
            _apply.value = ActionResult.InFlight
            val token = preferenceRepository.getUserToken().first()
            if (token == null) {
                _apply.value = ActionResult.Failure("Not authenticated"); return@launch
            }
            val request = CreateTeacherLeaveRequest(
                dateFrom = dateFrom,
                dateTo = dateTo,
                reason = reason.trim(),
                imageUrl = imageUrl?.ifBlank { null },
            )
            when (val result = repository.applyMyLeave(token, request)) {
                is NetworkResult.Success -> {
                    _apply.value = ActionResult.Success("Leave request submitted")
                    loadLeave()
                }
                is NetworkResult.Error -> _apply.value = ActionResult.Failure(result.message)
                is NetworkResult.ConnectionError -> _apply.value = ActionResult.Failure("Connection error")
            }
        }
    }

    fun clearApplyResult() { _apply.value = ActionResult.Idle }

    /**
     * Change the teacher's password. [oldPassword] may be null for the forced
     * first-change flow (the server allows it when must_change_password is set).
     */
    fun changePassword(oldPassword: String?, newPassword: String, confirmPassword: String) {
        viewModelScope.launch {
            if (newPassword.length < 8) {
                _password.value = ActionResult.Failure("New password must be at least 8 characters"); return@launch
            }
            if (newPassword != confirmPassword) {
                _password.value = ActionResult.Failure("Passwords don't match"); return@launch
            }
            _password.value = ActionResult.InFlight
            when (val result = authRepository.changePassword(oldPassword?.ifBlank { null }, newPassword)) {
                is NetworkResult.Success -> _password.value = ActionResult.Success("Password changed")
                is NetworkResult.Error -> _password.value = ActionResult.Failure(result.message)
                is NetworkResult.ConnectionError -> _password.value = ActionResult.Failure("Connection error")
            }
        }
    }

    fun clearPasswordResult() { _password.value = ActionResult.Idle }

    /** Set the global theme preference (legacy: "WARM" | "LIGHT" | "NIGHT"). */
    fun setTheme(name: String) {
        viewModelScope.launch {
            preferenceRepository.setThemeName(name.uppercase())
        }
    }

    /** Set the theme mode ("system" | "light" | "dark" | "custom"). */
    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            preferenceRepository.setThemeMode(mode)
        }
    }

    /** Set the custom theme id (used when mode == "custom"). */
    fun setCustomThemeId(id: String?) {
        viewModelScope.launch {
            preferenceRepository.setCustomThemeId(id)
        }
    }
}

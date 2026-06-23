package com.littlebridge.vidyaprayag.feature.teacher.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.core.prefs.PreferenceRepository
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.TeacherCheckInRequest
import com.littlebridge.vidyaprayag.feature.teacher.domain.repository.TeacherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * TeacherCheckInViewModel — backs the Today greeting band's self check-in pill
 * (T-106c, Doc 06 §2). It owns ONLY the transport: load today's status, and POST a
 * check-in once the UI's biometric ladder (biometric → PIN → manual) has produced a
 * `method`. The biometric prompt itself is a platform concern
 * ([com.littlebridge.vidyaprayag.platform.BiometricAuthenticator]) and lives in the
 * UI layer — this VM never sees the prompt, only the resulting honest `method`.
 *
 * Design rules honored:
 *  - **Server clock is authoritative** (Doc 06 §2.4): `checkedInAt` always comes from
 *    the server response; the device never stamps the time.
 *  - **Idempotent** (Doc 06 §2.4): the endpoint returns the existing row on a repeat
 *    POST, so a double-tap can't duplicate; we just reflect the returned status.
 *  - **Optimistic flip with revert** (Doc 10 §9): [checkIn] flips the pill to a
 *    pending state immediately, then reconciles to the server status or reverts +
 *    surfaces an inline error on failure.
 */
data class TeacherCheckInState(
    val isLoading: Boolean = false,        // initial status fetch
    val isCheckingIn: Boolean = false,     // a POST is in flight (optimistic pending)
    val checkedIn: Boolean = false,
    val checkedInAt: String? = null,       // server-stamped ISO timestamp
    val method: String? = null,            // biometric | pin | manual
    val date: String = "",
    // Honest "we couldn't even read the status" → the card shows a disabled,
    // captioned state rather than pretending (Doc 04 §5.1 graceful unavailable).
    val statusUnavailable: Boolean = false,
    val error: String? = null,             // inline check-in error
)

class TeacherCheckInViewModel(
    private val repository: TeacherRepository,
    private val preferenceRepository: PreferenceRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(TeacherCheckInState())
    val state: StateFlow<TeacherCheckInState> = _state.asStateFlow()

    init {
        loadStatus()
    }

    /** Fetch today's check-in status (drives the amber/green pill on open). */
    fun loadStatus() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, statusUnavailable = false) }
            val token = preferenceRepository.getUserToken().first()
            if (token == null) {
                _state.update { it.copy(isLoading = false, statusUnavailable = true) }
                return@launch
            }
            when (val result = repository.getCheckInStatus(token, date = null)) {
                is NetworkResult.Success -> {
                    val d = result.data.data
                    _state.update {
                        it.copy(
                            isLoading = false,
                            checkedIn = d.checkedIn,
                            checkedInAt = d.checkedInAt,
                            method = d.method,
                            date = d.date,
                            statusUnavailable = false,
                        )
                    }
                }
                // Status read failure is non-fatal: show the graceful "unavailable"
                // caption; the teacher can still attempt a manual check-in.
                is NetworkResult.Error, is NetworkResult.ConnectionError ->
                    _state.update { it.copy(isLoading = false, statusUnavailable = true) }
            }
        }
    }

    /**
     * Record a check-in with the honest [method] the biometric ladder produced
     * ("biometric" | "pin" | "manual"). Optimistically flips the pill to pending,
     * then reconciles to the server's authoritative response (idempotent).
     */
    fun checkIn(method: String, deviceId: String? = null) {
        if (_state.value.checkedIn || _state.value.isCheckingIn) return
        viewModelScope.launch {
            _state.update { it.copy(isCheckingIn = true, error = null) }
            val token = preferenceRepository.getUserToken().first()
            if (token == null) {
                _state.update { it.copy(isCheckingIn = false, error = "Not authenticated") }
                return@launch
            }
            val request = TeacherCheckInRequest(method = method, deviceId = deviceId)
            when (val result = repository.checkIn(token, request)) {
                is NetworkResult.Success -> {
                    val d = result.data.data
                    _state.update {
                        it.copy(
                            isCheckingIn = false,
                            checkedIn = d.checkedIn,
                            checkedInAt = d.checkedInAt, // server-stamped (authoritative)
                            method = d.method,
                            date = d.date,
                            error = null,
                        )
                    }
                }
                is NetworkResult.Error ->
                    _state.update { it.copy(isCheckingIn = false, error = result.message) }
                is NetworkResult.ConnectionError ->
                    _state.update { it.copy(isCheckingIn = false, error = "Connection error") }
            }
        }
    }
}

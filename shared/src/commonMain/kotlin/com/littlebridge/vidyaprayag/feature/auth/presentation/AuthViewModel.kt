package com.littlebridge.vidyaprayag.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.feature.auth.domain.model.*
import com.littlebridge.vidyaprayag.feature.auth.domain.repository.AuthRepository
import com.littlebridge.vidyaprayag.util.AppLogger
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class AuthStep {
    data object Identifier : AuthStep()
    data object LoginPassword : AuthStep()
    data object SignupDetails : AuthStep()
    data object Otp : AuthStep()
}

data class AuthUiState(
    val step: AuthStep = AuthStep.Identifier,
    val flow: AuthFlow? = null,
    val identifier: String = "",
    val name: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val otp: String = "",
    val role: String = "PARENT", // "ADMIN" | "TEACHER" | "PARENT"
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAuthSuccessful: Boolean = false
)

class AuthViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state = _state.asStateFlow()

    fun onIdentifierChanged(value: String) = _state.update { it.copy(identifier = value, error = null) }
    fun onNameChanged(value: String) = _state.update { it.copy(name = value) }
    fun onPasswordChanged(value: String) = _state.update { it.copy(password = value) }
    fun onConfirmPasswordChanged(value: String) = _state.update { it.copy(confirmPassword = value) }
    fun onOtpChanged(value: String) = _state.update { it.copy(otp = value) }
    fun onRoleChanged(value: String) = _state.update { it.copy(role = value) }

    fun onContinue() {
        val currentState = _state.value
        if (currentState.identifier.isBlank()) {
            _state.update { it.copy(error = "Please enter email or phone") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.checkUser(currentState.identifier)) {
                is NetworkResult.Success -> {
                    val flow = result.data
                    AppLogger.d("AuthViewModel", "Check user success. Flow determined: $flow")
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            flow = flow,
                            step = when (flow) {
                                AuthFlow.LOGIN_EMAIL -> AuthStep.LoginPassword
                                AuthFlow.SIGNUP_EMAIL -> AuthStep.SignupDetails
                                AuthFlow.LOGIN_PHONE, AuthFlow.SIGNUP_PHONE -> AuthStep.Otp
                            }
                        )
                    }
                    if (flow == AuthFlow.LOGIN_PHONE || flow == AuthFlow.SIGNUP_PHONE) {
                        repository.sendOtp(currentState.identifier, if (flow == AuthFlow.LOGIN_PHONE) "login" else "signup")
                    }
                }
                is NetworkResult.Error -> {
                    AppLogger.e("AuthViewModel", "Check user error: ${result.message}")
                    _state.update { it.copy(isLoading = false, error = result.message) }
                }
                is NetworkResult.ConnectionError -> {
                    AppLogger.e("AuthViewModel", "Check user: Connection Error")
                    _state.update { it.copy(isLoading = false, error = "Connection error. Please try again.") }
                }
            }
        }
    }

    fun onSubmit() {
        val currentState = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            val result = when (currentState.step) {
                AuthStep.LoginPassword -> {
                    repository.login(LoginRequest(
                        identifier = currentState.identifier,
                        password = currentState.password,
                        role = currentState.role
                    ))
                }
                AuthStep.SignupDetails -> {
                    if (currentState.name.isBlank()) {
                        _state.update { it.copy(isLoading = false, error = "Please enter your name") }
                        return@launch
                    }
                    if (currentState.password.isBlank()) {
                        _state.update { it.copy(isLoading = false, error = "Please create a password") }
                        return@launch
                    }
                    // Only enforce the confirm-password match when the UI actually collected a
                    // confirmation value. Otherwise confirmPassword is "" and the check would
                    // always fail even though the user only ever saw a single password field.
                    if (currentState.confirmPassword.isNotEmpty() &&
                        currentState.password != currentState.confirmPassword
                    ) {
                        _state.update { it.copy(isLoading = false, error = "Passwords do not match") }
                        return@launch
                    }
                    repository.signup(SignupRequest(
                        name = currentState.name,
                        identifier = currentState.identifier,
                        password = currentState.password,
                        role = currentState.role
                    ))
                }
                AuthStep.Otp -> {
                    if (currentState.flow == AuthFlow.SIGNUP_PHONE && currentState.name.isBlank()) {
                        _state.update { it.copy(isLoading = false, error = "Please enter your name") }
                        return@launch
                    }
                    
                    if (currentState.flow == AuthFlow.SIGNUP_PHONE) {
                        repository.signup(SignupRequest(
                            name = currentState.name,
                            identifier = currentState.identifier,
                            otp = currentState.otp,
                            role = currentState.role
                        ))
                    } else {
                        repository.login(LoginRequest(
                            identifier = currentState.identifier,
                            otp = currentState.otp,
                            role = currentState.role
                        ))
                    }
                }
                else -> return@launch
            }

            when (result) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isLoading = false, isAuthSuccessful = true) }
                }
                is NetworkResult.Error -> {
                    AppLogger.e("AuthViewModel", "Submit error: ${result.message}")
                    _state.update { it.copy(isLoading = false, error = result.message) }
                }
                is NetworkResult.ConnectionError -> {
                    AppLogger.e("AuthViewModel", "Submit: Connection Error")
                    _state.update { it.copy(isLoading = false, error = "Connection error. Please try again.") }
                }
            }
        }
    }
    
    fun goBack() {
        _state.update { it.copy(step = AuthStep.Identifier, error = null) }
    }

    /**
     * Clears every field of the auth form back to a pristine [AuthUiState].
     *
     * The [AuthViewModel] outlives a single sign-in attempt (it is bound to the unauth
     * ViewModelStoreOwner, so the same instance is reused after a logout → landing → login
     * round-trip). Without an explicit reset, the previously typed identifier/password —
     * and even a stale `isAuthSuccessful` flag — leak into the next login attempt. Each auth
     * screen calls this on mount so re-entering sign-in always starts from a blank form.
     */
    fun reset() {
        _state.value = AuthUiState()
    }
}

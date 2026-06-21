package com.littlebridge.vidyaprayag.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.vidyaprayag.core.prefs.PreferenceRepository
import com.littlebridge.vidyaprayag.feature.schools.domain.model.School
import com.littlebridge.vidyaprayag.feature.schools.domain.usecase.GetSchoolsUseCase
import com.littlebridge.vidyaprayag.domain.util.UiState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AuthState(
    val role: String? = null,
    val token: String? = null,
    val isLoaded: Boolean = false
)

class MainViewModel(
    private val getSchoolsUseCase: GetSchoolsUseCase,
    private val preferenceRepository: PreferenceRepository,
    private val authRepository: com.littlebridge.vidyaprayag.feature.auth.domain.repository.AuthRepository,
) : ViewModel() {

    private val _schools = MutableStateFlow<UiState<List<School>>>(UiState.Loading)
    val schools: StateFlow<UiState<List<School>>> = _schools.asStateFlow()

    val themeName: StateFlow<String> = preferenceRepository.getThemeName()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "LIGHT")

    val authState: StateFlow<AuthState> = combine(
        preferenceRepository.getUserRole(),
        preferenceRepository.getUserToken()
    ) { role, token ->
        AuthState(role, token, isLoaded = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AuthState())

    val userRole: StateFlow<String?> = preferenceRepository.getUserRole()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val userToken: StateFlow<String?> = preferenceRepository.getUserToken()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        refreshSchools()
    }

    fun refreshSchools() {
        viewModelScope.launch {
            _schools.value = UiState.Loading
            try {
                getSchoolsUseCase().collect { schoolList ->
                    _schools.value = UiState.Success(schoolList)
                }
            } catch (e: Exception) {
                _schools.value = UiState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    fun setTheme(name: String) {
        viewModelScope.launch {
            preferenceRepository.setThemeName(name)
        }
    }

    fun setRole(role: String) {
        viewModelScope.launch {
            preferenceRepository.setUserRole(role)
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}

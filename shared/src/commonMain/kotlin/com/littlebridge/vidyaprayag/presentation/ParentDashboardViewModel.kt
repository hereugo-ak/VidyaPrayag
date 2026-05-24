package com.littlebridge.vidyaprayag.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.vidyaprayag.domain.util.UiState
import com.littlebridge.vidyaprayag.feature.auth.domain.model.UserDetailsData
import com.littlebridge.vidyaprayag.feature.schools.domain.model.School
import com.littlebridge.vidyaprayag.feature.schools.domain.usecase.GetSchoolsUseCase
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ParentDashboardViewModel(
    private val getSchoolsUseCase: GetSchoolsUseCase,
    private val authRepository: com.littlebridge.vidyaprayag.feature.auth.domain.repository.AuthRepository,
    private val preferenceRepository: com.littlebridge.vidyaprayag.core.prefs.PreferenceRepository
) : ViewModel() {

    private val _schools = MutableStateFlow<UiState<List<School>>>(UiState.Loading)
    val schools: StateFlow<UiState<List<School>>> = _schools.asStateFlow()

    private val _userDetails = MutableStateFlow<UiState<UserDetailsData>>(UiState.Loading)
    val userDetails: StateFlow<UiState<UserDetailsData>> = _userDetails.asStateFlow()

    private val _shortlist = MutableStateFlow<Set<String>>(emptySet())
    val shortlist: StateFlow<Set<String>> = _shortlist.asStateFlow()

    private val _hasChildProfile = MutableStateFlow(false)
    val hasChildProfile: StateFlow<Boolean> = _hasChildProfile.asStateFlow()

    init {
        loadSchools()
        loadUserDetails()
    }

    private fun loadUserDetails() {
        viewModelScope.launch {
            _userDetails.value = UiState.Loading
            preferenceRepository.getUserToken().collect { token ->
                if (token != null) {
                    when (val result = authRepository.getUserDetails(token)) {
                        is NetworkResult.Success -> {
                            _userDetails.value = UiState.Success(result.data.data)
                        }
                        is NetworkResult.Error -> {
                            _userDetails.value = UiState.Error(result.message)
                        }
                        is NetworkResult.ConnectionError -> {
                            _userDetails.value = UiState.Error("Connection error")
                        }
                    }
                }
            }
        }
    }

    private fun loadSchools() {
        viewModelScope.launch {
            _schools.value = UiState.Loading
            try {
                getSchoolsUseCase().collect { schoolList ->
                    _schools.value = UiState.Success(schoolList)
                }
            } catch (e: Exception) {
                _schools.value = UiState.Error(e.message ?: "Failed to load schools")
            }
        }
    }

    fun toggleShortlist(schoolId: String) {
        val current = _shortlist.value
        if (current.contains(schoolId)) {
            _shortlist.value = current - schoolId
        } else {
            if (current.size < 3) {
                _shortlist.value = current + schoolId
            }
        }
    }
}

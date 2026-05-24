package com.littlebridge.vidyaprayag.feature.admin.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.vidyaprayag.domain.util.UiState
import com.littlebridge.vidyaprayag.feature.admin.domain.model.OnboardingStep
import com.littlebridge.vidyaprayag.feature.auth.domain.model.UserDetailsData
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.core.prefs.PreferenceRepository
import com.littlebridge.vidyaprayag.feature.auth.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class SchoolDashboardViewModel(
    private val authRepository: AuthRepository,
    private val preferenceRepository: PreferenceRepository
) : ViewModel() {
    private val _steps = MutableStateFlow<List<OnboardingStep>>(emptyList())
    val steps: StateFlow<List<OnboardingStep>> = _steps.asStateFlow()

    private val _userDetails = MutableStateFlow<UiState<UserDetailsData>>(UiState.Loading)
    val userDetails: StateFlow<UiState<UserDetailsData>> = _userDetails.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    init {
        loadSteps()
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

    private fun loadSteps() {
        viewModelScope.launch {
            _steps.value = listOf(
                OnboardingStep(1, "Institutional Basics", "School name, location, and IDs.", false, "https://lh3.googleusercontent.com/aida/ADBb0ui3w3WLMp1sm0R2kmuenNenOZqSGrVBKaJOeiOXpX6IPSgcckMovxvBj7g2dOp4l1iAyokDAmxblcKgEJfuqu0rl_FOvbj_O6L6wL4CFDq8AAtnNDIDgTiC1Y5YmVjcQ52_JrmxaNUSK1TMAosuIRz3dpD0biDFbXe4n7nd7PDtI9wnhziUfmXN9Hdu3tqVmYl0mfkV8Um4O9vkWSpZQ-Q9EEPt0ybBzu1OdjSk_erazYXNTAP9vlv-72WT"),
                OnboardingStep(2, "Branding & Identity", "Upload logos and color themes.", false, "https://lh3.googleusercontent.com/aida/ADBb0uivZGpYXMWFVbmza9NCLFM75_49F62AQo-FiHInRsISe_tI7P8vmFgQFv8ot3UKZDjtX-EZuhwKBUME7nCvPM0J_eFEAzUCDtvkHNV3XEGXqAbWv5EmLErs6DPRFHlIHkB1JrH96cNSReswDzQWg2KiniSrkj8V2C5KlWTXEH8zqopu3pS6LIGmNUHh_tCPXZUFPJ0dmxNDU-bS_AXbqNErjRlkot08PsScZ8y7_9dizEoBYQ_V905UQaRH"),
                OnboardingStep(3, "Academic Setup", "Classes, subjects, and teachers.", false, "https://lh3.googleusercontent.com/aida/ADBb0uieWqNAaRAQbAQiLIRFp0DQZ3DXwYjgUgdHDFuxWklfOWsKqMYs0ic9KrlMNMUGyx33EBTKK5oCs41V2FE1b_ZmSaNc8fKBhAz8ADU8bPAajI4bwxDCYeIMGwlWwXKOylU-sBoM-wn0T1SMJvq48WD-rCBapkzGDxKYxtgMlp28XatsV9TsMDTMxmbmsUcJEo8Cv-FAFhaFsmAfjqiR0mY7JUua-0-firD5nKVszFBZe5Yqpvq_JBS5_xE"),
                OnboardingStep(4, "Final Launch", "Verify and go live.", false)
            )
            updateProgress()
        }
    }

    private fun updateProgress() {
        val completed = _steps.value.count { it.isCompleted }
        _progress.value = if (_steps.value.isEmpty()) 0f else completed.toFloat() / _steps.value.size
    }
}

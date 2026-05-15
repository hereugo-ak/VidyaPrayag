package com.littlebridge.vidyaprayag.feature.admin.presentation

import androidx.lifecycle.ViewModel
import com.littlebridge.vidyaprayag.feature.admin.domain.model.OnboardingBasics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class InstitutionalBasicOBViewModel : ViewModel() {
    private val _state = MutableStateFlow(OnboardingBasics())
    val state: StateFlow<OnboardingBasics> = _state.asStateFlow()

    fun updateSchoolName(name: String) {
        _state.value = _state.value.copy(schoolName = name)
    }

    fun updateBoard(board: String) {
        _state.value = _state.value.copy(boardAffiliation = board)
    }

    fun updateEmail(email: String) {
        _state.value = _state.value.copy(officialEmail = email)
    }

    fun updateContact(number: String) {
        _state.value = _state.value.copy(contactNumber = number)
    }

    fun updateCountryCode(code: String) {
        _state.value = _state.value.copy(countryCode = code)
    }
}

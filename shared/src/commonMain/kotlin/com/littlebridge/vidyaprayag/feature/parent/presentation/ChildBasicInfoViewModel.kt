package com.littlebridge.vidyaprayag.feature.parent.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ChildBasicInfoState(
    val name: String = "",
    val grade: String = "",
    val dob: String = "",
    val selectedInterests: Set<String> = emptySet(),
    val availableInterests: List<String> = listOf("Science", "Visual Arts", "Mathematics", "Music", "Physical Ed", "Languages")
)

class ChildBasicInfoViewModel : ViewModel() {
    private val _state = MutableStateFlow(ChildBasicInfoState())
    val state: StateFlow<ChildBasicInfoState> = _state.asStateFlow()

    fun updateName(name: String) {
        _state.value = _state.value.copy(name = name)
    }

    fun updateGrade(grade: String) {
        _state.value = _state.value.copy(grade = grade)
    }

    fun updateDob(dob: String) {
        _state.value = _state.value.copy(dob = dob)
    }

    fun toggleInterest(interest: String) {
        val current = _state.value.selectedInterests
        val updated = if (current.contains(interest)) current - interest else current + interest
        _state.value = _state.value.copy(selectedInterests = updated)
    }
}

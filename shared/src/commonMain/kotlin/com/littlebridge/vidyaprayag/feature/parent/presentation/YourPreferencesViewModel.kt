package com.littlebridge.vidyaprayag.feature.parent.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SchoolPreference(
    val id: String,
    val title: String,
    val iconName: String
)

data class YourPreferencesState(
    val selectedPreferences: Set<String> = emptySet(),
    val budgetRange: ClosedFloatingPointRange<Float> = 2500f..8000f,
    val availablePreferences: List<SchoolPreference> = listOf(
        SchoolPreference("academics", "Top Academics", "school"),
        SchoolPreference("sports", "Sports Facilities", "sports_soccer"),
        SchoolPreference("arts", "Creative Arts", "palette"),
        SchoolPreference("holistic", "Holistic Growth", "psychology"),
        SchoolPreference("near_me", "Near Me", "location_on")
    )
)

class YourPreferencesViewModel : ViewModel() {
    private val _state = MutableStateFlow(YourPreferencesState())
    val state: StateFlow<YourPreferencesState> = _state.asStateFlow()

    fun togglePreference(id: String) {
        val current = _state.value.selectedPreferences
        val updated = if (current.contains(id)) current - id else current + id
        _state.value = _state.value.copy(selectedPreferences = updated)
    }

    fun updateBudgetRange(range: ClosedFloatingPointRange<Float>) {
        _state.value = _state.value.copy(budgetRange = range)
    }
}

package com.littlebridge.vidyaprayag.feature.parent.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LocationRequestState(
    val isLocationEnabled: Boolean = false,
    val features: List<LocationFeature> = listOf(
        LocationFeature(
            "Proximity Ranking",
            "See distance and estimated travel times from your location.",
            "distance"
        ),
        LocationFeature(
            "Interactive Mapping",
            "Explore institutional profiles in a dynamic map view.",
            "map"
        )
    )
)

data class LocationFeature(
    val title: String,
    val description: String,
    val iconName: String
)

class LocationRequestViewModel : ViewModel() {
    private val _state = MutableStateFlow(LocationRequestState())
    val state: StateFlow<LocationRequestState> = _state.asStateFlow()

    fun onLocationEnabled() {
        _state.value = _state.value.copy(isLocationEnabled = true)
    }
}

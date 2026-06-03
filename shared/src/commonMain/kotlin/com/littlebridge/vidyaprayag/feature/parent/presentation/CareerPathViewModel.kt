package com.littlebridge.vidyaprayag.feature.parent.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CareerMatch(
    val title: String,
    val matchPercentage: Int,
    val imageUrl: String,
    val industryGrowth: String,
    val tags: List<String>
)

data class CareerPathState(
    val predictedCount: Int = 12,
    val topMatch: CareerMatch = CareerMatch(
        title = "Aerospace Engineering",
        matchPercentage = 98,
        imageUrl = "https://picsum.photos/seed/vidyaprayag-career/640/360",
        industryGrowth = "Global Market",
        tags = listOf("STEM Focus", "Innovation", "High Growth")
    )
)

class CareerPathViewModel : ViewModel() {
    private val _state = MutableStateFlow(CareerPathState())
    val state: StateFlow<CareerPathState> = _state.asStateFlow()
}

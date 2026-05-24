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
        imageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuCRHBDXZqXjScvsWfe04wMOkXm0iLVFISpCreRVHsvmAdATV5-07X8DFkoQA3eq3_LQSVJwxq4Fhe55Iw5jMr74EN4JV3HOx20G2cr-38dGzZCxnzwNyC87XWd8BiFYNn5io4mPYa0xr6-ZQKxIIH_u8ZnAga7QnnlViM5ykSHGmq800q6fR4tIekTK-MTSyKmACFc3S4IP_vCIgYfxcteuqCEaA84-z5YTcEukwMXb77cS_Efnp4kDUJVM7ZU3Hm3OLckYvKZWioiP",
        industryGrowth = "Global Market",
        tags = listOf("STEM Focus", "Innovation", "High Growth")
    )
)

class CareerPathViewModel : ViewModel() {
    private val _state = MutableStateFlow(CareerPathState())
    val state: StateFlow<CareerPathState> = _state.asStateFlow()
}

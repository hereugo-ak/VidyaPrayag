package com.littlebridge.vidyaprayag.feature.admin.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BrandingInfoState(
    val coverImageUrl: String? = null,
    val logoUrl: String? = null,
    val pedagogicalMission: String = "",
    val visionStatement: String = "",
    val virtualTourUrl: String = ""
)

class BrandingInfoOBViewModel : ViewModel() {
    private val _state = MutableStateFlow(BrandingInfoState())
    val state: StateFlow<BrandingInfoState> = _state.asStateFlow()

    fun updateCoverImage(url: String) {
        _state.value = _state.value.copy(coverImageUrl = url)
    }

    fun updateLogo(url: String) {
        _state.value = _state.value.copy(logoUrl = url)
    }

    fun updatePedagogicalMission(text: String) {
        _state.value = _state.value.copy(pedagogicalMission = text)
    }

    fun updateVisionStatement(text: String) {
        _state.value = _state.value.copy(visionStatement = text)
    }

    fun updateVirtualTour(url: String) {
        _state.value = _state.value.copy(virtualTourUrl = url)
    }
}

package com.littlebridge.vidyaprayag.feature.admin.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.core.prefs.PreferenceRepository
import com.littlebridge.vidyaprayag.feature.admin.domain.model.ObPayloadKeys
import com.littlebridge.vidyaprayag.feature.admin.domain.model.ObStepType
import com.littlebridge.vidyaprayag.feature.admin.domain.model.OnboardingSubmitRequest
import com.littlebridge.vidyaprayag.feature.admin.domain.repository.OnboardingRepository
import com.littlebridge.vidyaprayag.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

data class BrandingInfoState(
    val coverImageUrl: String? = null,
    val logoUrl: String? = null,
    val pedagogicalMission: String = "",
    val visionStatement: String = "",
    val virtualTourUrl: String = "",
    /**
     * Hex string sent to the backend as `brand_color`. Defaults to the
     * brand-blue used by the rest of the app. We don't expose a color
     * picker in the UI yet, but the backend already supports persisting
     * this so we keep the slot ready.
     */
    val brandColorHex: String = "#2563EB"
)

/**
 * ViewModel for the **second** onboarding step: Branding & Showcase.
 *
 * Submits to `POST /api/v1/onboarding/submit` with `ob_step_type = "BRANDING"`.
 * The server's BRANDING schema currently persists two keys: `logo_url` and
 * `brand_color`. We forward those exactly. Mission / vision / tour fields
 * exist on the screen but are not yet part of the server schema, so they
 * remain local until the backend schema is widened.
 */
class BrandingInfoOBViewModel(
    private val onboardingRepository: OnboardingRepository,
    private val preferenceRepository: PreferenceRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BrandingInfoState())
    val state: StateFlow<BrandingInfoState> = _state.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // ---------- Form mutations (signatures unchanged) ----------
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

    fun updateBrandColor(hex: String) {
        _state.value = _state.value.copy(brandColorHex = hex)
    }

    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * POST the current branding info to /api/v1/onboarding/submit with
     * ob_step_type = "BRANDING", and on success call [onSuccess] (typically
     * a navigation to the next screen).
     */
    fun submit(onSuccess: () -> Unit) {
        if (_isSubmitting.value) return

        viewModelScope.launch {
            _isSubmitting.value = true
            _errorMessage.value = null

            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _errorMessage.value = "You are not signed in. Please log in again."
                _isSubmitting.value = false
                return@launch
            }

            val current = _state.value
            val payload = JsonObject(
                buildMap {
                    // Logo URL is optional - only send if user provided one.
                    current.logoUrl?.takeIf { it.isNotBlank() }?.let {
                        put(ObPayloadKeys.LOGO_URL, JsonPrimitive(it.trim()))
                    }
                    put(ObPayloadKeys.BRAND_COLOR, JsonPrimitive(current.brandColorHex.trim()))
                }
            )

            val request = OnboardingSubmitRequest(
                obStepType = ObStepType.BRANDING,
                isFinalSubmission = false,
                dataPayload = payload
            )

            when (val result = onboardingRepository.submitStep(token, request)) {
                is NetworkResult.Success -> {
                    AppLogger.d(
                        "OnboardingBranding",
                        "BRANDING step submitted. nextStep=${result.data.nextStep}"
                    )
                    _isSubmitting.value = false
                    onSuccess()
                }
                is NetworkResult.Error -> {
                    AppLogger.e("OnboardingBranding", "Submit failed: ${result.message} (code=${result.code})")
                    _errorMessage.value = result.message
                    _isSubmitting.value = false
                }
                is NetworkResult.ConnectionError -> {
                    AppLogger.e("OnboardingBranding", "Connection error while submitting BRANDING step")
                    _errorMessage.value = "No internet connection. Please try again."
                    _isSubmitting.value = false
                }
            }
        }
    }
}

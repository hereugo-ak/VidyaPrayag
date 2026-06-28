package com.littlebridge.enrollplus.feature.branding.presentation

import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.core.prefs.PreferenceRepository
import com.littlebridge.enrollplus.feature.branding.domain.model.SchoolBranding
import com.littlebridge.enrollplus.feature.branding.domain.repository.BrandingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * App-lifecycle singleton that holds the current school's branding.
 *
 * Fetched once after login; observed by [NavGraphV2] to override the
 * active theme's accent colors with the school's brand palette.
 *
 * Call [loadBranding] after authentication succeeds.
 * Call [clear] on logout.
 */
class BrandingThemeManager(
    private val repository: BrandingRepository,
    private val prefs: PreferenceRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _branding = MutableStateFlow<SchoolBranding?>(null)
    val branding: StateFlow<SchoolBranding?> = _branding.asStateFlow()

    fun loadBranding() {
        scope.launch {
            val token = prefs.getUserToken().first() ?: return@launch
            when (val result = repository.getBranding(token)) {
                is NetworkResult.Success -> {
                    val branding = result.data.data
                    if (branding != null && branding.isCustomized) {
                        _branding.value = branding
                    } else {
                        _branding.value = null
                    }
                }
                is NetworkResult.Error,
                is NetworkResult.ConnectionError -> {
                    // Silently fail — app falls back to default theme
                }
            }
        }
    }

    fun clear() {
        _branding.value = null
    }
}

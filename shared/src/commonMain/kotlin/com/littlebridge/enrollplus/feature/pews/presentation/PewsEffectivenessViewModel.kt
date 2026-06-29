/*
 * File: PewsEffectivenessViewModel.kt
 * Module: feature.pews.presentation
 *
 * ViewModel for the LEARN dashboard — shows effectiveness rollup +
 * cohort risk trend over time. Loaded from GET /pews/trend (which
 * bundles both the trend points and the effectiveness summary).
 */
package com.littlebridge.enrollplus.feature.pews.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.core.prefs.PreferenceRepository
import com.littlebridge.enrollplus.feature.pews.domain.model.PewsEffectivenessTrendDto
import com.littlebridge.enrollplus.feature.pews.domain.repository.PewsRepository
import com.littlebridge.enrollplus.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class PewsEffectivenessState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val trend: PewsEffectivenessTrendDto? = null,
) {
    val isEmpty: Boolean get() = !isLoading && error == null && trend == null
}

class PewsEffectivenessViewModel(
    private val repository: PewsRepository,
    private val preferenceRepository: PreferenceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PewsEffectivenessState())
    val state: StateFlow<PewsEffectivenessState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = _state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val t = token() ?: run {
                _state.value = _state.value.copy(isLoading = false, error = "Not signed in")
                return@launch
            }
            when (val r = repository.getTrend(t, days = 30)) {
                is NetworkResult.Success ->
                    _state.value = _state.value.copy(isLoading = false, trend = r.data.data)
                is NetworkResult.Error -> {
                    AppLogger.e("PewsEffectVM", "getTrend error: ${r.message}")
                    _state.value = _state.value.copy(isLoading = false, error = r.message)
                }
                is NetworkResult.ConnectionError ->
                    _state.value = _state.value.copy(isLoading = false, error = "Connection error. Check your internet.")
            }
        }
    }

    private suspend fun token(): String? =
        preferenceRepository.getUserToken().first()
}

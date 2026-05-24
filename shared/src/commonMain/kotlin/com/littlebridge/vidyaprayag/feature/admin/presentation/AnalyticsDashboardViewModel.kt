package com.littlebridge.vidyaprayag.feature.admin.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.core.prefs.PreferenceRepository
import com.littlebridge.vidyaprayag.feature.admin.domain.repository.AnalyticsRepository
import com.littlebridge.vidyaprayag.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement

data class AnalyticsDashboardState(
    val performanceTrend: List<Float> = emptyList(),
    val currentGrowth: String = "0%",
    val cards: List<JsonElement> = emptyList(),
    val insights: List<JsonElement> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class AnalyticsDashboardViewModel(
    private val analyticsRepository: AnalyticsRepository,
    private val preferenceRepository: PreferenceRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AnalyticsDashboardState())
    val state: StateFlow<AnalyticsDashboardState> = _state.asStateFlow()

    init { loadOverview() }

    fun loadOverview() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isLoading = false)
                return@launch
            }
            when (val result = analyticsRepository.getOverview(token)) {
                is NetworkResult.Success -> {
                    val data = result.data.data
                    _state.value = _state.value.copy(
                        performanceTrend = data?.performanceTrend?.map { it.toFloat() } ?: emptyList(),
                        currentGrowth = data?.currentGrowth ?: "0%",
                        cards = data?.cards ?: emptyList(),
                        insights = data?.insights ?: emptyList(),
                        isLoading = false
                    )
                }
                is NetworkResult.Error -> {
                    AppLogger.e("AnalyticsDashboardVM", "getOverview error: ${result.message}")
                    _state.value = _state.value.copy(isLoading = false, errorMessage = result.message)
                }
                is NetworkResult.ConnectionError -> {
                    AppLogger.e("AnalyticsDashboardVM", "getOverview connection error")
                    _state.value = _state.value.copy(isLoading = false, errorMessage = "Connection error. Check your internet.")
                }
            }
        }
    }
}

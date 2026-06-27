package com.littlebridge.enrollplus.feature.health.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.core.prefs.PreferenceRepository
import com.littlebridge.enrollplus.feature.health.domain.model.HealthAlertDto
import com.littlebridge.enrollplus.feature.health.domain.repository.HealthRepository
import com.littlebridge.enrollplus.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class TeacherHealthAlertsState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val alerts: List<HealthAlertDto> = emptyList(),
)

class TeacherHealthAlertsViewModel(
    private val repository: HealthRepository,
    private val prefs: PreferenceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TeacherHealthAlertsState())
    val state: StateFlow<TeacherHealthAlertsState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val token = prefs.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isLoading = false, error = "You are not signed in. Please log in again.")
                return@launch
            }
            when (val r = repository.getHealthAlerts(token)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = null,
                        alerts = r.data.data?.alerts.orEmpty(),
                    )
                }
                is NetworkResult.Error -> {
                    AppLogger.e("TeacherHealthVM", "getHealthAlerts error: ${r.message}")
                    _state.value = _state.value.copy(isLoading = false, error = r.message)
                }
                is NetworkResult.ConnectionError -> {
                    _state.value = _state.value.copy(isLoading = false, error = "Connection error. Check your internet.")
                }
            }
        }
    }
}

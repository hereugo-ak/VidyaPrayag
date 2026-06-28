/*
 * File: PewsCohortViewModel.kt
 * Module: feature.pews.presentation
 *
 * Drives the school-admin PEWS cohort screen: the live at-risk roster
 * (GET /api/v1/school/pews/cohort), a manual recompute (POST .../run), and the
 * config toggles (GET/PUT .../config). Three UI states via [PewsCohortState]
 * (LAW 3). Every student shown is a real deterministic snapshot — the screen
 * never invents anyone (RA-S10 / LAW 6).
 */
package com.littlebridge.enrollplus.feature.pews.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.core.prefs.PreferenceRepository
import com.littlebridge.enrollplus.feature.pews.domain.model.PewsCohortDto
import com.littlebridge.enrollplus.feature.pews.domain.model.PewsConfigDto
import com.littlebridge.enrollplus.feature.pews.domain.model.PewsEffectivenessDto
import com.littlebridge.enrollplus.feature.pews.domain.repository.PewsRepository
import com.littlebridge.enrollplus.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class PewsCohortState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val cohort: PewsCohortDto? = null,
    // filter band: watch | medium | high
    val minLevel: String = "watch",
    // manual recompute
    val isRunning: Boolean = false,
    val infoMessage: String? = null,
    // config sheet
    val config: PewsConfigDto? = null,
    val isSavingConfig: Boolean = false,
    val configError: String? = null,
    // effectiveness rollup (the LEARN loop) — admin parity with the web portal
    val effectiveness: PewsEffectivenessDto? = null,
) {
    val isEmpty: Boolean get() = !isLoading && error == null && (cohort?.students?.isEmpty() ?: true)
}

class PewsCohortViewModel(
    private val repository: PewsRepository,
    private val preferenceRepository: PreferenceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PewsCohortState())
    val state: StateFlow<PewsCohortState> = _state.asStateFlow()

    init { load() }

    private suspend fun token(): String? = preferenceRepository.getUserToken().first()

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val t = token()
            if (t.isNullOrBlank()) {
                _state.value = _state.value.copy(isLoading = false, error = "You are not signed in. Please log in again.")
                return@launch
            }
            when (val r = repository.getCohort(t, _state.value.minLevel)) {
                is NetworkResult.Success ->
                    _state.value = _state.value.copy(isLoading = false, error = null, cohort = r.data.data)
                is NetworkResult.Error -> {
                    AppLogger.e("PewsCohortVM", "getCohort error: ${r.message}")
                    _state.value = _state.value.copy(isLoading = false, error = r.message)
                }
                is NetworkResult.ConnectionError ->
                    _state.value = _state.value.copy(isLoading = false, error = "Connection error. Check your internet.")
            }
            // Side-load the admin extras (non-fatal) so the cohort screen has the
            // same effectiveness + config surface as the web portal.
            loadEffectiveness()
            loadConfig()
        }
    }

    /** Effectiveness rollup (LEARN loop). Non-fatal: failures leave the card hidden. */
    fun loadEffectiveness() {
        viewModelScope.launch {
            val t = token() ?: return@launch
            when (val r = repository.getEffectiveness(t)) {
                is NetworkResult.Success ->
                    _state.value = _state.value.copy(effectiveness = r.data.data)
                else -> { /* non-fatal: the effectiveness card simply stays hidden */ }
            }
        }
    }

    /** Change the minimum risk band shown and reload. */
    fun setMinLevel(level: String) {
        if (level == _state.value.minLevel) return
        _state.value = _state.value.copy(minLevel = level)
        load()
    }

    /** Manually recompute the cohort now (POST .../run), then reload. */
    fun runNow() {
        viewModelScope.launch {
            val t = token() ?: run {
                _state.value = _state.value.copy(error = "You are not signed in. Please log in again.")
                return@launch
            }
            _state.value = _state.value.copy(isRunning = true, infoMessage = null)
            when (val r = repository.runNow(t)) {
                is NetworkResult.Success -> {
                    val n = r.data.data?.atRisk ?: 0
                    _state.value = _state.value.copy(isRunning = false, infoMessage = "Recompute complete — $n students need attention")
                    load()
                    loadEffectiveness()
                }
                is NetworkResult.Error -> {
                    AppLogger.e("PewsCohortVM", "runNow error: ${r.message}")
                    _state.value = _state.value.copy(isRunning = false, error = r.message)
                }
                is NetworkResult.ConnectionError ->
                    _state.value = _state.value.copy(isRunning = false, error = "Connection error. Check your internet.")
            }
        }
    }

    fun loadConfig() {
        viewModelScope.launch {
            val t = token() ?: return@launch
            when (val r = repository.getConfig(t)) {
                is NetworkResult.Success -> _state.value = _state.value.copy(config = r.data.data, configError = null)
                is NetworkResult.Error -> _state.value = _state.value.copy(configError = r.message)
                is NetworkResult.ConnectionError -> _state.value = _state.value.copy(configError = "Connection error. Check your internet.")
            }
        }
    }

    fun saveConfig(config: PewsConfigDto) {
        viewModelScope.launch {
            val t = token() ?: run {
                _state.value = _state.value.copy(configError = "You are not signed in. Please log in again.")
                return@launch
            }
            _state.value = _state.value.copy(isSavingConfig = true, configError = null)
            when (val r = repository.updateConfig(t, config)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(isSavingConfig = false, config = r.data.data, infoMessage = "Settings saved")
                    load()
                }
                is NetworkResult.Error -> {
                    AppLogger.e("PewsCohortVM", "saveConfig error: ${r.message}")
                    _state.value = _state.value.copy(isSavingConfig = false, configError = r.message)
                }
                is NetworkResult.ConnectionError ->
                    _state.value = _state.value.copy(isSavingConfig = false, configError = "Connection error. Check your internet.")
            }
        }
    }

    fun clearMessages() {
        _state.value = _state.value.copy(infoMessage = null, configError = null)
    }
}

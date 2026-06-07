package com.littlebridge.vidyaprayag.feature.parent.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.core.prefs.PreferenceRepository
import com.littlebridge.vidyaprayag.feature.parent.domain.model.DashboardAlertDto
import com.littlebridge.vidyaprayag.feature.parent.domain.model.DashboardChildSummary
import com.littlebridge.vidyaprayag.feature.parent.domain.model.FeaturedSchoolDto
import com.littlebridge.vidyaprayag.feature.parent.domain.repository.ParentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** UI-facing parent-home state, sourced from the real `GET /api/v1/parent/dashboard`. */
data class ParentHomeState(
    val greeting: String = "",
    val childSummary: DashboardChildSummary? = null,
    val alerts: List<DashboardAlertDto> = emptyList(),
    val featuredSchools: List<FeaturedSchoolDto> = emptyList(),
    val curationLogic: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
)

/**
 * ParentHomeViewModel — drives [ParentHomeScreenV2] off the real
 * `GET /api/v1/parent/dashboard` endpoint (audit findings **J** §8.1 + **SHAREDVM** §5.4).
 *
 * Before this fix the Home tab borrowed `TrackProgressViewModel` (the *academics* VM,
 * also used by the Academics tab) as a stand-in, so the richest, genuinely-real parent
 * endpoint — child summary + overdue-fee alerts + featured schools + greeting — was
 * orphaned (never called by any client), and Home/Academics shared one VM/state.
 * This VM gives Home its own real source of truth and unshares the two tabs.
 */
class ParentHomeViewModel(
    private val repository: ParentRepository,
    private val preferenceRepository: PreferenceRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ParentHomeState())
    val state: StateFlow<ParentHomeState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val token = preferenceRepository.getUserToken().first()
            if (token == null) {
                _state.update { it.copy(isLoading = false, error = "Not signed in") }
                return@launch
            }
            when (val result = repository.getDashboard(token)) {
                is NetworkResult.Success -> {
                    val data = result.data.data
                    _state.update {
                        it.copy(
                            isLoading = false,
                            greeting = data.greeting,
                            childSummary = data.childSummary,
                            alerts = data.alerts,
                            featuredSchools = data.featuredSchools,
                            curationLogic = data.curationLogic,
                        )
                    }
                }
                is NetworkResult.Error ->
                    _state.update { it.copy(isLoading = false, error = result.message) }
                is NetworkResult.ConnectionError ->
                    _state.update { it.copy(isLoading = false, error = "Connection error") }
            }
        }
    }
}

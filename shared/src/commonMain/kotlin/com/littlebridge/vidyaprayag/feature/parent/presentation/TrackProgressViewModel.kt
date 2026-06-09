package com.littlebridge.vidyaprayag.feature.parent.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.core.prefs.PreferenceRepository
import com.littlebridge.vidyaprayag.feature.parent.domain.model.*
import com.littlebridge.vidyaprayag.feature.parent.domain.repository.ParentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AchievementBadge(
    val title: String,
    val iconName: String,
    val isLocked: Boolean = false,
    val gradientColors: List<Long>
)

data class AcademicCompetency(
    val title: String,
    val iconName: String,
    val progress: Float
)

data class PlayIndicator(
    val title: String,
    val description: String,
    val imageUrl: String,
    val isMet: Boolean = true
)

data class TrackProgressState(
    val childName: String = "",
    // RA-S06: the logged-in parent's own display name (persisted at sign-in,
    // RA-S03), used for the account avatar in the portal header instead of the
    // hardcoded literal "Parent". Empty until resolved.
    val accountName: String = "",
    val overallProgress: Float = 0f,
    val currentLevel: Int = 0,
    val journeyDescription: String = "",
    val badges: List<AchievementBadge> = emptyList(),
    val academicCompetencies: List<AcademicCompetency> = emptyList(),
    val emotionalIntelligence: Map<String, Float> = emptyMap(),
    val playIndicators: List<PlayIndicator> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class TrackProgressViewModel(
    private val repository: ParentRepository,
    private val preferenceRepository: PreferenceRepository
) : ViewModel() {
    private val _state = MutableStateFlow(TrackProgressState())
    val state: StateFlow<TrackProgressState> = _state.asStateFlow()

    init {
        loadTrackProgress()
        observeAccountName()
    }

    // RA-S06: keep the parent's own display name in state so the header avatar
    // greets the real user. Read reactively so a profile refresh (which
    // re-persists the name via getUserDetails) updates the header live.
    private fun observeAccountName() {
        viewModelScope.launch {
            preferenceRepository.getUserName().collect { name ->
                _state.update { it.copy(accountName = name ?: "") }
            }
        }
    }

    private fun loadTrackProgress() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            preferenceRepository.getUserToken().collect { token ->
                if (token != null) {
                    when (val result = repository.getTrackProgress(token)) {
                        is NetworkResult.Success -> {
                            val data = result.data.data
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    childName = data.childName,
                                    overallProgress = data.overallProgress,
                                    currentLevel = data.currentLevel,
                                    journeyDescription = data.journeyDescription,
                                    badges = data.badges.map { b ->
                                        AchievementBadge(b.title, b.iconName, b.isLocked, b.gradientColors)
                                    },
                                    academicCompetencies = data.academicCompetencies.map { c ->
                                        AcademicCompetency(c.title, c.iconName, c.progress)
                                    },
                                    emotionalIntelligence = data.emotionalIntelligence,
                                    playIndicators = data.playIndicators.map { p ->
                                        PlayIndicator(p.title, p.description, p.imageUrl, p.isMet)
                                    }
                                )
                            }
                        }
                        is NetworkResult.Error -> {
                            _state.update { it.copy(isLoading = false, error = result.message) }
                        }
                        is NetworkResult.ConnectionError -> {
                            _state.update { it.copy(isLoading = false, error = "Connection error") }
                        }
                    }
                }
            }
        }
    }
}

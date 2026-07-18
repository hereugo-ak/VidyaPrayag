package com.littlebridge.enrollplus.feature.admin.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.core.prefs.PreferenceRepository
import com.littlebridge.enrollplus.feature.admin.domain.model.SetupProgress
import com.littlebridge.enrollplus.feature.admin.domain.repository.OnboardingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SchoolSetupState(
    val progress: SetupProgress? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
) {
    val isVisible: Boolean get() = progress?.setupComplete == false
}

/**
 * Owns the post-onboarding operational setup checklist. Visibility is based only
 * on the server's current school-scoped counts; no local completion flag can
 * permanently hide an unfinished setup or resurrect a completed one.
 */
class SchoolSetupViewModel(
    private val onboardingRepository: OnboardingRepository,
    private val preferenceRepository: PreferenceRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(SchoolSetupState())
    val state: StateFlow<SchoolSetupState> = _state.asStateFlow()

    init {
        load()
    }

    fun load(forceRefresh: Boolean = false) {
        val current = _state.value
        if (current.isLoading || current.isRefreshing) return
        if (!forceRefresh && current.progress != null) return

        val hasProgress = current.progress != null
        _state.value = current.copy(
            isLoading = !hasProgress,
            isRefreshing = hasProgress,
            errorMessage = null,
        )

        viewModelScope.launch {
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.update { it.copy(isLoading = false, isRefreshing = false, errorMessage = "Session expired") }
                return@launch
            }
            when (val result = onboardingRepository.getSetupProgress(token)) {
                is NetworkResult.Success -> _state.update {
                    it.copy(progress = result.data, isLoading = false, isRefreshing = false, errorMessage = null)
                }
                is NetworkResult.Error -> _state.update {
                    it.copy(isLoading = false, isRefreshing = false, errorMessage = result.message)
                }
                is NetworkResult.ConnectionError -> _state.update {
                    it.copy(isLoading = false, isRefreshing = false, errorMessage = "Connection error")
                }
            }
        }
    }
}

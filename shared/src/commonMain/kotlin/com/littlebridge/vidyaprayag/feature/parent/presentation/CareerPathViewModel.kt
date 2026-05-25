package com.littlebridge.vidyaprayag.feature.parent.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.core.prefs.PreferenceRepository
import com.littlebridge.vidyaprayag.feature.parent.domain.repository.ParentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CareerMatch(
    val title: String,
    val matchPercentage: Int,
    val imageUrl: String,
    val industryGrowth: String,
    val tags: List<String>
)

data class CareerPathState(
    val predictedCount: Int = 0,
    val topMatch: CareerMatch = CareerMatch("", 0, "", "", emptyList()),
    val isLoading: Boolean = false,
    val error: String? = null
)

class CareerPathViewModel(
    private val repository: ParentRepository,
    private val preferenceRepository: PreferenceRepository
) : ViewModel() {
    private val _state = MutableStateFlow(CareerPathState())
    val state: StateFlow<CareerPathState> = _state.asStateFlow()

    init {
        loadCareerPath()
    }

    private fun loadCareerPath() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            preferenceRepository.getUserToken().collect { token ->
                if (token != null) {
                    when (val result = repository.getCareerPath(token)) {
                        is NetworkResult.Success -> {
                            val data = result.data.data
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    predictedCount = data.careerStats.predictedCount,
                                    topMatch = CareerMatch(
                                        title = data.careerStats.topMatch.title,
                                        matchPercentage = data.careerStats.topMatch.matchPercentage,
                                        imageUrl = data.careerStats.topMatch.imageUrl,
                                        industryGrowth = data.careerStats.topMatch.industryGrowthValue,
                                        tags = data.careerStats.topMatch.tags.map { t -> t.label }
                                    )
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

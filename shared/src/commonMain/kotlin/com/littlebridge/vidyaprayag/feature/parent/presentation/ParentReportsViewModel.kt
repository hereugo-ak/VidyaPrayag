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

data class AssessmentItem(
    val id: String,
    val subject: String,
    val date: String,
    val score: Int,
    val totalScore: Int,
    val classAverage: Int,
    val iconName: String
)

data class ParentReportsState(
    val childName: String = "",
    val termNarrative: String = "",
    val averageScore: Int = 0,
    val globalSubjectRank: Int = 0,
    val totalStudents: Int = 0,
    val improvementTrend: Float = 0f,
    val monthlyScores: List<Int> = emptyList(),
    val assessmentHistory: List<AssessmentItem> = emptyList(),
    val teacherRemarks: String = "",
    val leadInstructor: String = "",
    val pewsStatus: String = "",
    val pewsAlert: String = "",
    val learningStreak: Int = 0,
    val skillTrajectory: Map<String, Int> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class ParentReportsViewModel(
    private val repository: ParentRepository,
    private val preferenceRepository: PreferenceRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ParentReportsState())
    val state: StateFlow<ParentReportsState> = _state.asStateFlow()

    init {
        loadReports()
    }

    private fun loadReports() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            preferenceRepository.getUserToken().collect { token ->
                if (token != null) {
                    when (val result = repository.getReports(token)) {
                        is NetworkResult.Success -> {
                            val data = result.data.data
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    childName = data.childName,
                                    termNarrative = data.termNarrative,
                                    averageScore = data.averageScore,
                                    globalSubjectRank = data.globalSubjectRank,
                                    totalStudents = data.totalStudents,
                                    improvementTrend = data.improvementTrend,
                                    monthlyScores = data.monthlyScores,
                                    assessmentHistory = data.assessmentHistory.mapIndexed { i, a ->
                                        AssessmentItem(i.toString(), a.subject, a.date, a.score, a.totalScore, a.classAverage, a.iconName)
                                    },
                                    teacherRemarks = data.teacherRemarks,
                                    leadInstructor = data.leadInstructor,
                                    pewsStatus = data.pewsStatus,
                                    pewsAlert = data.pewsAlert,
                                    learningStreak = data.learningStreak,
                                    skillTrajectory = data.skillTrajectory
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

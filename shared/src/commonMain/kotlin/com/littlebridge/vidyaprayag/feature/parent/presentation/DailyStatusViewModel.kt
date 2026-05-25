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

data class TopicCovered(
    val id: String,
    val subject: String,
    val title: String,
    val description: String
)

data class HomeworkTask(
    val id: String,
    val subject: String,
    val title: String,
    val description: String,
    val isCritical: Boolean = false
)

data class UpcomingTest(
    val id: String,
    val month: String,
    val day: String,
    val subject: String,
    val topic: String,
    val isSecondary: Boolean = false
)

data class DailyStatusState(
    val childName: String = "",
    val absenceAlert: String? = null,
    val attendancePercentage: Int = 0,
    val attendanceNote: String = "",
    val topicsCovered: List<TopicCovered> = emptyList(),
    val homeworkTasks: List<HomeworkTask> = emptyList(),
    val upcomingTests: List<UpcomingTest> = emptyList(),
    val streakDays: Int = 0,
    val streakMessage: String = "",
    val schoolMessage: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

class DailyStatusViewModel(
    private val repository: ParentRepository,
    private val preferenceRepository: PreferenceRepository
) : ViewModel() {
    private val _state = MutableStateFlow(DailyStatusState())
    val state: StateFlow<DailyStatusState> = _state.asStateFlow()

    init {
        loadDailyStatus()
    }

    private fun loadDailyStatus() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            preferenceRepository.getUserToken().collect { token ->
                if (token != null) {
                    when (val result = repository.getDailyStatus(token)) {
                        is NetworkResult.Success -> {
                            val data = result.data.data
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    childName = data.childName,
                                    absenceAlert = data.absenceAlert,
                                    attendancePercentage = data.attendancePercentage,
                                    attendanceNote = data.attendanceNote,
                                    topicsCovered = data.topicsCovered.mapIndexed { i, t ->
                                        TopicCovered(i.toString(), t.subject, t.title, t.description)
                                    },
                                    homeworkTasks = data.homeworkTasks.mapIndexed { i, h ->
                                        HomeworkTask(i.toString(), h.subject, h.title, h.description, h.isCritical)
                                    },
                                    upcomingTests = data.upcomingTests.mapIndexed { i, u ->
                                        UpcomingTest(i.toString(), u.month, u.day, u.subject, u.topic, u.isSecondary)
                                    },
                                    streakDays = data.streakDays,
                                    streakMessage = data.streakMessage,
                                    schoolMessage = data.schoolMessage
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

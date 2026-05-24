package com.littlebridge.vidyaprayag.feature.parent.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
    val childName: String = "Rahul V.",
    val absenceAlert: String? = "Rahul was not marked present for the first-period assembly. No prior leave application was found in the system.",
    val attendancePercentage: Int = 90,
    val attendanceNote: String = "Arrived 15m late for Math; attended all other sessions.",
    val topicsCovered: List<TopicCovered> = listOf(
        TopicCovered("1", "Biology", "Photosynthesis Phase II", "Exploration of the Calvin cycle and light-independent reactions."),
        TopicCovered("2", "Literature", "The Great Gatsby", "Analysis of symbolism in Chapters 3-4 and character motivations.")
    ),
    val homeworkTasks: List<HomeworkTask> = listOf(
        HomeworkTask("1", "Math", "Quadrants Worksheet", "Complete exercises 1-15 on page 84.", isCritical = true),
        HomeworkTask("2", "Chemistry", "Lab Report Draft", "Observations from the titration experiment.")
    ),
    val upcomingTests: List<UpcomingTest> = listOf(
        UpcomingTest("1", "OCT", "27", "Physics: Mid-Term", "Quantum Mechanics & Dynamics"),
        UpcomingTest("2", "NOV", "02", "History Quiz", "Post-War Economics", isSecondary = true)
    ),
    val streakDays: Int = 12,
    val streakMessage: String = "Rahul has completed all homework on time for 12 consecutive days. Keep it up!",
    val schoolMessage: String = "Parents, please note the upcoming Winter Break starts Dec 15th."
)

class DailyStatusViewModel : ViewModel() {
    private val _state = MutableStateFlow(DailyStatusState())
    val state: StateFlow<DailyStatusState> = _state.asStateFlow()
}

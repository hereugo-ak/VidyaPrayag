package com.littlebridge.vidyaprayag.feature.parent.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
    val childName: String = "Aarav",
    val overallProgress: Float = 0.75f,
    val currentLevel: Int = 4,
    val journeyDescription: String = "Developmental milestones on track for Term 2",
    val badges: List<AchievementBadge> = listOf(
        AchievementBadge("Social Star", "workspace_premium", gradientColors = listOf(0xFFB6C7EB, 0xFF006C49)),
        AchievementBadge("Book Worm", "auto_stories", gradientColors = listOf(0xFFCBDBF5, 0xFF8293B5)),
        AchievementBadge("Fast Learner", "rocket_launch", gradientColors = listOf(0xFF4EDE93, 0xFF006C49)),
        AchievementBadge("Upcoming", "lock", isLocked = true, gradientColors = listOf(0xFFE5EEFF, 0xFFE5EEFF))
    ),
    val academicCompetencies: List<AcademicCompetency> = listOf(
        AcademicCompetency("Literacy", "translate", 0.85f),
        AcademicCompetency("Numeracy", "calculate", 0.70f)
    ),
    val emotionalIntelligence: Map<String, Float> = mapOf(
        "Empathy" to 0.8f,
        "Resilience" to 0.7f,
        "Social" to 0.9f,
        "Control" to 0.6f,
        "Focus" to 0.75f,
        "Sharing" to 0.85f
    ),
    val playIndicators: List<PlayIndicator> = listOf(
        PlayIndicator(
            "Creative Expression", 
            "Uses diverse materials to represent ideas", 
            "https://lh3.googleusercontent.com/aida-public/AB6AXuBJ0iy3QHsYrDK9vkmt05wDdmHmpgT8gBlcip2cJxtHhEZh8aRcsRMENEot_fma9PHySR3i7uOBCkzywjgrnyRweoIcsAippP8X0A0wqcgX-r5pfZvIL5UF_FG0Q8N_eb8FdFdPyQ48xEiykqbtT-Uh3PpA4KeOf2vv6fzHKyIidF-Y8ldvErlwE50_WVwRhhK7TMiQuKDOR9LRFN7cqu9v5ygC0nl9_0IMd4GuMkFoiDefldCGJStlfH48L5RIjTUZfLrJ-EITce_3"
        ),
        PlayIndicator(
            "Physical Agility", 
            "Gross motor coordination milestones met", 
            "https://lh3.googleusercontent.com/aida-public/AB6AXuC97aj-fXdWa8AknQylLrGHeKwdSE_wY776abXdGOHdPIyP7yGiA7uw8V8vXdYXudLVG-Sue3-oPaD7YeCkrMA9jLvA3cdnafmRz6kPJvG_QVv4_dfdXDGRVqTHRWTUUWzMrT85G_aJBx6fHQZtEKSfDOmAa0a22EmEhL6IIg4RHLERutQdBs7iO_oYDZ1Wy51bMcNXHZJ3S40pbvg_jqq0dvcBXWMeCetnJLVXn4PysxPN1MpyqE5i5yz6EgvotyPLPGpJ8xKgwtlv"
        )
    )
)

class TrackProgressViewModel : ViewModel() {
    private val _state = MutableStateFlow(TrackProgressState())
    val state: StateFlow<TrackProgressState> = _state.asStateFlow()
}

package com.littlebridge.vidyaprayag.feature.parent.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
    val childName: String = "Arjun",
    val termNarrative: String = "Arjun has shown exceptional resilience this term, particularly in grasping complex scientific concepts. His transition from theoretical understanding to practical application in Modern Science has been the highlight of his academic trajectory. While his focus remains sharp, encouraging peer-to-peer collaboration will further enhance his social interaction metrics.",
    val averageScore: Int = 90,
    val globalSubjectRank: Int = 4,
    val totalStudents: Int = 45,
    val improvementTrend: Float = 12.5f,
    val monthlyScores: List<Int> = listOf(78, 80, 84, 82, 90),
    val assessmentHistory: List<AssessmentItem> = listOf(
        AssessmentItem("1", "Mathematics", "12 May 2024", 94, 100, 82, "functions"),
        AssessmentItem("2", "Modern Science", "08 May 2024", 88, 100, 75, "biotech"),
        AssessmentItem("3", "English Literature", "05 May 2024", 91, 100, 88, "menu_book")
    ),
    val teacherRemarks: String = "Arjun's curiosity is infectious. He often leads group discussions and isn't afraid to ask 'why'. His written assignments have improved in structure.",
    val leadInstructor: String = "Mrs. Sarah Jenkins",
    val pewsStatus: String = "Moderate Risk",
    val pewsAlert: String = "Yellow Alert: Social Interaction metrics are 15% below term goals. Recommend team activities.",
    val learningStreak: Int = 18,
    val skillTrajectory: Map<String, Int> = mapOf(
        "Critical Thinking" to 92,
        "Social Interaction" to 64
    )
)

class ParentReportsViewModel : ViewModel() {
    private val _state = MutableStateFlow(ParentReportsState())
    val state: StateFlow<ParentReportsState> = _state.asStateFlow()
}

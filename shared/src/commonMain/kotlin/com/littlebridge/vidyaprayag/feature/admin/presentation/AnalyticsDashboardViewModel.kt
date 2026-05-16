package com.littlebridge.vidyaprayag.feature.admin.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AnalyticsCardData(
    val title: String,
    val value: String,
    val subValue: String,
    val iconUrl: String,
    val trend: String? = null
)

data class InsightItem(
    val title: String,
    val description: String,
    val iconName: String,
    val iconColor: Long // Hex color
)

data class AnalyticsDashboardState(
    val performanceTrend: List<Float> = listOf(0.4f, 0.55f, 0.48f, 0.85f, 0.65f, 0.75f),
    val currentGrowth: String = "+4.2%",
    val cards: List<AnalyticsCardData> = listOf(
        AnalyticsCardData("Student Tracking", "94%", "Avg Attendance", "https://lh3.googleusercontent.com/aida/ADBb0uiUyzzrF0kSedtzO6hnOelI8S6yvHTYv8qIVZrZl3qB5XP84vLtFLe8Zpf5LZn1XIO-cERZCNu4zFehcaM89NnM84KOqHI7C3NLXkJweAmc8rqtbRZGUBhs67vfAiHjT9MUB2iH9zHtjeMkWJjFjMUHKvY0e1Az0YS4jyUC8Qaq9TKqV5KXTf1ACw9WIB6Gmi-0-eeLNNSTS09-kt5jW6DCOIqMsXwe7-fwfehbS6ZgUUpQE19fYqNyvoBI"),
        AnalyticsCardData("Syllabus Coverage", "78%", "Logged Progress", "https://lh3.googleusercontent.com/aida/ADBb0ug5uSTvAE_12_BQv9mfA3RYnXO7WeFeeM3JBLcOlibkJdUGs26Q8C2oWHF5YR9_n87IIjcPEKsO37q7JFIlxZ48EafBzpxgDZ3s3q5WJDrSnF2RLeA6ouAojYLOIRljQEum_zAnLYsG16W4uFCWT_ASrvVMjX-VgSObU1uWc-OSH_EqmkqtYJ-HQqe_9tDhkqWK_s6Z5CLWv_2b1juizCY5fr_XaXVZOZM4I5S2lSCVnSEEwM32ywDXiQyB"),
        AnalyticsCardData("Teacher Accountability", "4.8", "Avg Rating", "https://lh3.googleusercontent.com/aida/ADBb0uh6bmjgNB9R7rHkxFN9BAClzfBhfnkjVE0zLcy9Hoj06m6sDsFs2-XAOFwOYPYSudOEj0TwCaZgd6Qs9GjVvbBPIQovFw_xoUPtJ6-iStpw2G33aeFjRb8Pk4ZjqRZMPlfMcrg04-Sr83Dme580oa_3WpNDVDLJUVrCjCBQ_GQHowOqwoeMxEsMW1ogJyPeQ1GCHYU34dL7slNnrZs0o0SyZqAOpkLMwavAIfu-jfZXkhr9Mw0CRzOY7pl6"),
        AnalyticsCardData("Class Performance", "82%", "Proficiency", "https://lh3.googleusercontent.com/aida/ADBb0ujDTP2HizlaoAEICYY1Fc9m28-JEebTXUpV1kEPnclpqbT3HeEqDaYhjNmzZjQqqyoFk0aM-qCtKGjsnVbWLCv1KL1TJ9gAYqYtkU4Ok8_qwX1AzYHup1BrU_oRJprQpVM9uprtNah-_Drcr7g8pfEiUSnaMcgyRsflhGcqSwNbT5J1gGVoeapVwMkzJ8e-6X7q5I1bEsJKJMFQ_4eN9c0Z7jKxZgFJsg_I2AI-P913SfVQDZ-voVGHkCQ")
    ),
    val insights: List<InsightItem> = listOf(
        InsightItem("Attendance Peak", "Class 10-A reached 99% attendance", "trending_up", 0xFF10B981),
        InsightItem("Syllabus Alert", "Mathematics Grade 8 is 5% behind", "warning", 0xFFF59E0B),
        InsightItem("Top Performer", "Dr. Jenkins: 5.0 Engagement Rating", "stars", 0xFF8B5CF6)
    )
)

class AnalyticsDashboardViewModel : ViewModel() {
    private val _state = MutableStateFlow(AnalyticsDashboardState())
    val state: StateFlow<AnalyticsDashboardState> = _state.asStateFlow()
}

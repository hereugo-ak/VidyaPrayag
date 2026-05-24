package com.littlebridge.vidyaprayag.feature.admin.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DepartmentProgress(
    val name: String,
    val progress: Float,
    val trend: String,
    val isDelayed: Boolean = false
)

data class LaggingAlert(
    val id: String,
    val subject: String,
    val className: String,
    val delayPercentage: Int,
    val instructor: String,
    val isCritical: Boolean = false
)

data class AcademicMilestone(
    val id: String,
    val month: String,
    val day: String,
    val title: String,
    val description: String,
    val isVerified: Boolean = false
)

data class SyllabusCoverageState(
    val departmentStats: List<Float> = listOf(0.82f, 0.65f, 0.92f, 0.40f, 0.70f),
    val departmentProgress: List<DepartmentProgress> = listOf(
        DepartmentProgress("Science Dept", 0.75f, "+5% from last week"),
        DepartmentProgress("Mathematics", 0.90f, "On Target"),
        DepartmentProgress("Humanities", 0.42f, "Delayed Entry", isDelayed = true),
        DepartmentProgress("Fine Arts", 0.60f, "Steady Growth")
    ),
    val alerts: List<LaggingAlert> = listOf(
        LaggingAlert("1", "Chemistry", "Grade 10-B", 14, "Dr. Miller", isCritical = true),
        LaggingAlert("2", "World History", "Grade 8-C", 8, "Sarah J.")
    ),
    val milestones: List<AcademicMilestone> = listOf(
        AcademicMilestone("1", "Oct", "12", "Mid-Term Syllabus Verification", "Department heads to submit progress audits for Q2.", isVerified = true),
        AcademicMilestone("2", "Oct", "28", "Practical Assessment Window", "Science labs opening for senior grade assessments.")
    )
)

class SyllabusCoverageViewModel : ViewModel() {
    private val _state = MutableStateFlow(SyllabusCoverageState())
    val state: StateFlow<SyllabusCoverageState> = _state.asStateFlow()
}

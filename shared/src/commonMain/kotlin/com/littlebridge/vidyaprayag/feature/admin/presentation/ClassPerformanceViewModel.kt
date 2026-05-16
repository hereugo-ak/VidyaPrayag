package com.littlebridge.vidyaprayag.feature.admin.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GradeDistribution(
    val grade: String,
    val percentage: Int,
    val value: Float // normalized 0-1 for chart
)

data class SubjectMatrixItem(
    val name: String,
    val percentage: Int,
    val trend: String // "up", "flat", "down"
)

data class ProgressMonitoringItem(
    val name: String,
    val initials: String,
    val math: String,
    val science: String,
    val literature: String,
    val attendance: String,
    val status: String // "EXCELLING", "PEWS ALERT", "CONSISTENT"
)

data class ClassPerformanceState(
    val gradeDistribution: List<GradeDistribution> = listOf(
        GradeDistribution("F", 12, 0.20f),
        GradeDistribution("D", 21, 0.35f),
        GradeDistribution("C", 32, 0.55f),
        GradeDistribution("B", 48, 0.85f),
        GradeDistribution("A", 38, 0.65f)
    ),
    val avgProficiency: String = "78.4%",
    val activeStudents: Int = 428,
    val medianGrade: String = "B+",
    val subjectMatrix: List<SubjectMatrixItem> = listOf(
        SubjectMatrixItem("Mathematics", 82, "up"),
        SubjectMatrixItem("Science", 76, "flat"),
        SubjectMatrixItem("Literature", 54, "down"),
        SubjectMatrixItem("History", 68, "up")
    ),
    val criticalRiskCount: Int = 12,
    val moderateRiskCount: Int = 28,
    val proficiencyTargetReach: Int = 75,
    val topPerformerName: String = "Elena Rodriguez",
    val topPerformerDetails: String = "GPA: 3.98 • Grade 11-B",
    val recentProgress: List<ProgressMonitoringItem> = listOf(
        ProgressMonitoringItem("Jordan Davis", "JD", "92%", "88%", "85%", "98%", "EXCELLING"),
        ProgressMonitoringItem("Sarah Miller", "SM", "58%", "62%", "78%", "74%", "PEWS ALERT"),
        ProgressMonitoringItem("Thomas Kim", "TK", "74%", "81%", "79%", "92%", "CONSISTENT")
    )
)

class ClassPerformanceViewModel : ViewModel() {
    private val _state = MutableStateFlow(ClassPerformanceState())
    val state: StateFlow<ClassPerformanceState> = _state.asStateFlow()
}

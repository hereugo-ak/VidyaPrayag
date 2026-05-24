package com.littlebridge.vidyaprayag.feature.admin.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SyllabusTarget(
    val id: String,
    val subject: String,
    val chapter: String,
    val deadline: String,
    val progress: Float,
    val status: String // "TARGET SET", "UPCOMING", "PENDING"
)

data class AcademicCalendarState(
    val currentMonth: String = "June 2024",
    val workingDays: Int = 184,
    val holidays: Int = 22,
    val conflicts: Int = 4,
    val syllabusTargets: List<SyllabusTarget> = listOf(
        SyllabusTarget("1", "Mathematics - Grade 10", "Chapter 1: Algebra Fundamentals", "June 15th", 0.85f, "TARGET SET"),
        SyllabusTarget("2", "Physics - Grade 11", "Chapter 4: Thermodynamics", "July 02nd", 0f, "UPCOMING")
    )
)

class AcademicCalendarViewModel : ViewModel() {
    private val _state = MutableStateFlow(AcademicCalendarState())
    val state: StateFlow<AcademicCalendarState> = _state.asStateFlow()
}

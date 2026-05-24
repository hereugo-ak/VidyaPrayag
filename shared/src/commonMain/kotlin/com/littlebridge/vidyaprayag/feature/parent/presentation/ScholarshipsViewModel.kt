package com.littlebridge.vidyaprayag.feature.parent.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Scholarship(
    val id: String,
    val title: String,
    val description: String,
    val amount: String,
    val timeLeft: String,
    val category: String, // "Full Funding", "Merit Based", "International"
    val isCritical: Boolean = false
)

data class ScholarshipApplication(
    val id: String,
    val institution: String,
    val program: String,
    val status: String, // "Shortlisted", "Under Review", "Received"
    val iconName: String
)

data class ScholarshipsState(
    val scholarships: List<Scholarship> = listOf(
        Scholarship(
            "1",
            "Global Excellence STEM Award 2024",
            "Awarded to top 5% of applicants pursuing Engineering or Mathematics in Central District partner institutions.",
            "$45,000",
            "3d : 12h",
            "Full Funding",
            isCritical = true
        ),
        Scholarship(
            "2",
            "Social Impact Grant",
            "Supporting student-led community initiatives.",
            "$5,000",
            "24h left",
            "Merit Based",
            isCritical = true
        ),
        Scholarship(
            "3",
            "Bridge-to-Learning Fund",
            "First-generation college student assistance program.",
            "$12,000",
            "14 days",
            "International"
        )
    ),
    val applications: List<ScholarshipApplication> = listOf(
        ScholarshipApplication("1", "University of Applied Sciences", "B.Arch - Sustainable Urbanism", "Shortlisted", "architecture"),
        ScholarshipApplication("2", "Tech Institute of Innovation", "M.Sc - Artificial Intelligence", "Under Review", "biotech"),
        ScholarshipApplication("3", "Royal Academy of Arts", "BFA - Digital Media Design", "Received", "history_edu")
    ),
    val profileStrength: Int = 85,
    val streakDays: Int = 3,
    val currentLevel: Int = 4
)

class ScholarshipsViewModel : ViewModel() {
    private val _state = MutableStateFlow(ScholarshipsState())
    val state: StateFlow<ScholarshipsState> = _state.asStateFlow()
}

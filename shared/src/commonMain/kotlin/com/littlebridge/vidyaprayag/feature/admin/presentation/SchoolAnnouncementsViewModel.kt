package com.littlebridge.vidyaprayag.feature.admin.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Announcement(
    val id: String,
    val title: String,
    val description: String,
    val category: String, // "Holidays", "PTM", "Events", "Update", "Reminder"
    val date: String,
    val imageUrl: String? = null,
    val isFeatured: Boolean = false,
    val participants: List<String> = emptyList()
)

data class SchoolAnnouncementsState(
    val announcements: List<Announcement> = listOf(
        Announcement(
            id = "1",
            title = "Diwali Semester Break Announcement",
            description = "The school will remain closed for the Diwali break. Students are encouraged to complete their mid-term projects during this period. School will resume regular classes from November 1st.",
            category = "Holidays",
            date = "Oct 24 - Oct 31",
            imageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuAPYTkxRqlhcsjmV_2jmsXHheVy4hGOSnuKTouRW1gEmU1pF0ICGwHeVvKPesr8Y7pw4h7DpUWNW_lZEaDgW7vYWMLWaopYbe8g2Ufl0BTXNZLNbLlgstpBsUPT09iT1vgIAzmeEDT8KlVVjDWHOfXK2phbCnpvzeFa8cWuj3vc9NVlSX2qPcistFf_lUUhAoFtvWTGTCZF-zahmGwCzZKd9D5rmKM79o2_iimlycoHRUCj7iCLC0WH9kjK2p87ziImPbuYnT2_PtOW",
            isFeatured = true
        ),
        Announcement(
            id = "2",
            title = "Grade 5 Parent Teacher Meet",
            description = "Individual feedback sessions with class teachers to discuss the Q3 performance report.",
            category = "PTM",
            date = "Saturday, Oct 12 • 09:00 AM"
        ),
        Announcement(
            id = "3",
            title = "Annual Sports Day 2024",
            description = "Nov 15 • Olympic Stadium",
            category = "Events",
            date = "Nov 15",
            participants = listOf("1", "2", "3")
        ),
        Announcement(
            id = "4",
            title = "New Bus Route: Sector 42",
            description = "The school has introduced a new dedicated bus route for Sector 42 and adjoining areas to reduce transit time.",
            category = "Update",
            date = "Effective from Oct 01"
        ),
        Announcement(
            id = "5",
            title = "Fee Submission",
            description = "Third quarter fee payment portal is now open. Last date: Oct 10.",
            category = "Reminder",
            date = "Last date: Oct 10"
        )
    ),
    val isWhatsAppSyncEnabled: Boolean = true
)

class SchoolAnnouncementsViewModel : ViewModel() {
    private val _state = MutableStateFlow(SchoolAnnouncementsState())
    val state: StateFlow<SchoolAnnouncementsState> = _state.asStateFlow()

    fun toggleWhatsAppSync(enabled: Boolean) {
        _state.value = _state.value.copy(isWhatsAppSyncEnabled = enabled)
    }
}

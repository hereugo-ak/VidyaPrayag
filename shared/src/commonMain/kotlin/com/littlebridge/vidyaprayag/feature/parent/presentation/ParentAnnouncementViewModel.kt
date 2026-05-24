package com.littlebridge.vidyaprayag.feature.parent.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ParentAnnouncement(
    val id: String,
    val title: String,
    val description: String,
    val date: String,
    val category: String, // "Holidays", "PTM", "Events", "Reminder"
    val isFeatured: Boolean = false,
    val imageUrl: String? = null
)

data class ParentAnnouncementState(
    val announcements: List<ParentAnnouncement> = listOf(
        ParentAnnouncement(
            "1", 
            "Annual Sports Day 2024", 
            "Join us for a day of athletic excellence and school spirit. Parents are invited to participate in the relay race!",
            "Oct 24, 2023",
            "Events",
            isFeatured = true,
            imageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuBJ0iy3QHsYrDK9vkmt05wDdmHmpgT8gBlcip2cJxtHhEZh8aRcsRMENEot_fma9PHySR3i7uOBCkzywjgrnyRweoIcsAippP8X0A0wqcgX-r5pfZvIL5UF_FG0Q8N_eb8FdFdPyQ48xEiykqbtT-Uh3PpA4KeOf2vv6fzHKyIidF-Y8ldvErlwE50_WVwRhhK7TMiQuKDOR9LRFN7cqu9v5ygC0nl9_0IMd4GuMkFoiDefldCGJStlfH48L5RIjTUZfLrJ-EITce_3"
        ),
        ParentAnnouncement(
            "2",
            "Mid-Term PTM",
            "Schedule your slot to discuss your child's progress for the first half of the academic year.",
            "Oct 28, 2023",
            "PTM"
        ),
        ParentAnnouncement(
            "3",
            "Winter Vacation Notice",
            "School will remain closed from Dec 20th to Jan 5th for the winter break.",
            "Dec 15, 2023",
            "Holidays"
        )
    ),
    val isWhatsAppSyncEnabled: Boolean = true
)

class ParentAnnouncementViewModel : ViewModel() {
    private val _state = MutableStateFlow(ParentAnnouncementState())
    val state: StateFlow<ParentAnnouncementState> = _state.asStateFlow()

    fun toggleWhatsAppSync(enabled: Boolean) {
        _state.value = _state.value.copy(isWhatsAppSyncEnabled = enabled)
    }
}

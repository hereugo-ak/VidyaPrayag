package com.littlebridge.vidyaprayag.feature.parent.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class FeeAnnouncement(
    val id: String,
    val title: String,
    val time: String,
    val description: String,
    val openRate: String,
    val engagement: String,
    val type: String // "Campaign", "Emergency", "Payment"
)

data class FeeState(
    val totalCollected: String = "$428,500",
    val collectionProgress: Float = 0.85f,
    val outstandingFees: String = "$72,120",
    val overdueCount: Int = 145,
    val announcements: List<FeeAnnouncement> = listOf(
        FeeAnnouncement("1", "Annual Sports Day Schedule", "2h ago", "Detailed itinerary for the upcoming Sports Day has been released. Please review the volunteer assignments.", "94%", "24", "Campaign"),
        FeeAnnouncement("2", "Weather Alert: Early Closure", "5h ago", "Due to anticipated heavy snowfall, the campus will close at 2:00 PM today. School buses will depart early.", "98%", "812", "Emergency"),
        FeeAnnouncement("3", "Fee Submission Deadline", "Yesterday", "Final reminder for Q3 tuition fee submission. Late fees will apply starting next Monday.", "62%", "3", "Payment")
    )
)

class FeeViewModel : ViewModel() {
    private val _state = MutableStateFlow(FeeState())
    val state: StateFlow<FeeState> = _state.asStateFlow()
}

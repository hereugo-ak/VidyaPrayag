package com.littlebridge.vidyaprayag.feature.admin.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LeaveRequestItem(
    val id: String,
    val requesterName: String,
    val dateRange: String,
    val reason: String,
    val imageUrl: String,
    val status: String = "Pending"
)

data class LeaveRequestsState(
    val requestType: String = "Student", // "Student" or "Teacher"
    val approvalRate: Int = 94,
    val weeklyCount: Int = 12,
    val requests: List<LeaveRequestItem> = listOf(
        LeaveRequestItem(
            id = "1",
            requesterName = "Marcus Holloway",
            dateRange = "Dec 12 - Dec 14",
            reason = "Family Function",
            imageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuADD51tvVqoilR317bXmVub2jVhfp03ym3tul0lBBnTDpn46NkORLXqKFqF4uEjK5R0Vd_TBJfz-g8ERJHaBb8_Pyjs5E0uJCo5CG0T8izpQQ8etO8PNyGnkvAy5_9Nz6kdz7Ij-P073gFTqG5JzVUsetyt0dHkdSWQjwzlmjCPkVKatzuwS96eh_wWRPqp--U-s0J7sC56yQodYFsnZ6qN2TqfyTvOyM7v6VGg636LkmWNI-D_046yB_EZVay364R6aEGyzSm7vKRf"
        ),
        LeaveRequestItem(
            id = "2",
            requesterName = "Sarah Jenkins",
            dateRange = "Dec 15 - Dec 15",
            reason = "Medical Appointment",
            imageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuDOsOeDKhKTw9eUW-s72iE3NmLYcbp6acUaU_gZoMcMkmfWg5PxZH0ZIonMVAx-skdBJk0RpkJIbxWouy1l6AfbMbyLufrIPlIGH1ht6XzNpqnwt4SkujG0TkDaGw5YYPYhZAbL55D9rzB8zAw4LLTWdunV4cLvjEIL5sG0Wr3olRwFSVaBL9XymA_2X-5Cja-YtGOVsDfYMhxmwPVGiRcuoRZd7Zdr1MoqhWBNLw0xEYZGnKTVegMWlCyATqwBAixGWgu0QNqNOBlP"
        ),
        LeaveRequestItem(
            id = "3",
            requesterName = "Liam Chen",
            dateRange = "Dec 18 - Dec 22",
            reason = "Out of Station",
            imageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuApS3hTBebNtgycw8IqT-I-KBRQywp7S2SaMYgn7q3MbYqJoSUFPexx6oYwwDmgZq6gIka7UZOzfCTvbSDIk7sUUUUiHsgP1A5woIegYg1hbtjkhAgKYmB3RNRjKXr7jwD3WZLrRos3EQMmpK1ilSZwJRcX9hJyJYXDjS87ZJ-uGd0OswQNBhEMNyIeWwY1vz_VLeb4a37NlOvZn0PfzNMxKG_4ECp3QBL0ykiUA66C8CXleoW-4lnN82FUHAygw_ULsOWZbjcCIXKU"
        )
    )
)

class LeaveRequestsViewModel : ViewModel() {
    private val _state = MutableStateFlow(LeaveRequestsState())
    val state: StateFlow<LeaveRequestsState> = _state.asStateFlow()

    fun setRequestType(type: String) {
        _state.value = _state.value.copy(requestType = type)
    }

    fun approveRequest(id: String) {
        // Handle approval
    }

    fun rejectRequest(id: String) {
        // Handle rejection
    }
}

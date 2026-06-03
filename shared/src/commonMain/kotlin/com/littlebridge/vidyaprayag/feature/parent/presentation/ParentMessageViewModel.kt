package com.littlebridge.vidyaprayag.feature.parent.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ParentMessageThread(
    val id: String,
    val senderName: String,
    val senderRole: String,
    val lastMessage: String,
    val time: String,
    val isRead: Boolean,
    val unreadCount: Int = 0,
    val senderImageUrl: String? = null,
    val iconName: String? = null
)

data class ParentMessageState(
    val threads: List<ParentMessageThread> = listOf(
        ParentMessageThread(
            "1",
            "Ms. Sarah Jenkins",
            "Class Teacher (10A)",
            "Aarav's math performance has improved significantly.",
            "10:45 AM",
            false,
            1,
            senderImageUrl = null
        ),
        ParentMessageThread(
            "2",
            "Accounts Dept",
            "Finance",
            "Q3 Fee receipt has been generated.",
            "Yesterday",
            true,
            iconName = "payments"
        ),
        ParentMessageThread(
            "3",
            "Transport Hub",
            "Logistics",
            "Bus Route 4 delayed by 15 mins due to rain.",
            "2 days ago",
            true,
            iconName = "directions_bus"
        )
    )
)

class ParentMessageViewModel : ViewModel() {
    private val _state = MutableStateFlow(ParentMessageState())
    val state: StateFlow<ParentMessageState> = _state.asStateFlow()

    fun markAsRead(threadId: String) {
        val updatedThreads = _state.value.threads.map {
            if (it.id == threadId) it.copy(isRead = true, unreadCount = 0) else it
        }
        _state.value = _state.value.copy(threads = updatedThreads)
    }
}

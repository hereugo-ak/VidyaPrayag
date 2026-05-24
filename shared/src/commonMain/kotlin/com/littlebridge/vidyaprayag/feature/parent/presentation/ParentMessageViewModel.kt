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
            senderImageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuC4bGKo5RDKEDVsqEANNGgLXgIlytdpqdrdA_0ZgHuCQGXpXdtFHxG3o7NqqVJ2omzhFKSILPKQ7zrevPEfbnZrtnqv9oMjaTvV6zQzlVckP-xwUNiRk1_cW8yHIK9-bnN1fvx4ZMaigbyh5AUwLsYzOfZ7xmkV15fr7Be-5nBa-7DKkZp5w5phY_k9KE40NjIFiULHjjEsVQfz7umnAyrGU_SsRtB7EeCeC8I0D_smfiUfWmSZSstZJ1NCShIzXAzOTFRIqS-JI5YQ"
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

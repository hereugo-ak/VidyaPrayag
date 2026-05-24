package com.littlebridge.vidyaprayag.feature.admin.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MessageThread(
    val id: String,
    val senderName: String,
    val senderRole: String,
    val lastMessage: String,
    val time: String,
    val unreadCount: Int = 0,
    val senderImageUrl: String? = null,
    val iconName: String? = null,
    val isRead: Boolean = true
)

data class MessagesState(
    val threads: List<MessageThread> = listOf(
        MessageThread(
            id = "1",
            senderName = "Mr. Adrian Chen",
            senderRole = "Class 10-A Teacher",
            lastMessage = "The project submission is due tomorrow. Please remind Leo about the bibliography section...",
            time = "10:45 AM",
            unreadCount = 2,
            senderImageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuAvG53Dwn5Skh5DI8l4sSHUYLltep7Cgx_l53KLVpq01YyzVkyehT_yUXiCFqCND4gh-BF6eYwqif4r7qRnud04qHdovWjm8Z3y6r0mbeAW2soHxVNf8Cn2f-rKSvOMav0o4DYmtPYWE8MXxGf1_7aq6n3ZABaw2mp5oSt4DtqzG-SpYXXQyzRSIfGR2dYf1Wr8Qz8ZwWBU2y2CbgyyByPS_G7H4seDWlGS9eb7u9Lhbp0zCcyOmmjjtdjnfex9mhJ7CEb43DPEYiwd",
            isRead = false
        ),
        MessageThread(
            id = "2",
            senderName = "Dr. Sarah Williams",
            senderRole = "Principal",
            lastMessage = "Thank you for your feedback on the new science lab initiative.",
            time = "09:12 AM",
            senderImageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuB5gi8fsqnjfZPkMm2-ObiDH0YwhyNpzHLmbi4jAn7XBwWw7xdkmZtmGxk_Ol8JjjzGwp69l2BBuXXaZPk9kSqFPofBuTBR8dsUO-hRZA-w6moZByvALd07aHsDpjBlzGlYuzarDYN4njtvSzcth-e_dXYqnnoQ-N9Y7uFvWw1dIwvvT_dXpWzA5hGtf6eM0rUCQ4n3z-gIMeS0Fwre0LBK5B2-80jGRryCXGkgjdZ_0wOzCQrOoKit6nBb2fHKweNNeawHTrt1ThbC"
        ),
        MessageThread(
            id = "3",
            senderName = "Account Office",
            senderRole = "Fee Department",
            lastMessage = "Your receipt for Term 3 fees is now available for download in the portal.",
            time = "Yesterday",
            iconName = "payments"
        ),
        MessageThread(
            id = "4",
            senderName = "Admin Desk",
            senderRole = "Support",
            lastMessage = "Please verify your contact details for the updated school directory.",
            time = "Mon",
            unreadCount = 1,
            senderImageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuB5gi8fsqnjfZPkMm2-ObiDH0YwhyNpzHLmbi4jAn7XBwWw7xdkmZtmGxk_Ol8JjjzGwp69l2BBuXXaZPk9kSqFPofBuTBR8dsUO-hRZA-w6moZByvALd07aHsDpjBlzGlYuzarDYN4njtvSzcth-e_dXYqnnoQ-N9Y7uFvWw1dIwvvT_dXpWzA5hGtf6eM0rUCQ4n3z-gIMeS0Fwre0LBK5B2-80jGRryCXGkgjdZ_0wOzCQrOoKit6nBb2fHKweNNeawHTrt1ThbC",
            isRead = false
        ),
        MessageThread(
            id = "5",
            senderName = "Mr. Samuel Okafor",
            senderRole = "Transport Lead",
            lastMessage = "Route 7 will be delayed by 15 minutes this afternoon due to road works.",
            time = "Oct 12",
            iconName = "directions_bus"
        ),
        MessageThread(
            id = "6",
            senderName = "Sports Dept",
            senderRole = "PE Staff",
            lastMessage = "Confirming the athletics trials schedule for next Wednesday.",
            time = "Oct 10",
            iconName = "fitness_center"
        )
    )
)

class MessagesViewModel : ViewModel() {
    private val _state = MutableStateFlow(MessagesState())
    val state: StateFlow<MessagesState> = _state.asStateFlow()

    fun markAsRead(threadId: String) {
        val updated = _state.value.threads.map {
            if (it.id == threadId) it.copy(isRead = true, unreadCount = 0) else it
        }
        _state.value = _state.value.copy(threads = updated)
    }
}

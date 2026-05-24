package com.littlebridge.vidyaprayag.feature.parent.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PTMBooking(
    val id: String,
    val subject: String,
    val teacher: String,
    val date: String,
    val time: String,
    val iconName: String = "science"
)

data class PTMTimeSlot(
    val time: String,
    val isAvailable: Boolean = true,
    val isSelected: Boolean = false
)

data class ParentSchedulePTMState(
    val selectedMonth: String = "October 2023",
    val selectedDate: Int = 6,
    val ptmWindowDays: List<Int> = listOf(6, 12),
    val teacherName: String = "Dr. Sarah Henderson",
    val teacherSubject: String = "Advanced Mathematics • Grade 10-A",
    val teacherImageUrl: String = "https://lh3.googleusercontent.com/aida-public/AB6AXuA9ovXHkp7pMxJJl22f7fZy3tNdf5J5w36mJD5Tl-F0TXhnQ6I-FRbkTkFDu5iUp4zsRapf3mTuaiGWhFjPZ8FNVxRmNk6cxgdmZZXs3lUeLOLrdrkypser3od5vej76hIdSwN3LfpvBQ8OSL0EqjBl7d16AvoecEbLvO5TduBawveJLoVZNYHzNG1cwyWuFiX5CjX-qfWV2kRn15qWVYib6Zm7_OR3LGkZH5ZMQHDkF6OgBbtM6j1LMai9MineP9ZmY3fRCUV1NGyZ",
    val slots: List<PTMTimeSlot> = listOf(
        PTMTimeSlot("10:00 AM"),
        PTMTimeSlot("10:15 AM", isSelected = true),
        PTMTimeSlot("10:30 AM"),
        PTMTimeSlot("10:45 AM", isAvailable = false),
        PTMTimeSlot("11:00 AM"),
        PTMTimeSlot("11:15 AM")
    ),
    val bookings: List<PTMBooking> = listOf(
        PTMBooking("1", "Physics Review", "Mr. Robert Chen", "Oct 08, 2023", "02:30 PM - 02:45 PM")
    ),
    val email: String = "parent@example.com"
)

class ParentSchedulePTMViewModel : ViewModel() {
    private val _state = MutableStateFlow(ParentSchedulePTMState())
    val state: StateFlow<ParentSchedulePTMState> = _state.asStateFlow()

    fun selectDate(date: Int) {
        _state.value = _state.value.copy(selectedDate = date)
    }

    fun selectSlot(time: String) {
        val updatedSlots = _state.value.slots.map {
            it.copy(isSelected = it.time == time)
        }
        _state.value = _state.value.copy(slots = updatedSlots)
    }
}

package com.littlebridge.vidyaprayag.feature.admin.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AttendanceStatus {
    PRESENT, ABSENT, LATE
}

data class Attendee(
    val id: String,
    val name: String,
    val initials: String,
    val status: AttendanceStatus
)

data class DailyAttendanceState(
    val attendanceType: String = "Students", // "Faculty" or "Students"
    val selectedClass: String = "Grade 10-A",
    val availableClasses: List<String> = listOf("Grade 10-A", "Grade 10-B", "Grade 11-A", "Grade 12-C"),
    val attendees: List<Attendee> = listOf(
        Attendee("1", "Alice Morgan", "AM", AttendanceStatus.PRESENT),
        Attendee("2", "Benjamin Jones", "BJ", AttendanceStatus.ABSENT),
        Attendee("3", "Chloe Henderson", "CH", AttendanceStatus.LATE),
        Attendee("4", "Daniel Thompson", "DT", AttendanceStatus.PRESENT)
    ),
    val totalCount: Int = 32,
    val presentCount: Int = 30
)

class DailyAttendanceViewModel : ViewModel() {
    private val _state = MutableStateFlow(DailyAttendanceState())
    val state: StateFlow<DailyAttendanceState> = _state.asStateFlow()

    fun setAttendanceType(type: String) {
        _state.value = _state.value.copy(attendanceType = type)
    }

    fun selectClass(className: String) {
        _state.value = _state.value.copy(selectedClass = className)
    }

    fun updateStatus(attendeeId: String, newStatus: AttendanceStatus) {
        val updated = _state.value.attendees.map {
            if (it.id == attendeeId) it.copy(status = newStatus) else it
        }
        _state.value = _state.value.copy(attendees = updated)
    }
}

package com.littlebridge.vidyaprayag.feature.admin.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PTMHistoryItem(
    val id: String,
    val date: String,
    val title: String,
    val turnout: Int,
    val totalMet: Int
)

data class ClassPTMProgress(
    val id: String,
    val className: String,
    val teacherName: String,
    val metCount: Int,
    val totalCount: Int,
    val progress: Float
)

data class SchedulePTMState(
    val activeEventTitle: String = "School-Wide PTM",
    val activeEventDate: String = "Oct 28, 2023",
    val activeEventSlot: String = "09:00 - 13:00",
    val expectedParents: Int = 450,
    val checkedInParents: Int = 312,
    val invitesDelivered: Int = 96,
    val readReceipts: Int = 78,
    val history: List<PTMHistoryItem> = listOf(
        PTMHistoryItem("1", "Sept 15, 2023", "Term 1 Performance Review", 78, 342)
    ),
    val classProgress: List<ClassPTMProgress> = listOf(
        ClassPTMProgress("1", "10A", "Ms. Sarah Jenkins", 28, 30, 0.93f),
        ClassPTMProgress("2", "10B", "Mr. David Chen", 14, 32, 0.44f)
    )
)

class SchedulePTMViewModel : ViewModel() {
    private val _state = MutableStateFlow(SchedulePTMState())
    val state: StateFlow<SchedulePTMState> = _state.asStateFlow()
}

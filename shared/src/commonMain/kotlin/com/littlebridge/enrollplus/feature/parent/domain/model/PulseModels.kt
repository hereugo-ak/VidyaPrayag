package com.littlebridge.enrollplus.feature.parent.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

// --- Parent Pulse (PARENT_PULSE_SPEC.md — weekly AI digest) ---

@Serializable
data class PulseResponse(
    val success: Boolean,
    val message: String = "",
    val data: PulseDto
)

@Serializable
data class PulseHistoryResponse(
    val success: Boolean,
    val message: String = "",
    val data: PulseHistoryData
)

@Serializable
data class PulseHistoryData(
    val pulses: List<PulseDto>
)

@Serializable
data class PulseDto(
    val id: String,
    val studentName: String,
    val weekRange: String,
    val weekStartDate: String,
    val weekEndDate: String,
    val attendancePercentage: Double? = null,
    val attendanceTrend: String? = null,
    val marksSummary: List<PulseMarkEntry> = emptyList(),
    val homeworkPending: Int = 0,
    val homeworkCompleted: Int = 0,
    val announcementsCount: Int = 0,
    val unreadMessages: Int = 0,
    val upcomingEvents: List<PulseUpcomingEvent> = emptyList(),
    val aiNarrative: String,
    val actionableItems: List<String> = emptyList(),
    val createdAt: String
)

@Serializable
data class PulseMarkEntry(
    val subject: String,
    val test: String,
    val marks: Double,
    val max: Int
)

@Serializable
data class PulseUpcomingEvent(
    val title: String,
    val date: String
)

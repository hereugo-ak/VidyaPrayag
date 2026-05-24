/*
 * File: CalendarModels.kt
 * Module: feature.admin.domain.model
 *
 * DTOs for academic calendar endpoints.
 * Matches server: feature.school.SchoolRouting.kt  GET /api/v1/school/calendar
 */
package com.littlebridge.vidyaprayag.feature.admin.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CalendarEventDto(
    val date: String,
    val day: String,
    @SerialName("event_id") val eventId: String,
    @SerialName("event_title") val eventTitle: String,
    @SerialName("event_description") val eventDescription: String = ""
)

@Serializable
data class CalendarSummaryDto(
    @SerialName("working_days") val workingDays: Int,
    @SerialName("public_holidays") val publicHolidays: Int,
    @SerialName("school_holidays") val schoolHolidays: Int
)

@Serializable
data class CalendarResponse(
    @SerialName("calendar_events") val calendarEvents: List<CalendarEventDto> = emptyList(),
    val summary: CalendarSummaryDto
)

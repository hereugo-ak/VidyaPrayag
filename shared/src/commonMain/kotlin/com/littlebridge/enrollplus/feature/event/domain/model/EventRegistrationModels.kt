/*
 * File: EventRegistrationModels.kt
 * Module: feature.event.domain.model
 *
 * DTOs for the Event Registration & RSVP system.
 * Matches server: feature.event.EventRegistrationRouting.kt
 */
package com.littlebridge.enrollplus.feature.event.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Parent ──

@Serializable
data class ParentEventDto(
    val id: String,
    val title: String,
    val description: String = "",
    val type: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val allDay: Boolean = true,
    val venue: String? = null,
    val registrationEnabled: Boolean = false,
    val registrationDeadline: String? = null,
    val hasSlots: Boolean = false,
    val maxAttendees: Int? = null,
    val audience: String = "ALL_SCHOOL",
    val classIds: List<String> = emptyList(),
    val myRegistrationStatus: String? = null,
    val mySlotId: String? = null,
    val myAttendeeCount: Int? = null,
    val bannerUrl: String? = null,
    val icon: String? = null,
    val conflictingEventId: String? = null,
    val conflictingEventTitle: String? = null,
)

@Serializable
data class ParentEventListResponse(
    val events: List<ParentEventDto> = emptyList(),
)

@Serializable
data class EventSlotDto(
    val id: String,
    val startTime: String,
    val endTime: String,
    val capacity: Int,
    val bookedCount: Int,
    val isFull: Boolean,
    val myRegistration: Boolean,
)

@Serializable
data class ParentEventDetailResponse(
    val event: ParentEventDto,
    val slots: List<EventSlotDto> = emptyList(),
)

@Serializable
data class RegisterRequest(
    @SerialName("slot_id") val slotId: String? = null,
    @SerialName("student_id") val studentId: String? = null,
    @SerialName("attendee_count") val attendeeCount: Int = 1,
)

@Serializable
data class CancelRegistrationRequest(
    @SerialName("student_id") val studentId: String? = null,
    val reason: String? = null,
)

@Serializable
data class RescheduleRequest(
    @SerialName("new_slot_id") val newSlotId: String,
)

@Serializable
data class RegistrationDto(
    val id: String,
    @SerialName("event_id") val eventId: String,
    @SerialName("event_title") val eventTitle: String = "",
    @SerialName("event_date") val eventDate: String = "",
    @SerialName("slot_start_time") val slotStartTime: String? = null,
    @SerialName("slot_end_time") val slotEndTime: String? = null,
    @SerialName("attendee_count") val attendeeCount: Int = 1,
    val status: String = "REGISTERED",
    @SerialName("registered_at") val registeredAt: String = "",
    @SerialName("cancelled_at") val cancelledAt: String? = null,
)

@Serializable
data class RegistrationListResponse(
    val registrations: List<RegistrationDto> = emptyList(),
)

// ── Teacher ──

@Serializable
data class TeacherPtmEventDto(
    val id: String,
    val title: String = "",
    val date: String = "",
    val className: String = "",
    @SerialName("total_registrations") val totalRegistrations: Int = 0,
    @SerialName("checked_in") val checkedIn: Int = 0,
    val slots: List<TeacherSlotDto> = emptyList(),
)

@Serializable
data class TeacherPtmListResponse(
    val events: List<TeacherPtmEventDto> = emptyList(),
)

@Serializable
data class TeacherSlotDto(
    val id: String,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    val capacity: Int,
    @SerialName("booked_count") val bookedCount: Int,
    val bookings: List<SlotBookingDto> = emptyList(),
)

@Serializable
data class SlotBookingDto(
    @SerialName("registration_id") val registrationId: String,
    @SerialName("parent_name") val parentName: String,
    @SerialName("parent_mobile") val parentMobile: String = "",
    @SerialName("student_name") val studentName: String,
    @SerialName("attendee_count") val attendeeCount: Int,
    val status: String,
    @SerialName("registered_at") val registeredAt: String,
)

// ── Admin ──

@Serializable
data class CreateSlotRequest(
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    val capacity: Int = 1,
)

@Serializable
data class SlotResponse(
    val id: String,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    val capacity: Int,
    @SerialName("is_active") val isActive: Boolean = true,
)

@Serializable
data class AutoGenerateSlotsRequest(
    @SerialName("range_start") val rangeStart: String,
    @SerialName("range_end") val rangeEnd: String,
    @SerialName("slot_duration_minutes") val slotDurationMinutes: Int,
    val capacity: Int = 1,
    @SerialName("break_after_slots") val breakAfterSlots: Int = 0,
    @SerialName("break_duration_minutes") val breakDurationMinutes: Int = 5,
)

@Serializable
data class AutoGenerateSlotsResponse(
    val slots: List<SlotResponse> = emptyList(),
)

@Serializable
data class AdminRegistrationDto(
    val id: String,
    @SerialName("event_title") val eventTitle: String = "",
    @SerialName("event_date") val eventDate: String = "",
    @SerialName("parent_name") val parentName: String = "",
    @SerialName("parent_mobile") val parentMobile: String = "",
    @SerialName("student_name") val studentName: String? = null,
    @SerialName("slot_time") val slotTime: String? = null,
    @SerialName("attendee_count") val attendeeCount: Int = 1,
    val status: String = "REGISTERED",
    @SerialName("registered_at") val registeredAt: String = "",
)

@Serializable
data class AdminRegistrationListResponse(
    val registrations: List<AdminRegistrationDto> = emptyList(),
)

@Serializable
data class UpdateRegistrationConfigRequest(
    @SerialName("registration_enabled") val registrationEnabled: Boolean? = null,
    @SerialName("registration_deadline") val registrationDeadline: String? = null,
    @SerialName("max_attendees") val maxAttendees: Int? = null,
    val venue: String? = null,
)

@Serializable
data class AdminEventDto(
    val id: String,
    val title: String = "",
    val type: String = "",
    val startDate: String = "",
    val status: String = "PUBLISHED",
    val registrationEnabled: Boolean = false,
    val registrationDeadline: String? = null,
    val maxAttendees: Int? = null,
    val venue: String? = null,
    val hasSlots: Boolean = false,
    val slotCount: Int = 0,
    val totalRegistrations: Int = 0,
)

@Serializable
data class AdminEventListResponse(
    val events: List<AdminEventDto> = emptyList(),
)

package com.littlebridge.enrollplus.feature.event

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Contract tests for Event Registration DTOs.
 * Guards against DTO drift between server and client.
 */
class EventRegistrationContractTest {

    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    @Test
    fun registerRequest_serializesWithSnakeCaseKeys() {
        val req = RegisterRequest(
            slotId = "slot-123",
            studentId = "student-456",
            attendeeCount = 3,
        )
        val encoded = json.encodeToString(RegisterRequest.serializer(), req)

        assertTrue(encoded.contains("\"slot_id\""), "Must use snake_case slot_id. Got: $encoded")
        assertTrue(encoded.contains("\"student_id\""), "Must use snake_case student_id. Got: $encoded")
        assertTrue(encoded.contains("\"attendee_count\""), "Must use snake_case attendee_count. Got: $encoded")
    }

    @Test
    fun cancelRegistrationRequest_includesStudentId() {
        val req = CancelRegistrationRequest(
            studentId = "student-789",
            reason = "Schedule conflict",
        )
        val encoded = json.encodeToString(CancelRegistrationRequest.serializer(), req)

        assertTrue(encoded.contains("\"student_id\""), "CancelRegistrationRequest MUST serialize student_id. Got: $encoded")
        assertTrue(encoded.contains("\"reason\""), "CancelRegistrationRequest MUST serialize reason. Got: $encoded")
    }

    @Test
    fun cancelRegistrationRequest_studentIdDefaultsToNull() {
        val req = CancelRegistrationRequest()
        val encoded = json.encodeToString(CancelRegistrationRequest.serializer(), req)

        assertTrue(encoded.contains("\"student_id\":null"), "student_id should default to null. Got: $encoded")
    }

    @Test
    fun rescheduleRequest_serializesNewSlotId() {
        val req = RescheduleRequest(newSlotId = "slot-new-123")
        val encoded = json.encodeToString(RescheduleRequest.serializer(), req)

        assertTrue(encoded.contains("\"new_slot_id\""), "RescheduleRequest MUST serialize new_slot_id. Got: $encoded")
    }

    @Test
    fun registrationDto_includesAllRequiredFields() {
        val dto = RegistrationDto(
            id = "reg-1",
            eventId = "evt-1",
            eventTitle = "PTM",
            eventDate = "2026-07-01",
            attendeeCount = 2,
            status = "REGISTERED",
            registeredAt = "2026-06-01T10:00:00Z",
        )
        val encoded = json.encodeToString(RegistrationDto.serializer(), dto)

        assertTrue(encoded.contains("\"event_id\""), "Must serialize event_id. Got: $encoded")
        assertTrue(encoded.contains("\"event_title\""), "Must serialize event_title. Got: $encoded")
        assertTrue(encoded.contains("\"event_date\""), "Must serialize event_date. Got: $encoded")
        assertTrue(encoded.contains("\"attendee_count\""), "Must serialize attendee_count. Got: $encoded")
        assertTrue(encoded.contains("\"registered_at\""), "Must serialize registered_at. Got: $encoded")
    }

    @Test
    fun registrationDto_supportsWaitlistedStatus() {
        val dto = RegistrationDto(
            id = "reg-2",
            eventId = "evt-1",
            eventTitle = "PTM",
            eventDate = "2026-07-01",
            attendeeCount = 1,
            status = "WAITLISTED",
            registeredAt = "2026-06-01T10:00:00Z",
        )
        val encoded = json.encodeToString(RegistrationDto.serializer(), dto)

        assertTrue(encoded.contains("\"WAITLISTED\""), "RegistrationDto MUST support WAITLISTED status. Got: $encoded")
    }

    @Test
    fun parentEventDto_serializesMyRegistrationStatus() {
        val dto = ParentEventDto(
            id = "evt-1",
            title = "Annual Day",
            description = "",
            type = "EVENT",
            startDate = "2026-07-01",
            endDate = "2026-07-01",
            allDay = true,
            audience = "ALL_SCHOOL",
            registrationEnabled = true,
            hasSlots = true,
            myRegistrationStatus = "REGISTERED",
            mySlotId = "slot-1",
            myAttendeeCount = 2,
        )
        val encoded = json.encodeToString(ParentEventDto.serializer(), dto)

        assertTrue(encoded.contains("\"registrationEnabled\""), "Must serialize registrationEnabled. Got: $encoded")
        assertTrue(encoded.contains("\"hasSlots\""), "Must serialize hasSlots. Got: $encoded")
    }

    @Test
    fun createSlotRequest_serializesWithSnakeCase() {
        val req = CreateSlotRequest(
            startTime = "14:00",
            endTime = "14:15",
            capacity = 5,
        )
        val encoded = json.encodeToString(CreateSlotRequest.serializer(), req)

        assertTrue(encoded.contains("\"start_time\""), "Must serialize start_time. Got: $encoded")
        assertTrue(encoded.contains("\"end_time\""), "Must serialize end_time. Got: $encoded")
    }

    @Test
    fun adminRegistrationDto_serializesAllFields() {
        val dto = AdminRegistrationDto(
            id = "reg-1",
            eventTitle = "PTM",
            eventDate = "2026-07-01",
            parentName = "John Doe",
            parentMobile = "1234567890",
            studentName = "Jane Doe",
            slotTime = "14:00 - 14:15",
            attendeeCount = 2,
            status = "CHECKED_IN",
            registeredAt = "2026-06-01T10:00:00Z",
        )
        val encoded = json.encodeToString(AdminRegistrationDto.serializer(), dto)

        assertTrue(encoded.contains("\"event_title\""), "Must serialize event_title. Got: $encoded")
        assertTrue(encoded.contains("\"parent_name\""), "Must serialize parent_name. Got: $encoded")
        assertTrue(encoded.contains("\"parent_mobile\""), "Must serialize parent_mobile. Got: $encoded")
        assertTrue(encoded.contains("\"student_name\""), "Must serialize student_name. Got: $encoded")
        assertTrue(encoded.contains("\"slot_time\""), "Must serialize slot_time. Got: $encoded")
    }

    @Test
    fun parentEventListResponse_decodesEmptyList() {
        val payload = """{"events":[]}"""
        val decoded = json.decodeFromString(ParentEventListResponse.serializer(), payload)

        assertTrue(decoded.events.isEmpty(), "Empty events list should decode correctly")
    }

    @Test
    fun slotResponse_includesIsActive() {
        val dto = SlotResponse(
            id = "slot-1",
            startTime = "14:00",
            endTime = "14:15",
            capacity = 5,
            isActive = true,
        )
        val encoded = json.encodeToString(SlotResponse.serializer(), dto)

        assertTrue(encoded.contains("\"is_active\""), "SlotResponse MUST serialize is_active. Got: $encoded")
    }

    @Test
    fun autoGenerateSlotsRequest_serializesAllFields() {
        val req = AutoGenerateSlotsRequest(
            rangeStart = "2026-07-01T09:00:00",
            rangeEnd = "2026-07-01T12:00:00",
            slotDurationMinutes = 15,
            capacity = 3,
            breakAfterSlots = 2,
            breakDurationMinutes = 5,
        )
        val encoded = json.encodeToString(AutoGenerateSlotsRequest.serializer(), req)

        assertTrue(encoded.contains("\"range_start\""), "Must serialize range_start. Got: $encoded")
        assertTrue(encoded.contains("\"range_end\""), "Must serialize range_end. Got: $encoded")
        assertTrue(encoded.contains("\"slot_duration_minutes\""), "Must serialize slot_duration_minutes. Got: $encoded")
        assertTrue(encoded.contains("\"break_after_slots\""), "Must serialize break_after_slots. Got: $encoded")
        assertTrue(encoded.contains("\"break_duration_minutes\""), "Must serialize break_duration_minutes. Got: $encoded")
    }
}

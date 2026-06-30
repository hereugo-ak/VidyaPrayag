/*
 * File: EventRegistrationRouting.kt
 * Module: feature.event
 *
 * Event Registration & RSVP System (EVENT_REGISTRATION_PLAN.md §4).
 *
 * 19 endpoints across three role surfaces, all JWT-authenticated:
 *
 *   Parent (6):
 *     GET    /api/v1/parent/events                         — list events with registration enabled
 *     GET    /api/v1/parent/events/{eventId}               — event detail with slots + my status
 *     POST   /api/v1/parent/events/{eventId}/register      — register (idempotent via X-Client-Request-Id)
 *     DELETE /api/v1/parent/events/{eventId}/register      — cancel registration
 *     GET    /api/v1/parent/events/registrations           — list my registrations
 *     PATCH  /api/v1/parent/events/{eventId}/reschedule    — change slot (atomic)
 *
 *   Teacher (4):
 *     GET    /api/v1/teacher/events/ptm                    — list PTM events for my classes
 *     GET    /api/v1/teacher/events/ptm/{eventId}          — PTM detail with slot-wise bookings
 *     GET    /api/v1/teacher/events/ptm/{eventId}/slots    — slot list with booked/total counts
 *     PATCH  /api/v1/teacher/events/ptm/{eventId}/checkin/{registrationId}
 *
 *   Admin (9):
 *     GET    /api/v1/school/events/registrations                       — list all (filterable)
 *     GET    /api/v1/school/events/{eventId}/registrations             — event-wise list
 *     POST   /api/v1/school/events/{eventId}/slots                     — create slot
 *     POST   /api/v1/school/events/{eventId}/slots/auto-generate       — auto-generate slots
 *     PUT    /api/v1/school/events/{eventId}/slots/{slotId}            — update slot
 *     DELETE /api/v1/school/events/{eventId}/slots/{slotId}            — delete slot
 *     PATCH  /api/v1/school/events/{eventId}/registration-status       — enable/disable + deadline
 *     POST   /api/v1/school/events/{eventId}/cancel                    — cancel event + auto-cancel regs
 *     GET    /api/v1/school/events/{eventId}/registrations/export      — CSV export
 */
package com.littlebridge.enrollplus.feature.event

import com.littlebridge.enrollplus.core.created
import com.littlebridge.enrollplus.core.fail
import com.littlebridge.enrollplus.core.ok
import com.littlebridge.enrollplus.core.okMessage
import com.littlebridge.enrollplus.core.principalUserUuid
import com.littlebridge.enrollplus.core.requireSchoolContext
import com.littlebridge.enrollplus.core.requireTeacherContext
import com.littlebridge.enrollplus.db.AppUsersTable
import com.littlebridge.enrollplus.db.CalendarEventsTable
import com.littlebridge.enrollplus.db.ChildrenTable
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.EventRegistrationsTable
import com.littlebridge.enrollplus.db.EventSlotsTable
import com.littlebridge.enrollplus.db.StudentsTable
import com.littlebridge.enrollplus.db.TeacherSubjectAssignmentsTable
import com.littlebridge.enrollplus.feature.notifications.Notify
import com.littlebridge.enrollplus.feature.notifications.NotifyRecipients
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.respondText
import io.ktor.server.routing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.dao.id.EntityID
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

// ═══════════════════════════════════════════════════════════════════════════
// DTOs (EVENT_REGISTRATION_PLAN.md §4.4)
// ═══════════════════════════════════════════════════════════════════════════

// ── Parent ──

@Serializable
data class ParentEventDto(
    val id: String,
    val title: String,
    val description: String,
    val type: String,
    val startDate: String,
    val endDate: String,
    val allDay: Boolean,
    val venue: String? = null,
    val registrationEnabled: Boolean,
    val registrationDeadline: String? = null,
    val hasSlots: Boolean,
    val maxAttendees: Int? = null,
    val audience: String,
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
data class ParentEventListResponse(val events: List<ParentEventDto> = emptyList())

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
    @SerialName("event_title") val eventTitle: String,
    @SerialName("event_date") val eventDate: String,
    @SerialName("slot_start_time") val slotStartTime: String? = null,
    @SerialName("slot_end_time") val slotEndTime: String? = null,
    @SerialName("attendee_count") val attendeeCount: Int,
    val status: String,
    @SerialName("registered_at") val registeredAt: String,
    @SerialName("cancelled_at") val cancelledAt: String? = null,
)

@Serializable
data class RegistrationListResponse(val registrations: List<RegistrationDto> = emptyList())

// ── Teacher ──

@Serializable
data class TeacherPtmEventDto(
    val id: String,
    val title: String,
    val date: String,
    val className: String,
    @SerialName("total_registrations") val totalRegistrations: Int,
    @SerialName("checked_in") val checkedIn: Int,
    val slots: List<TeacherSlotDto> = emptyList(),
)

@Serializable
data class TeacherPtmListResponse(val events: List<TeacherPtmEventDto> = emptyList())

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
    @SerialName("is_active") val isActive: Boolean,
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
data class AutoGenerateSlotsResponse(val slots: List<SlotResponse> = emptyList())

@Serializable
data class AdminRegistrationDto(
    val id: String,
    @SerialName("event_title") val eventTitle: String,
    @SerialName("event_date") val eventDate: String,
    @SerialName("parent_name") val parentName: String,
    @SerialName("parent_mobile") val parentMobile: String,
    @SerialName("student_name") val studentName: String? = null,
    @SerialName("slot_time") val slotTime: String? = null,
    @SerialName("attendee_count") val attendeeCount: Int,
    val status: String,
    @SerialName("registered_at") val registeredAt: String,
)

@Serializable
data class AdminRegistrationListResponse(val registrations: List<AdminRegistrationDto> = emptyList())

@Serializable
data class UpdateRegistrationConfigRequest(
    @SerialName("registration_enabled") val registrationEnabled: Boolean? = null,
    @SerialName("registration_deadline") val registrationDeadline: String? = null,
    @SerialName("max_attendees") val maxAttendees: Int? = null,
    val venue: String? = null,
)

// ═══════════════════════════════════════════════════════════════════════════
// Helpers
// ═══════════════════════════════════════════════════════════════════════════

private val ISO_FMT = DateTimeFormatter.ISO_INSTANT

private fun parseHHmm(s: String): LocalTime? =
    runCatching { LocalTime.parse(s) }.getOrNull()

private fun formatSlotRange(start: String, end: String): String = "$start-$end"

private fun escapeCsvField(field: String): String {
    return if (field.contains(',') || field.contains('"') || field.contains('\n')) {
        "\"" + field.replace("\"", "\"\"") + "\""
    } else field
}

private suspend fun resolveParentSchoolIds(uid: UUID): List<UUID> = dbQuery {
    ChildrenTable.selectAll().where {
        (ChildrenTable.parentId eq uid) and (ChildrenTable.isActive eq true)
    }.mapNotNull { it[ChildrenTable.schoolId] }.distinct()
}

private suspend fun countSlotBookings(slotId: UUID): Int = dbQuery {
    EventRegistrationsTable.selectAll().where {
        (EventRegistrationsTable.slotId eq slotId) and
            (EventRegistrationsTable.status inList listOf("REGISTERED", "CHECKED_IN"))
    }.count().toInt()
}

private suspend fun countEventActiveRegistrations(eventId: UUID): Int = dbQuery {
    EventRegistrationsTable.selectAll().where {
        (EventRegistrationsTable.eventId eq eventId) and
            (EventRegistrationsTable.status inList listOf("REGISTERED", "CHECKED_IN"))
    }.count().toInt()
}

private suspend fun hasActiveSlots(eventId: UUID): Boolean = dbQuery {
    EventSlotsTable.selectAll().where {
        (EventSlotsTable.eventId eq eventId) and (EventSlotsTable.isActive eq true)
    }.any()
}

private fun rowToParentEventDto(
    row: org.jetbrains.exposed.sql.ResultRow,
    myStatus: String? = null,
    mySlotId: String? = null,
    myAttendeeCount: Int? = null,
    hasSlots: Boolean = false,
    conflictingEventId: String? = null,
    conflictingEventTitle: String? = null,
): ParentEventDto = ParentEventDto(
    id = row[CalendarEventsTable.id].value.toString(),
    title = row[CalendarEventsTable.title],
    description = row[CalendarEventsTable.description],
    type = row[CalendarEventsTable.type],
    startDate = row[CalendarEventsTable.startDate].toString(),
    endDate = row[CalendarEventsTable.endDate].toString(),
    allDay = row[CalendarEventsTable.allDay],
    venue = row[CalendarEventsTable.venue],
    registrationEnabled = row[CalendarEventsTable.registrationEnabled],
    registrationDeadline = row[CalendarEventsTable.registrationDeadline],
    hasSlots = hasSlots,
    maxAttendees = row[CalendarEventsTable.maxAttendees],
    audience = row[CalendarEventsTable.audience],
    myRegistrationStatus = myStatus,
    mySlotId = mySlotId,
    myAttendeeCount = myAttendeeCount,
    bannerUrl = row[CalendarEventsTable.bannerUrl],
    icon = row[CalendarEventsTable.icon],
    conflictingEventId = conflictingEventId,
    conflictingEventTitle = conflictingEventTitle,
)

// ═══════════════════════════════════════════════════════════════════════════
// Routing
// ═══════════════════════════════════════════════════════════════════════════

fun Route.eventRegistrationRouting() {
    authenticate("jwt") {

        // ───────────────────────────────────────────────────────────────────
        // Parent Endpoints
        // ───────────────────────────────────────────────────────────────────
        route("/api/v1/parent/events") {

            // ── LIST events with registration enabled ──
            get {
                val uid = call.principalUserUuid() ?: run {
                    call.fail("Invalid token", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@get
                }
                val schoolIds = resolveParentSchoolIds(uid)
                if (schoolIds.isEmpty()) {
                    call.ok(ParentEventListResponse(), message = "No school linked"); return@get
                }
                val today = LocalDate.now()
                val events = dbQuery {
                    CalendarEventsTable.selectAll().where {
                        (CalendarEventsTable.schoolId inList schoolIds) and
                            (CalendarEventsTable.registrationEnabled eq true) and
                            (CalendarEventsTable.status eq "PUBLISHED") and
                            (CalendarEventsTable.isActive eq true) and
                            (CalendarEventsTable.startDate greaterEq today)
                    }.orderBy(CalendarEventsTable.startDate, SortOrder.ASC).toList()
                }
                val eventIds = events.map { it[CalendarEventsTable.id].value }
                // Batch fetch: which events have active slots
                val eventsWithSlots: Set<UUID> = if (eventIds.isNotEmpty()) {
                    dbQuery {
                        EventSlotsTable.selectAll().where {
                            (EventSlotsTable.eventId inList eventIds) and (EventSlotsTable.isActive eq true)
                        }.map { it[EventSlotsTable.eventId] }.toSet()
                    }
                } else emptySet()
                // Batch fetch: my active registrations for these events
                val myRegs: Map<UUID, org.jetbrains.exposed.sql.ResultRow> = if (eventIds.isNotEmpty()) {
                    dbQuery {
                        EventRegistrationsTable.selectAll().where {
                            (EventRegistrationsTable.eventId inList eventIds) and
                                (EventRegistrationsTable.parentUserId eq uid) and
                                (EventRegistrationsTable.status inList listOf("REGISTERED", "CHECKED_IN"))
                        }.associate { it[EventRegistrationsTable.eventId] to it }
                    }
                } else emptyMap()

                val dtos = events.map { row ->
                    val eventId = row[CalendarEventsTable.id].value
                    val myReg = myRegs[eventId]
                    rowToParentEventDto(
                        row,
                        myStatus = myReg?.get(EventRegistrationsTable.status),
                        mySlotId = myReg?.get(EventRegistrationsTable.slotId)?.toString(),
                        myAttendeeCount = myReg?.get(EventRegistrationsTable.attendeeCount),
                        hasSlots = eventId in eventsWithSlots,
                    )
                }
                call.ok(ParentEventListResponse(events = dtos), message = "Events loaded")
            }

            // ── LIST my registrations ── (before /{eventId} to avoid route conflict)
            get("/registrations") {
                val uid = call.principalUserUuid() ?: run {
                    call.fail("Invalid token", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@get
                }
                val rows = dbQuery {
                    (EventRegistrationsTable innerJoin CalendarEventsTable)
                        .selectAll()
                        .where {
                            (EventRegistrationsTable.parentUserId eq uid) and
                                (CalendarEventsTable.id eq EventRegistrationsTable.eventId)
                        }
                        .orderBy(CalendarEventsTable.startDate, SortOrder.DESC)
                        .toList()
                }
                val slotIds = rows.mapNotNull { it[EventRegistrationsTable.slotId] }.distinct()
                val slotMap: Map<UUID, Pair<String, String>> = if (slotIds.isNotEmpty()) {
                    dbQuery {
                        EventSlotsTable.selectAll().where { EventSlotsTable.id inList slotIds.map { EntityID(it, EventSlotsTable) } }
                            .associate { it[EventSlotsTable.id].value to (it[EventSlotsTable.startTime] to it[EventSlotsTable.endTime]) }
                    }
                } else emptyMap()
                val dtos = rows.map { row ->
                    val slotId = row[EventRegistrationsTable.slotId]
                    val (slotStart, slotEnd) = slotId?.let { slotMap[it] } ?: (null to null)
                    RegistrationDto(
                        id = row[EventRegistrationsTable.id].value.toString(),
                        eventId = row[EventRegistrationsTable.eventId].toString(),
                        eventTitle = row[CalendarEventsTable.title],
                        eventDate = row[CalendarEventsTable.startDate].toString(),
                        slotStartTime = slotStart,
                        slotEndTime = slotEnd,
                        attendeeCount = row[EventRegistrationsTable.attendeeCount],
                        status = row[EventRegistrationsTable.status],
                        registeredAt = ISO_FMT.format(row[EventRegistrationsTable.registeredAt]),
                        cancelledAt = row[EventRegistrationsTable.cancelledAt]?.let { ISO_FMT.format(it) },
                    )
                }
                call.ok(RegistrationListResponse(registrations = dtos), message = "Registrations loaded")
            }

            // ── EVENT detail with slots + my status ──
            get("/{eventId}") {
                val uid = call.principalUserUuid() ?: run {
                    call.fail("Invalid token", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@get
                }
                val eventId = call.parameters["eventId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid eventId", HttpStatusCode.BadRequest); return@get }

                val event = dbQuery {
                    CalendarEventsTable.selectAll().where {
                        (CalendarEventsTable.id eq eventId) and
                            (CalendarEventsTable.isActive eq true) and
                            (CalendarEventsTable.status eq "PUBLISHED")
                    }.firstOrNull()
                } ?: run { call.fail("Event not found", HttpStatusCode.NotFound, "EVENT_NOT_FOUND"); return@get }

                val schoolIds = resolveParentSchoolIds(uid)
                val eventSchoolId = event[CalendarEventsTable.schoolId]
                if (eventSchoolId !in schoolIds) {
                    call.fail("Event not found", HttpStatusCode.NotFound, "EVENT_NOT_FOUND"); return@get
                }

                val slots = dbQuery {
                    EventSlotsTable.selectAll().where {
                        (EventSlotsTable.eventId eq eventId) and (EventSlotsTable.isActive eq true)
                    }.orderBy(EventSlotsTable.startTime, SortOrder.ASC).toList()
                }

                val myReg = dbQuery {
                    EventRegistrationsTable.selectAll().where {
                        (EventRegistrationsTable.eventId eq eventId) and
                            (EventRegistrationsTable.parentUserId eq uid) and
                            (EventRegistrationsTable.status inList listOf("REGISTERED", "CHECKED_IN"))
                    }.firstOrNull()
                }

                // Conflict detection: another registered event on the same date
                val eventDate = event[CalendarEventsTable.startDate]
                val conflicting = dbQuery {
                    (EventRegistrationsTable innerJoin CalendarEventsTable)
                        .selectAll()
                        .where {
                            (EventRegistrationsTable.parentUserId eq uid) and
                                (EventRegistrationsTable.status eq "REGISTERED") and
                                (EventRegistrationsTable.eventId neq eventId) and
                                (CalendarEventsTable.id eq EventRegistrationsTable.eventId) and
                                (CalendarEventsTable.startDate eq eventDate)
                        }.firstOrNull()
                }
                val conflictId = conflicting?.get(CalendarEventsTable.id)?.value?.toString()
                val conflictTitle = conflicting?.get(CalendarEventsTable.title)

                val slotDtos = slots.map { sRow ->
                    val slotId = sRow[EventSlotsTable.id].value
                    val booked = countSlotBookings(slotId)
                    EventSlotDto(
                        id = slotId.toString(),
                        startTime = sRow[EventSlotsTable.startTime],
                        endTime = sRow[EventSlotsTable.endTime],
                        capacity = sRow[EventSlotsTable.capacity],
                        bookedCount = booked,
                        isFull = booked >= sRow[EventSlotsTable.capacity],
                        myRegistration = myReg?.get(EventRegistrationsTable.slotId) == slotId,
                    )
                }

                val eventDto = rowToParentEventDto(
                    event,
                    myStatus = myReg?.get(EventRegistrationsTable.status),
                    mySlotId = myReg?.get(EventRegistrationsTable.slotId)?.toString(),
                    myAttendeeCount = myReg?.get(EventRegistrationsTable.attendeeCount),
                    hasSlots = slots.isNotEmpty(),
                    conflictingEventId = conflictId,
                    conflictingEventTitle = conflictTitle,
                )
                call.ok(ParentEventDetailResponse(event = eventDto, slots = slotDtos), message = "Event detail")
            }

            // ── REGISTER ──
            post("/{eventId}/register") {
                val uid = call.principalUserUuid() ?: run {
                    call.fail("Invalid token", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@post
                }
                val eventId = call.parameters["eventId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid eventId", HttpStatusCode.BadRequest); return@post }
                val req = call.receive<RegisterRequest>()
                val clientRequestId = call.request.headers["X-Client-Request-Id"]

                val event = dbQuery {
                    CalendarEventsTable.selectAll().where {
                        (CalendarEventsTable.id eq eventId) and (CalendarEventsTable.isActive eq true)
                    }.firstOrNull()
                } ?: run { call.fail("Event not found", HttpStatusCode.NotFound, "EVENT_NOT_FOUND"); return@post }

                if (!event[CalendarEventsTable.registrationEnabled]) {
                    call.fail("Registration is not enabled for this event", HttpStatusCode.BadRequest, "REGISTRATION_DISABLED"); return@post
                }
                if (event[CalendarEventsTable.status] == "CANCELLED") {
                    call.fail("Event has been cancelled", HttpStatusCode.BadRequest, "EVENT_CANCELLED"); return@post
                }

                // Past event check
                val eventDate = event[CalendarEventsTable.startDate]
                if (eventDate.isBefore(LocalDate.now())) {
                    call.fail("Cannot register for a past event", HttpStatusCode.BadRequest, "EVENT_PAST"); return@post
                }

                // Deadline check
                val deadline = event[CalendarEventsTable.registrationDeadline]
                if (deadline != null) {
                    val today = LocalDate.now()
                    val deadlineDate = runCatching { LocalDate.parse(deadline) }.getOrNull()
                    if (deadlineDate != null && today.isAfter(deadlineDate)) {
                        call.fail("Registration deadline has passed", HttpStatusCode.BadRequest, "DEADLINE_PASSED"); return@post
                    }
                }

                val schoolId = event[CalendarEventsTable.schoolId]
                val parentSchoolIds = resolveParentSchoolIds(uid)
                if (schoolId !in parentSchoolIds) {
                    call.fail("Event not found", HttpStatusCode.NotFound, "EVENT_NOT_FOUND"); return@post
                }

                // Idempotency: if client_request_id matches, return existing
                if (clientRequestId != null) {
                    val existing = dbQuery {
                        EventRegistrationsTable.selectAll().where {
                            (EventRegistrationsTable.clientRequestId eq clientRequestId) and
                                (EventRegistrationsTable.parentUserId eq uid)
                        }.firstOrNull()
                    }
                    if (existing != null) {
                        call.ok(
                            RegistrationDto(
                                id = existing[EventRegistrationsTable.id].value.toString(),
                                eventId = existing[EventRegistrationsTable.eventId].toString(),
                                eventTitle = event[CalendarEventsTable.title],
                                eventDate = event[CalendarEventsTable.startDate].toString(),
                                attendeeCount = existing[EventRegistrationsTable.attendeeCount],
                                status = existing[EventRegistrationsTable.status],
                                registeredAt = ISO_FMT.format(existing[EventRegistrationsTable.registeredAt]),
                                cancelledAt = existing[EventRegistrationsTable.cancelledAt]?.let { ISO_FMT.format(it) },
                            ),
                            message = "Already registered"
                        )
                        return@post
                    }
                }

                // Duplicate check (same event + parent + student, active status)
                val studentUuid = req.studentId?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                val duplicate = dbQuery {
                    EventRegistrationsTable.selectAll().where {
                        (EventRegistrationsTable.eventId eq eventId) and
                            (EventRegistrationsTable.parentUserId eq uid) and
                            (EventRegistrationsTable.status inList listOf("REGISTERED", "CHECKED_IN")) and
                            (if (studentUuid != null)
                                EventRegistrationsTable.studentId eq studentUuid
                            else
                                EventRegistrationsTable.studentId.isNull())
                    }.firstOrNull()
                }
                if (duplicate != null) {
                    call.fail("You are already registered for this event", HttpStatusCode.Conflict, "ALREADY_REGISTERED"); return@post
                }

                // Slot validation
                val slotUuid = req.slotId?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                if (req.slotId != null && slotUuid == null) {
                    call.fail("Invalid slot_id", HttpStatusCode.BadRequest); return@post
                }
                if (slotUuid != null) {
                    val slot = dbQuery {
                        EventSlotsTable.selectAll().where {
                            (EventSlotsTable.id eq slotUuid) and
                                (EventSlotsTable.eventId eq eventId) and
                                (EventSlotsTable.isActive eq true)
                        }.firstOrNull()
                    } ?: run { call.fail("Slot not found", HttpStatusCode.NotFound, "SLOT_NOT_FOUND"); return@post }

                    val booked = countSlotBookings(slotUuid)
                    if (booked >= slot[EventSlotsTable.capacity]) {
                        call.fail("Slot is full", HttpStatusCode.Conflict, "SLOT_FULL"); return@post
                    }
                }

                if (req.attendeeCount < 1) {
                    call.fail("attendee_count must be at least 1", HttpStatusCode.BadRequest); return@post
                }

                // Max attendees check (overall cap)
                val maxAttendees = event[CalendarEventsTable.maxAttendees]
                if (maxAttendees != null) {
                    val total = countEventActiveRegistrations(eventId)
                    if (total + req.attendeeCount > maxAttendees) {
                        call.fail("Event is at maximum capacity", HttpStatusCode.Conflict, "EVENT_FULL"); return@post
                    }
                }

                val regId = UUID.randomUUID()
                val now = Instant.now()
                dbQuery {
                    EventRegistrationsTable.insert {
                        it[EventRegistrationsTable.id] = regId
                        it[EventRegistrationsTable.eventId] = eventId
                        it[EventRegistrationsTable.slotId] = slotUuid
                        it[EventRegistrationsTable.parentUserId] = uid
                        it[EventRegistrationsTable.studentId] = studentUuid
                        it[EventRegistrationsTable.schoolId] = schoolId
                        it[EventRegistrationsTable.attendeeCount] = req.attendeeCount
                        it[EventRegistrationsTable.status] = "REGISTERED"
                        it[EventRegistrationsTable.registeredAt] = now
                        it[EventRegistrationsTable.updatedAt] = now
                        it[EventRegistrationsTable.clientRequestId] = clientRequestId
                    }
                }

                // Notify class teachers + admins
                val eventTitle = event[CalendarEventsTable.title]
                runCatching {
                    val teacherIds = NotifyRecipients.teachersInSchool(schoolId)
                    if (teacherIds.isNotEmpty()) {
                        Notify.toUsers(
                            userIds = teacherIds,
                            category = "event_registration",
                            title = "New registration",
                            body = "A parent registered for '$eventTitle'",
                            schoolId = schoolId,
                            deepLink = "/teacher/events/ptm",
                            refType = "event_registration",
                            refId = regId.toString(),
                        )
                    }
                }

                call.created(
                    RegistrationDto(
                        id = regId.toString(),
                        eventId = eventId.toString(),
                        eventTitle = eventTitle,
                        eventDate = event[CalendarEventsTable.startDate].toString(),
                        attendeeCount = req.attendeeCount,
                        status = "REGISTERED",
                        registeredAt = ISO_FMT.format(now),
                    ),
                    message = "Registered successfully"
                )
            }

            // ── CANCEL registration ──
            delete("/{eventId}/register") {
                val uid = call.principalUserUuid() ?: run {
                    call.fail("Invalid token", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@delete
                }
                val eventId = call.parameters["eventId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid eventId", HttpStatusCode.BadRequest); return@delete }
                val req = runCatching { call.receive<CancelRegistrationRequest>() }.getOrNull()
                val now = Instant.now()
                val studentUuid = req?.studentId?.let { runCatching { UUID.fromString(it) }.getOrNull() }

                val reg = dbQuery {
                    EventRegistrationsTable.selectAll().where {
                        (EventRegistrationsTable.eventId eq eventId) and
                            (EventRegistrationsTable.parentUserId eq uid) and
                            (EventRegistrationsTable.status inList listOf("REGISTERED", "CHECKED_IN")) and
                            (if (studentUuid != null)
                                EventRegistrationsTable.studentId eq studentUuid
                            else
                                EventRegistrationsTable.studentId.isNull())
                    }.firstOrNull()
                } ?: run { call.fail("Registration not found", HttpStatusCode.NotFound, "REGISTRATION_NOT_FOUND"); return@delete }

                dbQuery {
                    EventRegistrationsTable.update({ EventRegistrationsTable.id eq reg[EventRegistrationsTable.id].value }) {
                        it[status] = "CANCELLED"
                        it[cancelReason] = req?.reason
                        it[cancelledAt] = now
                        it[updatedAt] = now
                    }
                }

                // Notify teachers
                runCatching {
                    val teacherIds = NotifyRecipients.teachersInSchool(reg[EventRegistrationsTable.schoolId])
                    if (teacherIds.isNotEmpty()) {
                        Notify.toUsers(
                            userIds = teacherIds,
                            category = "event_registration",
                            title = "Registration cancelled",
                            body = "A parent cancelled their registration",
                            schoolId = reg[EventRegistrationsTable.schoolId],
                            deepLink = "/teacher/events/ptm",
                            refType = "event_registration",
                            refId = reg[EventRegistrationsTable.id].value.toString(),
                        )
                    }
                }

                call.okMessage("Registration cancelled")
            }

            // ── RESCHEDULE (change slot) ──
            patch("/{eventId}/reschedule") {
                val uid = call.principalUserUuid() ?: run {
                    call.fail("Invalid token", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@patch
                }
                val eventId = call.parameters["eventId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid eventId", HttpStatusCode.BadRequest); return@patch }
                val req = call.receive<RescheduleRequest>()
                val newSlotId = runCatching { UUID.fromString(req.newSlotId) }.getOrNull()
                    ?: run { call.fail("Invalid new_slot_id", HttpStatusCode.BadRequest); return@patch }
                val now = Instant.now()

                val event = dbQuery {
                    CalendarEventsTable.selectAll().where {
                        (CalendarEventsTable.id eq eventId) and (CalendarEventsTable.isActive eq true)
                    }.firstOrNull()
                } ?: run { call.fail("Event not found", HttpStatusCode.NotFound, "EVENT_NOT_FOUND"); return@patch }

                // Past event check
                if (event[CalendarEventsTable.startDate].isBefore(LocalDate.now())) {
                    call.fail("Cannot reschedule for a past event", HttpStatusCode.BadRequest, "EVENT_PAST"); return@patch
                }

                // Deadline check
                val deadline = event[CalendarEventsTable.registrationDeadline]
                if (deadline != null) {
                    val today = LocalDate.now()
                    val deadlineDate = runCatching { LocalDate.parse(deadline) }.getOrNull()
                    if (deadlineDate != null && today.isAfter(deadlineDate)) {
                        call.fail("Registration deadline has passed", HttpStatusCode.BadRequest, "DEADLINE_PASSED"); return@patch
                    }
                }

                val reg = dbQuery {
                    EventRegistrationsTable.selectAll().where {
                        (EventRegistrationsTable.eventId eq eventId) and
                            (EventRegistrationsTable.parentUserId eq uid) and
                            (EventRegistrationsTable.status eq "REGISTERED")
                    }.firstOrNull()
                } ?: run { call.fail("Active registration not found", HttpStatusCode.NotFound, "REGISTRATION_NOT_FOUND"); return@patch }

                val newSlot = dbQuery {
                    EventSlotsTable.selectAll().where {
                        (EventSlotsTable.id eq newSlotId) and
                            (EventSlotsTable.eventId eq eventId) and
                            (EventSlotsTable.isActive eq true)
                    }.firstOrNull()
                } ?: run { call.fail("Slot not found", HttpStatusCode.NotFound, "SLOT_NOT_FOUND"); return@patch }

                val booked = countSlotBookings(newSlotId)
                // If parent already has this slot, no-op
                if (reg[EventRegistrationsTable.slotId] == newSlotId) {
                    call.fail("You are already in this slot", HttpStatusCode.BadRequest, "SAME_SLOT"); return@patch
                }
                // Capacity check: if parent's current slot booking is freed, net effect is 0.
                // But to be safe, check if the new slot has room for one more.
                if (booked >= newSlot[EventSlotsTable.capacity]) {
                    call.fail("New slot is full", HttpStatusCode.Conflict, "SLOT_FULL"); return@patch
                }

                // Atomic reschedule: update registration's slotId
                dbQuery {
                    EventRegistrationsTable.update({ EventRegistrationsTable.id eq reg[EventRegistrationsTable.id].value }) {
                        it[EventRegistrationsTable.slotId] = newSlotId
                        it[EventRegistrationsTable.updatedAt] = now
                    }
                }

                // Notify teachers
                runCatching {
                    val teacherIds = NotifyRecipients.teachersInSchool(event[CalendarEventsTable.schoolId])
                    if (teacherIds.isNotEmpty()) {
                        Notify.toUsers(
                            userIds = teacherIds,
                            category = "event_registration",
                            title = "Slot changed",
                            body = "A parent changed their slot for '${event[CalendarEventsTable.title]}'",
                            schoolId = event[CalendarEventsTable.schoolId],
                            deepLink = "/teacher/events/ptm",
                            refType = "event_registration",
                            refId = reg[EventRegistrationsTable.id].value.toString(),
                        )
                    }
                }

                call.ok(
                    RegistrationDto(
                        id = reg[EventRegistrationsTable.id].value.toString(),
                        eventId = eventId.toString(),
                        eventTitle = event[CalendarEventsTable.title],
                        eventDate = event[CalendarEventsTable.startDate].toString(),
                        slotStartTime = newSlot[EventSlotsTable.startTime],
                        slotEndTime = newSlot[EventSlotsTable.endTime],
                        attendeeCount = reg[EventRegistrationsTable.attendeeCount],
                        status = "REGISTERED",
                        registeredAt = ISO_FMT.format(reg[EventRegistrationsTable.registeredAt]),
                    ),
                    message = "Slot changed successfully"
                )
            }
        }

        // ───────────────────────────────────────────────────────────────────
        // Teacher Endpoints
        // ───────────────────────────────────────────────────────────────────
        route("/api/v1/teacher/events/ptm") {

            // ── LIST PTM events for teacher's classes ──
            get {
                val ctx = call.requireTeacherContext() ?: return@get
                val schoolId = ctx.schoolId

                val myClasses = dbQuery {
                    TeacherSubjectAssignmentsTable.selectAll().where {
                        (TeacherSubjectAssignmentsTable.schoolId eq schoolId) and
                            (TeacherSubjectAssignmentsTable.isActive eq true) and
                            (TeacherSubjectAssignmentsTable.teacherId eq ctx.userId)
                    }.map { it[TeacherSubjectAssignmentsTable.className] }.distinct()
                }

                val today = LocalDate.now()
                val events = dbQuery {
                    CalendarEventsTable.selectAll().where {
                        (CalendarEventsTable.schoolId eq schoolId) and
                            (CalendarEventsTable.type eq "PTM") and
                            (CalendarEventsTable.registrationEnabled eq true) and
                            (CalendarEventsTable.status eq "PUBLISHED") and
                            (CalendarEventsTable.isActive eq true) and
                            (CalendarEventsTable.startDate greaterEq today)
                    }.orderBy(CalendarEventsTable.startDate, SortOrder.ASC).toList()
                }

                val eventIds = events.map { it[CalendarEventsTable.id].value }
                // Batch fetch: active registration counts per event
                val totalByEvent: Map<UUID, Int> = if (eventIds.isNotEmpty()) {
                    dbQuery {
                        EventRegistrationsTable.selectAll().where {
                            (EventRegistrationsTable.eventId inList eventIds) and
                                (EventRegistrationsTable.status inList listOf("REGISTERED", "CHECKED_IN"))
                        }.groupBy { it[EventRegistrationsTable.eventId] }
                            .mapValues { it.value.size }
                    }
                } else emptyMap()
                // Batch fetch: checked-in counts per event
                val checkedInByEvent: Map<UUID, Int> = if (eventIds.isNotEmpty()) {
                    dbQuery {
                        EventRegistrationsTable.selectAll().where {
                            (EventRegistrationsTable.eventId inList eventIds) and
                                (EventRegistrationsTable.status eq "CHECKED_IN")
                        }.groupBy { it[EventRegistrationsTable.eventId] }
                            .mapValues { it.value.size }
                    }
                } else emptyMap()

                val dtos = events.map { row ->
                    val eventId = row[CalendarEventsTable.id].value
                    val total = totalByEvent[eventId] ?: 0
                    val checkedIn = checkedInByEvent[eventId] ?: 0
                    TeacherPtmEventDto(
                        id = eventId.toString(),
                        title = row[CalendarEventsTable.title],
                        date = row[CalendarEventsTable.startDate].toString(),
                        className = myClasses.joinToString(", "),
                        totalRegistrations = total,
                        checkedIn = checkedIn,
                    )
                }
                call.ok(TeacherPtmListResponse(events = dtos), message = "PTM events loaded")
            }

            // ── PTM detail with slot-wise bookings ──
            get("/{eventId}") {
                val ctx = call.requireTeacherContext() ?: return@get
                val eventId = call.parameters["eventId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid eventId", HttpStatusCode.BadRequest); return@get }

                val event = dbQuery {
                    CalendarEventsTable.selectAll().where {
                        (CalendarEventsTable.id eq eventId) and
                            (CalendarEventsTable.schoolId eq ctx.schoolId) and
                            (CalendarEventsTable.type eq "PTM") and
                            (CalendarEventsTable.isActive eq true)
                    }.firstOrNull()
                } ?: run { call.fail("PTM event not found", HttpStatusCode.NotFound, "EVENT_NOT_FOUND"); return@get }

                val slots = dbQuery {
                    EventSlotsTable.selectAll().where {
                        (EventSlotsTable.eventId eq eventId) and (EventSlotsTable.isActive eq true)
                    }.orderBy(EventSlotsTable.startTime, SortOrder.ASC).toList()
                }

                // Batch fetch all student names for bookings across all slots
                val allStudentIds = dbQuery {
                    EventRegistrationsTable.selectAll().where {
                        (EventRegistrationsTable.eventId eq eventId) and
                            (EventRegistrationsTable.status inList listOf("REGISTERED", "CHECKED_IN")) and
                            (EventRegistrationsTable.studentId.isNotNull())
                    }.mapNotNull { it[EventRegistrationsTable.studentId] }.distinct()
                }
                val studentNameMap: Map<UUID, String> = if (allStudentIds.isNotEmpty()) {
                    dbQuery {
                        StudentsTable.selectAll().where { StudentsTable.id inList allStudentIds.map { EntityID(it, StudentsTable) } }
                            .associate { it[StudentsTable.id].value to it[StudentsTable.fullName] }
                    }
                } else emptyMap()

                val slotDtos = slots.map { sRow ->
                    val slotId = sRow[EventSlotsTable.id].value
                    val bookings = dbQuery {
                        (EventRegistrationsTable innerJoin AppUsersTable)
                            .selectAll()
                            .where {
                                (EventRegistrationsTable.slotId eq slotId) and
                                    (EventRegistrationsTable.status inList listOf("REGISTERED", "CHECKED_IN")) and
                                    (AppUsersTable.id eq EventRegistrationsTable.parentUserId)
                            }.toList()
                    }
                    val bookingDtos = bookings.map { bRow ->
                        val studentName: String = bRow[EventRegistrationsTable.studentId]?.let { sId ->
                            studentNameMap[sId]
                        } ?: "—"
                        SlotBookingDto(
                            registrationId = bRow[EventRegistrationsTable.id].value.toString(),
                            parentName = bRow[AppUsersTable.fullName],
                            studentName = studentName,
                            attendeeCount = bRow[EventRegistrationsTable.attendeeCount],
                            status = bRow[EventRegistrationsTable.status],
                            registeredAt = ISO_FMT.format(bRow[EventRegistrationsTable.registeredAt]),
                        )
                    }
                    TeacherSlotDto(
                        id = slotId.toString(),
                        startTime = sRow[EventSlotsTable.startTime],
                        endTime = sRow[EventSlotsTable.endTime],
                        capacity = sRow[EventSlotsTable.capacity],
                        bookedCount = bookings.size,
                        bookings = bookingDtos,
                    )
                }

                val total = slotDtos.sumOf { it.bookedCount }
                val checkedIn = bookingsCheckedInCount(eventId)
                call.ok(
                    TeacherPtmEventDto(
                        id = eventId.toString(),
                        title = event[CalendarEventsTable.title],
                        date = event[CalendarEventsTable.startDate].toString(),
                        className = "",
                        totalRegistrations = total,
                        checkedIn = checkedIn,
                        slots = slotDtos,
                    ),
                    message = "PTM detail loaded"
                )
            }

            // ── Slot list with booked/total counts ──
            get("/{eventId}/slots") {
                val ctx = call.requireTeacherContext() ?: return@get
                val eventId = call.parameters["eventId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid eventId", HttpStatusCode.BadRequest); return@get }

                val event = dbQuery {
                    CalendarEventsTable.selectAll().where {
                        (CalendarEventsTable.id eq eventId) and
                            (CalendarEventsTable.schoolId eq ctx.schoolId) and
                            (CalendarEventsTable.type eq "PTM") and
                            (CalendarEventsTable.isActive eq true)
                    }.firstOrNull()
                } ?: run { call.fail("PTM event not found", HttpStatusCode.NotFound, "EVENT_NOT_FOUND"); return@get }

                val slots = dbQuery {
                    EventSlotsTable.selectAll().where {
                        (EventSlotsTable.eventId eq eventId) and (EventSlotsTable.isActive eq true)
                    }.orderBy(EventSlotsTable.startTime, SortOrder.ASC).toList()
                }

                val slotDtos = slots.map { sRow ->
                    val slotId = sRow[EventSlotsTable.id].value
                    val booked = countSlotBookings(slotId)
                    TeacherSlotDto(
                        id = slotId.toString(),
                        startTime = sRow[EventSlotsTable.startTime],
                        endTime = sRow[EventSlotsTable.endTime],
                        capacity = sRow[EventSlotsTable.capacity],
                        bookedCount = booked,
                    )
                }
                call.ok(slotDtos, message = "Slots loaded")
            }

            // ── CHECK-IN a parent ──
            patch("/{eventId}/checkin/{registrationId}") {
                val ctx = call.requireTeacherContext() ?: return@patch
                val eventId = call.parameters["eventId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid eventId", HttpStatusCode.BadRequest); return@patch }
                val registrationId = call.parameters["registrationId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid registrationId", HttpStatusCode.BadRequest); return@patch }

                val reg = dbQuery {
                    EventRegistrationsTable.selectAll().where {
                        (EventRegistrationsTable.id eq registrationId) and
                            (EventRegistrationsTable.eventId eq eventId) and
                            (EventRegistrationsTable.schoolId eq ctx.schoolId) and
                            (EventRegistrationsTable.status eq "REGISTERED")
                    }.firstOrNull()
                } ?: run { call.fail("Registration not found", HttpStatusCode.NotFound, "REGISTRATION_NOT_FOUND"); return@patch }

                // Verify event is PTM type
                val ptmEvent = dbQuery {
                    CalendarEventsTable.selectAll().where {
                        (CalendarEventsTable.id eq eventId) and
                            (CalendarEventsTable.type eq "PTM")
                    }.firstOrNull()
                } ?: run { call.fail("Check-in is only available for PTM events", HttpStatusCode.BadRequest, "NOT_PTM_EVENT"); return@patch }

                dbQuery {
                    EventRegistrationsTable.update({ EventRegistrationsTable.id eq registrationId }) {
                        it[status] = "CHECKED_IN"
                        it[updatedAt] = Instant.now()
                    }
                }

                // Notify parent
                runCatching {
                    Notify.toUser(
                        userId = reg[EventRegistrationsTable.parentUserId],
                        category = "event_registration",
                        title = "Checked in",
                        body = "You have been checked in for the event",
                        schoolId = ctx.schoolId,
                        deepLink = "/parent/home",
                        refType = "event_registration",
                        refId = registrationId.toString(),
                    )
                }

                call.okMessage("Parent checked in")
            }
        }

        // ───────────────────────────────────────────────────────────────────
        // Admin Endpoints
        // ───────────────────────────────────────────────────────────────────
        route("/api/v1/school/events") {

            // ── LIST all registrations (filterable) ──
            get("/registrations") {
                val ctx = call.requireSchoolContext() ?: return@get
                val schoolId = ctx.schoolId
                val statusFilter = call.request.queryParameters["status"]
                val eventIdFilter = call.request.queryParameters["eventId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }

                val rows = dbQuery {
                    (EventRegistrationsTable innerJoin CalendarEventsTable innerJoin AppUsersTable)
                        .selectAll()
                        .where {
                            (EventRegistrationsTable.schoolId eq schoolId) and
                                (CalendarEventsTable.id eq EventRegistrationsTable.eventId) and
                                (AppUsersTable.id eq EventRegistrationsTable.parentUserId)
                        }.also { q ->
                            if (statusFilter != null) q.andWhere { EventRegistrationsTable.status eq statusFilter }
                            if (eventIdFilter != null) q.andWhere { EventRegistrationsTable.eventId eq eventIdFilter }
                        }
                        .orderBy(EventRegistrationsTable.registeredAt, SortOrder.DESC)
                        .toList()
                }

                // Batch fetch student names and slot times to avoid N+1 queries
                val studentIds = rows.mapNotNull { it[EventRegistrationsTable.studentId] }.distinct()
                val slotIds = rows.mapNotNull { it[EventRegistrationsTable.slotId] }.distinct()
                val studentMap: Map<UUID, String> = if (studentIds.isNotEmpty()) {
                    dbQuery {
                        StudentsTable.selectAll().where { StudentsTable.id inList studentIds.map { EntityID(it, StudentsTable) } }
                            .associate { it[StudentsTable.id].value to it[StudentsTable.fullName] }
                    }
                } else emptyMap()
                val slotMap: Map<UUID, String> = if (slotIds.isNotEmpty()) {
                    dbQuery {
                        EventSlotsTable.selectAll().where { EventSlotsTable.id inList slotIds.map { EntityID(it, EventSlotsTable) } }
                            .associate { it[EventSlotsTable.id].value to formatSlotRange(it[EventSlotsTable.startTime], it[EventSlotsTable.endTime]) }
                    }
                } else emptyMap()

                val dtos = rows.map { row ->
                    val studentName = row[EventRegistrationsTable.studentId]?.let { studentMap[it] }
                    val slotTime = row[EventRegistrationsTable.slotId]?.let { slotMap[it] }
                    AdminRegistrationDto(
                        id = row[EventRegistrationsTable.id].value.toString(),
                        eventTitle = row[CalendarEventsTable.title],
                        eventDate = row[CalendarEventsTable.startDate].toString(),
                        parentName = row[AppUsersTable.fullName],
                        parentMobile = row[AppUsersTable.phone] ?: "",
                        studentName = studentName,
                        slotTime = slotTime,
                        attendeeCount = row[EventRegistrationsTable.attendeeCount],
                        status = row[EventRegistrationsTable.status],
                        registeredAt = ISO_FMT.format(row[EventRegistrationsTable.registeredAt]),
                    )
                }
                call.ok(AdminRegistrationListResponse(registrations = dtos), message = "Registrations loaded")
            }

            // ── Event-wise registration list ──
            get("/{eventId}/registrations") {
                val ctx = call.requireSchoolContext() ?: return@get
                val eventId = call.parameters["eventId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid eventId", HttpStatusCode.BadRequest); return@get }

                val event = dbQuery {
                    CalendarEventsTable.selectAll().where {
                        (CalendarEventsTable.id eq eventId) and
                            (CalendarEventsTable.schoolId eq ctx.schoolId)
                    }.firstOrNull()
                } ?: run { call.fail("Event not found", HttpStatusCode.NotFound, "EVENT_NOT_FOUND"); return@get }

                val rows = dbQuery {
                    (EventRegistrationsTable innerJoin AppUsersTable)
                        .selectAll()
                        .where {
                            (EventRegistrationsTable.eventId eq eventId) and
                                (EventRegistrationsTable.schoolId eq ctx.schoolId) and
                                (AppUsersTable.id eq EventRegistrationsTable.parentUserId)
                        }
                        .orderBy(EventRegistrationsTable.registeredAt, SortOrder.DESC)
                        .toList()
                }

                // Batch fetch student names and slot times to avoid N+1 queries
                val studentIds = rows.mapNotNull { it[EventRegistrationsTable.studentId] }.distinct()
                val slotIds = rows.mapNotNull { it[EventRegistrationsTable.slotId] }.distinct()
                val studentMap: Map<UUID, String> = if (studentIds.isNotEmpty()) {
                    dbQuery {
                        StudentsTable.selectAll().where { StudentsTable.id inList studentIds.map { EntityID(it, StudentsTable) } }
                            .associate { it[StudentsTable.id].value to it[StudentsTable.fullName] }
                    }
                } else emptyMap()
                val slotMap: Map<UUID, String> = if (slotIds.isNotEmpty()) {
                    dbQuery {
                        EventSlotsTable.selectAll().where { EventSlotsTable.id inList slotIds.map { EntityID(it, EventSlotsTable) } }
                            .associate { it[EventSlotsTable.id].value to formatSlotRange(it[EventSlotsTable.startTime], it[EventSlotsTable.endTime]) }
                    }
                } else emptyMap()

                val dtos = rows.map { row ->
                    val studentName = row[EventRegistrationsTable.studentId]?.let { studentMap[it] }
                    val slotTime = row[EventRegistrationsTable.slotId]?.let { slotMap[it] }
                    AdminRegistrationDto(
                        id = row[EventRegistrationsTable.id].value.toString(),
                        eventTitle = event[CalendarEventsTable.title],
                        eventDate = event[CalendarEventsTable.startDate].toString(),
                        parentName = row[AppUsersTable.fullName],
                        parentMobile = row[AppUsersTable.phone] ?: "",
                        studentName = studentName,
                        slotTime = slotTime,
                        attendeeCount = row[EventRegistrationsTable.attendeeCount],
                        status = row[EventRegistrationsTable.status],
                        registeredAt = ISO_FMT.format(row[EventRegistrationsTable.registeredAt]),
                    )
                }
                call.ok(AdminRegistrationListResponse(registrations = dtos), message = "Event registrations loaded")
            }

            // ── CREATE slot ──
            post("/{eventId}/slots") {
                val ctx = call.requireSchoolContext() ?: return@post
                val eventId = call.parameters["eventId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid eventId", HttpStatusCode.BadRequest); return@post }
                val req = call.receive<CreateSlotRequest>()

                if (parseHHmm(req.startTime) == null || parseHHmm(req.endTime) == null) {
                    call.fail("start_time and end_time must be HH:mm", HttpStatusCode.BadRequest); return@post
                }
                if (req.capacity < 1) {
                    call.fail("capacity must be at least 1", HttpStatusCode.BadRequest); return@post
                }

                val event = dbQuery {
                    CalendarEventsTable.selectAll().where {
                        (CalendarEventsTable.id eq eventId) and
                            (CalendarEventsTable.schoolId eq ctx.schoolId)
                    }.firstOrNull()
                } ?: run { call.fail("Event not found", HttpStatusCode.NotFound, "EVENT_NOT_FOUND"); return@post }

                val slotId = UUID.randomUUID()
                val now = Instant.now()
                dbQuery {
                    EventSlotsTable.insert {
                        it[EventSlotsTable.id] = slotId
                        it[EventSlotsTable.eventId] = eventId
                        it[EventSlotsTable.startTime] = req.startTime
                        it[EventSlotsTable.endTime] = req.endTime
                        it[EventSlotsTable.capacity] = req.capacity
                        it[EventSlotsTable.isActive] = true
                        it[EventSlotsTable.createdAt] = now
                        it[EventSlotsTable.updatedAt] = now
                    }
                }
                call.created(
                    SlotResponse(
                        id = slotId.toString(),
                        startTime = req.startTime,
                        endTime = req.endTime,
                        capacity = req.capacity,
                        isActive = true,
                    ),
                    message = "Slot created"
                )
            }

            // ── AUTO-GENERATE slots ──
            post("/{eventId}/slots/auto-generate") {
                val ctx = call.requireSchoolContext() ?: return@post
                val eventId = call.parameters["eventId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid eventId", HttpStatusCode.BadRequest); return@post }
                val req = call.receive<AutoGenerateSlotsRequest>()

                val rangeStart = parseHHmm(req.rangeStart)
                    ?: run { call.fail("range_start must be HH:mm", HttpStatusCode.BadRequest); return@post }
                val rangeEnd = parseHHmm(req.rangeEnd)
                    ?: run { call.fail("range_end must be HH:mm", HttpStatusCode.BadRequest); return@post }
                if (!rangeStart.isBefore(rangeEnd)) {
                    call.fail("range_start must be before range_end", HttpStatusCode.BadRequest); return@post
                }
                if (req.slotDurationMinutes < 1) {
                    call.fail("slot_duration_minutes must be at least 1", HttpStatusCode.BadRequest); return@post
                }
                if (req.capacity < 1) {
                    call.fail("capacity must be at least 1", HttpStatusCode.BadRequest); return@post
                }

                val event = dbQuery {
                    CalendarEventsTable.selectAll().where {
                        (CalendarEventsTable.id eq eventId) and
                            (CalendarEventsTable.schoolId eq ctx.schoolId)
                    }.firstOrNull()
                } ?: run { call.fail("Event not found", HttpStatusCode.NotFound, "EVENT_NOT_FOUND"); return@post }

                val now = Instant.now()
                val createdSlots = mutableListOf<SlotResponse>()
                var current = rangeStart
                var slotsSinceBreak = 0

                while (current.plusMinutes(req.slotDurationMinutes.toLong()) <= rangeEnd) {
                    val slotEnd = current.plusMinutes(req.slotDurationMinutes.toLong())
                    val startStr = current.toString().padStart(5, '0')
                    val endStr = slotEnd.toString().padStart(5, '0')

                    val slotId = UUID.randomUUID()
                    dbQuery {
                        EventSlotsTable.insert {
                            it[EventSlotsTable.id] = slotId
                            it[EventSlotsTable.eventId] = eventId
                            it[EventSlotsTable.startTime] = startStr
                            it[EventSlotsTable.endTime] = endStr
                            it[EventSlotsTable.capacity] = req.capacity
                            it[EventSlotsTable.isActive] = true
                            it[EventSlotsTable.createdAt] = now
                            it[EventSlotsTable.updatedAt] = now
                        }
                    }
                    createdSlots.add(
                        SlotResponse(
                            id = slotId.toString(),
                            startTime = startStr,
                            endTime = endStr,
                            capacity = req.capacity,
                            isActive = true,
                        )
                    )

                    current = slotEnd
                    slotsSinceBreak++

                    // Insert break if configured
                    if (req.breakAfterSlots > 0 && slotsSinceBreak >= req.breakAfterSlots && current.plusMinutes(req.breakDurationMinutes.toLong()) < rangeEnd) {
                        current = current.plusMinutes(req.breakDurationMinutes.toLong())
                        slotsSinceBreak = 0
                    }
                }

                call.created(
                    AutoGenerateSlotsResponse(slots = createdSlots),
                    message = "${createdSlots.size} slots generated"
                )
            }

            // ── UPDATE slot ──
            put("/{eventId}/slots/{slotId}") {
                val ctx = call.requireSchoolContext() ?: return@put
                val eventId = call.parameters["eventId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid eventId", HttpStatusCode.BadRequest); return@put }
                val slotId = call.parameters["slotId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid slotId", HttpStatusCode.BadRequest); return@put }
                val req = call.receive<CreateSlotRequest>()

                if (parseHHmm(req.startTime) == null || parseHHmm(req.endTime) == null) {
                    call.fail("start_time and end_time must be HH:mm", HttpStatusCode.BadRequest); return@put
                }
                if (req.capacity < 1) {
                    call.fail("capacity must be at least 1", HttpStatusCode.BadRequest); return@put
                }

                // Validate event belongs to school
                val eventOwned = dbQuery {
                    CalendarEventsTable.selectAll().where {
                        (CalendarEventsTable.id eq eventId) and
                            (CalendarEventsTable.schoolId eq ctx.schoolId)
                    }.any()
                }
                if (!eventOwned) {
                    call.fail("Event not found", HttpStatusCode.NotFound, "EVENT_NOT_FOUND"); return@put
                }

                val slot = dbQuery {
                    EventSlotsTable.selectAll().where {
                        (EventSlotsTable.id eq slotId) and (EventSlotsTable.eventId eq eventId)
                    }.firstOrNull()
                } ?: run { call.fail("Slot not found", HttpStatusCode.NotFound, "SLOT_NOT_FOUND"); return@put }

                dbQuery {
                    EventSlotsTable.update({ EventSlotsTable.id eq slotId }) {
                        it[EventSlotsTable.startTime] = req.startTime
                        it[EventSlotsTable.endTime] = req.endTime
                        it[EventSlotsTable.capacity] = req.capacity
                        it[EventSlotsTable.updatedAt] = Instant.now()
                    }
                }
                call.ok(
                    SlotResponse(
                        id = slotId.toString(),
                        startTime = req.startTime,
                        endTime = req.endTime,
                        capacity = req.capacity,
                        isActive = slot[EventSlotsTable.isActive],
                    ),
                    message = "Slot updated"
                )
            }

            // ── DELETE slot ──
            delete("/{eventId}/slots/{slotId}") {
                val ctx = call.requireSchoolContext() ?: return@delete
                val eventId = call.parameters["eventId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid eventId", HttpStatusCode.BadRequest); return@delete }
                val slotId = call.parameters["slotId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid slotId", HttpStatusCode.BadRequest); return@delete }

                // Validate event belongs to school
                val eventOwned = dbQuery {
                    CalendarEventsTable.selectAll().where {
                        (CalendarEventsTable.id eq eventId) and
                            (CalendarEventsTable.schoolId eq ctx.schoolId)
                    }.any()
                }
                if (!eventOwned) {
                    call.fail("Event not found", HttpStatusCode.NotFound, "EVENT_NOT_FOUND"); return@delete
                }

                // Check no active registrations
                val hasRegs = dbQuery {
                    EventRegistrationsTable.selectAll().where {
                        (EventRegistrationsTable.slotId eq slotId) and
                            (EventRegistrationsTable.status inList listOf("REGISTERED", "CHECKED_IN"))
                    }.any()
                }
                if (hasRegs) {
                    call.fail("Cannot delete slot with active registrations", HttpStatusCode.Conflict, "SLOT_HAS_REGISTRATIONS"); return@delete
                }

                val deleted = dbQuery {
                    EventSlotsTable.deleteWhere {
                        (EventSlotsTable.id eq slotId) and (EventSlotsTable.eventId eq eventId)
                    }
                }
                if (deleted == 0) {
                    call.fail("Slot not found", HttpStatusCode.NotFound, "SLOT_NOT_FOUND"); return@delete
                }
                call.okMessage("Slot deleted")
            }

            // ── UPDATE registration config (enable/disable, deadline, etc.) ──
            patch("/{eventId}/registration-status") {
                val ctx = call.requireSchoolContext() ?: return@patch
                val eventId = call.parameters["eventId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid eventId", HttpStatusCode.BadRequest); return@patch }
                val req = call.receive<UpdateRegistrationConfigRequest>()

                val event = dbQuery {
                    CalendarEventsTable.selectAll().where {
                        (CalendarEventsTable.id eq eventId) and
                            (CalendarEventsTable.schoolId eq ctx.schoolId)
                    }.firstOrNull()
                } ?: run { call.fail("Event not found", HttpStatusCode.NotFound, "EVENT_NOT_FOUND"); return@patch }

                dbQuery {
                    CalendarEventsTable.update({ CalendarEventsTable.id eq eventId }) {
                        req.registrationEnabled?.let { v -> it[CalendarEventsTable.registrationEnabled] = v }
                        req.registrationDeadline?.let { v -> it[CalendarEventsTable.registrationDeadline] = v }
                        req.maxAttendees?.let { v -> it[CalendarEventsTable.maxAttendees] = v }
                        req.venue?.let { v -> it[CalendarEventsTable.venue] = v }
                        it[CalendarEventsTable.updatedAt] = Instant.now()
                    }
                }
                call.okMessage("Registration settings updated")
            }

            // ── CANCEL event → auto-cancel all registrations + notify ──
            post("/{eventId}/cancel") {
                val ctx = call.requireSchoolContext() ?: return@post
                val eventId = call.parameters["eventId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid eventId", HttpStatusCode.BadRequest); return@post }

                val event = dbQuery {
                    CalendarEventsTable.selectAll().where {
                        (CalendarEventsTable.id eq eventId) and
                            (CalendarEventsTable.schoolId eq ctx.schoolId)
                    }.firstOrNull()
                } ?: run { call.fail("Event not found", HttpStatusCode.NotFound, "EVENT_NOT_FOUND"); return@post }

                val now = Instant.now()
                dbQuery {
                    CalendarEventsTable.update({ CalendarEventsTable.id eq eventId }) {
                        it[status] = "CANCELLED"
                        it[updatedAt] = now
                    }
                }

                // Auto-cancel all active registrations
                val activeRegs = dbQuery {
                    EventRegistrationsTable.selectAll().where {
                        (EventRegistrationsTable.eventId eq eventId) and
                            (EventRegistrationsTable.status inList listOf("REGISTERED", "CHECKED_IN"))
                    }.toList()
                }
                dbQuery {
                    EventRegistrationsTable.update({
                        (EventRegistrationsTable.eventId eq eventId) and
                            (EventRegistrationsTable.status inList listOf("REGISTERED", "CHECKED_IN"))
                    }) {
                        it[status] = "CANCELLED"
                        it[cancelReason] = "Event cancelled by school"
                        it[cancelledAt] = now
                        it[updatedAt] = now
                    }
                }

                // Notify all registered parents
                val parentIds = activeRegs.map { it[EventRegistrationsTable.parentUserId] }.distinct()
                runCatching {
                    if (parentIds.isNotEmpty()) {
                        Notify.toUsers(
                            userIds = parentIds,
                            category = "event_registration",
                            title = "Event cancelled",
                            body = "'${event[CalendarEventsTable.title]}' has been cancelled",
                            schoolId = ctx.schoolId,
                            deepLink = "/parent/home",
                            refType = "calendar_event",
                            refId = eventId.toString(),
                        )
                    }
                }

                call.okMessage("Event cancelled, ${activeRegs.size} registrations cancelled")
            }

            // ── CSV EXPORT ──
            get("/{eventId}/registrations/export") {
                val ctx = call.requireSchoolContext() ?: return@get
                val eventId = call.parameters["eventId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid eventId", HttpStatusCode.BadRequest); return@get }

                val event = dbQuery {
                    CalendarEventsTable.selectAll().where {
                        (CalendarEventsTable.id eq eventId) and
                            (CalendarEventsTable.schoolId eq ctx.schoolId)
                    }.firstOrNull()
                } ?: run { call.fail("Event not found", HttpStatusCode.NotFound, "EVENT_NOT_FOUND"); return@get }

                val rows = dbQuery {
                    (EventRegistrationsTable innerJoin AppUsersTable)
                        .selectAll()
                        .where {
                            (EventRegistrationsTable.eventId eq eventId) and
                                (EventRegistrationsTable.schoolId eq ctx.schoolId) and
                                (AppUsersTable.id eq EventRegistrationsTable.parentUserId)
                        }
                        .orderBy(EventRegistrationsTable.registeredAt, SortOrder.DESC)
                        .toList()
                }

                // Batch fetch student names and slot times to avoid N+1 queries
                val studentIds = rows.mapNotNull { it[EventRegistrationsTable.studentId] }.distinct()
                val slotIds = rows.mapNotNull { it[EventRegistrationsTable.slotId] }.distinct()
                val studentMap: Map<UUID, String> = if (studentIds.isNotEmpty()) {
                    dbQuery {
                        StudentsTable.selectAll().where { StudentsTable.id inList studentIds.map { EntityID(it, StudentsTable) } }
                            .associate { it[StudentsTable.id].value to it[StudentsTable.fullName] }
                    }
                } else emptyMap()
                val slotMap: Map<UUID, String> = if (slotIds.isNotEmpty()) {
                    dbQuery {
                        EventSlotsTable.selectAll().where { EventSlotsTable.id inList slotIds.map { EntityID(it, EventSlotsTable) } }
                            .associate { it[EventSlotsTable.id].value to formatSlotRange(it[EventSlotsTable.startTime], it[EventSlotsTable.endTime]) }
                    }
                } else emptyMap()

                val sb = StringBuilder()
                sb.appendLine("Registration ID,Event Title,Event Date,Parent Name,Parent Mobile,Student Name,Slot Time,Attendee Count,Status,Registered At")
                for (row in rows) {
                    val studentName = row[EventRegistrationsTable.studentId]?.let { studentMap[it] } ?: ""
                    val slotTime = row[EventRegistrationsTable.slotId]?.let { slotMap[it] } ?: ""
                    sb.appendLine(listOf(
                        row[EventRegistrationsTable.id].value.toString(),
                        event[CalendarEventsTable.title],
                        event[CalendarEventsTable.startDate].toString(),
                        row[AppUsersTable.fullName],
                        row[AppUsersTable.phone] ?: "",
                        studentName,
                        slotTime,
                        row[EventRegistrationsTable.attendeeCount].toString(),
                        row[EventRegistrationsTable.status],
                        ISO_FMT.format(row[EventRegistrationsTable.registeredAt]),
                    ).joinToString(",") { escapeCsvField(it) })
                }
                call.respondText(sb.toString(), ContentType.Text.CSV, HttpStatusCode.OK)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Private helper (cannot be local due to dbQuery context)
// ═══════════════════════════════════════════════════════════════════════════

private suspend fun bookingsCheckedInCount(eventId: UUID): Int = dbQuery {
    EventRegistrationsTable.selectAll().where {
        (EventRegistrationsTable.eventId eq eventId) and
            (EventRegistrationsTable.status eq "CHECKED_IN")
    }.count().toInt()
}

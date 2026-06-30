package com.littlebridge.enrollplus.feature.event.data.local

import com.littlebridge.enrollplus.core.database.AppDatabase
import com.littlebridge.enrollplus.core.model.ApiResponse
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.feature.event.data.repository.EventRegistrationRepositoryImpl
import com.littlebridge.enrollplus.feature.event.domain.model.CancelRegistrationRequest
import com.littlebridge.enrollplus.feature.event.domain.model.ParentEventDto
import com.littlebridge.enrollplus.feature.event.domain.model.ParentEventListResponse
import com.littlebridge.enrollplus.feature.event.domain.model.RegisterRequest
import com.littlebridge.enrollplus.feature.event.domain.model.RegistrationDto
import com.littlebridge.enrollplus.feature.event.domain.model.RescheduleRequest
import java.util.UUID

class OfflineAwareEventRepository(
    private val db: AppDatabase,
    private val remote: EventRegistrationRepositoryImpl,
) {
    suspend fun getParentEvents(token: String): NetworkResult<ApiResponse<ParentEventListResponse>> {
        val result = remote.listParentEvents(token)
        if (result is NetworkResult.Success) {
            val events = result.data.data?.events ?: emptyList()
            val entities = events.map { dto ->
                EventCacheEntity(
                    id = dto.id,
                    title = dto.title,
                    description = dto.description,
                    startDate = dto.startDate,
                    endDate = if (dto.endDate.isNotEmpty()) dto.endDate else null,
                    type = dto.type,
                    venue = dto.venue,
                    schoolId = "",
                    registrationEnabled = dto.registrationEnabled,
                    maxAttendees = dto.maxAttendees,
                    hasSlots = dto.hasSlots,
                    myStatus = dto.myRegistrationStatus,
                    mySlotId = dto.mySlotId,
                    myAttendeeCount = dto.myAttendeeCount,
                    cachedAt = System.currentTimeMillis(),
                )
            }
            if (entities.isNotEmpty()) {
                db.eventCacheDao().deleteAll()
                db.eventCacheDao().insertEvents(entities)
            }
        }
        // If network fails, try cache
        if (result is NetworkResult.ConnectionError || result is NetworkResult.Error) {
            val cached = db.eventCacheDao().getAllEvents()
            if (cached.isNotEmpty()) {
                val dtos = cached.map { e ->
                    ParentEventDto(
                        id = e.id,
                        title = e.title,
                        description = e.description,
                        startDate = e.startDate,
                        endDate = e.endDate ?: "",
                        type = e.type,
                        venue = e.venue,
                        registrationEnabled = e.registrationEnabled,
                        maxAttendees = e.maxAttendees,
                        hasSlots = e.hasSlots,
                        myRegistrationStatus = e.myStatus,
                        mySlotId = e.mySlotId,
                        myAttendeeCount = e.myAttendeeCount,
                    )
                }
                return NetworkResult.Success(ApiResponse(success = true, data = ParentEventListResponse(events = dtos)))
            }
        }
        return result
    }

    suspend fun register(
        token: String,
        eventId: String,
        request: RegisterRequest,
    ): NetworkResult<ApiResponse<RegistrationDto>> {
        val result = remote.register(token, eventId, request, null)
        if (result is NetworkResult.ConnectionError) {
            // Queue for offline sync
            db.eventOutboxDao().insert(
                EventOutboxEntity(
                    id = UUID.randomUUID().toString(),
                    operation = "REGISTER",
                    eventId = eventId,
                    slotId = request.slotId,
                    studentId = request.studentId,
                    attendeeCount = request.attendeeCount,
                    clientRequestId = UUID.randomUUID().toString(),
                    status = "PENDING",
                    attempts = 0,
                    lastError = null,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                )
            )
            return NetworkResult.Error("Offline — registration queued for sync")
        }
        return result
    }

    suspend fun cancelRegistration(
        token: String,
        eventId: String,
        request: CancelRegistrationRequest,
    ): NetworkResult<ApiResponse<Unit>> {
        val result = remote.cancelRegistration(token, eventId, request)
        if (result is NetworkResult.ConnectionError) {
            db.eventOutboxDao().insert(
                EventOutboxEntity(
                    id = UUID.randomUUID().toString(),
                    operation = "CANCEL",
                    eventId = eventId,
                    slotId = null,
                    studentId = request.studentId,
                    attendeeCount = 0,
                    clientRequestId = UUID.randomUUID().toString(),
                    status = "PENDING",
                    attempts = 0,
                    lastError = null,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                )
            )
            return NetworkResult.Error("Offline — cancellation queued for sync")
        }
        return result
    }

    suspend fun reschedule(
        token: String,
        eventId: String,
        request: RescheduleRequest,
    ): NetworkResult<ApiResponse<RegistrationDto>> {
        val result = remote.reschedule(token, eventId, request)
        if (result is NetworkResult.ConnectionError) {
            db.eventOutboxDao().insert(
                EventOutboxEntity(
                    id = UUID.randomUUID().toString(),
                    operation = "RESCHEDULE",
                    eventId = eventId,
                    slotId = request.newSlotId,
                    studentId = null,
                    attendeeCount = 0,
                    clientRequestId = UUID.randomUUID().toString(),
                    status = "PENDING",
                    attempts = 0,
                    lastError = null,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                )
            )
            return NetworkResult.Error("Offline — reschedule queued for sync")
        }
        return result
    }
}

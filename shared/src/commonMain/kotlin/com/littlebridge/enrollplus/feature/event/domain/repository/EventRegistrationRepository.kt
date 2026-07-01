/*
 * File: EventRegistrationRepository.kt
 * Module: feature.event.domain.repository
 *
 * Interface for Event Registration operations (parent, teacher, admin).
 */
package com.littlebridge.enrollplus.feature.event.domain.repository

import com.littlebridge.enrollplus.core.model.ApiResponse
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.feature.event.domain.model.*

interface EventRegistrationRepository {
    // ── Parent ──
    suspend fun listParentEvents(token: String): NetworkResult<ApiResponse<ParentEventListResponse>>
    suspend fun getParentEventDetail(token: String, eventId: String): NetworkResult<ApiResponse<ParentEventDetailResponse>>
    suspend fun register(token: String, eventId: String, request: RegisterRequest, clientRequestId: String? = null): NetworkResult<ApiResponse<RegistrationDto>>
    suspend fun cancelRegistration(token: String, eventId: String, request: CancelRegistrationRequest): NetworkResult<ApiResponse<Unit>>
    suspend fun listMyRegistrations(token: String): NetworkResult<ApiResponse<RegistrationListResponse>>
    suspend fun reschedule(token: String, eventId: String, request: RescheduleRequest): NetworkResult<ApiResponse<RegistrationDto>>

    // ── Teacher ──
    suspend fun getTeacherPtmEvents(token: String): NetworkResult<ApiResponse<TeacherPtmListResponse>>
    suspend fun getTeacherPtmDetail(token: String, eventId: String): NetworkResult<ApiResponse<TeacherPtmEventDto>>
    suspend fun getTeacherPtmSlots(token: String, eventId: String): NetworkResult<ApiResponse<List<TeacherSlotDto>>>
    suspend fun checkinParent(token: String, eventId: String, registrationId: String): NetworkResult<ApiResponse<Unit>>

    // ── Admin ──
    suspend fun listAdminEvents(token: String): NetworkResult<ApiResponse<AdminEventListResponse>>
    suspend fun listAllRegistrations(token: String, status: String? = null, eventId: String? = null): NetworkResult<ApiResponse<AdminRegistrationListResponse>>
    suspend fun listEventRegistrations(token: String, eventId: String): NetworkResult<ApiResponse<AdminRegistrationListResponse>>
    suspend fun createSlot(token: String, eventId: String, request: CreateSlotRequest): NetworkResult<ApiResponse<SlotResponse>>
    suspend fun autoGenerateSlots(token: String, eventId: String, request: AutoGenerateSlotsRequest): NetworkResult<ApiResponse<AutoGenerateSlotsResponse>>
    suspend fun updateSlot(token: String, eventId: String, slotId: String, request: CreateSlotRequest): NetworkResult<ApiResponse<SlotResponse>>
    suspend fun deleteSlot(token: String, eventId: String, slotId: String): NetworkResult<ApiResponse<Unit>>
    suspend fun updateRegistrationConfig(token: String, eventId: String, request: UpdateRegistrationConfigRequest): NetworkResult<ApiResponse<Unit>>
    suspend fun cancelEvent(token: String, eventId: String): NetworkResult<ApiResponse<Unit>>
    suspend fun exportRegistrationsCsv(token: String, eventId: String): NetworkResult<String>
}

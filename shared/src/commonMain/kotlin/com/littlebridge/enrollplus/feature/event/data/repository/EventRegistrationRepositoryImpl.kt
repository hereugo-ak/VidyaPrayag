/*
 * File: EventRegistrationRepositoryImpl.kt
 * Module: feature.event.data.repository
 *
 * Implements EventRegistrationRepository by delegating to EventRegistrationApi.
 */
package com.littlebridge.enrollplus.feature.event.data.repository

import com.littlebridge.enrollplus.core.model.ApiResponse
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.feature.event.data.remote.EventRegistrationApi
import com.littlebridge.enrollplus.feature.event.domain.model.*
import com.littlebridge.enrollplus.feature.event.domain.repository.EventRegistrationRepository

class EventRegistrationRepositoryImpl(
    private val api: EventRegistrationApi,
) : EventRegistrationRepository {

    override suspend fun listParentEvents(token: String) = api.listParentEvents(token)
    override suspend fun getParentEventDetail(token: String, eventId: String) = api.getParentEventDetail(token, eventId)
    override suspend fun register(token: String, eventId: String, request: RegisterRequest, clientRequestId: String?) = api.register(token, eventId, request, clientRequestId)
    override suspend fun cancelRegistration(token: String, eventId: String, request: CancelRegistrationRequest) = api.cancelRegistration(token, eventId, request)
    override suspend fun listMyRegistrations(token: String) = api.listMyRegistrations(token)
    override suspend fun reschedule(token: String, eventId: String, request: RescheduleRequest) = api.reschedule(token, eventId, request)

    override suspend fun getTeacherPtmEvents(token: String) = api.getTeacherPtmEvents(token)
    override suspend fun getTeacherPtmDetail(token: String, eventId: String) = api.getTeacherPtmDetail(token, eventId)
    override suspend fun getTeacherPtmSlots(token: String, eventId: String) = api.getTeacherPtmSlots(token, eventId)
    override suspend fun checkinParent(token: String, eventId: String, registrationId: String) = api.checkinParent(token, eventId, registrationId)

    override suspend fun listAllRegistrations(token: String, status: String?, eventId: String?) = api.listAllRegistrations(token, status, eventId)
    override suspend fun listEventRegistrations(token: String, eventId: String) = api.listEventRegistrations(token, eventId)
    override suspend fun createSlot(token: String, eventId: String, request: CreateSlotRequest) = api.createSlot(token, eventId, request)
    override suspend fun autoGenerateSlots(token: String, eventId: String, request: AutoGenerateSlotsRequest) = api.autoGenerateSlots(token, eventId, request)
    override suspend fun updateSlot(token: String, eventId: String, slotId: String, request: CreateSlotRequest) = api.updateSlot(token, eventId, slotId, request)
    override suspend fun deleteSlot(token: String, eventId: String, slotId: String) = api.deleteSlot(token, eventId, slotId)
    override suspend fun updateRegistrationConfig(token: String, eventId: String, request: UpdateRegistrationConfigRequest) = api.updateRegistrationConfig(token, eventId, request)
    override suspend fun cancelEvent(token: String, eventId: String) = api.cancelEvent(token, eventId)
    override suspend fun exportRegistrationsCsv(token: String, eventId: String) = api.exportRegistrationsCsv(token, eventId)
}

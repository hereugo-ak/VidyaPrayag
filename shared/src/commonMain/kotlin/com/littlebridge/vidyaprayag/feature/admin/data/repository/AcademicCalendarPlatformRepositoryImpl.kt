/*
 * File: AcademicCalendarPlatformRepositoryImpl.kt
 * Module: feature.admin.data.repository
 */
package com.littlebridge.vidyaprayag.feature.admin.data.repository

import com.littlebridge.vidyaprayag.core.model.ApiResponse
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.feature.admin.data.remote.AcademicCalendarPlatformApi
import com.littlebridge.vidyaprayag.feature.admin.domain.model.AcademicCalendarEventDto
import com.littlebridge.vidyaprayag.feature.admin.domain.model.CalendarDashboardDto
import com.littlebridge.vidyaprayag.feature.admin.domain.model.CalendarEventsListResponse
import com.littlebridge.vidyaprayag.feature.admin.domain.model.CreateCalendarEventRequest
import com.littlebridge.vidyaprayag.feature.admin.domain.model.DuplicateCalendarEventRequest
import com.littlebridge.vidyaprayag.feature.admin.domain.model.UpdateCalendarEventRequest
import com.littlebridge.vidyaprayag.feature.admin.domain.repository.AcademicCalendarPlatformRepository

class AcademicCalendarPlatformRepositoryImpl(
    private val api: AcademicCalendarPlatformApi
) : AcademicCalendarPlatformRepository {

    override suspend fun getDashboard(token: String): NetworkResult<ApiResponse<CalendarDashboardDto>> =
        api.getDashboard(token)

    override suspend fun getEvents(
        token: String,
        month: String?,
        status: String?,
        type: String?
    ): NetworkResult<ApiResponse<CalendarEventsListResponse>> =
        api.getEvents(token, month, status, type)

    override suspend fun getEvent(token: String, eventId: String): NetworkResult<ApiResponse<AcademicCalendarEventDto>> =
        api.getEvent(token, eventId)

    override suspend fun createEvent(token: String, request: CreateCalendarEventRequest): NetworkResult<ApiResponse<AcademicCalendarEventDto>> =
        api.createEvent(token, request)

    override suspend fun updateEvent(token: String, eventId: String, request: UpdateCalendarEventRequest): NetworkResult<ApiResponse<AcademicCalendarEventDto>> =
        api.updateEvent(token, eventId, request)

    override suspend fun deleteEvent(token: String, eventId: String): NetworkResult<ApiResponse<Unit>> =
        api.deleteEvent(token, eventId)

    override suspend fun duplicateEvent(token: String, eventId: String, request: DuplicateCalendarEventRequest): NetworkResult<ApiResponse<AcademicCalendarEventDto>> =
        api.duplicateEvent(token, eventId, request)
}

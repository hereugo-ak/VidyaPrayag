/*
 * File: EventRegistrationApi.kt
 * Module: feature.event.data.remote
 *
 * Network client for Event Registration endpoints.
 * Bearer token is attached by the shared HttpClient Auth plugin.
 *
 * Server routes (feature.event.EventRegistrationRouting.kt):
 *   Parent:
 *     GET    /api/v1/parent/events
 *     GET    /api/v1/parent/events/{eventId}
 *     POST   /api/v1/parent/events/{eventId}/register
 *     DELETE /api/v1/parent/events/{eventId}/register
 *     GET    /api/v1/parent/events/registrations
 *     PATCH  /api/v1/parent/events/{eventId}/reschedule
 *   Teacher:
 *     GET    /api/v1/teacher/events/ptm
 *     GET    /api/v1/teacher/events/ptm/{eventId}
 *     GET    /api/v1/teacher/events/ptm/{eventId}/slots
 *     PATCH  /api/v1/teacher/events/ptm/{eventId}/checkin/{registrationId}
 *   Admin:
 *     GET    /api/v1/school/events                              — list all events for management
 *     GET    /api/v1/school/events/registrations
 *     GET    /api/v1/school/events/{eventId}/registrations
 *     POST   /api/v1/school/events/{eventId}/slots
 *     POST   /api/v1/school/events/{eventId}/slots/auto-generate
 *     PUT    /api/v1/school/events/{eventId}/slots/{slotId}
 *     DELETE /api/v1/school/events/{eventId}/slots/{slotId}
 *     PATCH  /api/v1/school/events/{eventId}/registration-status
 *     POST   /api/v1/school/events/{eventId}/cancel
 *     GET    /api/v1/school/events/{eventId}/registrations/export
 */
package com.littlebridge.enrollplus.feature.event.data.remote

import com.littlebridge.enrollplus.core.model.ApiResponse
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.core.network.safeApiCall
import com.littlebridge.enrollplus.feature.event.domain.model.*
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class EventRegistrationApi(
    private val client: HttpClient,
    private val baseUrl: String,
) {
    private fun url(path: String): String {
        val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val cleanPath = if (path.startsWith("/")) path.substring(1) else path
        return "$base$cleanPath"
    }

    // ── Parent ──

    suspend fun listParentEvents(token: String): NetworkResult<ApiResponse<ParentEventListResponse>> =
        safeApiCall { client.get(url("api/v1/parent/events")) }

    suspend fun getParentEventDetail(token: String, eventId: String): NetworkResult<ApiResponse<ParentEventDetailResponse>> =
        safeApiCall { client.get(url("api/v1/parent/events/$eventId")) }

    suspend fun register(
        token: String,
        eventId: String,
        request: RegisterRequest,
        clientRequestId: String? = null,
    ): NetworkResult<ApiResponse<RegistrationDto>> = safeApiCall {
        client.post(url("api/v1/parent/events/$eventId/register")) {
            contentType(ContentType.Application.Json)
            setBody(request)
            clientRequestId?.let { header("X-Client-Request-Id", it) }
        }
    }

    suspend fun cancelRegistration(
        token: String,
        eventId: String,
        request: CancelRegistrationRequest,
    ): NetworkResult<ApiResponse<Unit>> = safeApiCall {
        client.delete(url("api/v1/parent/events/$eventId/register")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun listMyRegistrations(token: String): NetworkResult<ApiResponse<RegistrationListResponse>> =
        safeApiCall { client.get(url("api/v1/parent/events/registrations")) }

    suspend fun reschedule(
        token: String,
        eventId: String,
        request: RescheduleRequest,
    ): NetworkResult<ApiResponse<RegistrationDto>> = safeApiCall {
        client.patch(url("api/v1/parent/events/$eventId/reschedule")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    // ── Teacher ──

    suspend fun getTeacherPtmEvents(token: String): NetworkResult<ApiResponse<TeacherPtmListResponse>> =
        safeApiCall { client.get(url("api/v1/teacher/events/ptm")) }

    suspend fun getTeacherPtmDetail(token: String, eventId: String): NetworkResult<ApiResponse<TeacherPtmEventDto>> =
        safeApiCall { client.get(url("api/v1/teacher/events/ptm/$eventId")) }

    suspend fun getTeacherPtmSlots(token: String, eventId: String): NetworkResult<ApiResponse<List<TeacherSlotDto>>> =
        safeApiCall { client.get(url("api/v1/teacher/events/ptm/$eventId/slots")) }

    suspend fun checkinParent(token: String, eventId: String, registrationId: String): NetworkResult<ApiResponse<Unit>> =
        safeApiCall { client.patch(url("api/v1/teacher/events/ptm/$eventId/checkin/$registrationId")) }

    // ── Admin ──

    suspend fun listAdminEvents(token: String): NetworkResult<ApiResponse<AdminEventListResponse>> =
        safeApiCall { client.get(url("api/v1/school/events")) }

    suspend fun listAllRegistrations(
        token: String,
        status: String? = null,
        eventId: String? = null,
    ): NetworkResult<ApiResponse<AdminRegistrationListResponse>> = safeApiCall {
        val params = mutableListOf<String>()
        status?.let { params.add("status=$it") }
        eventId?.let { params.add("eventId=$it") }
        val query = if (params.isEmpty()) "" else "?" + params.joinToString("&")
        client.get(url("api/v1/school/events/registrations$query"))
    }

    suspend fun listEventRegistrations(token: String, eventId: String): NetworkResult<ApiResponse<AdminRegistrationListResponse>> =
        safeApiCall { client.get(url("api/v1/school/events/$eventId/registrations")) }

    suspend fun createSlot(token: String, eventId: String, request: CreateSlotRequest): NetworkResult<ApiResponse<SlotResponse>> =
        safeApiCall {
            client.post(url("api/v1/school/events/$eventId/slots")) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }

    suspend fun autoGenerateSlots(token: String, eventId: String, request: AutoGenerateSlotsRequest): NetworkResult<ApiResponse<AutoGenerateSlotsResponse>> =
        safeApiCall {
            client.post(url("api/v1/school/events/$eventId/slots/auto-generate")) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }

    suspend fun updateSlot(token: String, eventId: String, slotId: String, request: CreateSlotRequest): NetworkResult<ApiResponse<SlotResponse>> =
        safeApiCall {
            client.put(url("api/v1/school/events/$eventId/slots/$slotId")) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }

    suspend fun deleteSlot(token: String, eventId: String, slotId: String): NetworkResult<ApiResponse<Unit>> =
        safeApiCall { client.delete(url("api/v1/school/events/$eventId/slots/$slotId")) }

    suspend fun updateRegistrationConfig(token: String, eventId: String, request: UpdateRegistrationConfigRequest): NetworkResult<ApiResponse<Unit>> =
        safeApiCall {
            client.patch(url("api/v1/school/events/$eventId/registration-status")) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }

    suspend fun cancelEvent(token: String, eventId: String): NetworkResult<ApiResponse<Unit>> =
        safeApiCall { client.post(url("api/v1/school/events/$eventId/cancel")) }

    suspend fun exportRegistrationsCsv(token: String, eventId: String): NetworkResult<String> =
        safeApiCall { client.get(url("api/v1/school/events/$eventId/registrations/export")) }
}

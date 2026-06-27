package com.littlebridge.enrollplus.feature.health.data.remote

import com.littlebridge.enrollplus.core.model.ApiResponse
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.core.network.safeApiCall
import com.littlebridge.enrollplus.feature.health.domain.model.*
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class HealthApi(
    private val client: HttpClient,
    private val baseUrl: String,
) {
    private fun getUrl(path: String): String {
        val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val cleanPath = if (path.startsWith("/")) path.substring(1) else path
        return "$base$cleanPath"
    }

    // ── Admin/Nurse: school health ──────────────────────────────────

    suspend fun getHealthProfile(
        token: String,
        studentId: String,
    ): NetworkResult<ApiResponse<HealthProfileDto>> = safeApiCall {
        client.get(getUrl("api/v1/school/health/profiles/$studentId"))
    }

    suspend fun upsertHealthProfile(
        token: String,
        studentId: String,
        request: UpsertHealthProfileRequest,
    ): NetworkResult<ApiResponse<HealthProfileDto>> = safeApiCall {
        client.post(getUrl("api/v1/school/health/profiles/$studentId")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun getImmunizations(
        token: String,
        studentId: String,
    ): NetworkResult<ApiResponse<ImmunizationListResponse>> = safeApiCall {
        client.get(getUrl("api/v1/school/health/immunizations/$studentId"))
    }

    suspend fun addImmunization(
        token: String,
        request: AddImmunizationRequest,
    ): NetworkResult<ApiResponse<ImmunizationDto>> = safeApiCall {
        client.post(getUrl("api/v1/school/health/immunizations")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun getIncidents(
        token: String,
        studentId: String? = null,
        dateFrom: String? = null,
        dateTo: String? = null,
    ): NetworkResult<ApiResponse<HealthIncidentListResponse>> = safeApiCall {
        client.get(getUrl("api/v1/school/health/incidents")) {
            studentId?.takeIf { it.isNotBlank() }?.let { parameter("student_id", it) }
            dateFrom?.takeIf { it.isNotBlank() }?.let { parameter("date_from", it) }
            dateTo?.takeIf { it.isNotBlank() }?.let { parameter("date_to", it) }
        }
    }

    suspend fun logIncident(
        token: String,
        request: LogIncidentRequest,
    ): NetworkResult<ApiResponse<HealthIncidentDto>> = safeApiCall {
        client.post(getUrl("api/v1/school/health/incidents")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun markIncidentNotified(
        token: String,
        incidentId: String,
    ): NetworkResult<ApiResponse<HealthIncidentDto>> = safeApiCall {
        client.patch(getUrl("api/v1/school/health/incidents/$incidentId/notify"))
    }

    // ── Teacher: health alerts ──────────────────────────────────────

    suspend fun getHealthAlerts(
        token: String,
    ): NetworkResult<ApiResponse<HealthAlertsResponse>> = safeApiCall {
        client.get(getUrl("api/v1/teacher/health/alerts"))
    }

    // ── Parent: child health record ─────────────────────────────────

    suspend fun getChildHealth(
        token: String,
        childId: String,
    ): NetworkResult<ApiResponse<ParentHealthResponse>> = safeApiCall {
        client.get(getUrl("api/v1/parent/health/$childId"))
    }
}

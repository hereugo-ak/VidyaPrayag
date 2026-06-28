package com.littlebridge.enrollplus.feature.scholarship.data.remote

import com.littlebridge.enrollplus.core.model.ApiResponse
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.core.network.safeApiCall
import com.littlebridge.enrollplus.feature.scholarship.domain.model.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*

class ScholarshipApi(
    private val client: HttpClient,
    private val baseUrl: String,
) {
    private fun getUrl(path: String): String {
        val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val cleanPath = if (path.startsWith("/")) path.substring(1) else path
        return "$base$cleanPath"
    }

    // ── Admin: Scheme Management ──────────────────────────────────────

    suspend fun listSchemes(token: String, all: Boolean = false): NetworkResult<ApiResponse<List<ScholarshipScheme>>> = safeApiCall {
        client.get(getUrl("api/v1/school/scholarships")) {
            bearerAuth(token)
            if (all) parameter("all", "true")
        }
    }

    suspend fun createScheme(token: String, request: CreateSchemeRequest): NetworkResult<ApiResponse<ScholarshipScheme>> = safeApiCall {
        client.post(getUrl("api/v1/school/scholarships")) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun updateScheme(token: String, schemeId: String, request: UpdateSchemeRequest): NetworkResult<ApiResponse<Unit>> = safeApiCall {
        client.put(getUrl("api/v1/school/scholarships/$schemeId")) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun deleteScheme(token: String, schemeId: String): NetworkResult<ApiResponse<Unit>> = safeApiCall {
        client.delete(getUrl("api/v1/school/scholarships/$schemeId")) {
            bearerAuth(token)
        }
    }

    // ── Admin: Application Review ─────────────────────────────────────

    suspend fun listApplications(token: String, status: String? = null): NetworkResult<ApiResponse<List<ScholarshipApplication>>> = safeApiCall {
        client.get(getUrl("api/v1/school/scholarship-applications")) {
            bearerAuth(token)
            status?.let { parameter("status", it) }
        }
    }

    suspend fun getApplication(token: String, applicationId: String): NetworkResult<ApiResponse<ScholarshipApplication>> = safeApiCall {
        client.get(getUrl("api/v1/school/scholarship-applications/$applicationId")) {
            bearerAuth(token)
        }
    }

    suspend fun approveApplication(token: String, applicationId: String, request: ApproveApplicationRequest): NetworkResult<ApiResponse<ScholarshipApplication>> = safeApiCall {
        client.post(getUrl("api/v1/school/scholarship-applications/$applicationId/approve")) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun rejectApplication(token: String, applicationId: String, request: RejectApplicationRequest): NetworkResult<ApiResponse<ScholarshipApplication>> = safeApiCall {
        client.post(getUrl("api/v1/school/scholarship-applications/$applicationId/reject")) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun disburse(token: String, applicationId: String, request: DisburseRequest): NetworkResult<ApiResponse<ScholarshipApplication>> = safeApiCall {
        client.post(getUrl("api/v1/school/scholarship-applications/$applicationId/disburse")) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    // ── Admin: Renewals ───────────────────────────────────────────────

    suspend fun listRenewals(token: String, status: String? = null): NetworkResult<ApiResponse<List<ScholarshipRenewal>>> = safeApiCall {
        client.get(getUrl("api/v1/school/scholarship-renewals")) {
            bearerAuth(token)
            status?.let { parameter("status", it) }
        }
    }

    suspend fun approveRenewal(token: String, renewalId: String, request: ApproveRenewalRequest): NetworkResult<ApiResponse<ScholarshipRenewal>> = safeApiCall {
        client.post(getUrl("api/v1/school/scholarship-renewals/$renewalId/approve")) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun rejectRenewal(token: String, renewalId: String, request: RejectApplicationRequest): NetworkResult<ApiResponse<ScholarshipRenewal>> = safeApiCall {
        client.post(getUrl("api/v1/school/scholarship-renewals/$renewalId/reject")) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    // ── Parent ────────────────────────────────────────────────────────

    suspend fun getParentScholarships(token: String): NetworkResult<ApiResponse<ParentScholarshipsData>> = safeApiCall {
        client.get(getUrl("api/v1/parent/scholarships")) {
            bearerAuth(token)
        }
    }

    suspend fun applyScholarship(token: String, request: ApplyScholarshipRequest): NetworkResult<ApiResponse<ScholarshipApplication>> = safeApiCall {
        client.post(getUrl("api/v1/parent/scholarships/apply")) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun getParentApplications(token: String): NetworkResult<ApiResponse<List<ScholarshipApplication>>> = safeApiCall {
        client.get(getUrl("api/v1/parent/scholarships/applications")) {
            bearerAuth(token)
        }
    }

    suspend fun applyRenewal(token: String, request: ApplyRenewalRequest): NetworkResult<ApiResponse<ScholarshipRenewal>> = safeApiCall {
        client.post(getUrl("api/v1/parent/scholarships/${request.originalApplicationId}/renew")) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }
}

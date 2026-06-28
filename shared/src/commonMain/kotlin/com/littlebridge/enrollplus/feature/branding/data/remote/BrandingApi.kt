package com.littlebridge.enrollplus.feature.branding.data.remote

import com.littlebridge.enrollplus.core.model.ApiResponse
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.core.network.safeApiCall
import com.littlebridge.enrollplus.feature.branding.domain.model.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*

class BrandingApi(
    private val client: HttpClient,
    private val baseUrl: String,
) {
    private fun getUrl(path: String): String {
        val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val cleanPath = if (path.startsWith("/")) path.substring(1) else path
        return "$base$cleanPath"
    }

    // ── Admin ────────────────────────────────────────────────────────────

    suspend fun getBranding(token: String): NetworkResult<ApiResponse<SchoolBranding>> = safeApiCall {
        client.get(getUrl("api/v1/school/branding")) {
            bearerAuth(token)
        }
    }

    suspend fun updateBranding(token: String, request: UpdateBrandingRequest): NetworkResult<ApiResponse<SchoolBranding>> = safeApiCall {
        client.patch(getUrl("api/v1/school/branding")) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun resetBranding(token: String): NetworkResult<ApiResponse<SchoolBranding>> = safeApiCall {
        client.post(getUrl("api/v1/school/branding/reset")) {
            bearerAuth(token)
        }
    }

    suspend fun checkSubdomain(token: String, subdomain: String): NetworkResult<ApiResponse<SubdomainCheckResponse>> = safeApiCall {
        client.get(getUrl("api/v1/school/branding/subdomain/check")) {
            bearerAuth(token)
            parameter("subdomain", subdomain)
        }
    }

    suspend fun updateSubdomain(token: String, request: SubdomainRequest): NetworkResult<ApiResponse<SubdomainResponse>> = safeApiCall {
        client.post(getUrl("api/v1/school/branding/subdomain")) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun removeSubdomain(token: String): NetworkResult<ApiResponse<RemoveSubdomainResponse>> = safeApiCall {
        client.delete(getUrl("api/v1/school/branding/subdomain")) {
            bearerAuth(token)
        }
    }

    // ── Public ───────────────────────────────────────────────────────────

    suspend fun getPublicBranding(schoolId: String): NetworkResult<ApiResponse<SchoolBranding>> = safeApiCall {
        client.get(getUrl("api/v1/branding/$schoolId"))
    }

    suspend fun resolveSubdomain(subdomain: String): NetworkResult<ApiResponse<SubdomainResolution>> = safeApiCall {
        client.get(getUrl("api/v1/branding/subdomain/$subdomain"))
    }
}

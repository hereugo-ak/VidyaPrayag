package com.littlebridge.vidyaprayag.feature.admin.data.remote

import com.littlebridge.vidyaprayag.core.model.ApiResponse
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.core.network.safeApiCall
import com.littlebridge.vidyaprayag.feature.admin.domain.model.ClassDetailsResponse
import com.littlebridge.vidyaprayag.feature.admin.domain.model.OnboardingStepResponse
import com.littlebridge.vidyaprayag.feature.admin.domain.model.OnboardingSubmitRequest
import com.littlebridge.vidyaprayag.feature.admin.domain.model.OnboardingSubmitResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType

/**
 * Network client for the school onboarding endpoints.
 *
 * Server routes are defined in:
 *   server/.../feature/onboarding/OnboardingRouting.kt
 *
 *   GET  /api/v1/onboarding/step?obStepType={BASIC|BRANDING|ACADEMIC|REVIEW}
 *   GET  /api/v1/onboarding/academic/class-details?classId={code}
 *   POST /api/v1/onboarding/submit
 *
 * All requests require a Bearer token. We follow the same pattern as
 * [com.littlebridge.vidyaprayag.feature.parent.data.remote.ParentApi] - the
 * caller passes the token in and we attach it as an Authorization header.
 */
class OnboardingApi(
    private val client: HttpClient,
    private val baseUrl: String
) {

    private fun getUrl(path: String): String {
        val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val cleanPath = if (path.startsWith("/")) path.substring(1) else path
        return "$base$cleanPath"
    }

    /**
     * GET /api/v1/onboarding/step?obStepType=...
     */
    suspend fun getStep(
        token: String,
        obStepType: String
    ): NetworkResult<ApiResponse<OnboardingStepResponse>> {
        return safeApiCall {
            client.get(getUrl("api/v1/onboarding/step")) {
                header(HttpHeaders.Authorization, "Bearer $token")
                parameter("obStepType", obStepType)
            }
        }
    }

    /**
     * POST /api/v1/onboarding/submit
     */
    suspend fun submitStep(
        token: String,
        request: OnboardingSubmitRequest
    ): NetworkResult<ApiResponse<OnboardingSubmitResponse>> {
        return safeApiCall {
            client.post(getUrl("api/v1/onboarding/submit")) {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    /**
     * GET /api/v1/onboarding/academic/class-details?classId=...
     */
    suspend fun getClassDetails(
        token: String,
        classId: String
    ): NetworkResult<ApiResponse<ClassDetailsResponse>> {
        return safeApiCall {
            client.get(getUrl("api/v1/onboarding/academic/class-details")) {
                header(HttpHeaders.Authorization, "Bearer $token")
                parameter("classId", classId)
            }
        }
    }
}

package com.littlebridge.enrollplus.feature.idcard.data.remote

import com.littlebridge.enrollplus.core.model.ApiResponse
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.core.network.safeApiCall
import com.littlebridge.enrollplus.feature.idcard.domain.model.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*

class IdCardApi(
    private val client: HttpClient,
    private val baseUrl: String,
) {
    private fun getUrl(path: String): String {
        val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val cleanPath = if (path.startsWith("/")) path.substring(1) else path
        return "$base$cleanPath"
    }

    // ── Admin ────────────────────────────────────────────────────────────

    suspend fun getTemplates(token: String): NetworkResult<ApiResponse<List<IdCardTemplateDto>>> = safeApiCall {
        client.get(getUrl("api/v1/school/id-cards/templates")) {
            bearerAuth(token)
        }
    }

    suspend fun createTemplate(token: String, request: CreateTemplateRequest): NetworkResult<ApiResponse<IdCardTemplateDto>> = safeApiCall {
        client.post(getUrl("api/v1/school/id-cards/templates")) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun deactivateTemplate(token: String, templateId: String): NetworkResult<ApiResponse<Unit>> = safeApiCall {
        client.delete(getUrl("api/v1/school/id-cards/templates/$templateId")) {
            bearerAuth(token)
        }
    }

    suspend fun generateCards(token: String, request: GenerateIdCardRequest): NetworkResult<ApiResponse<GenerateIdCardResponse>> = safeApiCall {
        client.post(getUrl("api/v1/school/id-cards/generate")) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun getCards(token: String): NetworkResult<ApiResponse<List<IdCardDto>>> = safeApiCall {
        client.get(getUrl("api/v1/school/id-cards")) {
            bearerAuth(token)
        }
    }

    suspend fun getPdfUrl(token: String, cardId: String): NetworkResult<ApiResponse<Map<String, String>>> = safeApiCall {
        client.get(getUrl("api/v1/school/id-cards/$cardId/pdf")) {
            bearerAuth(token)
        }
    }

    suspend fun deleteCard(token: String, cardId: String): NetworkResult<ApiResponse<Unit>> = safeApiCall {
        client.delete(getUrl("api/v1/school/id-cards/$cardId")) {
            bearerAuth(token)
        }
    }

    // ── Parent ───────────────────────────────────────────────────────────

    suspend fun getChildIdCard(token: String, childId: String): NetworkResult<ApiResponse<IdCardDto>> = safeApiCall {
        client.get(getUrl("api/v1/parent/id-card/$childId")) {
            bearerAuth(token)
        }
    }

    // ── Teacher ──────────────────────────────────────────────────────────

    suspend fun getTeacherIdCard(token: String): NetworkResult<ApiResponse<IdCardDto>> = safeApiCall {
        client.get(getUrl("api/v1/teacher/id-card")) {
            bearerAuth(token)
        }
    }

    // ── Staff ────────────────────────────────────────────────────────────

    suspend fun getStaffIdCard(token: String): NetworkResult<ApiResponse<IdCardDto>> = safeApiCall {
        client.get(getUrl("api/v1/staff/id-card")) {
            bearerAuth(token)
        }
    }
}

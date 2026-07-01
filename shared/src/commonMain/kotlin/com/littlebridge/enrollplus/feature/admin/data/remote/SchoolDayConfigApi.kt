package com.littlebridge.enrollplus.feature.admin.data.remote

import com.littlebridge.enrollplus.core.model.ApiResponse
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.core.network.safeApiCall
import com.littlebridge.enrollplus.feature.admin.domain.model.CreateSchoolDayConfigRequest
import com.littlebridge.enrollplus.feature.admin.domain.model.SchoolDayConfigDto
import com.littlebridge.enrollplus.feature.admin.domain.model.SchoolDayConfigListResponse
import com.littlebridge.enrollplus.feature.admin.domain.model.UpdateSchoolDayConfigRequest
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class SchoolDayConfigApi(
    private val client: HttpClient,
    private val baseUrl: String,
) {
    private fun getUrl(path: String): String {
        val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val cleanPath = if (path.startsWith("/")) path.substring(1) else path
        return "$base$cleanPath"
    }

    suspend fun list(
        token: String,
    ): NetworkResult<ApiResponse<SchoolDayConfigListResponse>> = safeApiCall {
        client.get(getUrl("api/v1/school/day-config")) {
            bearerAuth(token)
        }
    }

    suspend fun getById(
        token: String,
        id: String,
    ): NetworkResult<ApiResponse<SchoolDayConfigDto>> = safeApiCall {
        client.get(getUrl("api/v1/school/day-config/$id")) {
            bearerAuth(token)
        }
    }

    suspend fun create(
        token: String,
        request: CreateSchoolDayConfigRequest,
    ): NetworkResult<ApiResponse<SchoolDayConfigDto>> = safeApiCall {
        client.post(getUrl("api/v1/school/day-config")) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun update(
        token: String,
        id: String,
        request: UpdateSchoolDayConfigRequest,
    ): NetworkResult<ApiResponse<SchoolDayConfigDto>> = safeApiCall {
        client.put(getUrl("api/v1/school/day-config/$id")) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun deactivate(
        token: String,
        id: String,
    ): NetworkResult<ApiResponse<Unit>> = safeApiCall {
        client.delete(getUrl("api/v1/school/day-config/$id")) {
            bearerAuth(token)
        }
    }

    suspend fun forClass(
        token: String,
        classLevel: String,
        weekday: Int,
    ): NetworkResult<ApiResponse<SchoolDayConfigDto>> = safeApiCall {
        client.get(getUrl("api/v1/school/day-config/for-class?class_level=$classLevel&weekday=$weekday")) {
            bearerAuth(token)
        }
    }
}

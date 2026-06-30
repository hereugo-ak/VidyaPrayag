package com.littlebridge.enrollplus.feature.scheduling.data.remote

import com.littlebridge.enrollplus.core.model.ApiResponse
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.core.network.safeApiCall
import com.littlebridge.enrollplus.feature.scheduling.domain.model.CreateScheduledMessageRequest
import com.littlebridge.enrollplus.feature.scheduling.domain.model.ScheduledMessageDto
import com.littlebridge.enrollplus.feature.scheduling.domain.model.ScheduledMessageListResponse
import com.littlebridge.enrollplus.feature.scheduling.domain.model.UpdateScheduledMessageRequest
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class ScheduledMessageApi(
    private val client: HttpClient,
    private val baseUrl: String
) {

    private fun getUrl(path: String): String {
        val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val cleanPath = if (path.startsWith("/")) path.substring(1) else path
        return "$base$cleanPath"
    }

    private val basePath = "api/v1/school/scheduled-messages"

    suspend fun createScheduledMessage(
        token: String,
        request: CreateScheduledMessageRequest
    ): NetworkResult<ApiResponse<ScheduledMessageDto>> = safeApiCall {
        client.post(getUrl(basePath)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun getScheduledMessages(
        token: String,
        status: String? = null
    ): NetworkResult<ApiResponse<ScheduledMessageListResponse>> = safeApiCall {
        client.get(getUrl(basePath)) {
            status?.let { parameter("status", it) }
        }
    }

    suspend fun getScheduledMessage(
        token: String,
        id: String
    ): NetworkResult<ApiResponse<ScheduledMessageDto>> = safeApiCall {
        client.get(getUrl("$basePath/$id"))
    }

    suspend fun updateScheduledMessage(
        token: String,
        id: String,
        request: UpdateScheduledMessageRequest
    ): NetworkResult<ApiResponse<ScheduledMessageDto>> = safeApiCall {
        client.put(getUrl("$basePath/$id")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun cancelScheduledMessage(
        token: String,
        id: String
    ): NetworkResult<ApiResponse<Unit>> = safeApiCall {
        client.delete(getUrl("$basePath/$id"))
    }

    suspend fun dispatchNow(
        token: String,
        id: String
    ): NetworkResult<ApiResponse<Unit>> = safeApiCall {
        client.post(getUrl("$basePath/$id/dispatch-now")) {
            contentType(ContentType.Application.Json)
            setBody(emptyMap<String, String>())
        }
    }
}

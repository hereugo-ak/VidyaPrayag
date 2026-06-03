/*
 * File: MessagesApi.kt
 * Module: feature.admin.data.remote
 *
 * Network client for the school-messages endpoints. Mirrors [OnboardingApi]
 * and [AdmissionApi] - caller passes the JWT in, we attach Authorization.
 *
 * Server routes:
 *   GET  /api/v1/school/messages/threads
 *   GET  /api/v1/school/messages/threads/{id}/messages
 *   POST /api/v1/school/messages/threads/{id}/read
 *   POST /api/v1/school/messages
 */
package com.littlebridge.vidyaprayag.feature.admin.data.remote

import com.littlebridge.vidyaprayag.core.model.ApiResponse
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.core.network.safeApiCall
import com.littlebridge.vidyaprayag.feature.admin.domain.model.MessageThreadsResponse
import com.littlebridge.vidyaprayag.feature.admin.domain.model.SendMessageRequest
import com.littlebridge.vidyaprayag.feature.admin.domain.model.SendMessageResponse
import com.littlebridge.vidyaprayag.feature.admin.domain.model.ThreadMessagesResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType

class MessagesApi(
    private val client: HttpClient,
    private val baseUrl: String
) {

    private fun getUrl(path: String): String {
        val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val cleanPath = if (path.startsWith("/")) path.substring(1) else path
        return "$base$cleanPath"
    }

    suspend fun getThreads(
        token: String
    ): NetworkResult<ApiResponse<MessageThreadsResponse>> = safeApiCall {
        client.get(getUrl("api/v1/school/messages/threads")) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
    }

    /**
     * GET /api/v1/school/messages/threads/{id}/messages
     *
     * Returns the full conversation for a thread (ascending by created_at).
     * The server also clears the thread's unread badge as a side effect.
     */
    suspend fun getThreadMessages(
        token: String,
        threadId: String
    ): NetworkResult<ApiResponse<ThreadMessagesResponse>> = safeApiCall {
        client.get(getUrl("api/v1/school/messages/threads/$threadId/messages")) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
    }

    /**
     * Server replies with `{ success, message }` (no data payload), so the
     * envelope is `ApiResponse<Unit>`.
     */
    suspend fun markThreadRead(
        token: String,
        threadId: String
    ): NetworkResult<ApiResponse<Unit>> = safeApiCall {
        client.post(getUrl("api/v1/school/messages/threads/$threadId/read")) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
    }

    suspend fun sendMessage(
        token: String,
        request: SendMessageRequest
    ): NetworkResult<ApiResponse<SendMessageResponse>> = safeApiCall {
        client.post(getUrl("api/v1/school/messages")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }
}

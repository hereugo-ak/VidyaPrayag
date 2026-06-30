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
package com.littlebridge.enrollplus.feature.admin.data.remote

import com.littlebridge.enrollplus.core.model.ApiResponse
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.core.network.safeApiCall
import com.littlebridge.enrollplus.feature.admin.domain.model.MessageThreadsResponse
import com.littlebridge.enrollplus.feature.admin.domain.model.SendMessageRequest
import com.littlebridge.enrollplus.feature.admin.domain.model.SendMessageResponse
import com.littlebridge.enrollplus.feature.admin.domain.model.SchoolRecipientsResponse
import com.littlebridge.enrollplus.feature.admin.domain.model.ThreadMessagesResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.parameters

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
        client.get(getUrl("api/v1/school/messages/threads"))
    }

    /**
     * GET /api/v1/school/messages/threads/{id}/messages
     *
     * Returns a paginated conversation for a thread (ascending by seq/created_at).
     * The server also clears the thread's unread badge as a side effect.
     *
     * Phase 1: offset/limit pagination support.
     */
    suspend fun getThreadMessages(
        token: String,
        threadId: String,
        offset: Int = 0,
        limit: Int = 50,
    ): NetworkResult<ApiResponse<ThreadMessagesResponse>> = safeApiCall {
        client.get(getUrl("api/v1/school/messages/threads/$threadId/messages")) {
            url {
                if (offset > 0) parameters.append("offset", offset.toString())
                if (limit != 50) parameters.append("limit", limit.toString())
            }
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
        client.post(getUrl("api/v1/school/messages/threads/$threadId/read"))
    }

    suspend fun sendMessage(
        token: String,
        request: SendMessageRequest
    ): NetworkResult<ApiResponse<SendMessageResponse>> = safeApiCall {
        client.post(getUrl("api/v1/school/messages")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun getRecipients(
        token: String
    ): NetworkResult<ApiResponse<SchoolRecipientsResponse>> = safeApiCall {
        client.get(getUrl("api/v1/school/messages/recipients"))
    }

    /** Phase 1 (§9.4): PATCH /api/v1/school/messages/messages/{id} — edit a message's body. */
    suspend fun editMessage(
        token: String,
        messageId: String,
        body: String,
    ): NetworkResult<ApiResponse<Map<String, String>>> = safeApiCall {
        client.patch(getUrl("api/v1/school/messages/messages/$messageId")) {
            contentType(ContentType.Application.Json)
            setBody(mapOf("body" to body))
        }
    }

    /** Phase 1 (§9.4): DELETE /api/v1/school/messages/messages/{id} — soft-delete a message. */
    suspend fun deleteMessage(
        token: String,
        messageId: String,
        scope: String = "everyone",
    ): NetworkResult<ApiResponse<Map<String, String>>> = safeApiCall {
        client.delete(getUrl("api/v1/school/messages/messages/$messageId?scope=$scope"))
    }
}

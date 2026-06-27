/*
 * File: MessagesRepository.kt
 * Module: feature.admin.domain.repository
 *
 * Domain abstraction over the school-messages endpoints. Mirrors the design
 * of [OnboardingRepository] / [AdmissionRepository] - the implementation
 * unwraps the ApiResponse envelope so callers (ViewModels) only deal with the
 * inner data types.
 */
package com.littlebridge.enrollplus.feature.admin.domain.repository

import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.feature.admin.domain.model.MessageThread
import com.littlebridge.enrollplus.feature.admin.domain.model.SendMessageRequest
import com.littlebridge.enrollplus.feature.admin.domain.model.SendMessageResponse
import com.littlebridge.enrollplus.feature.admin.domain.model.SchoolRecipient
import com.littlebridge.enrollplus.feature.admin.domain.model.ThreadMessagesResponse

interface MessagesRepository {

    /**
     * GET /api/v1/school/messages/threads
     *
     * Returns the threads owned by the calling user, newest first (server
     * orders by lastMessageAt DESC).
     */
    suspend fun getThreads(token: String): NetworkResult<List<MessageThread>>

    /**
     * GET /api/v1/school/messages/threads/{id}/messages
     *
     * Returns the full conversation for a thread. The server clears the
     * thread's unread badge as a side effect of opening it.
     */
    suspend fun getThreadMessages(
        token: String,
        threadId: String
    ): NetworkResult<ThreadMessagesResponse>

    /**
     * POST /api/v1/school/messages/threads/{id}/read
     *
     * Resets the thread's unreadCount to 0 and flips isRead = true.
     * Server replies with just `{ success, message }`, hence [Unit].
     */
    suspend fun markThreadRead(token: String, threadId: String): NetworkResult<Unit>

    /**
     * POST /api/v1/school/messages
     *
     * Sends a message. Pass [SendMessageRequest.threadId] = null to create a
     * fresh thread on the fly.
     */
    suspend fun sendMessage(
        token: String,
        request: SendMessageRequest
    ): NetworkResult<SendMessageResponse>

    /**
     * GET /api/v1/school/messages/recipients
     *
     * Returns all teachers, admins, and parents (with enrolled children)
     * that the school admin can start a conversation with.
     */
    suspend fun getRecipients(token: String): NetworkResult<List<SchoolRecipient>>

    /** Phase 1 (§9.4): PATCH /api/v1/school/messages/messages/{id} — edit a message. */
    suspend fun editMessage(token: String, messageId: String, body: String): NetworkResult<Map<String, String>>

    /** Phase 1 (§9.4): DELETE /api/v1/school/messages/messages/{id} — soft-delete a message. */
    suspend fun deleteMessage(token: String, messageId: String, scope: String = "everyone"): NetworkResult<Map<String, String>>
}

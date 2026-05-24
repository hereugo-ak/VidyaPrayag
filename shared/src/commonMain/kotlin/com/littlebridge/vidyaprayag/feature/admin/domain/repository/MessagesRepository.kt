/*
 * File: MessagesRepository.kt
 * Module: feature.admin.domain.repository
 *
 * Domain abstraction over the school-messages endpoints. Mirrors the design
 * of [OnboardingRepository] / [AdmissionRepository] - the implementation
 * unwraps the ApiResponse envelope so callers (ViewModels) only deal with the
 * inner data types.
 */
package com.littlebridge.vidyaprayag.feature.admin.domain.repository

import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.feature.admin.domain.model.MessageThread
import com.littlebridge.vidyaprayag.feature.admin.domain.model.SendMessageRequest
import com.littlebridge.vidyaprayag.feature.admin.domain.model.SendMessageResponse

interface MessagesRepository {

    /**
     * GET /api/v1/school/messages/threads
     *
     * Returns the threads owned by the calling user, newest first (server
     * orders by lastMessageAt DESC).
     */
    suspend fun getThreads(token: String): NetworkResult<List<MessageThread>>

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
}

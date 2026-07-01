/*
 * File: MessagesRepositoryImpl.kt
 * Module: feature.admin.data.repository
 *
 * Default [MessagesRepository] implementation. Standard ApiResponse envelope
 * unwrapping, identical pattern to OnboardingRepositoryImpl /
 * AdmissionRepositoryImpl.
 */
package com.littlebridge.enrollplus.feature.admin.data.repository

import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.feature.admin.data.remote.MessagesApi
import com.littlebridge.enrollplus.feature.admin.domain.model.MessageThread
import com.littlebridge.enrollplus.feature.admin.domain.model.SendMessageRequest
import com.littlebridge.enrollplus.feature.admin.domain.model.SendMessageResponse
import com.littlebridge.enrollplus.feature.admin.domain.model.SchoolRecipient
import com.littlebridge.enrollplus.feature.admin.domain.model.ThreadMessagesResponse
import com.littlebridge.enrollplus.feature.admin.domain.model.UnreadCountDto
import com.littlebridge.enrollplus.feature.admin.domain.repository.MessagesRepository

class MessagesRepositoryImpl(
    private val api: MessagesApi
) : MessagesRepository {

    override suspend fun getThreads(token: String): NetworkResult<List<MessageThread>> {
        return when (val result = api.getThreads(token)) {
            is NetworkResult.Success -> {
                val envelope = result.data
                val data = envelope.data
                when {
                    !envelope.success -> NetworkResult.Error(
                        envelope.message.ifBlank { "Failed to fetch message threads" }
                    )
                    data == null -> NetworkResult.Error("No data in response")
                    else -> NetworkResult.Success(data.threads)
                }
            }
            is NetworkResult.Error -> NetworkResult.Error(result.message, result.code)
            is NetworkResult.ConnectionError -> NetworkResult.ConnectionError
        }
    }

    override suspend fun getThreadMessages(
        token: String,
        threadId: String
    ): NetworkResult<ThreadMessagesResponse> {
        return when (val result = api.getThreadMessages(token, threadId)) {
            is NetworkResult.Success -> {
                val envelope = result.data
                val data = envelope.data
                when {
                    !envelope.success -> NetworkResult.Error(
                        envelope.message.ifBlank { "Failed to fetch conversation" }
                    )
                    data == null -> NetworkResult.Error("No data in response")
                    else -> NetworkResult.Success(data)
                }
            }
            is NetworkResult.Error -> NetworkResult.Error(result.message, result.code)
            is NetworkResult.ConnectionError -> NetworkResult.ConnectionError
        }
    }

    override suspend fun markThreadRead(
        token: String,
        threadId: String
    ): NetworkResult<Unit> {
        return when (val result = api.markThreadRead(token, threadId)) {
            is NetworkResult.Success -> {
                val envelope = result.data
                if (!envelope.success) {
                    NetworkResult.Error(
                        envelope.message.ifBlank { "Failed to mark thread as read" }
                    )
                } else {
                    NetworkResult.Success(Unit)
                }
            }
            is NetworkResult.Error -> NetworkResult.Error(result.message, result.code)
            is NetworkResult.ConnectionError -> NetworkResult.ConnectionError
        }
    }

    override suspend fun getUnreadCount(token: String): NetworkResult<Int> {
        return when (val result = api.getUnreadCount(token)) {
            is NetworkResult.Success -> {
                val envelope = result.data
                if (!envelope.success) {
                    NetworkResult.Error(envelope.message.ifBlank { "Failed to fetch unread count" })
                } else {
                    NetworkResult.Success(envelope.data?.unreadCount ?: 0)
                }
            }
            is NetworkResult.Error -> NetworkResult.Error(result.message, result.code)
            is NetworkResult.ConnectionError -> NetworkResult.ConnectionError
        }
    }

    override suspend fun sendMessage(
        token: String,
        request: SendMessageRequest
    ): NetworkResult<SendMessageResponse> {
        return when (val result = api.sendMessage(token, request)) {
            is NetworkResult.Success -> {
                val envelope = result.data
                val data = envelope.data
                when {
                    !envelope.success -> NetworkResult.Error(
                        envelope.message.ifBlank { "Failed to send message" }
                    )
                    data == null -> NetworkResult.Error("No data in response")
                    else -> NetworkResult.Success(data)
                }
            }
            is NetworkResult.Error -> NetworkResult.Error(result.message, result.code)
            is NetworkResult.ConnectionError -> NetworkResult.ConnectionError
        }
    }

    override suspend fun getRecipients(token: String): NetworkResult<List<SchoolRecipient>> {
        return when (val result = api.getRecipients(token)) {
            is NetworkResult.Success -> {
                val envelope = result.data
                val data = envelope.data
                when {
                    !envelope.success -> NetworkResult.Error(
                        envelope.message.ifBlank { "Failed to fetch recipients" }
                    )
                    data == null -> NetworkResult.Error("No data in response")
                    else -> NetworkResult.Success(data.recipients)
                }
            }
            is NetworkResult.Error -> NetworkResult.Error(result.message, result.code)
            is NetworkResult.ConnectionError -> NetworkResult.ConnectionError
        }
    }

    override suspend fun editMessage(token: String, messageId: String, body: String): NetworkResult<Map<String, String>> {
        return when (val result = api.editMessage(token, messageId, body)) {
            is NetworkResult.Success -> {
                val envelope = result.data
                if (!envelope.success) NetworkResult.Error(envelope.message.ifBlank { "Failed to edit message" })
                else NetworkResult.Success(envelope.data ?: emptyMap())
            }
            is NetworkResult.Error -> NetworkResult.Error(result.message, result.code)
            is NetworkResult.ConnectionError -> NetworkResult.ConnectionError
        }
    }

    override suspend fun deleteMessage(token: String, messageId: String, scope: String): NetworkResult<Map<String, String>> {
        return when (val result = api.deleteMessage(token, messageId, scope)) {
            is NetworkResult.Success -> {
                val envelope = result.data
                if (!envelope.success) NetworkResult.Error(envelope.message.ifBlank { "Failed to delete message" })
                else NetworkResult.Success(envelope.data ?: emptyMap())
            }
            is NetworkResult.Error -> NetworkResult.Error(result.message, result.code)
            is NetworkResult.ConnectionError -> NetworkResult.ConnectionError
        }
    }
}

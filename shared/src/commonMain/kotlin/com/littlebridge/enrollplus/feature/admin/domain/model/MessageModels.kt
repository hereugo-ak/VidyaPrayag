/*
 * File: MessageModels.kt
 * Module: feature.admin.domain.model
 *
 * Domain models that mirror the server DTOs from
 *   server/.../feature/school/MessagesRouting.kt
 *
 * Endpoints:
 *   GET  /api/v1/school/messages/threads
 *   POST /api/v1/school/messages/threads/{id}/read
 *   POST /api/v1/school/messages
 *
 * Notes:
 * - `time` is server-formatted ("10:45 AM" / "Yesterday" / "MMM dd"). The UI
 *   does no date math.
 * - When `threadId` is null on [SendMessageRequest], the server creates a
 *   fresh thread on the fly with the calling user as the recipient.
 */
package com.littlebridge.enrollplus.feature.admin.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One thread in the admin's inbox. Field names intentionally mirror the
 * existing `MessageThread` data class the screen was built against, so the
 * UI layer changes minimally when we swap from mock to network-backed state.
 */
@Serializable
data class MessageThread(
    val id: String,
    @SerialName("sender_name") val senderName: String,
    @SerialName("sender_role") val senderRole: String,
    @SerialName("last_message") val lastMessage: String,
    val time: String,
    @SerialName("unread_count") val unreadCount: Int = 0,
    @SerialName("sender_image_url") val senderImageUrl: String? = null,
    @SerialName("icon_name") val iconName: String? = null,
    @SerialName("is_read") val isRead: Boolean = true
)

/** GET /api/v1/school/messages/threads response payload. */
@Serializable
data class MessageThreadsResponse(
    val threads: List<MessageThread>
)

/**
 * POST /api/v1/school/messages request body.
 *
 * `threadId` null = create a new thread; non-null = append to existing thread.
 * `body` is required (server rejects blank).
 *
 * Phase 1: `clientMsgId` for idempotency (prevents duplicates on network retry),
 * `replyToId` for reply-quoting, `attachments` for media in messages.
 */
@Serializable
data class SendMessageRequest(
    @SerialName("thread_id") val threadId: String? = null,
    @SerialName("sender_name") val senderName: String? = null,
    @SerialName("sender_role") val senderRole: String? = null,
    @SerialName("sender_image_url") val senderImageUrl: String? = null,
    @SerialName("icon_name") val iconName: String? = null,
    @SerialName("recipient_user_id") val recipientUserId: String? = null,
    @SerialName("client_msg_id") val clientMsgId: String? = null,
    @SerialName("reply_to_id") val replyToId: String? = null,
    val attachments: List<AttachmentInput> = emptyList(),
    val body: String
)

/** POST /api/v1/school/messages response payload. */
@Serializable
data class SendMessageResponse(
    @SerialName("thread_id") val threadId: String,
    @SerialName("message_id") val messageId: String,
    val seq: Int? = null,
    @SerialName("server_timestamp") val serverTimestamp: String? = null,
)

/**
 * A single message inside a thread.
 * Mirrors the server `MessageDto` in MessagesRouting.kt.
 * `isMine` lets the UI right-align the admin's own messages.
 * `time` is server-formatted ("10:45 AM" / "Yesterday" / "MMM dd").
 *
 * Phase 1: `seq` for ordering, `status` for delivery ticks (SENT/DELIVERED/READ),
 * `editedAt`/`deletedAt` for edit/delete display, `replyToId` for reply-quoting,
 * `attachments` for media rendering.
 */
@Serializable
data class Message(
    val id: String,
    val body: String,
    @SerialName("is_mine") val isMine: Boolean = false,
    @SerialName("sender_id") val senderId: String? = null,
    @SerialName("created_at") val createdAt: String,
    val time: String,
    val seq: Int? = null,
    val status: String? = null,
    @SerialName("edited_at") val editedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null,
    @SerialName("reply_to_id") val replyToId: String? = null,
    val attachments: List<MessageAttachment> = emptyList(),
)

/**
 * GET /api/v1/school/messages/threads/{id}/messages response payload.
 * Mirrors the server `ThreadMessagesResponse`.
 *
 * Phase 1: `hasMore`/`totalCount` for pagination support.
 */
@Serializable
data class ThreadMessagesResponse(
    @SerialName("thread_id") val threadId: String,
    @SerialName("sender_name") val senderName: String,
    val messages: List<Message>,
    @SerialName("has_more") val hasMore: Boolean = false,
    @SerialName("total_count") val totalCount: Long = 0,
)

@Serializable
data class SchoolRecipient(
    val id: String,
    val name: String,
    val role: String,
    val subtitle: String,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("child_name") val childName: String? = null,
)

@Serializable
data class SchoolRecipientsResponse(
    val recipients: List<SchoolRecipient>
)

/** Phase 1 (§12): attachment metadata for sending in a message. */
@Serializable
data class AttachmentInput(
    @SerialName("file_name") val fileName: String,
    @SerialName("mime_type") val mimeType: String,
    @SerialName("size_bytes") val sizeBytes: Long,
    @SerialName("storage_url") val storageUrl: String,
    @SerialName("attachment_type") val attachmentType: String = "IMAGE",
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    @SerialName("duration_ms") val durationMs: Int? = null,
)

/** Phase 1 (§12): attachment metadata returned by the server in message responses. */
@Serializable
data class MessageAttachment(
    val id: String,
    @SerialName("file_name") val fileName: String,
    @SerialName("mime_type") val mimeType: String,
    @SerialName("size_bytes") val sizeBytes: Long,
    @SerialName("storage_url") val storageUrl: String,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerialName("attachment_type") val attachmentType: String,
    val width: Int? = null,
    val height: Int? = null,
    @SerialName("duration_ms") val durationMs: Int? = null,
)

/** Read Receipts Phase 2: GET /api/v1/school/messages/unread-count response payload. */
@Serializable
data class UnreadCountDto(
    @SerialName("unread_count") val unreadCount: Int = 0,
)

/*
 * File: MessagesRouting.kt
 * Module: feature.school
 *
 * Endpoints (all JWT):
 *   GET  /api/v1/school/messages/threads
 *   GET  /api/v1/school/messages/threads/{id}/messages
 *   POST /api/v1/school/messages/threads/{id}/read
 *   POST /api/v1/school/messages
 *
 * Spec ref: school_api_spec.artifact.md §Module: Messages
 *
 * Threads are scoped to the authenticated user via `owner_user_id`, so the
 * admin only ever sees their own inbox.  `time` is formatted server-side
 * ("10:45 AM" for today, "Yesterday" / "MMM dd" otherwise) so the UI does
 * no date math.
 *
 * `POST /api/v1/school/messages` is the "send" path.  If `thread_id` is
 * null we create a fresh thread on the fly (recipient defaults to the
 * authenticated user — useful for system-generated alerts) and return both
 * the new thread id and message id.  When `thread_id` is supplied we append
 * a message and refresh the thread's preview / timestamp.
 */
package com.littlebridge.enrollplus.feature.school

import com.littlebridge.enrollplus.core.created
import com.littlebridge.enrollplus.core.fail
import com.littlebridge.enrollplus.core.ok
import com.littlebridge.enrollplus.core.okMessage
import com.littlebridge.enrollplus.core.requireSchoolContext
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.AppUsersTable
import com.littlebridge.enrollplus.db.ChildrenTable
import com.littlebridge.enrollplus.db.MessageAttachmentsTable
import com.littlebridge.enrollplus.db.MessageThreadsTable
import com.littlebridge.enrollplus.db.MessagesTable
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

// ---------------- DTOs ----------------

@Serializable
data class MessageThreadDto(
    val id: String,
    @SerialName("sender_name") val senderName: String,
    @SerialName("sender_role") val senderRole: String,
    @SerialName("last_message") val lastMessage: String,
    val time: String,
    @SerialName("unread_count") val unreadCount: Int,
    @SerialName("sender_image_url") val senderImageUrl: String? = null,
    @SerialName("icon_name") val iconName: String? = null,
    @SerialName("is_read") val isRead: Boolean
)

@Serializable
data class MessageThreadsResponse(val threads: List<MessageThreadDto>)

// Phase 1 (§9.1): enhanced send DTO with idempotency, reply, attachments.
@Serializable
data class AttachmentInputDto(
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

@Serializable
data class SendMessageDto(
    @SerialName("thread_id") val threadId: String? = null,
    @SerialName("sender_name") val senderName: String? = null,
    @SerialName("sender_role") val senderRole: String? = null,
    @SerialName("sender_image_url") val senderImageUrl: String? = null,
    @SerialName("icon_name") val iconName: String? = null,
    @SerialName("recipient_user_id") val recipientUserId: String? = null,
    @SerialName("client_msg_id") val clientMsgId: String? = null,
    @SerialName("reply_to_id") val replyToId: String? = null,
    val attachments: List<AttachmentInputDto> = emptyList(),
    val body: String
)

// Phase 1 (§9.1): enhanced response with seq + server_timestamp.
@Serializable
data class SendMessageResponse(
    @SerialName("thread_id") val threadId: String,
    @SerialName("message_id") val messageId: String,
    val seq: Int? = null,
    @SerialName("server_timestamp") val serverTimestamp: String? = null,
)

// Phase 1 (§9.2): attachment DTO in message responses.
@Serializable
data class AttachmentDto(
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

@Serializable
data class MessageDto(
    val id: String,
    val body: String,
    @SerialName("is_mine") val isMine: Boolean,
    @SerialName("sender_id") val senderId: String? = null,
    @SerialName("created_at") val createdAt: String,
    val time: String,
    // Phase 1: seq, status, edit/delete, attachments.
    val seq: Int? = null,
    val status: String? = null,
    @SerialName("edited_at") val editedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null,
    @SerialName("reply_to_id") val replyToId: String? = null,
    val attachments: List<AttachmentDto> = emptyList(),
)

@Serializable
data class ThreadMessagesResponse(
    @SerialName("thread_id") val threadId: String,
    @SerialName("sender_name") val senderName: String,
    val messages: List<MessageDto>,
    // Phase 1 (§9.2): pagination metadata.
    @SerialName("has_more") val hasMore: Boolean = false,
    @SerialName("total_count") val totalCount: Long = 0,
)

// Phase 1 (§9.4): edit message DTO.
@Serializable
data class EditMessageDto(
    val body: String,
)

@Serializable
data class SchoolRecipientDto(
    val id: String,
    val name: String,
    val role: String,
    val subtitle: String,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("child_name") val childName: String? = null,
)

@Serializable
data class SchoolRecipientsResponse(val recipients: List<SchoolRecipientDto>)

// ---------------- helpers ----------------

private fun formatThreadTime(ts: Instant): String {
    val zid = ZoneId.systemDefault()
    val zdt = ts.atZone(zid)
    val today = LocalDate.now(zid)
    return when (zdt.toLocalDate()) {
        today -> zdt.format(DateTimeFormatter.ofPattern("h:mm a"))
        today.minusDays(1) -> "Yesterday"
        else -> zdt.format(DateTimeFormatter.ofPattern("MMM dd"))
    }
}

private fun org.jetbrains.exposed.sql.ResultRow.toThreadDto() = MessageThreadDto(
    id = this[MessageThreadsTable.id].value.toString(),
    senderName = this[MessageThreadsTable.senderName],
    senderRole = this[MessageThreadsTable.senderRole],
    lastMessage = this[MessageThreadsTable.lastMessage],
    time = formatThreadTime(this[MessageThreadsTable.lastMessageAt]),
    unreadCount = this[MessageThreadsTable.unreadCount],
    senderImageUrl = this[MessageThreadsTable.senderImageUrl],
    iconName = this[MessageThreadsTable.iconName],
    isRead = this[MessageThreadsTable.isRead]
)

// Phase 1 (§9.2): convert an attachment row to DTO.
private fun org.jetbrains.exposed.sql.ResultRow.toAttachmentDto() = AttachmentDto(
    id = this[MessageAttachmentsTable.id].value.toString(),
    fileName = this[MessageAttachmentsTable.fileName],
    mimeType = this[MessageAttachmentsTable.mimeType],
    sizeBytes = this[MessageAttachmentsTable.sizeBytes],
    storageUrl = this[MessageAttachmentsTable.storageUrl],
    thumbnailUrl = this[MessageAttachmentsTable.thumbnailUrl],
    attachmentType = this[MessageAttachmentsTable.attachmentType],
    width = this[MessageAttachmentsTable.width],
    height = this[MessageAttachmentsTable.height],
    durationMs = this[MessageAttachmentsTable.durationMs],
)

fun Route.messagesRouting() {
    authenticate("jwt") {
        route("/api/v1/school/messages") {

            // -------- RECIPIENTS (compose-new picker: teachers + parents) --------
            // Who can the school admin START a conversation with? Every active
            // teacher in the school PLUS every parent who has a child enrolled.
            get("/recipients") {
                val ctx = call.requireSchoolContext() ?: return@get
                val schoolId = ctx.schoolId

                val payload = dbQuery {
                    // Active teachers + admins in the school (addressable desk staff).
                    val roleFilter = listOf("teacher", "admin", "school_admin")
                        .map { r -> AppUsersTable.role eq r }
                        .reduce { acc, op -> acc or op }
                    val staff = AppUsersTable.selectAll()
                        .where {
                            (AppUsersTable.schoolId eq schoolId) and
                                (AppUsersTable.isActive eq true) and
                                roleFilter
                        }
                        .map { row ->
                            SchoolRecipientDto(
                                id = row[AppUsersTable.id].value.toString(),
                                name = row[AppUsersTable.fullName],
                                role = row[AppUsersTable.role],
                                subtitle = when (row[AppUsersTable.role]) {
                                    "teacher" -> "Teacher"
                                    else -> "School office"
                                },
                                imageUrl = row[AppUsersTable.profilePicUrl],
                                childName = null,
                            )
                        }

                    // Parents who have at least one active child in this school.
                    val parentIds = ChildrenTable.selectAll()
                        .where {
                            (ChildrenTable.schoolId eq schoolId) and
                                (ChildrenTable.isActive eq true)
                        }
                        .map { it[ChildrenTable.parentId] }
                        .distinct()

                    val parents = if (parentIds.isEmpty()) emptyList() else {
                        val parentFilter = parentIds.map { AppUsersTable.id eq it }
                            .reduce { acc, op -> acc or op }
                        AppUsersTable.selectAll()
                            .where {
                                (AppUsersTable.isActive eq true) and parentFilter
                            }
                            .map { row ->
                                val pid = row[AppUsersTable.id].value
                                val childNames = ChildrenTable.selectAll()
                                    .where {
                                        (ChildrenTable.parentId eq pid) and
                                            (ChildrenTable.schoolId eq schoolId) and
                                            (ChildrenTable.isActive eq true)
                                    }
                                    .map { it[ChildrenTable.childName] }
                                SchoolRecipientDto(
                                    id = pid.toString(),
                                    name = row[AppUsersTable.fullName],
                                    role = "parent",
                                    subtitle = if (childNames.size == 1) "Parent of ${childNames[0]}" else "Parent",
                                    imageUrl = row[AppUsersTable.profilePicUrl],
                                    childName = childNames.joinToString(", "),
                                )
                            }
                    }

                    // Staff first (teachers, then admins), then parents; alphabetical within each band.
                    val all = (staff + parents).sortedWith(
                        compareBy<SchoolRecipientDto> { it.role != "teacher" }
                            .thenBy { it.role != "admin" && it.role != "school_admin" }
                            .thenBy { it.role != "parent" }
                            .thenBy { it.name.lowercase() }
                    )
                    SchoolRecipientsResponse(all)
                }
                call.ok(payload, message = "Recipients fetched")
            }

            // -------- LIST THREADS --------
            get("/threads") {
                val ctx = call.requireSchoolContext() ?: return@get
                val uid = ctx.userId

                val payload = dbQuery {
                    val rows = MessageThreadsTable.selectAll()
                        .where {
                            (MessageThreadsTable.ownerUserId eq uid) and
                                (MessageThreadsTable.schoolId eq ctx.schoolId)
                        }
                        .orderBy(MessageThreadsTable.lastMessageAt, SortOrder.DESC)
                        .map { it.toThreadDto() }
                    MessageThreadsResponse(rows)
                }
                call.ok(payload, message = "Threads fetched successfully")
            }

            // -------- CONVERSATION (messages inside a thread) --------
            get("/threads/{id}/messages") {
                val ctx = call.requireSchoolContext() ?: return@get
                val id = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid id"); return@get }

                // Phase 1 (§9.2): pagination via offset/limit query params.
                val offset = call.parameters["offset"]?.toIntOrNull() ?: 0
                val limit = call.parameters["limit"]?.toIntOrNull() ?: 50

                val payload = dbQuery {
                    // The thread must belong to the caller (owner) AND school.
                    val thread = MessageThreadsTable.selectAll()
                        .where {
                            (MessageThreadsTable.id eq id) and
                                (MessageThreadsTable.ownerUserId eq ctx.userId) and
                                (MessageThreadsTable.schoolId eq ctx.schoolId)
                        }
                        .singleOrNull()
                        ?: return@dbQuery null

                    // Phase 1: paginated fetch with seq ordering + pagination metadata.
                    val paged = conversationMessagesFor(id, ctx.userId, offset = offset, limit = limit)
                        ?: return@dbQuery null

                    // Phase 1 (§9.2): load attachments for the page of messages.
                    val msgIds = paged.messages.map { it[MessagesTable.id].value }
                    val attachmentsMap = loadAttachmentsForMessages(msgIds)

                    val msgs = paged.messages.map { row ->
                        val sid = row[MessagesTable.senderId]
                        val createdInstant = row[MessagesTable.createdAt]
                        val msgId = row[MessagesTable.id].value
                        val status = if (sid != ctx.userId && paged.conversationId != null) {
                            loadMessageStatus(msgId, ctx.userId)
                        } else null
                        MessageDto(
                            id = msgId.toString(),
                            body = row[MessagesTable.body] ?: "",
                            isMine = sid == ctx.userId,
                            senderId = sid?.toString(),
                            createdAt = createdInstant.toString(),
                            time = formatThreadTime(createdInstant),
                            seq = row[MessagesTable.seq],
                            status = status,
                            editedAt = row[MessagesTable.editedAt]?.toString(),
                            deletedAt = row[MessagesTable.deletedAt]?.toString(),
                            replyToId = row[MessagesTable.replyToId]?.toString(),
                            attachments = (attachmentsMap[msgId] ?: emptyList()).map { it.toAttachmentDto() },
                        )
                    }
                    ThreadMessagesResponse(
                        threadId = id.toString(),
                        senderName = thread[MessageThreadsTable.senderName],
                        messages = msgs,
                        hasMore = paged.hasMore,
                        totalCount = paged.totalCount,
                    )
                }
                if (payload == null) call.fail("Thread not found", HttpStatusCode.NotFound)
                else {
                    // Opening a conversation clears its unread badge.
                    dbQuery {
                        MessageThreadsTable.update({
                            (MessageThreadsTable.id eq id) and (MessageThreadsTable.ownerUserId eq ctx.userId)
                        }) {
                            it[unreadCount] = 0
                            it[isRead] = true
                            it[updatedAt] = Instant.now()
                        }
                    }
                    call.ok(payload, message = "Conversation fetched")
                }
            }

            // -------- UNREAD COUNT --------
            get("/unread-count") {
                val ctx = call.requireSchoolContext() ?: return@get
                val count = dbQuery { getUnreadCount(ctx.userId) }
                call.ok(UnreadCountDto(count))
            }

            // -------- MARK READ --------
            post("/threads/{id}/read") {
                val ctx = call.requireSchoolContext() ?: return@post
                val uid = ctx.userId
                val id = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid id"); return@post }

                val n = dbQuery {
                    MessageThreadsTable.update({
                        (MessageThreadsTable.id eq id) and (MessageThreadsTable.ownerUserId eq uid)
                    }) {
                        it[unreadCount] = 0
                        it[isRead] = true
                        it[updatedAt] = Instant.now()
                    }
                    // Read Receipts Phase 1: also bulk-update per-message status rows to READ
                    val convId = MessageThreadsTable.selectAll()
                        .where { (MessageThreadsTable.id eq id) and (MessageThreadsTable.ownerUserId eq uid) }
                        .singleOrNull()?.get(MessageThreadsTable.conversationId) ?: id
                    markConversationRead(uid, convId)
                }
                if (n == 0) call.fail("Thread not found", HttpStatusCode.NotFound)
                else call.okMessage("Thread marked as read")
            }

            // -------- SEND (creates thread on the fly when thread_id is null) --------
            post {
                val ctx = call.requireSchoolContext() ?: return@post
                val uid = ctx.userId
                val req = call.receive<SendMessageDto>()
                if (req.body.isBlank()) {
                    call.fail("body is required"); return@post
                }
                if (req.body.length > 4096) {
                    call.fail("Message body exceeds 4096 characters", HttpStatusCode.BadRequest, errorCode = "BODY_TOO_LONG")
                    return@post
                }

                val schoolId = ctx.schoolId
                val now = Instant.now()

                // For an existing thread, verify ownership+school BEFORE writing.
                if (req.threadId != null) {
                    val parsed = runCatching { UUID.fromString(req.threadId) }.getOrNull()
                        ?: run { call.fail("Invalid thread_id"); return@post }
                    val owns = dbQuery {
                        MessageThreadsTable.selectAll().where {
                            (MessageThreadsTable.id eq parsed) and
                                (MessageThreadsTable.ownerUserId eq uid) and
                                (MessageThreadsTable.schoolId eq schoolId)
                        }.count() > 0L
                    }
                    if (!owns) {
                        call.fail("Thread not found", HttpStatusCode.NotFound); return@post
                    }
                }

                // Phase 1: parse client_msg_id for idempotency.
                val clientMsgId = req.clientMsgId?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                val replyTo = req.replyToId?.let { runCatching { UUID.fromString(it) }.getOrNull() }

                // RA-51: one shared engine handles append / two-party / self.
                val recipientId = req.recipientUserId
                    ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                // Resolve the admin's display name once so we can reuse it for the notification.
                val actorName = req.senderName ?: dbQuery { resolveMessagingUser(uid)?.fullName } ?: "Admin Desk"

                // Phase 1: map attachment DTOs to core AttachmentInput.
                val attachmentInputs = req.attachments.map { att ->
                    AttachmentInput(
                        fileName = att.fileName,
                        mimeType = att.mimeType,
                        sizeBytes = att.sizeBytes,
                        storageUrl = att.storageUrl,
                        attachmentType = att.attachmentType,
                        thumbnailUrl = att.thumbnailUrl,
                        width = att.width,
                        height = att.height,
                        durationMs = att.durationMs,
                    )
                }

                val result = dbQuery {
                    sendInConversation(
                        senderId = uid,
                        senderSchoolId = schoolId,
                        body = req.body,
                        threadId = req.threadId?.let { UUID.fromString(it) },
                        recipientId = recipientId,
                        senderName = actorName,
                        senderRole = req.senderRole ?: "Admin",
                        senderImageUrl = req.senderImageUrl,
                        iconName = req.iconName,
                        now = now,
                        clientMsgId = clientMsgId,
                        replyToId = replyTo,
                        attachments = attachmentInputs,
                    )
                }
                if (result == null) {
                    call.fail("Thread not found", HttpStatusCode.NotFound)
                } else if (result.isDuplicate) {
                    // Phase 1 (§9.1): 409 on duplicate client_msg_id.
                    call.fail(
                        "Message already sent",
                        HttpStatusCode.Conflict,
                        errorCode = "MSG_DUPLICATE",
                    )
                } else {
                    // RA-S08: notify the recipient (parity with the teacher send path).
                    notifyMessageRecipient(
                        recipientId = result.recipientId,
                        schoolId = schoolId,
                        actorId = uid,
                        actorName = actorName,
                        threadId = result.senderThreadId,
                        body = req.body,
                    )
                    call.created(
                        SendMessageResponse(
                            threadId = result.senderThreadId.toString(),
                            messageId = result.messageId.toString(),
                            seq = result.seq,
                            serverTimestamp = result.serverTimestamp?.toString(),
                        ),
                        message = "Message sent"
                    )
                }
            }

            // -------- Phase 1 (§9.4): EDIT MESSAGE --------
            patch("/messages/{id}") {
                val ctx = call.requireSchoolContext() ?: return@patch
                val msgId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid message id"); return@patch }
                val req = call.receive<EditMessageDto>()
                if (req.body.isBlank()) {
                    call.fail("body is required"); return@patch
                }
                if (req.body.length > 4096) {
                    call.fail("Message body exceeds 4096 characters", HttpStatusCode.BadRequest, errorCode = "BODY_TOO_LONG")
                    return@patch
                }

                val now = Instant.now()
                val result = dbQuery {
                    editMessage(msgId, ctx.userId, req.body, now)
                }
                if (result == null) {
                    call.fail(
                        "Message not found, not yours, already deleted, or edit window expired",
                        HttpStatusCode.Forbidden,
                        errorCode = "MSG_EDIT_WINDOW_EXPIRED",
                    )
                } else {
                    call.ok(
                        mapOf(
                            "message_id" to result.messageId.toString(),
                            "conversation_id" to result.conversationId.toString(),
                            "body" to result.newBody,
                            "edited_at" to result.editedAt.toString(),
                        ),
                        message = "Message edited",
                    )
                }
            }

            // -------- Phase 1 (§9.4): DELETE MESSAGE --------
            delete("/messages/{id}") {
                val ctx = call.requireSchoolContext() ?: return@delete
                val msgId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid message id"); return@delete }
                val scope = call.parameters["scope"] ?: "everyone"

                val now = Instant.now()
                val result = dbQuery {
                    deleteMessage(msgId, ctx.userId, callerIsAdmin = true, scope = scope, now = now)
                }
                if (result == null && scope == "me") {
                    call.okMessage("Message deleted for you")
                } else if (result == null) {
                    call.fail(
                        "Message not found or already deleted",
                        HttpStatusCode.NotFound,
                        errorCode = "MSG_NOT_FOUND",
                    )
                } else {
                    call.ok(
                        mapOf(
                            "message_id" to result.messageId.toString(),
                            "conversation_id" to result.conversationId.toString(),
                        ),
                        message = "Message deleted",
                    )
                }
            }

            // -------- Phase 1 (§9.4, §12): ATTACHMENT UPLOAD --------
            post("/attachments") {
                val ctx = call.requireSchoolContext() ?: return@post
                val result = call.handleAttachmentUpload(ctx.userId, ctx.schoolId)
                if (result != null) {
                    call.ok(result, message = "Attachment uploaded")
                }
            }
        }
    }
}

/*
 * File: TeacherMessagesRouting.kt
 * Module: feature.teacher
 *
 * RA-51 — the TEACHER leg of parent ↔ teacher messaging. Teachers are not in
 * SCHOOL_ROLES, so they cannot use /api/v1/school/messages; this mirror exposes
 * the SAME two-party conversation engine (MessagingCore) on a teacher-gated path.
 *
 *   GET   /api/v1/teacher/messages/threads
 *   GET   /api/v1/teacher/messages/threads/{id}/messages
 *   POST  /api/v1/teacher/messages/threads/{id}/read
 *   POST  /api/v1/teacher/messages                 { thread_id?|recipient_user_id?, body }
 *   POST  /api/v1/teacher/messages/class           { class_name, section?, body }
 *
 * The /class endpoint wires the formerly-dead "Message class parents" button
 * (TeacherClassesScreenV2): it fans out a 1:1 conversation to EACH parent of the
 * named class the teacher owns, so every parent gets it in their own inbox.
 *
 * MULTI-TENANCY: every row carries ctx.schoolId; a teacher can only broadcast to
 * a (class, section) they are assigned to (teacher_subject_assignments).
 */
package com.littlebridge.enrollplus.feature.teacher

import com.littlebridge.enrollplus.core.created
import com.littlebridge.enrollplus.core.fail
import com.littlebridge.enrollplus.core.ok
import com.littlebridge.enrollplus.core.okMessage
import com.littlebridge.enrollplus.core.requireTeacherContext
import com.littlebridge.enrollplus.core.teacherAssignmentsFor
import com.littlebridge.enrollplus.db.ChildrenTable
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.MessageAttachmentsTable
import com.littlebridge.enrollplus.db.MessageThreadsTable
import com.littlebridge.enrollplus.db.MessagesTable
import com.littlebridge.enrollplus.db.StudentsTable
import com.littlebridge.enrollplus.feature.notifications.Notify
import com.littlebridge.enrollplus.feature.school.AttachmentInput
import com.littlebridge.enrollplus.feature.school.conversationMessagesFor
import com.littlebridge.enrollplus.feature.school.deleteMessage
import com.littlebridge.enrollplus.feature.school.editMessage
import com.littlebridge.enrollplus.feature.school.getUnreadCount
import com.littlebridge.enrollplus.feature.school.markConversationRead
import com.littlebridge.enrollplus.feature.school.UnreadCountDto
import com.littlebridge.enrollplus.feature.school.handleAttachmentUpload
import com.littlebridge.enrollplus.feature.school.loadAttachmentsForMessages
import com.littlebridge.enrollplus.feature.school.loadMessageStatus
import com.littlebridge.enrollplus.feature.school.notifyMessageRecipient
import com.littlebridge.enrollplus.feature.school.resolveMessagingUser
import com.littlebridge.enrollplus.feature.school.sendInConversation
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

// ---------------- DTOs (mirror the parent/admin message shapes) ----------------

@Serializable
data class TeacherMessageThreadDto(
    val id: String,
    @SerialName("sender_name") val senderName: String,
    @SerialName("sender_role") val senderRole: String,
    @SerialName("last_message") val lastMessage: String,
    val time: String,
    @SerialName("unread_count") val unreadCount: Int,
    @SerialName("sender_image_url") val senderImageUrl: String? = null,
    @SerialName("icon_name") val iconName: String? = null,
    @SerialName("is_read") val isRead: Boolean,
)

@Serializable
data class TeacherMessageThreadsResponse(val threads: List<TeacherMessageThreadDto>)

@Serializable
data class TeacherMessageDto(
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
    val attachments: List<TeacherAttachmentDto> = emptyList(),
)

@Serializable
data class TeacherAttachmentDto(
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
data class TeacherThreadMessagesResponse(
    @SerialName("thread_id") val threadId: String,
    @SerialName("sender_name") val senderName: String,
    val messages: List<TeacherMessageDto>,
    // Phase 1 (§9.2): pagination metadata.
    @SerialName("has_more") val hasMore: Boolean = false,
    @SerialName("total_count") val totalCount: Long = 0,
)

@Serializable
data class TeacherAttachmentInputDto(
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
data class TeacherSendMessageDto(
    @SerialName("thread_id") val threadId: String? = null,
    @SerialName("recipient_user_id") val recipientUserId: String? = null,
    @SerialName("client_msg_id") val clientMsgId: String? = null,
    @SerialName("reply_to_id") val replyToId: String? = null,
    val attachments: List<TeacherAttachmentInputDto> = emptyList(),
    val body: String,
)

@Serializable
data class TeacherSendMessageResponse(
    @SerialName("thread_id") val threadId: String,
    @SerialName("message_id") val messageId: String,
    val seq: Int? = null,
    @SerialName("server_timestamp") val serverTimestamp: String? = null,
)

@Serializable
data class TeacherEditMessageDto(
    val body: String,
)

@Serializable
data class TeacherClassBroadcastDto(
    @SerialName("class_name") val className: String,
    val section: String? = null,
    val body: String,
)

@Serializable
data class TeacherClassBroadcastResponse(
    @SerialName("recipients") val recipients: Int,
)

// ---------------- helpers ----------------

private fun fmtTeacherTime(ts: Instant): String {
    val zid = ZoneId.systemDefault()
    val zdt = ts.atZone(zid)
    val today = LocalDate.now(zid)
    return when (zdt.toLocalDate()) {
        today -> zdt.format(DateTimeFormatter.ofPattern("h:mm a"))
        today.minusDays(1) -> "Yesterday"
        else -> zdt.format(DateTimeFormatter.ofPattern("MMM dd"))
    }
}

private fun org.jetbrains.exposed.sql.ResultRow.toTeacherThreadDto() = TeacherMessageThreadDto(
    id = this[MessageThreadsTable.id].value.toString(),
    senderName = this[MessageThreadsTable.senderName],
    senderRole = this[MessageThreadsTable.senderRole],
    lastMessage = this[MessageThreadsTable.lastMessage],
    time = fmtTeacherTime(this[MessageThreadsTable.lastMessageAt]),
    unreadCount = this[MessageThreadsTable.unreadCount],
    senderImageUrl = this[MessageThreadsTable.senderImageUrl],
    iconName = this[MessageThreadsTable.iconName],
    isRead = this[MessageThreadsTable.isRead],
)

// Phase 1 (§9.2): convert an attachment row to teacher DTO.
private fun org.jetbrains.exposed.sql.ResultRow.toTeacherAttachmentDto() = TeacherAttachmentDto(
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

/** Parent user-ids of the students in a (class, section) within a school. */
private fun parentsOfClass(schoolId: UUID, className: String, section: String): List<UUID> {
    val codes = StudentsTable.selectAll().where {
        (StudentsTable.schoolId eq schoolId) and
            (StudentsTable.className eq className) and
            (StudentsTable.section eq section) and
            (StudentsTable.isActive eq true)
    }.map { it[StudentsTable.studentCode] }.toSet()
    if (codes.isEmpty()) return emptyList()
    // Portable OR-reduce instead of `inList` (kept consistent with the
    // project's Exposed-version-safe approach — see AnnouncementRouting).
    return ChildrenTable.selectAll().where {
        (ChildrenTable.schoolId eq schoolId) and (ChildrenTable.isActive eq true)
    }.filter { it[ChildrenTable.studentCode] in codes }
        .map { it[ChildrenTable.parentId] }
        .distinct()
}

fun Route.teacherMessagesRouting() {
    authenticate("jwt") {
        route("/api/v1/teacher/messages") {

            // -------- LIST THREADS --------
            get("/threads") {
                val ctx = call.requireTeacherContext() ?: return@get
                val payload = dbQuery {
                    val rows = MessageThreadsTable.selectAll()
                        .where {
                            (MessageThreadsTable.ownerUserId eq ctx.userId) and
                                (MessageThreadsTable.schoolId eq ctx.schoolId)
                        }
                        .orderBy(MessageThreadsTable.lastMessageAt, SortOrder.DESC)
                        .map { it.toTeacherThreadDto() }
                    TeacherMessageThreadsResponse(rows)
                }
                call.ok(payload, message = "Threads fetched successfully")
            }

            // -------- CONVERSATION --------
            get("/threads/{id}/messages") {
                val ctx = call.requireTeacherContext() ?: return@get
                val id = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid id"); return@get }

                // Phase 1 (§9.2): pagination via offset/limit query params.
                val offset = call.parameters["offset"]?.toIntOrNull() ?: 0
                val limit = call.parameters["limit"]?.toIntOrNull() ?: 50

                val payload = dbQuery {
                    val thread = MessageThreadsTable.selectAll()
                        .where {
                            (MessageThreadsTable.id eq id) and
                                (MessageThreadsTable.ownerUserId eq ctx.userId) and
                                (MessageThreadsTable.schoolId eq ctx.schoolId)
                        }
                        .singleOrNull() ?: return@dbQuery null

                    // Phase 1: paginated fetch with seq ordering + pagination metadata.
                    val paged = conversationMessagesFor(id, ctx.userId, offset = offset, limit = limit)
                        ?: return@dbQuery null

                    // Phase 1 (§9.2): load attachments for the page of messages.
                    val msgIds = paged.messages.map { it[MessagesTable.id].value }
                    val attachmentsMap = loadAttachmentsForMessages(msgIds)

                    val msgs = paged.messages.map { row ->
                        val sid = row[MessagesTable.senderId]
                        val created = row[MessagesTable.createdAt]
                        val msgId = row[MessagesTable.id].value
                        val status = if (sid != ctx.userId && paged.conversationId != null) {
                            loadMessageStatus(msgId, ctx.userId)
                        } else null
                        TeacherMessageDto(
                            id = msgId.toString(),
                            body = row[MessagesTable.body] ?: "",
                            isMine = sid == ctx.userId,
                            senderId = sid?.toString(),
                            createdAt = created.toString(),
                            time = fmtTeacherTime(created),
                            seq = row[MessagesTable.seq],
                            status = status,
                            editedAt = row[MessagesTable.editedAt]?.toString(),
                            deletedAt = row[MessagesTable.deletedAt]?.toString(),
                            replyToId = row[MessagesTable.replyToId]?.toString(),
                            attachments = (attachmentsMap[msgId] ?: emptyList()).map { it.toTeacherAttachmentDto() },
                        )
                    }
                    TeacherThreadMessagesResponse(
                        threadId = id.toString(),
                        senderName = thread[MessageThreadsTable.senderName],
                        messages = msgs,
                        hasMore = paged.hasMore,
                        totalCount = paged.totalCount,
                    )
                }
                if (payload == null) call.fail("Thread not found", HttpStatusCode.NotFound)
                else {
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
                val ctx = call.requireTeacherContext() ?: return@get
                val count = dbQuery { getUnreadCount(ctx.userId) }
                call.ok(UnreadCountDto(count))
            }

            // -------- MARK READ --------
            post("/threads/{id}/read") {
                val ctx = call.requireTeacherContext() ?: return@post
                val id = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid id"); return@post }
                val n = dbQuery {
                    MessageThreadsTable.update({
                        (MessageThreadsTable.id eq id) and (MessageThreadsTable.ownerUserId eq ctx.userId)
                    }) {
                        it[unreadCount] = 0
                        it[isRead] = true
                        it[updatedAt] = Instant.now()
                    }
                    // Read Receipts Phase 1: also bulk-update per-message status rows to READ
                    val convId = MessageThreadsTable.selectAll()
                        .where { (MessageThreadsTable.id eq id) and (MessageThreadsTable.ownerUserId eq ctx.userId) }
                        .singleOrNull()?.get(MessageThreadsTable.conversationId) ?: id
                    markConversationRead(ctx.userId, convId)
                }
                if (n == 0) call.fail("Thread not found", HttpStatusCode.NotFound)
                else call.okMessage("Thread marked as read")
            }

            // -------- SEND (reply / start 1:1) --------
            post {
                val ctx = call.requireTeacherContext() ?: return@post
                val req = call.receive<TeacherSendMessageDto>()
                if (req.body.isBlank()) { call.fail("body is required"); return@post }
                if (req.body.length > 4096) {
                    call.fail("Message body exceeds 4096 characters", HttpStatusCode.BadRequest, errorCode = "BODY_TOO_LONG")
                    return@post
                }

                val recipientId = req.recipientUserId?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                val clientMsgId = req.clientMsgId?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                val replyTo = req.replyToId?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                val now = Instant.now()

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
                        senderId = ctx.userId,
                        senderSchoolId = ctx.schoolId,
                        body = req.body,
                        threadId = req.threadId?.let { UUID.fromString(it) },
                        recipientId = recipientId,
                        senderName = ctx.fullName.ifBlank { "Teacher" },
                        senderRole = "Teacher",
                        senderImageUrl = null,
                        iconName = null,
                        now = now,
                        clientMsgId = clientMsgId,
                        replyToId = replyTo,
                        attachments = attachmentInputs,
                    )
                }
                if (result == null) {
                    call.fail("Thread not found", HttpStatusCode.NotFound)
                } else if (result.isDuplicate) {
                    call.fail("Message already sent", HttpStatusCode.Conflict, errorCode = "MSG_DUPLICATE")
                } else {
                    // RA-S08: shared helper; use the peer the engine resolved (works for
                    // new-thread and append sends alike) and include the message body.
                    notifyMessageRecipient(result.recipientId, ctx.schoolId, ctx.userId, ctx.fullName, result.senderThreadId, req.body)
                    call.created(
                        TeacherSendMessageResponse(
                            threadId = result.senderThreadId.toString(),
                            messageId = result.messageId.toString(),
                            seq = result.seq,
                            serverTimestamp = result.serverTimestamp?.toString(),
                        ),
                        message = "Message sent",
                    )
                }
            }

            // -------- Phase 1 (§9.4): EDIT MESSAGE --------
            patch("/messages/{id}") {
                val ctx = call.requireTeacherContext() ?: return@patch
                val msgId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid message id"); return@patch }
                val req = call.receive<TeacherEditMessageDto>()
                if (req.body.isBlank()) { call.fail("body is required"); return@patch }
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
                val ctx = call.requireTeacherContext() ?: return@delete
                val msgId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid message id"); return@delete }
                val scope = call.parameters["scope"] ?: "everyone"

                val now = Instant.now()
                val result = dbQuery {
                    deleteMessage(msgId, ctx.userId, callerIsAdmin = false, scope = scope, now = now)
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
                val ctx = call.requireTeacherContext() ?: return@post
                val result = call.handleAttachmentUpload(ctx.userId, ctx.schoolId)
                if (result != null) {
                    call.ok(result, message = "Attachment uploaded")
                }
            }

            // -------- BROADCAST TO CLASS PARENTS --------
            post("/class") {
                val ctx = call.requireTeacherContext() ?: return@post
                val req = call.receive<TeacherClassBroadcastDto>()
                if (req.body.isBlank()) { call.fail("body is required"); return@post }
                if (req.className.isBlank()) { call.fail("class_name is required"); return@post }

                // The teacher must own the (class, section) — admins may broadcast to any.
                val owned = teacherAssignmentsFor(ctx)
                val privileged = ctx.role == "school_admin" || ctx.role == "admin"
                val section = req.section
                    ?: owned.firstOrNull { it.className == req.className }?.section
                    ?: "A"
                val ownsClass = privileged || owned.any { it.className == req.className && it.section == section }
                if (!ownsClass) {
                    call.fail("You are not assigned to this class", HttpStatusCode.Forbidden)
                    return@post
                }

                val now = Instant.now()
                val parents = dbQuery { parentsOfClass(ctx.schoolId, req.className, section) }
                if (parents.isEmpty()) {
                    call.fail("No parents found for ${req.className}-$section", HttpStatusCode.NotFound)
                    return@post
                }

                dbQuery {
                    parents.forEach { parentId ->
                        sendInConversation(
                            senderId = ctx.userId,
                            senderSchoolId = ctx.schoolId,
                            body = req.body,
                            threadId = null,
                            recipientId = parentId,
                            senderName = ctx.fullName.ifBlank { "Teacher" },
                            senderRole = "Teacher (${req.className}-$section)",
                            senderImageUrl = null,
                            iconName = null,
                            now = now,
                        )
                    }
                }
                // One notification fan-out to every parent.
                Notify.toUsers(
                    userIds = parents,
                    category = "message",
                    title = "Message from ${ctx.fullName.ifBlank { "your child's teacher" }}",
                    body = req.body.take(120),
                    schoolId = ctx.schoolId,
                    actorId = ctx.userId,
                    deepLink = "parent/messages",
                    refType = "message",
                )
                call.created(TeacherClassBroadcastResponse(parents.size), message = "Message sent to ${parents.size} parents")
            }
        }
    }
}

// RA-S08: the per-recipient message notification now lives in the shared MessagingCore
// (notifyMessageRecipient) so the admin and teacher send paths can't drift. The teacher
// broadcast-to-class path still uses Notify.toUsers directly above.

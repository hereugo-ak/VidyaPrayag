/*
 * File: ParentMessagesRouting.kt
 * Module: feature.user
 *
 * Endpoints (all JWT):
 *   GET  /api/v1/parent/messages/threads
 *   GET  /api/v1/parent/messages/threads/{id}/messages
 *   POST /api/v1/parent/messages/threads/{id}/read
 *   POST /api/v1/parent/messages
 *
 * Parent-school harmony (SCHOOL_SIDE_STATUS_REPORT §9.2):
 *   Parent messaging now uses the SAME `message_threads` / `messages` tables as
 *   the school side (feature/school/MessagesRouting.kt), instead of a static
 *   sample list on the client. Threads are scoped to the authenticated parent
 *   via `owner_user_id`, exactly mirroring the school inbox model, so the two
 *   surfaces share one source of truth and can converse through the same rows.
 *
 *   `POST /api/v1/parent/messages` lets a parent reply to an existing thread or
 *   start a new one (recipient defaults to self when omitted, e.g. a parent
 *   note). This mirrors the school send semantics for symmetry.
 */
package com.littlebridge.enrollplus.feature.user

import com.littlebridge.enrollplus.core.created
import com.littlebridge.enrollplus.core.fail
import com.littlebridge.enrollplus.core.ok
import com.littlebridge.enrollplus.core.okMessage
import com.littlebridge.enrollplus.core.principalUserUuid
import com.littlebridge.enrollplus.db.AppUsersTable
import com.littlebridge.enrollplus.db.ChildrenTable
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.MessageAttachmentsTable
import com.littlebridge.enrollplus.db.MessageThreadsTable
import com.littlebridge.enrollplus.db.MessagesTable
import com.littlebridge.enrollplus.db.TeacherPeriodsTable
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

// ---------------- DTOs (mirror school MessagesRouting for client reuse) ----------------

@Serializable
data class ParentMessageThreadDto(
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
data class ParentMessageThreadsResponse(val threads: List<ParentMessageThreadDto>)

@Serializable
data class ParentMessageDto(
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
    val attachments: List<ParentAttachmentDto> = emptyList(),
)

@Serializable
data class ParentAttachmentDto(
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
data class ParentThreadMessagesResponse(
    @SerialName("thread_id") val threadId: String,
    @SerialName("sender_name") val senderName: String,
    val messages: List<ParentMessageDto>,
    // Phase 1 (§9.2): pagination metadata.
    @SerialName("has_more") val hasMore: Boolean = false,
    @SerialName("total_count") val totalCount: Long = 0,
)

@Serializable
data class ParentAttachmentInputDto(
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
data class ParentSendMessageDto(
    @SerialName("thread_id") val threadId: String? = null,
    @SerialName("sender_name") val senderName: String? = null,
    @SerialName("sender_role") val senderRole: String? = null,
    @SerialName("sender_image_url") val senderImageUrl: String? = null,
    @SerialName("icon_name") val iconName: String? = null,
    @SerialName("recipient_user_id") val recipientUserId: String? = null,
    @SerialName("client_msg_id") val clientMsgId: String? = null,
    @SerialName("reply_to_id") val replyToId: String? = null,
    val attachments: List<ParentAttachmentInputDto> = emptyList(),
    val body: String
)

@Serializable
data class ParentSendMessageResponse(
    @SerialName("thread_id") val threadId: String,
    @SerialName("message_id") val messageId: String,
    val seq: Int? = null,
    @SerialName("server_timestamp") val serverTimestamp: String? = null,
)

@Serializable
data class ParentEditMessageDto(
    val body: String,
)

/**
 * RA-S07 — a person a parent can START a conversation with: a teacher or admin in
 * the parent's child's school. `id` is the recipient's `app_users.id`, which is
 * exactly what `POST /api/v1/parent/messages` accepts as `recipient_user_id`.
 * `isClassTeacher` flags teachers who teach one of the child's grades (resolved via
 * the school timetable) so the client can surface them first / label them.
 */
@Serializable
data class ParentRecipientDto(
    val id: String,
    val name: String,
    val role: String,
    val subtitle: String,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("is_class_teacher") val isClassTeacher: Boolean = false,
)

@Serializable
data class ParentRecipientsResponse(val recipients: List<ParentRecipientDto>)

// ---------------- helpers ----------------

private fun fmtParentTime(ts: Instant): String {
    val zid = ZoneId.systemDefault()
    val zdt = ts.atZone(zid)
    val today = LocalDate.now(zid)
    return when (zdt.toLocalDate()) {
        today -> zdt.format(DateTimeFormatter.ofPattern("h:mm a"))
        today.minusDays(1) -> "Yesterday"
        else -> zdt.format(DateTimeFormatter.ofPattern("MMM dd"))
    }
}

private fun org.jetbrains.exposed.sql.ResultRow.toParentThreadDto() = ParentMessageThreadDto(
    id = this[MessageThreadsTable.id].value.toString(),
    senderName = this[MessageThreadsTable.senderName],
    senderRole = this[MessageThreadsTable.senderRole],
    lastMessage = this[MessageThreadsTable.lastMessage],
    time = fmtParentTime(this[MessageThreadsTable.lastMessageAt]),
    unreadCount = this[MessageThreadsTable.unreadCount],
    senderImageUrl = this[MessageThreadsTable.senderImageUrl],
    iconName = this[MessageThreadsTable.iconName],
    isRead = this[MessageThreadsTable.isRead]
)

// Phase 1 (§9.2): convert an attachment row to parent DTO.
private fun org.jetbrains.exposed.sql.ResultRow.toParentAttachmentDto() = ParentAttachmentDto(
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

/** Resolve the (first active) school the parent's child belongs to, if any. */
private fun resolveParentSchoolId(parentId: UUID): UUID? = ChildrenTable.selectAll()
    .where { (ChildrenTable.parentId eq parentId) and (ChildrenTable.isActive eq true) }
    .mapNotNull { it[ChildrenTable.schoolId] }
    .firstOrNull()

fun Route.parentMessagesRouting() {
    authenticate("jwt") {
        route("/api/v1/parent/messages") {

            // -------- LIST THREADS --------
            get("/threads") {
                val uid = call.principalUserUuid() ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized); return@get }
                val payload = dbQuery {
                    val rows = MessageThreadsTable.selectAll()
                        .where { MessageThreadsTable.ownerUserId eq uid }
                        .orderBy(MessageThreadsTable.lastMessageAt, SortOrder.DESC)
                        .map { it.toParentThreadDto() }
                    ParentMessageThreadsResponse(rows)
                }
                call.ok(payload, message = "Threads fetched successfully")
            }

            // -------- RECIPIENTS (RA-S07: compose-new picker) --------
            // Who can the parent START a conversation with? Every active teacher and
            // admin in the parent's child's school. Teachers who teach one of the
            // child's grades (per the school timetable) are flagged + sorted first.
            // The school is derived from the child (NEVER the body / a user id).
            get("/recipients") {
                val uid = call.principalUserUuid() ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized); return@get }

                val payload = dbQuery {
                    val schoolId = resolveParentSchoolId(uid) ?: resolveMessagingUser(uid)?.schoolId
                        ?: return@dbQuery ParentRecipientsResponse(emptyList())

                    // The child's grade(s) — used to flag the class teacher(s).
                    val childGrades = ChildrenTable.selectAll()
                        .where { (ChildrenTable.parentId eq uid) and (ChildrenTable.isActive eq true) }
                        .mapNotNull { it[ChildrenTable.currentGrade]?.takeIf { g -> g.isNotBlank() } }
                        .toSet()

                    // Teacher ids who teach one of those grades (timetable-driven; empty if no
                    // timetable). Filter the grade match in-memory (portable, set-based) to stay
                    // consistent with the project's Exposed-version-safe approach (no `inList`).
                    val classTeacherIds: Set<UUID> = if (childGrades.isEmpty()) emptySet() else
                        TeacherPeriodsTable.selectAll()
                            .where { TeacherPeriodsTable.schoolId eq schoolId }
                            .filter { it[TeacherPeriodsTable.className] in childGrades }
                            .map { it[TeacherPeriodsTable.teacherId] }
                            .toSet()

                    // All active teachers + admins in the school (the addressable desk).
                    // Portable OR-reduce instead of `inList` (Exposed-version-safe, see
                    // AnnouncementRouting / TeacherMessagesRouting).
                    val roleFilter = listOf("teacher", "admin", "school_admin")
                        .map { r -> AppUsersTable.role eq r }
                        .reduce { acc, op -> acc or op }
                    val recipients = AppUsersTable.selectAll()
                        .where {
                            (AppUsersTable.schoolId eq schoolId) and
                                (AppUsersTable.isActive eq true) and
                                roleFilter
                        }
                        .map { row ->
                            val id = row[AppUsersTable.id].value
                            val role = row[AppUsersTable.role]
                            val isClassTeacher = id in classTeacherIds
                            ParentRecipientDto(
                                id = id.toString(),
                                name = row[AppUsersTable.fullName],
                                role = role,
                                subtitle = when {
                                    isClassTeacher -> "Class teacher"
                                    role == "teacher" -> "Teacher"
                                    else -> "School office"
                                },
                                imageUrl = row[AppUsersTable.profilePicUrl],
                                isClassTeacher = isClassTeacher,
                            )
                        }
                        // Class teachers first, then teachers, then admins; alphabetical within a band.
                        .sortedWith(
                            compareByDescending<ParentRecipientDto> { it.isClassTeacher }
                                .thenByDescending { it.role == "teacher" }
                                .thenBy { it.name.lowercase() }
                        )

                    ParentRecipientsResponse(recipients)
                }
                call.ok(payload, message = "Recipients fetched")
            }

            // -------- CONVERSATION --------
            get("/threads/{id}/messages") {
                val uid = call.principalUserUuid() ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized); return@get }
                val id = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid id"); return@get }

                // Phase 1 (§9.2): pagination via offset/limit query params.
                val offset = call.parameters["offset"]?.toIntOrNull() ?: 0
                val limit = call.parameters["limit"]?.toIntOrNull() ?: 50

                val payload = dbQuery {
                    val thread = MessageThreadsTable.selectAll()
                        .where { (MessageThreadsTable.id eq id) and (MessageThreadsTable.ownerUserId eq uid) }
                        .singleOrNull() ?: return@dbQuery null

                    // Phase 1: paginated fetch with seq ordering + pagination metadata.
                    val paged = conversationMessagesFor(id, uid, offset = offset, limit = limit)
                        ?: return@dbQuery null

                    // Phase 1 (§9.2): load attachments for the page of messages.
                    val msgIds = paged.messages.map { it[MessagesTable.id].value }
                    val attachmentsMap = loadAttachmentsForMessages(msgIds)

                    val msgs = paged.messages.map { row ->
                        val sid = row[MessagesTable.senderId]
                        val created = row[MessagesTable.createdAt]
                        val msgId = row[MessagesTable.id].value
                        val status = if (sid != uid && paged.conversationId != null) {
                            loadMessageStatus(msgId, uid)
                        } else null
                        ParentMessageDto(
                            id = msgId.toString(),
                            body = row[MessagesTable.body] ?: "",
                            isMine = sid == uid,
                            senderId = sid?.toString(),
                            createdAt = created.toString(),
                            time = fmtParentTime(created),
                            seq = row[MessagesTable.seq],
                            status = status,
                            editedAt = row[MessagesTable.editedAt]?.toString(),
                            deletedAt = row[MessagesTable.deletedAt]?.toString(),
                            replyToId = row[MessagesTable.replyToId]?.toString(),
                            attachments = (attachmentsMap[msgId] ?: emptyList()).map { it.toParentAttachmentDto() },
                        )
                    }
                    ParentThreadMessagesResponse(
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
                            (MessageThreadsTable.id eq id) and (MessageThreadsTable.ownerUserId eq uid)
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
                val uid = call.principalUserUuid() ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized); return@get }
                val count = dbQuery { getUnreadCount(uid) }
                call.ok(UnreadCountDto(count))
            }

            // -------- MARK READ --------
            post("/threads/{id}/read") {
                val uid = call.principalUserUuid() ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized); return@post }
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

            // -------- SEND --------
            post {
                val uid = call.principalUserUuid() ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized); return@post }
                val req = call.receive<ParentSendMessageDto>()
                if (req.body.isBlank()) { call.fail("body is required"); return@post }
                if (req.body.length > 4096) {
                    call.fail("Message body exceeds 4096 characters", HttpStatusCode.BadRequest, errorCode = "BODY_TOO_LONG")
                    return@post
                }

                // For existing threads, verify ownership BEFORE writing.
                if (req.threadId != null) {
                    val parsed = runCatching { UUID.fromString(req.threadId) }.getOrNull()
                        ?: run { call.fail("Invalid thread_id"); return@post }
                    val owns = dbQuery {
                        MessageThreadsTable.selectAll().where {
                            (MessageThreadsTable.id eq parsed) and (MessageThreadsTable.ownerUserId eq uid)
                        }.count() > 0L
                    }
                    if (!owns) { call.fail("Thread not found", HttpStatusCode.NotFound); return@post }
                }

                val now = Instant.now()

                // RA-51: the parent's school is derived from their child — NEVER the
                // body and NEVER a user-id fallback (the old `schoolId ?: uid` wrote a
                // user UUID into the school_id column, corrupting school-scoped reads).
                // A brand-new conversation requires a known school; replies to an
                // existing owned thread reuse that thread's school.
                val recipientId = req.recipientUserId
                    ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                // Resolve a REAL school id (child's school, else the parent's own
                // app_users.school_id) — never a user id (the old `schoolId ?: uid`
                // corrupted the school_id column).
                val parentSchoolId = dbQuery { resolveParentSchoolId(uid) ?: resolveMessagingUser(uid)?.schoolId }
                val actorName = req.senderName ?: dbQuery { resolveMessagingUser(uid)?.fullName } ?: "Parent"

                // A new conversation (not a reply to an owned thread) needs a school.
                if (req.threadId == null && parentSchoolId == null) {
                    call.fail("Link a child to a school before messaging", HttpStatusCode.Conflict)
                    return@post
                }

                // Phase 1: parse client_msg_id for idempotency.
                val clientMsgId = req.clientMsgId?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                val replyTo = req.replyToId?.let { runCatching { UUID.fromString(it) }.getOrNull() }

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
                    // For a reply (threadId given) the engine ignores senderSchoolId;
                    // for a new conversation parentSchoolId is guaranteed non-null above.
                    sendInConversation(
                        senderId = uid,
                        senderSchoolId = parentSchoolId ?: uid /* unused on the reply path */,
                        body = req.body,
                        threadId = req.threadId?.let { UUID.fromString(it) },
                        recipientId = recipientId,
                        senderName = actorName,
                        senderRole = req.senderRole ?: "Parent",
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
                    call.fail("Message already sent", HttpStatusCode.Conflict, errorCode = "MSG_DUPLICATE")
                } else {
                    // RA-S08: notify the recipient (teacher/admin) of the parent's message — parity
                    // with the admin/teacher send paths via the shared helper. Best-effort.
                    notifyMessageRecipient(
                        recipientId = result.recipientId,
                        schoolId = parentSchoolId ?: uid,
                        actorId = uid,
                        actorName = actorName,
                        threadId = result.senderThreadId,
                        body = req.body,
                    )
                    call.created(
                        ParentSendMessageResponse(
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
                val uid = call.principalUserUuid() ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized); return@patch }
                val msgId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid message id"); return@patch }
                val req = call.receive<ParentEditMessageDto>()
                if (req.body.isBlank()) { call.fail("body is required"); return@patch }
                if (req.body.length > 4096) {
                    call.fail("Message body exceeds 4096 characters", HttpStatusCode.BadRequest, errorCode = "BODY_TOO_LONG")
                    return@patch
                }

                val now = Instant.now()
                val result = dbQuery {
                    editMessage(msgId, uid, req.body, now)
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
                val uid = call.principalUserUuid() ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized); return@delete }
                val msgId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid message id"); return@delete }
                val scope = call.parameters["scope"] ?: "everyone"

                val now = Instant.now()
                val result = dbQuery {
                    deleteMessage(msgId, uid, callerIsAdmin = false, scope = scope, now = now)
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
                val uid = call.principalUserUuid() ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized); return@post }
                val schoolId = dbQuery { resolveParentSchoolId(uid) ?: resolveMessagingUser(uid)?.schoolId }
                    ?: run { call.fail("Link a child to a school before uploading", HttpStatusCode.Conflict); return@post }
                val result = call.handleAttachmentUpload(uid, schoolId)
                if (result != null) {
                    call.ok(result, message = "Attachment uploaded")
                }
            }
        }
    }
}

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
package com.littlebridge.vidyaprayag.feature.user

import com.littlebridge.vidyaprayag.core.created
import com.littlebridge.vidyaprayag.core.fail
import com.littlebridge.vidyaprayag.core.ok
import com.littlebridge.vidyaprayag.core.okMessage
import com.littlebridge.vidyaprayag.core.principalUserUuid
import com.littlebridge.vidyaprayag.db.ChildrenTable
import com.littlebridge.vidyaprayag.db.DatabaseFactory.dbQuery
import com.littlebridge.vidyaprayag.db.MessageThreadsTable
import com.littlebridge.vidyaprayag.db.MessagesTable
import com.littlebridge.vidyaprayag.feature.school.conversationMessagesFor
import com.littlebridge.vidyaprayag.feature.school.resolveMessagingUser
import com.littlebridge.vidyaprayag.feature.school.sendInConversation
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
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
    val time: String
)

@Serializable
data class ParentThreadMessagesResponse(
    @SerialName("thread_id") val threadId: String,
    @SerialName("sender_name") val senderName: String,
    val messages: List<ParentMessageDto>
)

@Serializable
data class ParentSendMessageDto(
    @SerialName("thread_id") val threadId: String? = null,
    @SerialName("sender_name") val senderName: String? = null,
    @SerialName("sender_role") val senderRole: String? = null,
    @SerialName("sender_image_url") val senderImageUrl: String? = null,
    @SerialName("icon_name") val iconName: String? = null,
    @SerialName("recipient_user_id") val recipientUserId: String? = null,
    val body: String
)

@Serializable
data class ParentSendMessageResponse(
    @SerialName("thread_id") val threadId: String,
    @SerialName("message_id") val messageId: String
)

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

            // -------- CONVERSATION --------
            get("/threads/{id}/messages") {
                val uid = call.principalUserUuid() ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized); return@get }
                val id = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid id"); return@get }

                val payload = dbQuery {
                    val thread = MessageThreadsTable.selectAll()
                        .where { (MessageThreadsTable.id eq id) and (MessageThreadsTable.ownerUserId eq uid) }
                        .singleOrNull() ?: return@dbQuery null

                    // RA-51: read by conversation_id so the parent sees both sides.
                    val (_, rows) = conversationMessagesFor(id, uid) ?: return@dbQuery null
                    val msgs = rows.map { row ->
                        val sid = row[MessagesTable.senderId]
                        val created = row[MessagesTable.createdAt]
                        ParentMessageDto(
                            id = row[MessagesTable.id].value.toString(),
                            body = row[MessagesTable.body],
                            isMine = sid == uid,
                            senderId = sid?.toString(),
                            createdAt = created.toString(),
                            time = fmtParentTime(created)
                        )
                    }
                    ParentThreadMessagesResponse(
                        threadId = id.toString(),
                        senderName = thread[MessageThreadsTable.senderName],
                        messages = msgs
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
                }
                if (n == 0) call.fail("Thread not found", HttpStatusCode.NotFound)
                else call.okMessage("Thread marked as read")
            }

            // -------- SEND --------
            post {
                val uid = call.principalUserUuid() ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized); return@post }
                val req = call.receive<ParentSendMessageDto>()
                if (req.body.isBlank()) { call.fail("body is required"); return@post }

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

                // A new conversation (not a reply to an owned thread) needs a school.
                if (req.threadId == null && parentSchoolId == null) {
                    call.fail("Link a child to a school before messaging", HttpStatusCode.Conflict)
                    return@post
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
                        senderName = req.senderName ?: resolveMessagingUser(uid)?.fullName ?: "Parent",
                        senderRole = req.senderRole ?: "Parent",
                        senderImageUrl = req.senderImageUrl,
                        iconName = req.iconName,
                        now = now,
                    )
                }
                if (result == null) {
                    call.fail("Thread not found", HttpStatusCode.NotFound)
                } else {
                    call.created(
                        ParentSendMessageResponse(result.senderThreadId.toString(), result.messageId.toString()),
                        message = "Message sent"
                    )
                }
            }
        }
    }
}

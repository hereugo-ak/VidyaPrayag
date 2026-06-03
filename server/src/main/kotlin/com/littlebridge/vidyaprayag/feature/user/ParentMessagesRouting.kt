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
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
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

                    val msgs = MessagesTable.selectAll()
                        .where { MessagesTable.threadId eq id }
                        .orderBy(MessagesTable.createdAt, SortOrder.ASC)
                        .map { row ->
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
                val result = dbQuery {
                    val schoolId = resolveParentSchoolId(uid)
                    val threadId: UUID = if (req.threadId != null) {
                        val parsed = UUID.fromString(req.threadId)
                        MessageThreadsTable.update({
                            (MessageThreadsTable.id eq parsed) and (MessageThreadsTable.ownerUserId eq uid)
                        }) {
                            it[lastMessage] = req.body.take(200)
                            it[lastMessageAt] = now
                            it[updatedAt] = now
                            it[isRead] = true
                        }
                        parsed
                    } else {
                        val recipient = req.recipientUserId
                            ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                            ?: uid
                        val newThread = UUID.randomUUID()
                        MessageThreadsTable.insert {
                            it[id] = newThread
                            // school_id is non-null in the table; fall back to the
                            // parent's own id namespace only if no child-school yet.
                            it[MessageThreadsTable.schoolId] = schoolId ?: uid
                            it[ownerUserId] = recipient
                            it[senderName] = req.senderName ?: "School"
                            it[senderRole] = req.senderRole ?: "Support"
                            it[senderImageUrl] = req.senderImageUrl
                            it[iconName] = req.iconName
                            it[lastMessage] = req.body.take(200)
                            it[lastMessageAt] = now
                            it[unreadCount] = if (recipient == uid) 0 else 1
                            it[isRead] = recipient == uid
                            it[createdAt] = now
                            it[updatedAt] = now
                        }
                        newThread
                    }

                    val msgId = UUID.randomUUID()
                    MessagesTable.insert {
                        it[id] = msgId
                        it[MessagesTable.threadId] = threadId
                        it[senderId] = uid
                        it[body] = req.body
                        it[createdAt] = now
                    }
                    ParentSendMessageResponse(threadId.toString(), msgId.toString())
                }
                call.created(result, message = "Message sent")
            }
        }
    }
}

/*
 * File: MessagesRouting.kt
 * Module: feature.school
 *
 * Endpoints (all JWT):
 *   GET  /api/v1/school/messages/threads
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
package com.littlebridge.vidyaprayag.feature.school

import com.littlebridge.vidyaprayag.core.created
import com.littlebridge.vidyaprayag.core.fail
import com.littlebridge.vidyaprayag.core.ok
import com.littlebridge.vidyaprayag.core.okMessage
import com.littlebridge.vidyaprayag.core.principalUserId
import com.littlebridge.vidyaprayag.db.AppUsersTable
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

@Serializable
data class SendMessageDto(
    @SerialName("thread_id") val threadId: String? = null,
    @SerialName("sender_name") val senderName: String? = null,
    @SerialName("sender_role") val senderRole: String? = null,
    @SerialName("sender_image_url") val senderImageUrl: String? = null,
    @SerialName("icon_name") val iconName: String? = null,
    @SerialName("recipient_user_id") val recipientUserId: String? = null,
    val body: String
)

@Serializable
data class SendMessageResponse(
    @SerialName("thread_id") val threadId: String,
    @SerialName("message_id") val messageId: String
)

// ---------------- helpers ----------------

private fun resolveSchoolId(uid: UUID): UUID? = AppUsersTable
    .selectAll().where { AppUsersTable.id eq uid }
    .singleOrNull()
    ?.get(AppUsersTable.schoolId)

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

fun Route.messagesRouting() {
    authenticate("jwt") {
        route("/api/v1/school/messages") {

            // -------- LIST THREADS --------
            get("/threads") {
                val uid = call.principalUserId()?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid token", HttpStatusCode.Unauthorized); return@get }

                val payload = dbQuery {
                    val rows = MessageThreadsTable.selectAll()
                        .where { MessageThreadsTable.ownerUserId eq uid }
                        .orderBy(MessageThreadsTable.lastMessageAt, SortOrder.DESC)
                        .map { it.toThreadDto() }
                    MessageThreadsResponse(rows)
                }
                call.ok(payload, message = "Threads fetched successfully")
            }

            // -------- MARK READ --------
            post("/threads/{id}/read") {
                val uid = call.principalUserId()?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid token", HttpStatusCode.Unauthorized); return@post }
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

            // -------- SEND (creates thread on the fly when thread_id is null) --------
            post {
                val uid = call.principalUserId()?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid token", HttpStatusCode.Unauthorized); return@post }
                val req = call.receive<SendMessageDto>()
                if (req.body.isBlank()) {
                    call.fail("body is required"); return@post
                }

                val schoolId = dbQuery { resolveSchoolId(uid) }
                    ?: run { call.fail("User not associated with any school", HttpStatusCode.NotFound); return@post }

                val now = Instant.now()

                val result = dbQuery {
                    val threadId: UUID = if (req.threadId != null) {
                        val parsed = runCatching { UUID.fromString(req.threadId) }.getOrNull()
                            ?: error("Invalid thread_id")
                        // Refresh thread metadata.
                        MessageThreadsTable.update({ MessageThreadsTable.id eq parsed }) {
                            it[lastMessage] = req.body.take(200)
                            it[lastMessageAt] = now
                            it[updatedAt] = now
                            // Sender = current user, so this thread is "read" for them.
                            it[isRead] = true
                        }
                        parsed
                    } else {
                        // Brand-new thread.  Default recipient = self (admin desk).
                        val recipient = req.recipientUserId
                            ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                            ?: uid
                        val newThread = UUID.randomUUID()
                        MessageThreadsTable.insert {
                            it[id] = newThread
                            it[MessageThreadsTable.schoolId] = schoolId
                            it[ownerUserId] = recipient
                            it[senderName] = req.senderName ?: "Admin Desk"
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
                    SendMessageResponse(threadId.toString(), msgId.toString())
                }

                call.created(result, message = "Message sent")
            }
        }
    }
}

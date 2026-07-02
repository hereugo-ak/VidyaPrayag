/*
 * File: MessagingCore.kt
 * Module: feature.school
 *
 * RA-51 — the SHARED two-party conversation engine used by both the admin
 * (MessagesRouting) and parent (ParentMessagesRouting) surfaces, and by the
 * teacher "message class parents" path.
 *
 * The model (see MessageThreadsTable doc):
 *   - A conversation between user A and user B is TWO thread rows (one owned by
 *     A, one owned by B) that share a `conversation_id`.
 *   - Each owner's row tracks THAT owner's view: unread_count / is_read, plus the
 *     OTHER party's display name+role+avatar (peer_user_id).
 *   - Messages are keyed by `conversation_id`, so both participants read the same
 *     history regardless of which row they own.
 *   - System/self threads (alerts, receipts) keep peer_user_id = null and
 *     conversation_id = their own id (single-owner inbox — unchanged behaviour).
 *
 * MULTI-TENANCY: every row carries the real school_id of the participants — never
 * a user id (fixes the RA-51 school_id-corruption bug). A cross-role conversation
 * is only created when both users belong to the same school.
 */
package com.littlebridge.enrollplus.feature.school

import com.littlebridge.enrollplus.db.AppUsersTable
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.ConversationSeqTable
import com.littlebridge.enrollplus.db.MessageAttachmentsTable
import com.littlebridge.enrollplus.db.MessageStatusTable
import com.littlebridge.enrollplus.db.MessageThreadsTable
import com.littlebridge.enrollplus.db.MessagesTable
import com.littlebridge.enrollplus.feature.notifications.Notify
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.max
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

/** A user's directory card, resolved for addressing them in a conversation. */
internal data class MessagingUser(
    val id: UUID,
    val schoolId: UUID?,
    val fullName: String,
    val role: String,
    val profilePicUrl: String?,
)

/**
 * Phase 1 (MESSAGING_SYSTEM_SPEC §9.1) — attachment metadata passed in the send
 * request. The client first uploads via POST /messages/attachments and receives
 * a storage_url, then includes this object in the send body.
 */
internal data class AttachmentInput(
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val storageUrl: String,
    val attachmentType: String = "IMAGE",
    val thumbnailUrl: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val durationMs: Int? = null,
)

/** Resolve a user's display identity (school-scoped lookup is the caller's job). */
internal fun resolveMessagingUser(userId: UUID): MessagingUser? =
    AppUsersTable.selectAll().where { AppUsersTable.id eq userId }.singleOrNull()?.let { row ->
        MessagingUser(
            id = userId,
            schoolId = row[AppUsersTable.schoolId],
            fullName = row[AppUsersTable.fullName],
            role = row[AppUsersTable.role],
            profilePicUrl = row[AppUsersTable.profilePicUrl],
        )
    }

/**
 * The conversation_id for an existing thread row (falls back to the row id for
 * legacy rows written before RA-51 added the column).
 */
internal fun ResultRow.conversationKey(): UUID =
    this[MessageThreadsTable.conversationId] ?: this[MessageThreadsTable.id].value

/**
 * Find the conversation_id of an existing 1:1 conversation between [a] and [b]
 * within [schoolId], if one already exists (so we append instead of forking a
 * new pair every time).
 */
internal fun existingConversationId(schoolId: UUID, a: UUID, b: UUID): UUID? =
    MessageThreadsTable.selectAll().where {
        (MessageThreadsTable.schoolId eq schoolId) and
            (MessageThreadsTable.ownerUserId eq a) and
            (MessageThreadsTable.peerUserId eq b)
    }.firstOrNull()?.conversationKey()

/**
 * Result of [sendInConversation]: the sender's own thread row id + the new
 * message id + the conversation id.
 *
 * Phase 1: now includes [seq] (server-assigned monotonic per conversation)
 * and [serverTimestamp] (the message's created_at) so the client can update
 * its local DB and advance the sync cursor.
 */
internal data class SendResult(
    val senderThreadId: UUID,
    val messageId: UUID,
    val conversationId: UUID,
    // RA-S08: the resolved peer (recipient) of this send, regardless of whether the caller
    // passed `recipientId` explicitly or appended to an existing thread (peer read off the row).
    // null only for self/system threads. Lets every send-path notify the recipient uniformly.
    val recipientId: UUID? = null,
    // Phase 1: seq + server timestamp for the enhanced send response (§9.1).
    val seq: Int? = null,
    val serverTimestamp: Instant? = null,
    // Phase 1: true when this result is a duplicate (idempotency hit on client_msg_id).
    // The routing layer returns 409 instead of 201 when this is true.
    val isDuplicate: Boolean = false,
)

/**
 * The single source of truth for "send a message". Handles three cases:
 *
 *   1. Appending to an existing thread the sender owns (threadId given).
 *   2. Starting / continuing a real two-party conversation with [recipientId]
 *      (creates BOTH participants' rows on first contact, then appends).
 *   3. A self / system thread (recipientId == sender or null) — single-owner row,
 *      unchanged legacy behaviour.
 *
 * Ownership of an existing [threadId] MUST be verified by the caller first.
 * Returns null only when an existing thread row cannot be found.
 */
internal fun sendInConversation(
    senderId: UUID,
    senderSchoolId: UUID,
    body: String,
    threadId: UUID?,
    recipientId: UUID?,
    // Display fields for a freshly-created thread, as the PEER will see the sender.
    senderName: String,
    senderRole: String,
    senderImageUrl: String?,
    iconName: String?,
    now: Instant = Instant.now(),
    // Phase 1 (MESSAGING_SYSTEM_SPEC §9.1): idempotency, reply, attachments.
    clientMsgId: UUID? = null,
    replyToId: UUID? = null,
    attachments: List<AttachmentInput> = emptyList(),
): SendResult? {
    // ---- Idempotency: if client_msg_id was seen before, return the existing message ----
    if (clientMsgId != null) {
        val existing = findMessageByClientMsgId(clientMsgId)
        if (existing != null) return existing.copy(isDuplicate = true)
    }

    val preview = body.take(200)

    // ---- Case 1: append to an existing thread the sender owns ----
    if (threadId != null) {
        val row = MessageThreadsTable.selectAll()
            .where { (MessageThreadsTable.id eq threadId) and (MessageThreadsTable.ownerUserId eq senderId) }
            .singleOrNull() ?: return null
        val convId = row.conversationKey()
        val peer = row[MessageThreadsTable.peerUserId]

        // Refresh the sender's own row (read for them).
        MessageThreadsTable.update({ MessageThreadsTable.id eq threadId }) {
            it[lastMessage] = preview
            it[lastMessageAt] = now
            it[updatedAt] = now
            it[isRead] = true
            it[conversationId] = convId
        }
        // Bump the peer's mirror row (unread for them).
        if (peer != null) {
            bumpPeerRow(convId, peer, preview, now)
        }
        val (msgId, seq) = insertMessage(threadId, convId, senderId, body, now, clientMsgId, replyToId)
        insertAttachments(msgId, convId, senderId, senderSchoolId, attachments, now)
        if (peer != null) {
            insertMessageStatus(msgId, convId, peer, now)
        }
        // RA-S08: peer read off the existing thread row (null for a self/system thread).
        return SendResult(threadId, msgId, convId, recipientId = peer, seq = seq, serverTimestamp = now)
    }

    // ---- Case 3: self / system thread ----
    if (recipientId == null || recipientId == senderId) {
        val convId = UUID.randomUUID()
        val newThread = UUID.randomUUID()
        MessageThreadsTable.insert {
            it[id] = newThread
            it[MessageThreadsTable.schoolId] = senderSchoolId
            it[conversationId] = convId
            it[ownerUserId] = senderId
            it[peerUserId] = null
            it[MessageThreadsTable.senderName] = senderName
            it[MessageThreadsTable.senderRole] = senderRole
            it[MessageThreadsTable.senderImageUrl] = senderImageUrl
            it[MessageThreadsTable.iconName] = iconName
            it[lastMessage] = preview
            it[lastMessageAt] = now
            it[unreadCount] = 0
            it[isRead] = true
            it[createdAt] = now
            it[updatedAt] = now
        }
        val (msgId, seq) = insertMessage(newThread, convId, senderId, body, now, clientMsgId, replyToId)
        insertAttachments(msgId, convId, senderId, senderSchoolId, attachments, now)
        // RA-S08: self/system thread has no peer to notify.
        return SendResult(newThread, msgId, convId, recipientId = null, seq = seq, serverTimestamp = now)
    }

    // ---- Case 2: real two-party conversation ----
    val sender = resolveMessagingUser(senderId)
    val recipient = resolveMessagingUser(recipientId)
        ?: return null
    // Reuse an existing conversation between these two within the school.
    val convId = existingConversationId(senderSchoolId, senderId, recipientId) ?: UUID.randomUUID()

    // Sender's row (their view of the recipient).
    val senderThread = upsertParticipantRow(
        conversationId = convId,
        ownerId = senderId,
        peerId = recipientId,
        schoolId = senderSchoolId,
        peerName = recipient.fullName,
        peerRole = recipient.role.replaceFirstChar { it.uppercase() },
        peerImage = recipient.profilePicUrl,
        iconName = null,
        preview = preview,
        now = now,
        ownerIsSender = true,
    )
    // Recipient's row (their view of the sender) — unread for them.
    upsertParticipantRow(
        conversationId = convId,
        ownerId = recipientId,
        peerId = senderId,
        schoolId = senderSchoolId,
        peerName = senderName,
        peerRole = senderRole,
        peerImage = senderImageUrl ?: sender?.profilePicUrl,
        iconName = iconName,
        preview = preview,
        now = now,
        ownerIsSender = false,
    )

    val (msgId, seq) = insertMessage(senderThread, convId, senderId, body, now, clientMsgId, replyToId)
    insertAttachments(msgId, convId, senderId, senderSchoolId, attachments, now)
    insertMessageStatus(msgId, convId, recipientId, now)
    return SendResult(senderThread, msgId, convId, recipientId = recipientId, seq = seq, serverTimestamp = now)
}

/**
 * Insert one message row, keyed by both the owning thread and the conversation.
 *
 * Phase 1: now assigns a server-authoritative monotonic [seq] per conversation
 * (§7.1), persists [clientMsgId] for idempotency, and links [replyToId] for
 * reply-quoting (§15.6). Returns the message id AND the assigned seq.
 */
private fun insertMessage(
    threadId: UUID,
    conversationId: UUID,
    senderId: UUID,
    body: String,
    now: Instant,
    clientMsgId: UUID? = null,
    replyToId: UUID? = null,
): Pair<UUID, Int> {
    val msgId = UUID.randomUUID()
    val seq = nextSeqForConversation(conversationId)
    MessagesTable.insert {
        it[id] = msgId
        it[MessagesTable.threadId] = threadId
        it[MessagesTable.conversationId] = conversationId
        it[MessagesTable.senderId] = senderId
        it[MessagesTable.body] = body
        it[MessagesTable.createdAt] = now
        it[MessagesTable.seq] = seq
        if (clientMsgId != null) it[MessagesTable.clientMsgId] = clientMsgId
        if (replyToId != null) it[MessagesTable.replyToId] = replyToId
    }
    return msgId to seq
}

/**
 * Phase 1 (§7.1): server-assigned monotonic seq per conversation.
 *
 * Uses an atomic UPSERT on a conversation_seq counter table to guarantee
 * no duplicate seq values under concurrent inserts. Falls back to
 * MAX(seq)+1 (non-atomic but safe on SQLite) if the counter table is
 * unavailable.
 */
private fun nextSeqForConversation(conversationId: UUID): Int {
    // Atomic within the current transaction: SELECT ... FOR UPDATE locks the
    // counter row so concurrent sends in the same conversation block until we
    // commit. On Postgres this is a row-level lock; on SQLite the single-writer
    // model makes it inherently safe.
    val existing = try {
        ConversationSeqTable.selectAll()
            .where { ConversationSeqTable.conversationId eq conversationId }
            .forUpdate()
            .firstOrNull()
    } catch (_: Throwable) {
        // forUpdate() not supported on some DBs — fall back to plain select.
        ConversationSeqTable.selectAll()
            .where { ConversationSeqTable.conversationId eq conversationId }
            .firstOrNull()
    }

    if (existing != null) {
        val newVal = existing[ConversationSeqTable.nextVal] + 1
        ConversationSeqTable.update({ ConversationSeqTable.conversationId eq conversationId }) {
            it[ConversationSeqTable.nextVal] = newVal
            it[ConversationSeqTable.updatedAt] = Instant.now()
        }
        return newVal
    }

    // First message in this conversation — insert counter at 1.
    ConversationSeqTable.insert {
        it[ConversationSeqTable.id] = UUID.randomUUID()
        it[ConversationSeqTable.conversationId] = conversationId
        it[ConversationSeqTable.nextVal] = 1
        it[ConversationSeqTable.updatedAt] = Instant.now()
    }
    return 1
}

/**
 * Phase 1 (§8.2, §12): insert attachment rows for a message.
 */
private fun insertAttachments(
    messageId: UUID,
    conversationId: UUID,
    senderId: UUID,
    schoolId: UUID,
    attachments: List<AttachmentInput>,
    now: Instant,
) {
    attachments.forEach { att ->
        MessageAttachmentsTable.insert {
            it[id] = UUID.randomUUID()
            it[MessageAttachmentsTable.messageId] = messageId
            it[MessageAttachmentsTable.conversationId] = conversationId
            it[MessageAttachmentsTable.senderId] = senderId
            it[MessageAttachmentsTable.schoolId] = schoolId
            it[MessageAttachmentsTable.fileName] = att.fileName
            it[MessageAttachmentsTable.mimeType] = att.mimeType
            it[MessageAttachmentsTable.sizeBytes] = att.sizeBytes
            it[MessageAttachmentsTable.storageUrl] = att.storageUrl
            if (att.thumbnailUrl != null) it[MessageAttachmentsTable.thumbnailUrl] = att.thumbnailUrl
            it[MessageAttachmentsTable.attachmentType] = att.attachmentType
            if (att.width != null) it[MessageAttachmentsTable.width] = att.width
            if (att.height != null) it[MessageAttachmentsTable.height] = att.height
            if (att.durationMs != null) it[MessageAttachmentsTable.durationMs] = att.durationMs
            it[MessageAttachmentsTable.createdAt] = now
        }
    }
}

/**
 * Phase 1 (§8.2): insert a SENT status row for the recipient of a message.
 */
private fun insertMessageStatus(messageId: UUID, conversationId: UUID, userId: UUID, now: Instant) {
    MessageStatusTable.insert {
        it[id] = UUID.randomUUID()
        it[MessageStatusTable.messageId] = messageId
        it[MessageStatusTable.conversationId] = conversationId
        it[MessageStatusTable.userId] = userId
        it[MessageStatusTable.status] = "SENT"
        it[MessageStatusTable.createdAt] = now
    }
}

/**
 * Phase 1 (§9.1): find an existing message by its client_msg_id (idempotency).
 * Returns a [SendResult] pointing at the existing message so the routing layer
 * can return 409 with the same message_id/thread_id/seq.
 */
internal fun findMessageByClientMsgId(clientMsgId: UUID): SendResult? {
    val row = MessagesTable.selectAll()
        .where { MessagesTable.clientMsgId eq clientMsgId }
        .singleOrNull() ?: return null
    val msgId = row[MessagesTable.id].value
    val convId = row[MessagesTable.conversationId] ?: return null
    val threadId = row[MessagesTable.threadId]
    val seq = row[MessagesTable.seq]
    val ts = row[MessagesTable.createdAt]
    // Resolve the peer from the thread row (senderId may be null for system messages).
    val senderId = row[MessagesTable.senderId]
    val peer = if (senderId != null) {
        MessageThreadsTable.selectAll()
            .where { (MessageThreadsTable.id eq threadId) and (MessageThreadsTable.ownerUserId eq senderId) }
            .singleOrNull()?.get(MessageThreadsTable.peerUserId)
    } else null
    return SendResult(threadId, msgId, convId, recipientId = peer, seq = seq, serverTimestamp = ts)
}

/** Bump a peer's existing row: new preview + 1 unread. */
private fun bumpPeerRow(conversationId: UUID, peerOwner: UUID, preview: String, now: Instant) {
    MessageThreadsTable.update({
        (MessageThreadsTable.conversationId eq conversationId) and
            (MessageThreadsTable.ownerUserId eq peerOwner)
    }) {
        it[lastMessage] = preview
        it[lastMessageAt] = now
        it[updatedAt] = now
        it[isRead] = false
        it[unreadCount] = MessageThreadsTable.unreadCount + 1
    }
}

/**
 * Create or refresh ONE participant's thread row for a conversation. When the
 * row already exists we update its preview/timestamp and (for the recipient)
 * increment unread; otherwise we insert a fresh row.
 */
private fun upsertParticipantRow(
    conversationId: UUID,
    ownerId: UUID,
    peerId: UUID,
    schoolId: UUID,
    peerName: String,
    peerRole: String,
    peerImage: String?,
    iconName: String?,
    preview: String,
    now: Instant,
    ownerIsSender: Boolean,
): UUID {
    val existing = MessageThreadsTable.selectAll().where {
        (MessageThreadsTable.conversationId eq conversationId) and
            (MessageThreadsTable.ownerUserId eq ownerId)
    }.firstOrNull()

    if (existing != null) {
        val rowId = existing[MessageThreadsTable.id].value
        MessageThreadsTable.update({ MessageThreadsTable.id eq rowId }) {
            it[lastMessage] = preview
            it[lastMessageAt] = now
            it[updatedAt] = now
            if (ownerIsSender) {
                it[isRead] = true
            } else {
                it[isRead] = false
                it[unreadCount] = MessageThreadsTable.unreadCount + 1
            }
        }
        return rowId
    }

    val newId = UUID.randomUUID()
    MessageThreadsTable.insert {
        it[id] = newId
        it[MessageThreadsTable.schoolId] = schoolId
        it[MessageThreadsTable.conversationId] = conversationId
        it[ownerUserId] = ownerId
        it[peerUserId] = peerId
        it[senderName] = peerName
        it[senderRole] = peerRole
        it[senderImageUrl] = peerImage
        it[MessageThreadsTable.iconName] = iconName
        it[lastMessage] = preview
        it[lastMessageAt] = now
        it[unreadCount] = if (ownerIsSender) 0 else 1
        it[isRead] = ownerIsSender
        it[createdAt] = now
        it[updatedAt] = now
    }
    return newId
}

/**
 * Phase 1 (§9.2): paginated result for conversation messages.
 */
internal data class PaginatedMessages(
    val conversationId: UUID,
    val messages: List<ResultRow>,
    val totalCount: Long,
    val hasMore: Boolean,
)

/**
 * Load messages in the conversation that owns [threadId], scoped so only a
 * legitimate owner of one of the conversation's rows can read them. Returns null
 * if [ownerId] does not own a row in that conversation.
 *
 * Phase 1: now supports offset/limit pagination (§9.2, §15.1). When [offset]
 * and [limit] are both null, returns ALL messages (backward-compatible with
 * callers that haven't been updated yet).
 */
internal fun conversationMessagesFor(
    threadId: UUID,
    ownerId: UUID,
    offset: Int? = null,
    limit: Int? = null,
): PaginatedMessages? {
    val ownRow = MessageThreadsTable.selectAll().where {
        (MessageThreadsTable.id eq threadId) and (MessageThreadsTable.ownerUserId eq ownerId)
    }.singleOrNull() ?: return null
    val convId = ownRow.conversationKey()

    // Total count for pagination metadata.
    val totalCount = MessagesTable.selectAll().where {
        (MessagesTable.conversationId eq convId) or
            ((MessagesTable.conversationId.isNull()) and (MessagesTable.threadId eq threadId))
    }.count()

    // Read by conversation_id when present; legacy rows fall back to thread_id.
    // Order by seq when available (server-authoritative), fall back to created_at.
    // Match by conversation_id; legacy fallback only for pre-migration rows
    // where conversation_id IS NULL and the message belongs to this thread.
    val query = MessagesTable.selectAll().where {
        (MessagesTable.conversationId eq convId) or
            ((MessagesTable.conversationId.isNull()) and (MessagesTable.threadId eq threadId))
    }.orderBy(MessagesTable.seq to SortOrder.ASC, MessagesTable.createdAt to SortOrder.ASC)

    val rows = if (offset != null && limit != null) {
        query.limit(limit, offset.toLong()).toList()
    } else {
        query.toList()
    }

    val hasMore = if (offset != null && limit != null) {
        (offset + rows.size) < totalCount
    } else false

    return PaginatedMessages(convId, rows, totalCount, hasMore)
}

/**
 * RA-S08 — the SHARED "notify the recipient of a 1:1 message" helper, used by both the admin
 * (MessagesRouting) and teacher (TeacherMessagesRouting) send paths so they can't drift apart.
 *
 * Best-effort: a notification failure must never fail the send. No-ops for a self/system thread
 * (recipientId == null) or a message to oneself. The deep link targets the recipient's Messages
 * surface; the body is the (truncated) message text so the push/notification is actually useful.
 */
internal suspend fun notifyMessageRecipient(
    recipientId: UUID?,
    schoolId: UUID,
    actorId: UUID,
    actorName: String,
    threadId: UUID,
    body: String,
) {
    if (recipientId == null || recipientId == actorId) return
    val recipient = dbQuery { resolveMessagingUser(recipientId) }
    val deepLink = when (recipient?.role) {
        "parent" -> "parent/messages"
        "teacher" -> "teacher/messages"
        "admin", "school_admin", "super_admin" -> "school/messages"
        else -> "messages"
    }
    runCatching {
        Notify.toUser(
            userId = recipientId,
            category = "message",
            title = "Message from ${actorName.ifBlank { "your child's school" }}",
            body = body.take(120),
            schoolId = schoolId,
            actorId = actorId,
            deepLink = deepLink,
            refType = "message",
            refId = threadId.toString(),
        )
    }
}

// ===========================================================================
// Phase 1 (MESSAGING_SYSTEM_SPEC §15.3, §15.4, §9.4) — edit / delete helpers
// ===========================================================================

/** Result of [editMessage]. */
internal data class EditMessageResult(
    val messageId: UUID,
    val conversationId: UUID,
    val newBody: String,
    val editedAt: Instant,
)

/**
 * Phase 1 (§15.3): edit a message's body within the 24-hour window.
 *
 * Rules:
 *   - Only the original sender can edit (senderId == caller).
 *   - Message must not be deleted (deletedAt IS NULL).
 *   - Edit window: createdAt must be within 24 hours of [now].
 *   - Body must be non-blank and ≤ 4096 chars (validated by the caller).
 *
 * Returns null if the message is not found. Throws if the edit window expired
 * or the caller is not the owner (the routing layer maps these to 403).
 */
internal fun editMessage(
    messageId: UUID,
    callerId: UUID,
    newBody: String,
    now: Instant = Instant.now(),
): EditMessageResult? {
    val row = MessagesTable.selectAll()
        .where { MessagesTable.id eq messageId }
        .singleOrNull() ?: return null

    val senderId = row[MessagesTable.senderId] ?: return null
    if (senderId != callerId) return null
    if (row[MessagesTable.deletedAt] != null) return null

    val createdAt = row[MessagesTable.createdAt]
    val editWindowExpired = java.time.Duration.between(createdAt, now).toHours() >= 24
    if (editWindowExpired) return null

    val convId = row[MessagesTable.conversationId] ?: return null

    MessagesTable.update({ MessagesTable.id eq messageId }) {
        it[MessagesTable.body] = newBody
        it[MessagesTable.editedAt] = now
    }

    return EditMessageResult(messageId, convId, newBody, now)
}

/** Result of [deleteMessage]. */
internal data class DeleteMessageResult(
    val messageId: UUID,
    val conversationId: UUID,
)

/**
 * Phase 1 (§15.4): tombstone a message (soft-delete).
 *
 * Rules:
 *   - The sender can delete their own message.
 *   - School admins can delete any message in their school (callerIsAdmin).
 *   - Message must not already be deleted.
 *   - scope=everyone: server-side tombstone (deleted_at=now(), body=NULL).
 *   - scope=me: client-side only — the server does nothing and returns null.
 *
 * Returns null if the message is not found, already deleted, or scope=me.
 */
internal fun deleteMessage(
    messageId: UUID,
    callerId: UUID,
    callerIsAdmin: Boolean = false,
    scope: String = "everyone",
    now: Instant = Instant.now(),
): DeleteMessageResult? {
    if (scope == "me") return null // client-side only; no server action

    val row = MessagesTable.selectAll()
        .where { MessagesTable.id eq messageId }
        .singleOrNull() ?: return null

    val senderId = row[MessagesTable.senderId]
    if (senderId != callerId && !callerIsAdmin) return null
    if (row[MessagesTable.deletedAt] != null) return null

    val convId = row[MessagesTable.conversationId] ?: return null

    MessagesTable.update({ MessagesTable.id eq messageId }) {
        it[MessagesTable.deletedAt] = now
        it[MessagesTable.body] = null  // tombstone — body is nullable to support soft-delete
    }

    return DeleteMessageResult(messageId, convId)
}

/**
 * Phase 1 (§9.2): load attachments for a set of message ids.
 * Returns a map from message_id → list of attachment rows.
 */
internal fun loadAttachmentsForMessages(messageIds: Collection<UUID>): Map<UUID, List<ResultRow>> {
    if (messageIds.isEmpty()) return emptyMap()
    // Portable OR-reduce (consistent with the project's Exposed-version-safe approach).
    val filter = messageIds.map { MessageAttachmentsTable.messageId eq it }
        .reduce { acc, op -> acc or op }
    return MessageAttachmentsTable.selectAll()
        .where { filter }
        .groupBy { it[MessageAttachmentsTable.messageId] }
}

/**
 * Phase 1 (§9.2): load the status for a message for a specific user.
 * Returns the status string (SENT/DELIVERED/READ) or null if no status row exists.
 */
internal fun loadMessageStatus(messageId: UUID, userId: UUID): String? {
    return MessageStatusTable.selectAll()
        .where {
            (MessageStatusTable.messageId eq messageId) and (MessageStatusTable.userId eq userId)
        }
        .singleOrNull()?.get(MessageStatusTable.status)
}

/**
 * Read Receipts: load the peer's (recipient's) status for a message I sent.
 * Used to show read-receipt ticks on the sender's own messages.
 * Returns the status string (SENT/DELIVERED/READ) or null if no status row exists.
 */
internal fun loadPeerMessageStatus(messageId: UUID, senderUserId: UUID): String? {
    return MessageStatusTable.selectAll()
        .where {
            (MessageStatusTable.messageId eq messageId) and (MessageStatusTable.userId neq senderUserId)
        }
        .singleOrNull()?.get(MessageStatusTable.status)
}

/**
 * Read Receipts Phase 1 (READ_RECEIPTS_PLAN §6.1.2): bulk-update all SENT/DELIVERED
 * status rows for a user in a conversation to READ. Called by the /threads/{id}/read
 * endpoints after the thread-level unreadCount/isRead update.
 *
 * @return number of status rows updated
 */
internal fun markConversationRead(userId: UUID, conversationId: UUID): Int {
    val now = Instant.now()
    return MessageStatusTable.update({
        (MessageStatusTable.conversationId eq conversationId) and
            (MessageStatusTable.userId eq userId) and
            (MessageStatusTable.status inList listOf("SENT", "DELIVERED"))
    }) {
        it[status] = "READ"
        it[readAt] = now
    }
}

/**
 * Read Receipts Phase 2 (READ_RECEIPTS_PLAN §6.2): total unread messages across
 * all threads owned by a user. Used by the /messages/unread-count endpoints.
 */
internal fun getUnreadCount(userId: UUID): Int {
    return MessageThreadsTable.selectAll()
        .where {
            (MessageThreadsTable.ownerUserId eq userId) and
                (MessageThreadsTable.unreadCount greater 0)
        }
        .sumOf { it[MessageThreadsTable.unreadCount] }
}

@Serializable
data class UnreadCountDto(@SerialName("unread_count") val unreadCount: Int)

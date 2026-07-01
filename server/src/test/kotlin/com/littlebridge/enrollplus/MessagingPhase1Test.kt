package com.littlebridge.enrollplus

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Phase 1 (MESSAGING_SYSTEM_SPEC) — static-source guard for the messaging
 * enhancements.  These assertions run with zero DB/network setup and fail fast
 * in CI if someone accidentally removes a Phase 1 endpoint, table, or column.
 *
 * Live integration tests (with H2/SQLite) are out of scope for this pass —
 * the project's existing test suite is all source-level or pure-function
 * (see TeacherAccessTest, RouteInventoryTest).
 */
class MessagingPhase1Test {

    private val serverMain = File("src/main/kotlin/com/littlebridge/enrollplus")

    private fun source(relative: String): String {
        val f = File(serverMain, relative)
        assertTrue(f.exists(), "Expected source file to exist: ${f.path}")
        return f.readText()
    }

    // ── Tables.kt: new columns on MessagesTable ──────────────────────────────

    @Test
    fun messagesTable_hasSeqColumn() {
        val src = source("db/Tables.kt")
        assertTrue(src.contains("val seq"), "MessagesTable must have seq column")
        assertTrue(src.contains("integer(\"seq\")"), "seq must be integer type")
    }

    @Test
    fun messagesTable_hasClientMsgIdColumn() {
        val src = source("db/Tables.kt")
        assertTrue(src.contains("val clientMsgId"), "MessagesTable must have clientMsgId column")
        assertTrue(src.contains("\"client_msg_id\""), "clientMsgId must map to client_msg_id")
    }

    @Test
    fun messagesTable_hasEditedAtColumn() {
        val src = source("db/Tables.kt")
        assertTrue(src.contains("val editedAt"), "MessagesTable must have editedAt column")
        assertTrue(src.contains("\"edited_at\""), "editedAt must map to edited_at")
    }

    @Test
    fun messagesTable_hasDeletedAtColumn() {
        val src = source("db/Tables.kt")
        assertTrue(src.contains("val deletedAt"), "MessagesTable must have deletedAt column")
        assertTrue(src.contains("\"deleted_at\""), "deletedAt must map to deleted_at")
    }

    @Test
    fun messagesTable_hasReplyToIdColumn() {
        val src = source("db/Tables.kt")
        assertTrue(src.contains("val replyToId"), "MessagesTable must have replyToId column")
        assertTrue(src.contains("\"reply_to_id\""), "replyToId must map to reply_to_id")
    }

    // ── Tables.kt: new columns on MessageThreadsTable ────────────────────────

    @Test
    fun messageThreadsTable_hasIsMutedColumn() {
        val src = source("db/Tables.kt")
        assertTrue(src.contains("val isMuted"), "MessageThreadsTable must have isMuted column")
        assertTrue(src.contains("\"is_muted\""), "isMuted must map to is_muted")
    }

    @Test
    fun messageThreadsTable_hasIsPinnedColumn() {
        val src = source("db/Tables.kt")
        assertTrue(src.contains("val isPinned"), "MessageThreadsTable must have isPinned column")
        assertTrue(src.contains("\"is_pinned\""), "isPinned must map to is_pinned")
    }

    @Test
    fun messageThreadsTable_hasIsArchivedColumn() {
        val src = source("db/Tables.kt")
        assertTrue(src.contains("val isArchived"), "MessageThreadsTable must have isArchived column")
        assertTrue(src.contains("\"is_archived\""), "isArchived must map to is_archived")
    }

    @Test
    fun messageThreadsTable_hasDraftBodyColumn() {
        val src = source("db/Tables.kt")
        assertTrue(src.contains("val draftBody"), "MessageThreadsTable must have draftBody column")
        assertTrue(src.contains("\"draft_body\""), "draftBody must map to draft_body")
    }

    // ── Tables.kt: new table objects ─────────────────────────────────────────

    @Test
    fun messageStatusTable_exists() {
        val src = source("db/Tables.kt")
        assertTrue(src.contains("object MessageStatusTable"), "MessageStatusTable must be defined")
        assertTrue(src.contains("\"message_status\""), "MessageStatusTable must map to 'message_status' table")
    }

    @Test
    fun messageAttachmentsTable_exists() {
        val src = source("db/Tables.kt")
        assertTrue(src.contains("object MessageAttachmentsTable"), "MessageAttachmentsTable must be defined")
        assertTrue(src.contains("\"message_attachments\""), "MessageAttachmentsTable must map to 'message_attachments' table")
    }

    // ── DatabaseFactory: new tables registered ───────────────────────────────

    @Test
    fun databaseFactory_registersNewTables() {
        val src = source("db/DatabaseFactory.kt")
        assertTrue(src.contains("MessageStatusTable"), "DatabaseFactory.allTables must include MessageStatusTable")
        assertTrue(src.contains("MessageAttachmentsTable"), "DatabaseFactory.allTables must include MessageAttachmentsTable")
    }

    // ── MessagingCore.kt: Phase 1 functions exist ────────────────────────────

    @Test
    fun messagingCore_hasIdempotencyCheck() {
        val src = source("feature/school/MessagingCore.kt")
        assertTrue(src.contains("clientMsgId"), "sendInConversation must accept clientMsgId")
        assertTrue(src.contains("findMessageByClientMsgId"), "findMessageByClientMsgId must exist for idempotency")
        assertTrue(src.contains("isDuplicate"), "SendResult must have isDuplicate field")
    }

    @Test
    fun messagingCore_hasSeqAssignment() {
        val src = source("feature/school/MessagingCore.kt")
        assertTrue(src.contains("nextSeqForConversation"), "nextSeqForConversation must exist")
        assertTrue(src.contains("MessagesTable.seq.max()"), "seq must use MAX(seq)+1 pattern")
    }

    @Test
    fun messagingCore_hasAttachmentSupport() {
        val src = source("feature/school/MessagingCore.kt")
        assertTrue(src.contains("AttachmentInput"), "AttachmentInput data class must exist")
        assertTrue(src.contains("insertAttachments"), "insertAttachments must exist")
        assertTrue(src.contains("loadAttachmentsForMessages"), "loadAttachmentsForMessages must exist")
    }

    @Test
    fun messagingCore_hasPagination() {
        val src = source("feature/school/MessagingCore.kt")
        assertTrue(src.contains("PaginatedMessages"), "PaginatedMessages data class must exist")
        assertTrue(src.contains("offset"), "conversationMessagesFor must accept offset")
        assertTrue(src.contains("limit"), "conversationMessagesFor must accept limit")
        assertTrue(src.contains("hasMore"), "PaginatedMessages must have hasMore field")
    }

    @Test
    fun messagingCore_hasEditMessage() {
        val src = source("feature/school/MessagingCore.kt")
        assertTrue(src.contains("fun editMessage("), "editMessage function must exist")
        assertTrue(src.contains("EditMessageResult"), "EditMessageResult data class must exist")
        assertTrue(src.contains("24"), "editMessage must enforce 24-hour window")
    }

    @Test
    fun messagingCore_hasDeleteMessage() {
        val src = source("feature/school/MessagingCore.kt")
        assertTrue(src.contains("fun deleteMessage("), "deleteMessage function must exist")
        assertTrue(src.contains("DeleteMessageResult"), "DeleteMessageResult data class must exist")
        assertTrue(src.contains("scope"), "deleteMessage must accept scope parameter")
    }

    @Test
    fun messagingCore_hasMessageStatusSupport() {
        val src = source("feature/school/MessagingCore.kt")
        assertTrue(src.contains("insertMessageStatus"), "insertMessageStatus must exist")
        assertTrue(src.contains("loadMessageStatus"), "loadMessageStatus must exist")
    }

    // ── School routing (MessagesRouting.kt) ──────────────────────────────────

    @Test
    fun schoolRouting_hasEnhancedSendDto() {
        val src = source("feature/school/MessagesRouting.kt")
        assertTrue(src.contains("client_msg_id"), "SendMessageDto must have client_msg_id field")
        assertTrue(src.contains("reply_to_id"), "SendMessageDto must have reply_to_id field")
        assertTrue(src.contains("attachments"), "SendMessageDto must have attachments field")
    }

    @Test
    fun schoolRouting_hasPaginationParams() {
        val src = source("feature/school/MessagesRouting.kt")
        assertTrue(src.contains("offset"), "GET messages endpoint must read offset query param")
        assertTrue(src.contains("limit"), "GET messages endpoint must read limit query param")
        assertTrue(src.contains("has_more"), "ThreadMessagesResponse must have has_more field")
        assertTrue(src.contains("total_count"), "ThreadMessagesResponse must have total_count field")
    }

    @Test
    fun schoolRouting_hasEditEndpoint() {
        val src = source("feature/school/MessagesRouting.kt")
        assertTrue(src.contains("patch(\"/messages/{id}\")"), "School routing must have PATCH /messages/{id}")
    }

    @Test
    fun schoolRouting_hasDeleteEndpoint() {
        val src = source("feature/school/MessagesRouting.kt")
        assertTrue(src.contains("delete(\"/messages/{id}\")"), "School routing must have DELETE /messages/{id}")
    }

    @Test
    fun schoolRouting_hasAttachmentUploadEndpoint() {
        val src = source("feature/school/MessagesRouting.kt")
        assertTrue(src.contains("post(\"/attachments\")"), "School routing must have POST /attachments")
    }

    @Test
    fun schoolRouting_hasDuplicateConflictResponse() {
        val src = source("feature/school/MessagesRouting.kt")
        assertTrue(src.contains("MSG_DUPLICATE"), "School routing must return MSG_DUPLICATE error code on idempotency hit")
        assertTrue(src.contains("HttpStatusCode.Conflict"), "School routing must return 409 on duplicate")
    }

    @Test
    fun schoolRouting_hasBodyLengthValidation() {
        val src = source("feature/school/MessagesRouting.kt")
        assertTrue(src.contains("4096"), "School routing must validate body length ≤ 4096")
        assertTrue(src.contains("BODY_TOO_LONG"), "School routing must return BODY_TOO_LONG error code")
    }

    // ── Teacher routing (TeacherMessagesRouting.kt) ──────────────────────────

    @Test
    fun teacherRouting_hasEnhancedSendDto() {
        val src = source("feature/teacher/TeacherMessagesRouting.kt")
        assertTrue(src.contains("client_msg_id"), "TeacherSendMessageDto must have client_msg_id")
        assertTrue(src.contains("reply_to_id"), "TeacherSendMessageDto must have reply_to_id")
        assertTrue(src.contains("attachments"), "TeacherSendMessageDto must have attachments")
    }

    @Test
    fun teacherRouting_hasPaginationParams() {
        val src = source("feature/teacher/TeacherMessagesRouting.kt")
        assertTrue(src.contains("offset"), "Teacher GET messages must read offset")
        assertTrue(src.contains("limit"), "Teacher GET messages must read limit")
        assertTrue(src.contains("has_more"), "TeacherThreadMessagesResponse must have has_more")
    }

    @Test
    fun teacherRouting_hasEditEndpoint() {
        val src = source("feature/teacher/TeacherMessagesRouting.kt")
        assertTrue(src.contains("patch(\"/messages/{id}\")"), "Teacher routing must have PATCH /messages/{id}")
    }

    @Test
    fun teacherRouting_hasDeleteEndpoint() {
        val src = source("feature/teacher/TeacherMessagesRouting.kt")
        assertTrue(src.contains("delete(\"/messages/{id}\")"), "Teacher routing must have DELETE /messages/{id}")
    }

    @Test
    fun teacherRouting_hasAttachmentUploadEndpoint() {
        val src = source("feature/teacher/TeacherMessagesRouting.kt")
        assertTrue(src.contains("post(\"/attachments\")"), "Teacher routing must have POST /attachments")
    }

    // ── Parent routing (ParentMessagesRouting.kt) ────────────────────────────

    @Test
    fun parentRouting_hasEnhancedSendDto() {
        val src = source("feature/user/ParentMessagesRouting.kt")
        assertTrue(src.contains("client_msg_id"), "ParentSendMessageDto must have client_msg_id")
        assertTrue(src.contains("reply_to_id"), "ParentSendMessageDto must have reply_to_id")
        assertTrue(src.contains("attachments"), "ParentSendMessageDto must have attachments")
    }

    @Test
    fun parentRouting_hasPaginationParams() {
        val src = source("feature/user/ParentMessagesRouting.kt")
        assertTrue(src.contains("offset"), "Parent GET messages must read offset")
        assertTrue(src.contains("limit"), "Parent GET messages must read limit")
        assertTrue(src.contains("has_more"), "ParentThreadMessagesResponse must have has_more")
    }

    @Test
    fun parentRouting_hasEditEndpoint() {
        val src = source("feature/user/ParentMessagesRouting.kt")
        assertTrue(src.contains("patch(\"/messages/{id}\")"), "Parent routing must have PATCH /messages/{id}")
    }

    @Test
    fun parentRouting_hasDeleteEndpoint() {
        val src = source("feature/user/ParentMessagesRouting.kt")
        assertTrue(src.contains("delete(\"/messages/{id}\")"), "Parent routing must have DELETE /messages/{id}")
    }

    @Test
    fun parentRouting_hasAttachmentUploadEndpoint() {
        val src = source("feature/user/ParentMessagesRouting.kt")
        assertTrue(src.contains("post(\"/attachments\")"), "Parent routing must have POST /attachments")
    }

    // ── Attachment upload helper exists ──────────────────────────────────────

    @Test
    fun attachmentUploadHelper_exists() {
        val f = File(serverMain, "feature/school/MessageAttachmentUpload.kt")
        assertTrue(f.exists(), "MessageAttachmentUpload.kt must exist")
        val src = f.readText()
        assertTrue(src.contains("handleAttachmentUpload"), "handleAttachmentUpload function must exist")
        assertTrue(src.contains("AttachmentUploadResponse"), "AttachmentUploadResponse DTO must exist")
    }

    // ── SupabaseStorage supports document/audio types ────────────────────────

    @Test
    fun supabaseStorage_supportsDocumentAndAudio() {
        val src = source("feature/media/SupabaseStorage.kt")
        assertTrue(src.contains("DOCUMENT_TYPES"), "SupabaseStorage must have DOCUMENT_TYPES map")
        assertTrue(src.contains("AUDIO_TYPES"), "SupabaseStorage must have AUDIO_TYPES map")
        assertTrue(src.contains("\"DOCUMENT\""), "extensionFor must handle DOCUMENT kind")
        assertTrue(src.contains("\"AUDIO\""), "extensionFor must handle AUDIO kind")
    }

    // ── Migration files exist ────────────────────────────────────────────────

    @Test
    fun migrationFiles_020_to_023_exist() {
        val migrationsDir = File("../docs/db")
        listOf(
            "migration_020_messaging_seq.sql",
            "migration_021_messaging_threads_ext.sql",
            "migration_022_message_status.sql",
            "migration_023_message_attachments.sql",
        ).forEach { name ->
            assertTrue(
                File(migrationsDir, name).exists(),
                "Migration file $name must exist in docs/db/"
            )
        }
    }

    // ── No forbidden legacy schema references ────────────────────────────────

    @Test
    fun noLegacyVidyaSetuSchemaReferences() {
        val tablesSrc = source("db/Tables.kt")
        assertFalse(
            tablesSrc.contains("VIDYASETU") || tablesSrc.contains("vidya_setu"),
            "Tables.kt must not reference the abandoned VIDYASETU v2.1 schema"
        )
    }

    // ── Read Receipts: markConversationRead + readAt column ──────────────────

    @Test
    fun messageStatusTable_hasReadAtColumn() {
        val src = source("db/Tables.kt")
        assertTrue(src.contains("val readAt"), "MessageStatusTable must have readAt column")
        assertTrue(src.contains("\"read_at\""), "readAt must map to read_at column")
    }

    @Test
    fun messagingCore_hasMarkConversationRead() {
        val src = source("feature/school/MessagingCore.kt")
        assertTrue(src.contains("fun markConversationRead("), "MessagingCore must have markConversationRead function")
        assertTrue(src.contains("\"READ\""), "markConversationRead must set status to READ")
        assertTrue(src.contains("\"SENT\", \"DELIVERED\""), "markConversationRead must filter SENT and DELIVERED")
    }

    @Test
    fun messagingCore_hasGetUnreadCount() {
        val src = source("feature/school/MessagingCore.kt")
        assertTrue(src.contains("fun getUnreadCount("), "MessagingCore must have getUnreadCount function")
    }

    @Test
    fun adminRouting_callsMarkConversationRead() {
        val src = source("feature/school/MessagesRouting.kt")
        assertTrue(src.contains("markConversationRead("), "Admin MessagesRouting must call markConversationRead")
        assertTrue(src.contains("\"/unread-count\""), "Admin MessagesRouting must have GET /unread-count endpoint")
    }

    @Test
    fun teacherRouting_callsMarkConversationRead() {
        val src = source("feature/teacher/TeacherMessagesRouting.kt")
        assertTrue(src.contains("markConversationRead("), "Teacher MessagesRouting must call markConversationRead")
        assertTrue(src.contains("\"/unread-count\""), "Teacher MessagesRouting must have GET /unread-count endpoint")
    }

    @Test
    fun parentRouting_callsMarkConversationRead() {
        val src = source("feature/user/ParentMessagesRouting.kt")
        assertTrue(src.contains("markConversationRead("), "Parent MessagesRouting must call markConversationRead")
        assertTrue(src.contains("\"/unread-count\""), "Parent MessagesRouting must have GET /unread-count endpoint")
    }

    // ── Read Receipts: loadPeerMessageStatus for sender read receipts ─────────

    @Test
    fun messagingCore_hasLoadPeerMessageStatus() {
        val src = source("feature/school/MessagingCore.kt")
        assertTrue(src.contains("fun loadPeerMessageStatus("), "MessagingCore must have loadPeerMessageStatus function")
        assertTrue(src.contains("neq"), "loadPeerMessageStatus must query for userId != sender")
    }

    @Test
    fun adminRouting_callsLoadPeerMessageStatus() {
        val src = source("feature/school/MessagesRouting.kt")
        assertTrue(src.contains("loadPeerMessageStatus("), "Admin MessagesRouting must call loadPeerMessageStatus for sent messages")
    }

    @Test
    fun teacherRouting_callsLoadPeerMessageStatus() {
        val src = source("feature/teacher/TeacherMessagesRouting.kt")
        assertTrue(src.contains("loadPeerMessageStatus("), "Teacher MessagesRouting must call loadPeerMessageStatus for sent messages")
    }

    @Test
    fun parentRouting_callsLoadPeerMessageStatus() {
        val src = source("feature/user/ParentMessagesRouting.kt")
        assertTrue(src.contains("loadPeerMessageStatus("), "Parent MessagesRouting must call loadPeerMessageStatus for sent messages")
    }
}

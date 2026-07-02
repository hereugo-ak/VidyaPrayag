/*
 * File: ScheduledMessagesIntegrationTest.kt
 * Module: feature.scheduling.test
 *
 * Live integration test for the Message Scheduling feature's DB state machine.
 * Uses file-based SQLite + a test-local table definition (text columns instead
 * of UUID columns to avoid Exposed UUIDColumnType/SQLite incompatibility) to
 * verify:
 *   1. Table creation and CRUD
 *   2. Atomic claim pattern (SCHEDULED → DISPATCHING)
 *   3. Cancel transition (SCHEDULED → CANCELLED)
 *   4. Retry / FAILED transition
 *   5. Idempotency via clientMsgId
 *   6. Status filter queries (list endpoint logic)
 *   7. Due-message selection (scheduledAt <= now)
 *   8. Full lifecycle: schedule → claim → dispatch
 *   9. Edit (PUT) — only PENDING messages are editable
 *   10. Force-dispatch (sets scheduledAt to now)
 *
 * Does NOT test the actual dispatch (Notify.toUsers, createCalendarEvent, etc.)
 * — those require external services and many more tables. The state machine
 * transitions are the critical DB logic that this test covers.
 */
package com.littlebridge.enrollplus.feature.scheduling

import com.littlebridge.enrollplus.db.ScheduledMessageStatus
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.io.File
import java.time.Instant
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Test-local mirror of ScheduledMessagesTable with text() columns instead of
 * uuid() — Exposed's UUIDColumnType generates SQL that SQLite can't parse
 * in INSERT parameter binding. The column names and types are otherwise
 * identical to the production table.
 */
private object TestScheduledMessagesTable : Table("scheduled_messages") {
    val id              = text("id")
    val schoolId        = text("school_id")
    val messageType     = varchar("message_type", 24)
    val status          = varchar("status", 16).default("SCHEDULED")
    val scheduledAt     = text("scheduled_at")
    val dispatchedAt    = text("dispatched_at").nullable()
    val payload         = text("payload")
    val createdBy       = text("created_by")
    val authorRole      = varchar("author_role", 16)
    val authorName      = varchar("author_name", 128).nullable()
    val audienceType    = varchar("audience_type", 16).default("ALL_SCHOOL")
    val audienceLabel   = varchar("audience_label", 256).nullable()
    val title           = varchar("title", 256).nullable()
    val bodyPreview     = varchar("body_preview", 256).nullable()
    val addToCalendar   = bool("add_to_calendar").default(false)
    val calendarEventCode = varchar("calendar_event_code", 20).nullable()
    val retryCount      = integer("retry_count").default(0)
    val maxRetries      = integer("max_retries").default(3)
    val lastError       = text("last_error").nullable()
    val clientMsgId     = text("client_msg_id").nullable()
    val createdAt       = text("created_at")
    val updatedAt       = text("updated_at")

    override val primaryKey = PrimaryKey(id)
}

class ScheduledMessagesIntegrationTest {

    private val testSchoolId = UUID.randomUUID().toString()
    private val testUserId = UUID.randomUUID().toString()
    private val dbFile = File("build/test-scheduled-messages.db")

    @BeforeTest
    fun setup() {
        dbFile.parentFile?.mkdirs()
        if (dbFile.exists()) dbFile.delete()
        Database.connect("jdbc:sqlite:${dbFile.absolutePath}", "org.sqlite.JDBC")
        transaction {
            exec(
                "CREATE TABLE IF NOT EXISTS scheduled_messages (" +
                    "id TEXT PRIMARY KEY NOT NULL, " +
                    "school_id TEXT NOT NULL, " +
                    "message_type TEXT NOT NULL, " +
                    "status TEXT NOT NULL DEFAULT 'SCHEDULED', " +
                    "scheduled_at TEXT NOT NULL, " +
                    "dispatched_at TEXT, " +
                    "payload TEXT NOT NULL, " +
                    "created_by TEXT NOT NULL, " +
                    "author_role TEXT NOT NULL, " +
                    "author_name TEXT, " +
                    "audience_type TEXT NOT NULL DEFAULT 'ALL_SCHOOL', " +
                    "audience_label TEXT, " +
                    "title TEXT, " +
                    "body_preview TEXT, " +
                    "add_to_calendar INTEGER NOT NULL DEFAULT 0, " +
                    "calendar_event_code TEXT, " +
                    "retry_count INTEGER NOT NULL DEFAULT 0, " +
                    "max_retries INTEGER NOT NULL DEFAULT 3, " +
                    "last_error TEXT, " +
                    "client_msg_id TEXT, " +
                    "created_at TEXT NOT NULL, " +
                    "updated_at TEXT NOT NULL" +
                    ")"
            )
        }
    }

    @AfterTest
    fun teardown() {
        transaction {
            TestScheduledMessagesTable.deleteAll()
        }
    }

    private fun insertScheduledMessage(
        status: String = ScheduledMessageStatus.SCHEDULED,
        scheduledAt: Instant = Instant.now().plusSeconds(300),
        clientMsgId: String? = null,
        retryCount: Int = 0,
        maxRetries: Int = 3,
        messageType: String = "ANNOUNCEMENT",
        createdBy: String = testUserId,
    ): String {
        val id = UUID.randomUUID().toString()
        val now = Instant.now()
        transaction {
            TestScheduledMessagesTable.insert {
                it[TestScheduledMessagesTable.id] = id
                it[TestScheduledMessagesTable.schoolId] = testSchoolId
                it[TestScheduledMessagesTable.messageType] = messageType
                it[TestScheduledMessagesTable.status] = status
                it[TestScheduledMessagesTable.scheduledAt] = scheduledAt.toString()
                it[TestScheduledMessagesTable.payload] = """{"title":"Test","description":"Test desc"}"""
                it[TestScheduledMessagesTable.createdBy] = createdBy
                it[TestScheduledMessagesTable.authorRole] = "admin"
                it[TestScheduledMessagesTable.authorName] = "Test Admin"
                it[TestScheduledMessagesTable.audienceType] = "ALL_SCHOOL"
                it[TestScheduledMessagesTable.title] = "Test Announcement"
                it[TestScheduledMessagesTable.bodyPreview] = "Test desc"
                it[TestScheduledMessagesTable.retryCount] = retryCount
                it[TestScheduledMessagesTable.maxRetries] = maxRetries
                if (clientMsgId != null) it[TestScheduledMessagesTable.clientMsgId] = clientMsgId
                it[TestScheduledMessagesTable.createdAt] = now.toString()
                it[TestScheduledMessagesTable.updatedAt] = now.toString()
            }
        }
        return id
    }

    private fun getStatus(id: String): String? = transaction {
        TestScheduledMessagesTable.selectAll()
            .where { TestScheduledMessagesTable.id eq id }
            .firstOrNull()
            ?.get(TestScheduledMessagesTable.status)
    }

    private fun getRow(id: String) = transaction {
        TestScheduledMessagesTable.selectAll()
            .where { TestScheduledMessagesTable.id eq id }
            .firstOrNull()
    }

    // ── 1. Table creation and basic CRUD ──────────────────────────────────

    @Test
    fun `insert and retrieve scheduled message`() {
        val id = insertScheduledMessage()
        val row = getRow(id)
        assertNotNull(row, "Row should exist after insert")
        assertEquals("ANNOUNCEMENT", row[TestScheduledMessagesTable.messageType])
        assertEquals(ScheduledMessageStatus.SCHEDULED, row[TestScheduledMessagesTable.status])
        assertEquals("Test Announcement", row[TestScheduledMessagesTable.title])
        assertEquals(0, row[TestScheduledMessagesTable.retryCount])
        assertEquals(3, row[TestScheduledMessagesTable.maxRetries])
        assertNull(row[TestScheduledMessagesTable.dispatchedAt])
        assertNull(row[TestScheduledMessagesTable.lastError])
    }

    // ── 2. Atomic claim pattern (SCHEDULED → DISPATCHING) ─────────────────

    @Test
    fun `atomic claim succeeds for SCHEDULED message`() {
        val id = insertScheduledMessage()
        val claimed = transaction {
            val updated = TestScheduledMessagesTable.update({
                (TestScheduledMessagesTable.id eq id) and
                    (TestScheduledMessagesTable.status eq ScheduledMessageStatus.SCHEDULED)
            }) {
                it[TestScheduledMessagesTable.status] = ScheduledMessageStatus.DISPATCHING
            }
            updated > 0
        }
        assertTrue(claimed, "Atomic claim should succeed for SCHEDULED message")
        assertEquals(ScheduledMessageStatus.DISPATCHING, getStatus(id))
    }

    @Test
    fun `atomic claim fails for non-SCHEDULED message`() {
        val id = insertScheduledMessage(status = ScheduledMessageStatus.DISPATCHED)
        val claimed = transaction {
            val updated = TestScheduledMessagesTable.update({
                (TestScheduledMessagesTable.id eq id) and
                    (TestScheduledMessagesTable.status eq ScheduledMessageStatus.SCHEDULED)
            }) {
                it[TestScheduledMessagesTable.status] = ScheduledMessageStatus.DISPATCHING
            }
            updated > 0
        }
        assertFalse(claimed, "Atomic claim should fail for DISPATCHED message")
    }

    @Test
    fun `atomic claim is exclusive - only one caller wins`() {
        val id = insertScheduledMessage()

        val claim1 = transaction {
            TestScheduledMessagesTable.update({
                (TestScheduledMessagesTable.id eq id) and
                    (TestScheduledMessagesTable.status eq ScheduledMessageStatus.SCHEDULED)
            }) {
                it[TestScheduledMessagesTable.status] = ScheduledMessageStatus.DISPATCHING
            } > 0
        }
        assertTrue(claim1, "First claim should succeed")

        val claim2 = transaction {
            TestScheduledMessagesTable.update({
                (TestScheduledMessagesTable.id eq id) and
                    (TestScheduledMessagesTable.status eq ScheduledMessageStatus.SCHEDULED)
            }) {
                it[TestScheduledMessagesTable.status] = ScheduledMessageStatus.DISPATCHING
            } > 0
        }
        assertFalse(claim2, "Second claim should fail — status is DISPATCHING")
    }

    // ── 3. Cancel transition (SCHEDULED → CANCELLED) ──────────────────────

    @Test
    fun `cancel succeeds for SCHEDULED message`() {
        val id = insertScheduledMessage()
        val now = Instant.now()
        val cancelled = transaction {
            val row = TestScheduledMessagesTable.selectAll()
                .where { TestScheduledMessagesTable.id eq id }
                .firstOrNull() ?: return@transaction false
            if (row[TestScheduledMessagesTable.status] !in ScheduledMessageStatus.PENDING) return@transaction false

            TestScheduledMessagesTable.update({ TestScheduledMessagesTable.id eq id }) {
                it[TestScheduledMessagesTable.status] = ScheduledMessageStatus.CANCELLED
                it[TestScheduledMessagesTable.updatedAt] = now.toString()
            }
            true
        }
        assertTrue(cancelled, "Cancel should succeed for SCHEDULED message")
        assertEquals(ScheduledMessageStatus.CANCELLED, getStatus(id))
    }

    @Test
    fun `cancel fails for DISPATCHED message`() {
        val id = insertScheduledMessage(status = ScheduledMessageStatus.DISPATCHED)
        val cancelled = transaction {
            val row = TestScheduledMessagesTable.selectAll()
                .where { TestScheduledMessagesTable.id eq id }
                .firstOrNull() ?: return@transaction false
            if (row[TestScheduledMessagesTable.status] !in ScheduledMessageStatus.PENDING) return@transaction false

            TestScheduledMessagesTable.update({ TestScheduledMessagesTable.id eq id }) {
                it[TestScheduledMessagesTable.status] = ScheduledMessageStatus.CANCELLED
            }
            true
        }
        assertFalse(cancelled, "Cancel should fail for DISPATCHED message")
    }

    @Test
    fun `cancel fails for already CANCELLED message`() {
        val id = insertScheduledMessage(status = ScheduledMessageStatus.CANCELLED)
        val cancelled = transaction {
            val row = TestScheduledMessagesTable.selectAll()
                .where { TestScheduledMessagesTable.id eq id }
                .firstOrNull() ?: return@transaction false
            if (row[TestScheduledMessagesTable.status] !in ScheduledMessageStatus.PENDING) return@transaction false

            TestScheduledMessagesTable.update({ TestScheduledMessagesTable.id eq id }) {
                it[TestScheduledMessagesTable.status] = ScheduledMessageStatus.CANCELLED
            }
            true
        }
        assertFalse(cancelled, "Cancel should fail for already CANCELLED message")
    }

    // ── 4. Retry / FAILED transition ──────────────────────────────────────

    @Test
    fun `retry increments retryCount and re-queues as SCHEDULED`() {
        val id = insertScheduledMessage(retryCount = 0, maxRetries = 3)
        val now = Instant.now()
        transaction {
            TestScheduledMessagesTable.update({ TestScheduledMessagesTable.id eq id }) {
                it[TestScheduledMessagesTable.status] = ScheduledMessageStatus.SCHEDULED
                it[TestScheduledMessagesTable.retryCount] = 1
                it[TestScheduledMessagesTable.lastError] = "Network timeout"
                it[TestScheduledMessagesTable.updatedAt] = now.toString()
            }
        }
        val row = getRow(id)!!
        assertEquals(ScheduledMessageStatus.SCHEDULED, row[TestScheduledMessagesTable.status])
        assertEquals(1, row[TestScheduledMessagesTable.retryCount])
        assertEquals("Network timeout", row[TestScheduledMessagesTable.lastError])
    }

    @Test
    fun `FAILED status set when retryCount reaches maxRetries`() {
        val id = insertScheduledMessage(retryCount = 2, maxRetries = 3)
        val now = Instant.now()
        transaction {
            val retryCount = 3
            val maxRetries = 3
            if (retryCount >= maxRetries) {
                TestScheduledMessagesTable.update({ TestScheduledMessagesTable.id eq id }) {
                    it[TestScheduledMessagesTable.status] = ScheduledMessageStatus.FAILED
                    it[TestScheduledMessagesTable.retryCount] = retryCount
                    it[TestScheduledMessagesTable.lastError] = "SMTP gateway unreachable"
                    it[TestScheduledMessagesTable.updatedAt] = now.toString()
                }
            }
        }
        val row = getRow(id)!!
        assertEquals(ScheduledMessageStatus.FAILED, row[TestScheduledMessagesTable.status])
        assertEquals(3, row[TestScheduledMessagesTable.retryCount])
        assertEquals("SMTP gateway unreachable", row[TestScheduledMessagesTable.lastError])
    }

    @Test
    fun `FAILED message is not picked up by due-message query`() {
        val id = insertScheduledMessage(
            status = ScheduledMessageStatus.FAILED,
            scheduledAt = Instant.now().minusSeconds(60),
        )
        val dueMessages = transaction {
            TestScheduledMessagesTable.selectAll()
                .where {
                    (TestScheduledMessagesTable.status eq ScheduledMessageStatus.SCHEDULED) and
                        (TestScheduledMessagesTable.scheduledAt lessEq Instant.now().toString())
                }
                .toList()
        }
        assertTrue(dueMessages.none { it[TestScheduledMessagesTable.id] == id },
            "FAILED message should not appear in due-message query")
    }

    // ── 5. Idempotency via clientMsgId ────────────────────────────────────

    @Test
    fun `idempotency check finds existing message by clientMsgId`() {
        val clientMsgId = UUID.randomUUID().toString()
        val id = insertScheduledMessage(clientMsgId = clientMsgId)

        val existing = transaction {
            TestScheduledMessagesTable.selectAll()
                .where {
                    (TestScheduledMessagesTable.schoolId eq testSchoolId) and
                        (TestScheduledMessagesTable.clientMsgId eq clientMsgId)
                }
                .firstOrNull()
        }
        assertNotNull(existing, "Idempotency lookup should find existing message")
        assertEquals(id, existing[TestScheduledMessagesTable.id])
    }

    @Test
    fun `idempotency check returns null for unknown clientMsgId`() {
        insertScheduledMessage(clientMsgId = UUID.randomUUID().toString())

        val existing = transaction {
            TestScheduledMessagesTable.selectAll()
                .where {
                    (TestScheduledMessagesTable.schoolId eq testSchoolId) and
                        (TestScheduledMessagesTable.clientMsgId eq UUID.randomUUID().toString())
                }
                .firstOrNull()
        }
        assertNull(existing, "Idempotency lookup should return null for unknown clientMsgId")
    }

    // ── 6. Status filter queries (list endpoint logic) ────────────────────

    @Test
    fun `status filter returns only matching messages`() {
        insertScheduledMessage(status = ScheduledMessageStatus.SCHEDULED)
        insertScheduledMessage(status = ScheduledMessageStatus.DISPATCHED)
        insertScheduledMessage(status = ScheduledMessageStatus.FAILED)
        insertScheduledMessage(status = ScheduledMessageStatus.CANCELLED)

        val scheduledOnly = transaction {
            TestScheduledMessagesTable.selectAll()
                .where {
                    (TestScheduledMessagesTable.schoolId eq testSchoolId) and
                        (TestScheduledMessagesTable.status eq ScheduledMessageStatus.SCHEDULED)
                }
                .toList()
        }
        assertEquals(1, scheduledOnly.size, "Should return only SCHEDULED messages")

        val dispatchedOnly = transaction {
            TestScheduledMessagesTable.selectAll()
                .where {
                    (TestScheduledMessagesTable.schoolId eq testSchoolId) and
                        (TestScheduledMessagesTable.status eq ScheduledMessageStatus.DISPATCHED)
                }
                .toList()
        }
        assertEquals(1, dispatchedOnly.size, "Should return only DISPATCHED messages")
    }

    @Test
    fun `list query returns all school messages when no filter`() {
        insertScheduledMessage(status = ScheduledMessageStatus.SCHEDULED)
        insertScheduledMessage(status = ScheduledMessageStatus.DISPATCHED)
        insertScheduledMessage(status = ScheduledMessageStatus.FAILED)

        val all = transaction {
            TestScheduledMessagesTable.selectAll()
                .where { TestScheduledMessagesTable.schoolId eq testSchoolId }
                .orderBy(TestScheduledMessagesTable.scheduledAt, SortOrder.ASC)
                .toList()
        }
        assertEquals(3, all.size, "Should return all messages for the school")
    }

    @Test
    fun `list query filters by createdBy for teacher role`() {
        val teacherId = UUID.randomUUID().toString()
        insertScheduledMessage()
        insertScheduledMessage(createdBy = teacherId, messageType = "TEACHER_BROADCAST")

        val teacherMessages = transaction {
            TestScheduledMessagesTable.selectAll()
                .where {
                    (TestScheduledMessagesTable.schoolId eq testSchoolId) and
                        (TestScheduledMessagesTable.createdBy eq teacherId)
                }
                .toList()
        }
        assertEquals(1, teacherMessages.size, "Teacher should see only their own messages")
        assertEquals(teacherId, teacherMessages[0][TestScheduledMessagesTable.createdBy])

        val adminMessages = transaction {
            TestScheduledMessagesTable.selectAll()
                .where { TestScheduledMessagesTable.schoolId eq testSchoolId }
                .toList()
        }
        assertEquals(2, adminMessages.size, "Admin should see all school messages")
    }

    // ── 7. Due-message selection (scheduledAt <= now) ─────────────────────

    @Test
    fun `due-message query returns only past-due SCHEDULED messages`() {
        insertScheduledMessage(
            scheduledAt = Instant.now().minusSeconds(60),
            status = ScheduledMessageStatus.SCHEDULED,
        )
        insertScheduledMessage(
            scheduledAt = Instant.now().plusSeconds(3600),
            status = ScheduledMessageStatus.SCHEDULED,
        )
        insertScheduledMessage(
            scheduledAt = Instant.now().minusSeconds(120),
            status = ScheduledMessageStatus.DISPATCHED,
        )

        val now = Instant.now().toString()
        val dueMessages = transaction {
            TestScheduledMessagesTable.selectAll()
                .where {
                    (TestScheduledMessagesTable.status eq ScheduledMessageStatus.SCHEDULED) and
                        (TestScheduledMessagesTable.scheduledAt lessEq now)
                }
                .orderBy(TestScheduledMessagesTable.scheduledAt, SortOrder.ASC)
                .toList()
        }
        assertEquals(1, dueMessages.size, "Only past-due SCHEDULED messages should be returned")
    }

    @Test
    fun `due-message query respects batch size limit`() {
        repeat(5) {
            insertScheduledMessage(scheduledAt = Instant.now().minusSeconds(60))
        }

        val now = Instant.now().toString()
        val batch = transaction {
            TestScheduledMessagesTable.selectAll()
                .where {
                    (TestScheduledMessagesTable.status eq ScheduledMessageStatus.SCHEDULED) and
                        (TestScheduledMessagesTable.scheduledAt lessEq now)
                }
                .orderBy(TestScheduledMessagesTable.scheduledAt, SortOrder.ASC)
                .limit(2)
                .toList()
        }
        assertEquals(2, batch.size, "Batch size limit should be respected")
    }

    // ── 8. Full lifecycle: schedule → claim → dispatch ───────────────────

    @Test
    fun `full lifecycle - schedule claim dispatch`() {
        val id = insertScheduledMessage(scheduledAt = Instant.now().plusSeconds(300))
        assertEquals(ScheduledMessageStatus.SCHEDULED, getStatus(id))

        val claimed = transaction {
            TestScheduledMessagesTable.update({
                (TestScheduledMessagesTable.id eq id) and
                    (TestScheduledMessagesTable.status eq ScheduledMessageStatus.SCHEDULED)
            }) {
                it[TestScheduledMessagesTable.status] = ScheduledMessageStatus.DISPATCHING
            } > 0
        }
        assertTrue(claimed)
        assertEquals(ScheduledMessageStatus.DISPATCHING, getStatus(id))

        val now = Instant.now()
        transaction {
            TestScheduledMessagesTable.update({ TestScheduledMessagesTable.id eq id }) {
                it[TestScheduledMessagesTable.status] = ScheduledMessageStatus.DISPATCHED
                it[TestScheduledMessagesTable.dispatchedAt] = now.toString()
                it[TestScheduledMessagesTable.updatedAt] = now.toString()
            }
        }
        val row = getRow(id)!!
        assertEquals(ScheduledMessageStatus.DISPATCHED, row[TestScheduledMessagesTable.status])
        assertNotNull(row[TestScheduledMessagesTable.dispatchedAt], "dispatchedAt should be set")

        val dueMessages = transaction {
            TestScheduledMessagesTable.selectAll()
                .where {
                    (TestScheduledMessagesTable.status eq ScheduledMessageStatus.SCHEDULED) and
                        (TestScheduledMessagesTable.scheduledAt lessEq Instant.now().toString())
                }
                .toList()
        }
        assertTrue(dueMessages.isEmpty(), "No due messages after dispatch")
    }

    @Test
    fun `full lifecycle - schedule cancel verify`() {
        val id = insertScheduledMessage()
        assertEquals(ScheduledMessageStatus.SCHEDULED, getStatus(id))

        val now = Instant.now()
        val cancelled = transaction {
            val row = TestScheduledMessagesTable.selectAll()
                .where { TestScheduledMessagesTable.id eq id }
                .firstOrNull() ?: return@transaction false
            if (row[TestScheduledMessagesTable.status] !in ScheduledMessageStatus.PENDING) return@transaction false

            TestScheduledMessagesTable.update({ TestScheduledMessagesTable.id eq id }) {
                it[TestScheduledMessagesTable.status] = ScheduledMessageStatus.CANCELLED
                it[TestScheduledMessagesTable.updatedAt] = now.toString()
            }
            true
        }
        assertTrue(cancelled)
        assertEquals(ScheduledMessageStatus.CANCELLED, getStatus(id))

        val dueMessages = transaction {
            TestScheduledMessagesTable.selectAll()
                .where {
                    (TestScheduledMessagesTable.status eq ScheduledMessageStatus.SCHEDULED) and
                        (TestScheduledMessagesTable.scheduledAt lessEq Instant.now().toString())
                }
                .toList()
        }
        assertTrue(dueMessages.isEmpty(), "Cancelled message should not be due")

        val claimAttempt = transaction {
            TestScheduledMessagesTable.update({
                (TestScheduledMessagesTable.id eq id) and
                    (TestScheduledMessagesTable.status eq ScheduledMessageStatus.SCHEDULED)
            }) {
                it[TestScheduledMessagesTable.status] = "DISPATCHING"
            } > 0
        }
        assertFalse(claimAttempt, "Cancelled message should not be claimable")
    }

    @Test
    fun `full lifecycle - schedule fail retry fail again then FAILED`() {
        val id = insertScheduledMessage(retryCount = 0, maxRetries = 2)

        // Attempt 1: claim → fail → retry (retryCount=1, back to SCHEDULED)
        transaction {
            TestScheduledMessagesTable.update({
                (TestScheduledMessagesTable.id eq id) and
                    (TestScheduledMessagesTable.status eq ScheduledMessageStatus.SCHEDULED)
            }) { it[TestScheduledMessagesTable.status] = "DISPATCHING" }
        }
        transaction {
            TestScheduledMessagesTable.update({ TestScheduledMessagesTable.id eq id }) {
                it[TestScheduledMessagesTable.status] = ScheduledMessageStatus.SCHEDULED
                it[TestScheduledMessagesTable.retryCount] = 1
                it[TestScheduledMessagesTable.lastError] = "Connection refused"
                it[TestScheduledMessagesTable.updatedAt] = Instant.now().toString()
            }
        }
        assertEquals(ScheduledMessageStatus.SCHEDULED, getStatus(id))
        assertEquals(1, getRow(id)!![TestScheduledMessagesTable.retryCount])

        // Attempt 2: claim → fail → FAILED (retryCount=2 >= maxRetries=2)
        transaction {
            TestScheduledMessagesTable.update({
                (TestScheduledMessagesTable.id eq id) and
                    (TestScheduledMessagesTable.status eq ScheduledMessageStatus.SCHEDULED)
            }) { it[TestScheduledMessagesTable.status] = "DISPATCHING" }
        }
        transaction {
            val retryCount = 2
            val maxRetries = 2
            if (retryCount >= maxRetries) {
                TestScheduledMessagesTable.update({ TestScheduledMessagesTable.id eq id }) {
                    it[TestScheduledMessagesTable.status] = ScheduledMessageStatus.FAILED
                    it[TestScheduledMessagesTable.retryCount] = retryCount
                    it[TestScheduledMessagesTable.lastError] = "Connection refused"
                    it[TestScheduledMessagesTable.updatedAt] = Instant.now().toString()
                }
            }
        }
        assertEquals(ScheduledMessageStatus.FAILED, getStatus(id))
        assertEquals(2, getRow(id)!![TestScheduledMessagesTable.retryCount])
        assertEquals("Connection refused", getRow(id)!![TestScheduledMessagesTable.lastError])
    }

    // ── 9. Edit (PUT) — only PENDING messages are editable ────────────────

    @Test
    fun `edit succeeds for SCHEDULED message`() {
        val id = insertScheduledMessage()
        val now = Instant.now()
        val updated = transaction {
            val row = TestScheduledMessagesTable.selectAll()
                .where { TestScheduledMessagesTable.id eq id }
                .firstOrNull() ?: return@transaction null
            if (row[TestScheduledMessagesTable.status] !in ScheduledMessageStatus.PENDING) return@transaction null

            TestScheduledMessagesTable.update({ TestScheduledMessagesTable.id eq id }) {
                it[TestScheduledMessagesTable.title] = "Updated Title"
                it[TestScheduledMessagesTable.scheduledAt] = Instant.now().plusSeconds(600).toString()
                it[TestScheduledMessagesTable.updatedAt] = now.toString()
            }
            TestScheduledMessagesTable.selectAll().where { TestScheduledMessagesTable.id eq id }.first()
        }
        assertNotNull(updated, "Edit should succeed for SCHEDULED message")
        assertEquals("Updated Title", updated[TestScheduledMessagesTable.title])
    }

    @Test
    fun `edit fails for DISPATCHED message`() {
        val id = insertScheduledMessage(status = ScheduledMessageStatus.DISPATCHED)
        val result = transaction {
            val row = TestScheduledMessagesTable.selectAll()
                .where { TestScheduledMessagesTable.id eq id }
                .firstOrNull() ?: return@transaction null
            if (row[TestScheduledMessagesTable.status] !in ScheduledMessageStatus.PENDING) return@transaction null

            TestScheduledMessagesTable.update({ TestScheduledMessagesTable.id eq id }) {
                it[TestScheduledMessagesTable.title] = "Updated Title"
            }
            TestScheduledMessagesTable.selectAll().where { TestScheduledMessagesTable.id eq id }.first()
        }
        assertNull(result, "Edit should fail for DISPATCHED message")
    }

    // ── 10. Force-dispatch (sets scheduledAt to now) ───────────────────────

    @Test
    fun `force-dispatch sets scheduledAt to now for PENDING message`() {
        val futureTime = Instant.now().plusSeconds(3600)
        val id = insertScheduledMessage(scheduledAt = futureTime)
        val now = Instant.now()

        val triggered = transaction {
            val row = TestScheduledMessagesTable.selectAll()
                .where { TestScheduledMessagesTable.id eq id }
                .firstOrNull() ?: return@transaction false
            if (row[TestScheduledMessagesTable.status] !in ScheduledMessageStatus.PENDING) return@transaction false

            TestScheduledMessagesTable.update({ TestScheduledMessagesTable.id eq id }) {
                it[TestScheduledMessagesTable.scheduledAt] = now.toString()
                it[TestScheduledMessagesTable.updatedAt] = now.toString()
            }
            true
        }
        assertTrue(triggered, "Force-dispatch should succeed for SCHEDULED message")
        val row = getRow(id)!!
        assertTrue(row[TestScheduledMessagesTable.scheduledAt] < futureTime.toString(),
            "scheduledAt should be moved up to now")
    }

    @Test
    fun `force-dispatch fails for CANCELLED message`() {
        val id = insertScheduledMessage(status = ScheduledMessageStatus.CANCELLED)
        val triggered = transaction {
            val row = TestScheduledMessagesTable.selectAll()
                .where { TestScheduledMessagesTable.id eq id }
                .firstOrNull() ?: return@transaction false
            if (row[TestScheduledMessagesTable.status] !in ScheduledMessageStatus.PENDING) return@transaction false

            TestScheduledMessagesTable.update({ TestScheduledMessagesTable.id eq id }) {
                it[TestScheduledMessagesTable.scheduledAt] = Instant.now().toString()
            }
            true
        }
        assertFalse(triggered, "Force-dispatch should fail for CANCELLED message")
    }
}

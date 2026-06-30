/*
 * File: ScheduledMessageRouting.kt
 * Module: feature.scheduling
 *
 * The Message Scheduling REST surface. Allows school admins and teachers to
 * schedule announcements, admin broadcasts, and teacher class broadcasts for
 * future delivery. The MessageDispatchScheduler polls the scheduled_messages
 * table and dispatches due rows at the configured time.
 *
 * Endpoints (all under /api/v1/school/scheduled-messages, JWT-guarded):
 *   POST   /              — create scheduled message (admin, teacher)
 *   GET    /              — list (filterable by status) (admin, teacher)
 *   GET    /{id}          — get detail (admin, teacher)
 *   PUT    /{id}          — edit before dispatch (admin, teacher)
 *   DELETE /{id}          — cancel (sets CANCELLED) (admin, teacher)
 *   POST   /{id}/dispatch-now — admin force-dispatch
 *
 * Authorization:
 *   Teachers can only create TEACHER_BROADCAST and see/edit/cancel their own.
 *   Admins can create all types and see all school scheduled messages.
 *
 * Spec ref: MESSAGE_SCHEDULING_PLAN.md §5
 */
package com.littlebridge.enrollplus.feature.scheduling

import com.littlebridge.enrollplus.core.created
import com.littlebridge.enrollplus.core.fail
import com.littlebridge.enrollplus.core.ok
import com.littlebridge.enrollplus.core.okMessage
import com.littlebridge.enrollplus.core.requireSchoolAdmin
import com.littlebridge.enrollplus.core.requireSchoolOrTeacherContext
import com.littlebridge.enrollplus.db.AppUsersTable
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.ScheduledMessageStatus
import com.littlebridge.enrollplus.db.ScheduledMessageType
import com.littlebridge.enrollplus.db.ScheduledMessagesTable
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

@Serializable
data class CreateScheduledMessageRequest(
    val messageType: String,
    val scheduledAt: String,
    val payload: JsonElement,
    val addToCalendar: Boolean = false,
    val audienceType: String = "ALL_SCHOOL",
    val audienceLabel: String? = null,
    val title: String? = null,
    val bodyPreview: String? = null,
    @SerialName("client_msg_id") val clientMsgId: String? = null,
)

@Serializable
data class UpdateScheduledMessageRequest(
    val scheduledAt: String? = null,
    val payload: JsonElement? = null,
    val title: String? = null,
    val bodyPreview: String? = null,
    val audienceType: String? = null,
    val audienceLabel: String? = null,
    val addToCalendar: Boolean? = null,
)

@Serializable
data class ScheduledMessageDto(
    val id: String,
    val messageType: String,
    val status: String,
    val scheduledAt: String,
    val dispatchedAt: String? = null,
    val payload: JsonElement,
    val createdBy: String,
    val authorRole: String,
    val authorName: String? = null,
    val audienceType: String,
    val audienceLabel: String? = null,
    val title: String? = null,
    val bodyPreview: String? = null,
    val addToCalendar: Boolean = false,
    val calendarEventCode: String? = null,
    val retryCount: Int = 0,
    val lastError: String? = null,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class ScheduledMessageListResponse(
    val messages: List<ScheduledMessageDto>,
    val total: Int,
)

private val lenientJson = kotlinx.serialization.json.Json {
    ignoreUnknownKeys = true
    isLenient = true
}

private fun ResultRow.toDto(): ScheduledMessageDto {
    val payloadText = this[ScheduledMessagesTable.payload]
    val payloadJson = runCatching { lenientJson.parseToJsonElement(payloadText) }.getOrNull()
        ?: kotlinx.serialization.json.JsonPrimitive(payloadText)
    return ScheduledMessageDto(
        id = this[ScheduledMessagesTable.id].value.toString(),
        messageType = this[ScheduledMessagesTable.messageType],
        status = this[ScheduledMessagesTable.status],
        scheduledAt = this[ScheduledMessagesTable.scheduledAt].toString(),
        dispatchedAt = this[ScheduledMessagesTable.dispatchedAt]?.toString(),
        payload = payloadJson,
        createdBy = this[ScheduledMessagesTable.createdBy].toString(),
        authorRole = this[ScheduledMessagesTable.authorRole],
        authorName = this[ScheduledMessagesTable.authorName],
        audienceType = this[ScheduledMessagesTable.audienceType],
        audienceLabel = this[ScheduledMessagesTable.audienceLabel],
        title = this[ScheduledMessagesTable.title],
        bodyPreview = this[ScheduledMessagesTable.bodyPreview],
        addToCalendar = this[ScheduledMessagesTable.addToCalendar],
        calendarEventCode = this[ScheduledMessagesTable.calendarEventCode],
        retryCount = this[ScheduledMessagesTable.retryCount],
        lastError = this[ScheduledMessagesTable.lastError],
        createdAt = this[ScheduledMessagesTable.createdAt].toString(),
        updatedAt = this[ScheduledMessagesTable.updatedAt].toString(),
    )
}

private val VALID_MESSAGE_TYPES = setOf(
    ScheduledMessageType.ANNOUNCEMENT,
    ScheduledMessageType.ADMIN_BROADCAST,
    ScheduledMessageType.TEACHER_BROADCAST
)

fun Route.scheduledMessageRouting() {
    authenticate("jwt") {
        route("/api/v1/school/scheduled-messages") {

            // ---- create ----
            post {
                val ctx = call.requireSchoolOrTeacherContext() ?: return@post
                val req = call.receive<CreateScheduledMessageRequest>()

                val msgType = req.messageType.uppercase()
                if (msgType !in VALID_MESSAGE_TYPES) {
                    call.fail("Invalid message_type. Allowed: ANNOUNCEMENT, ADMIN_BROADCAST, TEACHER_BROADCAST", HttpStatusCode.BadRequest)
                    return@post
                }

                val isTeacher = ctx.role == "teacher"
                if (isTeacher && msgType != ScheduledMessageType.TEACHER_BROADCAST) {
                    call.fail("Teachers can only schedule TEACHER_BROADCAST messages", HttpStatusCode.Forbidden)
                    return@post
                }

                val scheduledAt = runCatching { Instant.parse(req.scheduledAt) }.getOrNull()
                if (scheduledAt == null) {
                    call.fail("Invalid scheduledAt format. Use ISO-8601: 2026-06-15T09:30:00Z", HttpStatusCode.BadRequest)
                    return@post
                }
                if (scheduledAt.isBefore(Instant.now().plusSeconds(60))) {
                    call.fail("scheduledAt must be at least 1 minute in the future", HttpStatusCode.BadRequest)
                    return@post
                }

                val clientMsgId = req.clientMsgId?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                if (clientMsgId != null) {
                    val existing = dbQuery {
                        ScheduledMessagesTable.selectAll()
                            .where {
                                (ScheduledMessagesTable.schoolId eq ctx.schoolId) and
                                    (ScheduledMessagesTable.clientMsgId eq clientMsgId)
                            }
                            .firstOrNull()
                    }
                    if (existing != null) {
                        call.ok(existing.toDto(), message = "Scheduled message already exists (idempotent)")
                        return@post
                    }
                }

                val authorName = dbQuery {
                    AppUsersTable.selectAll()
                        .where { AppUsersTable.id eq ctx.userId }
                        .singleOrNull()?.get(AppUsersTable.fullName)
                }

                val now = Instant.now()
                val row = dbQuery {
                    ScheduledMessagesTable.insert {
                        it[ScheduledMessagesTable.schoolId] = ctx.schoolId
                        it[ScheduledMessagesTable.messageType] = msgType
                        it[ScheduledMessagesTable.status] = ScheduledMessageStatus.SCHEDULED
                        it[ScheduledMessagesTable.scheduledAt] = scheduledAt
                        it[ScheduledMessagesTable.payload] = req.payload.toString()
                        it[ScheduledMessagesTable.createdBy] = ctx.userId
                        it[ScheduledMessagesTable.authorRole] = ctx.role
                        it[ScheduledMessagesTable.authorName] = authorName
                        it[ScheduledMessagesTable.audienceType] = req.audienceType
                        it[ScheduledMessagesTable.audienceLabel] = req.audienceLabel
                        it[ScheduledMessagesTable.title] = req.title
                        it[ScheduledMessagesTable.bodyPreview] = req.bodyPreview
                        it[ScheduledMessagesTable.addToCalendar] = req.addToCalendar
                        if (clientMsgId != null) it[ScheduledMessagesTable.clientMsgId] = clientMsgId
                        it[ScheduledMessagesTable.createdAt] = now
                        it[ScheduledMessagesTable.updatedAt] = now
                    }
                    ScheduledMessagesTable.selectAll()
                        .where {
                            (ScheduledMessagesTable.schoolId eq ctx.schoolId) and
                                (ScheduledMessagesTable.createdAt eq now)
                        }
                        .orderBy(ScheduledMessagesTable.createdAt, SortOrder.DESC)
                        .first()
                }

                call.created(row.toDto(), message = "Scheduled message created")
            }

            // ---- list ----
            get {
                val ctx = call.requireSchoolOrTeacherContext() ?: return@get
                val statusFilter = call.request.queryParameters["status"]

                val rows = dbQuery {
                    val query = ScheduledMessagesTable.selectAll()
                        .where { ScheduledMessagesTable.schoolId eq ctx.schoolId }
                    if (statusFilter != null && statusFilter in ScheduledMessageStatus.ALL) {
                        query.andWhere { ScheduledMessagesTable.status eq statusFilter }
                    }
                    if (ctx.role == "teacher") {
                        query.andWhere { ScheduledMessagesTable.createdBy eq ctx.userId }
                    }
                    query.orderBy(ScheduledMessagesTable.scheduledAt, SortOrder.ASC).toList()
                }
                val dtos = rows.map { it.toDto() }
                call.ok(ScheduledMessageListResponse(dtos, dtos.size), message = "Scheduled messages fetched")
            }

            // ---- get detail ----
            get("{id}") {
                val ctx = call.requireSchoolOrTeacherContext() ?: return@get
                val id = runCatching { UUID.fromString(call.parameters["id"]) }.getOrNull()
                if (id == null) {
                    call.fail("Invalid id", HttpStatusCode.BadRequest)
                    return@get
                }
                val row = dbQuery {
                    ScheduledMessagesTable.selectAll()
                        .where { ScheduledMessagesTable.id eq id }
                        .firstOrNull()
                }
                if (row == null) {
                    call.fail("Scheduled message not found", HttpStatusCode.NotFound)
                    return@get
                }
                if (row[ScheduledMessagesTable.schoolId] != ctx.schoolId) {
                    call.fail("Scheduled message not found", HttpStatusCode.NotFound)
                    return@get
                }
                if (ctx.role == "teacher" && row[ScheduledMessagesTable.createdBy] != ctx.userId) {
                    call.fail("You can only view your own scheduled messages", HttpStatusCode.Forbidden)
                    return@get
                }
                call.ok(row.toDto(), message = "Scheduled message fetched")
            }

            // ---- edit (before dispatch) ----
            put("{id}") {
                val ctx = call.requireSchoolOrTeacherContext() ?: return@put
                val id = runCatching { UUID.fromString(call.parameters["id"]) }.getOrNull()
                if (id == null) {
                    call.fail("Invalid id", HttpStatusCode.BadRequest)
                    return@put
                }
                val req = call.receive<UpdateScheduledMessageRequest>()
                val now = Instant.now()

                val updated = dbQuery {
                    val row = ScheduledMessagesTable.selectAll()
                        .where { ScheduledMessagesTable.id eq id }
                        .firstOrNull() ?: return@dbQuery null
                    if (row[ScheduledMessagesTable.schoolId] != ctx.schoolId) return@dbQuery null
                    if (ctx.role == "teacher" && row[ScheduledMessagesTable.createdBy] != ctx.userId) return@dbQuery null
                    val currentStatus = row[ScheduledMessagesTable.status]
                    if (currentStatus !in ScheduledMessageStatus.PENDING) return@dbQuery null

                    val scheduledAt = req.scheduledAt?.let {
                        runCatching { Instant.parse(it) }.getOrNull()
                    } ?: row[ScheduledMessagesTable.scheduledAt]

                    ScheduledMessagesTable.update({ ScheduledMessagesTable.id eq id }) {
                        if (req.scheduledAt != null) it[ScheduledMessagesTable.scheduledAt] = scheduledAt
                        if (req.payload != null) it[ScheduledMessagesTable.payload] = req.payload.toString()
                        if (req.title != null) it[ScheduledMessagesTable.title] = req.title
                        if (req.bodyPreview != null) it[ScheduledMessagesTable.bodyPreview] = req.bodyPreview
                        if (req.audienceType != null) it[ScheduledMessagesTable.audienceType] = req.audienceType
                        if (req.audienceLabel != null) it[ScheduledMessagesTable.audienceLabel] = req.audienceLabel
                        if (req.addToCalendar != null) it[ScheduledMessagesTable.addToCalendar] = req.addToCalendar
                        it[ScheduledMessagesTable.updatedAt] = now
                    }
                    ScheduledMessagesTable.selectAll().where { ScheduledMessagesTable.id eq id }.first()
                }
                if (updated == null) {
                    call.fail("Scheduled message not found, not editable, or access denied", HttpStatusCode.NotFound)
                    return@put
                }
                call.ok(updated.toDto(), message = "Scheduled message updated")
            }

            // ---- cancel (soft delete — sets CANCELLED) ----
            delete("{id}") {
                val ctx = call.requireSchoolOrTeacherContext() ?: return@delete
                val id = runCatching { UUID.fromString(call.parameters["id"]) }.getOrNull()
                if (id == null) {
                    call.fail("Invalid id", HttpStatusCode.BadRequest)
                    return@delete
                }
                val now = Instant.now()
                val cancelled = dbQuery {
                    val row = ScheduledMessagesTable.selectAll()
                        .where { ScheduledMessagesTable.id eq id }
                        .firstOrNull() ?: return@dbQuery false
                    if (row[ScheduledMessagesTable.schoolId] != ctx.schoolId) return@dbQuery false
                    if (ctx.role == "teacher" && row[ScheduledMessagesTable.createdBy] != ctx.userId) return@dbQuery false
                    val currentStatus = row[ScheduledMessagesTable.status]
                    if (currentStatus !in ScheduledMessageStatus.PENDING) return@dbQuery false

                    ScheduledMessagesTable.update({ ScheduledMessagesTable.id eq id }) {
                        it[ScheduledMessagesTable.status] = ScheduledMessageStatus.CANCELLED
                        it[ScheduledMessagesTable.updatedAt] = now
                    }
                    true
                }
                if (!cancelled) {
                    call.fail("Scheduled message not found, not cancellable, or access denied", HttpStatusCode.NotFound)
                    return@delete
                }
                call.okMessage("Scheduled message cancelled")
            }

            // ---- force-dispatch (admin only) ----
            post("{id}/dispatch-now") {
                val ctx = call.requireSchoolAdmin() ?: return@post
                val id = runCatching { UUID.fromString(call.parameters["id"]) }.getOrNull()
                if (id == null) {
                    call.fail("Invalid id", HttpStatusCode.BadRequest)
                    return@post
                }
                val now = Instant.now()
                val triggered = dbQuery {
                    val row = ScheduledMessagesTable.selectAll()
                        .where { ScheduledMessagesTable.id eq id }
                        .firstOrNull() ?: return@dbQuery false
                    if (row[ScheduledMessagesTable.schoolId] != ctx.schoolId) return@dbQuery false
                    if (row[ScheduledMessagesTable.status] !in ScheduledMessageStatus.PENDING) return@dbQuery false

                    ScheduledMessagesTable.update({ ScheduledMessagesTable.id eq id }) {
                        it[ScheduledMessagesTable.scheduledAt] = now
                        it[ScheduledMessagesTable.updatedAt] = now
                    }
                    true
                }
                if (!triggered) {
                    call.fail("Scheduled message not found, not dispatchable, or access denied", HttpStatusCode.NotFound)
                    return@post
                }
                call.okMessage("Scheduled message queued for immediate dispatch")
            }
        }
    }
}

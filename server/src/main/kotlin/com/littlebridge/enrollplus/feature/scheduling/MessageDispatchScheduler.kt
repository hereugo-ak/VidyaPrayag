/*
 * File: MessageDispatchScheduler.kt
 * Module: feature.scheduling
 *
 * Polls the scheduled_messages table every 1 minute for rows with
 * status='SCHEDULED' and scheduled_at <= now(), and dispatches them via
 * the existing Notify.toUsers / sendInConversation / createCalendarEvent
 * primitives. Follows the NotificationScheduler pattern.
 *
 * Dispatch per type:
 *   ANNOUNCEMENT      — insert into AnnouncementsTable, resolve recipients
 *                       via NotifyRecipients, call Notify.toUsers(), optionally
 *                       createCalendarEvent().
 *   TEACHER_BROADCAST — resolve parents via parentsOfClass(), sendInConversation()
 *                       per parent, Notify.toUsers() for push.
 *   ADMIN_BROADCAST   — resolve by audience, sendInConversation() + Notify.toUsers().
 *
 * Concurrency & retry:
 *   Atomic claim: UPDATE ... WHERE status='SCHEDULED' — if updated==0, skip.
 *   Retry: increment retryCount. If >= maxRetries, set FAILED. Otherwise
 *   re-queue as SCHEDULED (the next poll will retry).
 *
 * Spec ref: MESSAGE_SCHEDULING_PLAN.md §6
 */
package com.littlebridge.enrollplus.feature.scheduling

import com.littlebridge.enrollplus.db.AnnouncementsTable
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.ScheduledMessageStatus
import com.littlebridge.enrollplus.db.ScheduledMessageType
import com.littlebridge.enrollplus.db.ScheduledMessagesTable
import com.littlebridge.enrollplus.feature.calendar.createCalendarEvent
import com.littlebridge.enrollplus.feature.calendar.EventStatus
import com.littlebridge.enrollplus.feature.calendar.EventSource
import com.littlebridge.enrollplus.feature.notifications.Notify
import com.littlebridge.enrollplus.feature.notifications.NotifyRecipients
import com.littlebridge.enrollplus.feature.school.sendInConversation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

object MessageDispatchScheduler {
    private const val TAG = "MessageDispatchScheduler"
    private const val POLL_INTERVAL_MS = 60_000L
    private const val BATCH_SIZE = 50

    private val lenientJson = Json { ignoreUnknownKeys = true; isLenient = true }

    fun start(scope: CoroutineScope) {
        scope.launch {
            while (true) {
                delay(POLL_INTERVAL_MS)
                runCatching { checkAndDispatch() }
                    .onFailure { println("[$TAG] failed: ${it.message}") }
            }
        }
    }

    private suspend fun checkAndDispatch() {
        val now = Instant.now()
        val dueMessages = dbQuery {
            ScheduledMessagesTable.selectAll()
                .where {
                    (ScheduledMessagesTable.status eq ScheduledMessageStatus.SCHEDULED) and
                        (ScheduledMessagesTable.scheduledAt lessEq now)
                }
                .orderBy(ScheduledMessagesTable.scheduledAt, SortOrder.ASC)
                .limit(BATCH_SIZE)
                .toList()
        }
        if (dueMessages.isEmpty()) return
        println("[$TAG] Found ${dueMessages.size} due message(s) to dispatch")

        var successCount = 0
        var failCount = 0
        for (row in dueMessages) {
            val id = row[ScheduledMessagesTable.id].value
            val claimed = dbQuery {
                val updated = ScheduledMessagesTable.update({
                    (ScheduledMessagesTable.id eq id) and
                        (ScheduledMessagesTable.status eq ScheduledMessageStatus.SCHEDULED)
                }) {
                    it[ScheduledMessagesTable.status] = ScheduledMessageStatus.DISPATCHING
                }
                updated > 0
            }
            if (!claimed) continue

            runCatching { dispatchMessage(row) }
                .onSuccess {
                    dbQuery {
                        ScheduledMessagesTable.update({ ScheduledMessagesTable.id eq id }) {
                            it[ScheduledMessagesTable.status] = ScheduledMessageStatus.DISPATCHED
                            it[ScheduledMessagesTable.dispatchedAt] = Instant.now()
                            it[ScheduledMessagesTable.updatedAt] = Instant.now()
                        }
                    }
                    println("[$TAG] Dispatched $id (${row[ScheduledMessagesTable.messageType]})")
                    successCount++
                }
                .onFailure { e ->
                    val retryCount = row[ScheduledMessagesTable.retryCount] + 1
                    val maxRetries = row[ScheduledMessagesTable.maxRetries]
                    dbQuery {
                        if (retryCount >= maxRetries) {
                            ScheduledMessagesTable.update({ ScheduledMessagesTable.id eq id }) {
                                it[ScheduledMessagesTable.status] = ScheduledMessageStatus.FAILED
                                it[ScheduledMessagesTable.retryCount] = retryCount
                                it[ScheduledMessagesTable.lastError] = e.message?.take(500)
                                it[ScheduledMessagesTable.updatedAt] = Instant.now()
                            }
                            println("[$TAG] FAILED $id after $retryCount retries: ${e.message}")
                        } else {
                            ScheduledMessagesTable.update({ ScheduledMessagesTable.id eq id }) {
                                it[ScheduledMessagesTable.status] = ScheduledMessageStatus.SCHEDULED
                                it[ScheduledMessagesTable.retryCount] = retryCount
                                it[ScheduledMessagesTable.lastError] = e.message?.take(500)
                                it[ScheduledMessagesTable.updatedAt] = Instant.now()
                            }
                            println("[$TAG] Retry $id (attempt $retryCount/$maxRetries): ${e.message}")
                        }
                    }
                    failCount++
                }
        }
        println("[$TAG] Tick complete: $successCount dispatched, $failCount failed")
    }

    private suspend fun dispatchMessage(row: org.jetbrains.exposed.sql.ResultRow) {
        val msgType = row[ScheduledMessagesTable.messageType]
        val schoolId = row[ScheduledMessagesTable.schoolId]
        val createdBy = row[ScheduledMessagesTable.createdBy]
        val authorRole = row[ScheduledMessagesTable.authorRole]
        val authorName = row[ScheduledMessagesTable.authorName] ?: "School"
        val payloadText = row[ScheduledMessagesTable.payload]
        val payload = lenientJson.parseToJsonElement(payloadText).jsonObject
        val title = row[ScheduledMessagesTable.title] ?: payload["title"]?.jsonPrimitive?.contentOrNull ?: ""
        val bodyPreview = row[ScheduledMessagesTable.bodyPreview] ?: payload["description"]?.jsonPrimitive?.contentOrNull ?: ""
        val audienceType = row[ScheduledMessagesTable.audienceType]
        val addToCalendar = row[ScheduledMessagesTable.addToCalendar]
        val isCalendarOnly = payload["is_calendar_only"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false

        when (msgType) {
            ScheduledMessageType.ANNOUNCEMENT -> dispatchAnnouncement(
                schoolId, createdBy, authorRole, payload, title, bodyPreview,
                audienceType, addToCalendar, isCalendarOnly, row
            )
            ScheduledMessageType.TEACHER_BROADCAST -> dispatchTeacherBroadcast(
                schoolId, createdBy, authorName, payload, title, bodyPreview
            )
            ScheduledMessageType.ADMIN_BROADCAST -> dispatchAdminBroadcast(
                schoolId, createdBy, authorName, payload, title, bodyPreview, audienceType
            )
        }
    }

    private suspend fun dispatchAnnouncement(
        schoolId: UUID,
        createdBy: UUID,
        authorRole: String,
        payload: JsonObject,
        title: String,
        bodyPreview: String,
        audienceType: String,
        addToCalendar: Boolean,
        isCalendarOnly: Boolean,
        row: org.jetbrains.exposed.sql.ResultRow
    ) {
        val now = Instant.now()
        val eventId = "EVT_" + UUID.randomUUID().toString().take(8).uppercase()
        val type = payload["type"]?.jsonPrimitive?.contentOrNull ?: "Update"
        val description = payload["description"]?.jsonPrimitive?.contentOrNull ?: ""
        val date = payload["date"]?.jsonPrimitive?.contentOrNull ?: now.toString().take(10)
        val subTitle = payload["sub_title"]?.jsonPrimitive?.contentOrNull
        val eventImage = payload["event_image"]?.jsonPrimitive?.contentOrNull
        val audienceFilter = payload["audience_filter"]?.toString()

        dbQuery {
            AnnouncementsTable.insert {
                it[AnnouncementsTable.schoolId] = schoolId
                it[AnnouncementsTable.eventId] = eventId
                it[AnnouncementsTable.type] = type
                it[AnnouncementsTable.title] = title
                it[AnnouncementsTable.subTitle] = subTitle
                it[AnnouncementsTable.description] = description
                it[AnnouncementsTable.eventImage] = eventImage
                it[AnnouncementsTable.date] = date
                it[AnnouncementsTable.audienceType] = audienceType
                it[AnnouncementsTable.audienceFilter] = audienceFilter
                it[AnnouncementsTable.authorRole] = authorRole
                it[AnnouncementsTable.isCalendarOnly] = isCalendarOnly
                it[AnnouncementsTable.syncedToWa] = false
                it[AnnouncementsTable.createdBy] = createdBy
                it[AnnouncementsTable.createdAt] = now
                it[AnnouncementsTable.updatedAt] = now
            }
        }

        val classNames = audienceStrList(payload, "class_names") + audienceStr(payload, "class_name")
        val subjects = audienceStrList(payload, "subjects") + audienceStr(payload, "subject")
        val studentCodes = audienceStrList(payload, "student_codes") + audienceStr(payload, "student_code")

        if (!isCalendarOnly) {
        val audienceParents = NotifyRecipients.parentsForAudience(
            schoolId = schoolId,
            audienceType = audienceType,
            classNames = classNames,
            subjects = subjects,
            studentCodes = studentCodes,
        )
        val recipients = if (audienceType == "ALL_SCHOOL") {
            (audienceParents + NotifyRecipients.teachersInSchool(schoolId)).distinct()
        } else {
            audienceParents.distinct()
        }
        if (recipients.isNotEmpty()) {
            Notify.toUsers(
                userIds = recipients,
                category = "announcement",
                title = title,
                body = subTitle ?: description.take(140),
                schoolId = schoolId,
                actorId = createdBy,
                deepLink = "announcements/$eventId",
                refType = "announcement",
                refId = eventId,
            )
        }
        }

        if (addToCalendar) {
            runCatching {
                createCalendarEvent(
                    schoolId = schoolId,
                    title = title,
                    description = description,
                    type = calendarTypeForAnnouncement(type),
                    status = EventStatus.PUBLISHED,
                    source = EventSource.ANNOUNCEMENT,
                    sourceRef = eventId,
                    startDate = date,
                    createdBy = createdBy,
                )
            }
        }

        val calCode = if (addToCalendar) {
            runCatching {
                dbQuery {
                    com.littlebridge.enrollplus.db.CalendarEventsTable.selectAll()
                        .where {
                            (com.littlebridge.enrollplus.db.CalendarEventsTable.schoolId eq schoolId) and
                                (com.littlebridge.enrollplus.db.CalendarEventsTable.eventSource eq EventSource.ANNOUNCEMENT) and
                                (com.littlebridge.enrollplus.db.CalendarEventsTable.sourceRef eq eventId)
                        }
                        .firstOrNull()?.get(com.littlebridge.enrollplus.db.CalendarEventsTable.eventCode)
                }
            }.getOrNull()
        } else null

        if (calCode != null) {
            dbQuery {
                ScheduledMessagesTable.update({ ScheduledMessagesTable.id eq row[ScheduledMessagesTable.id].value }) {
                    it[ScheduledMessagesTable.calendarEventCode] = calCode
                }
            }
        }
    }

    private suspend fun dispatchTeacherBroadcast(
        schoolId: UUID,
        teacherId: UUID,
        teacherName: String,
        payload: JsonObject,
        title: String,
        bodyPreview: String
    ) {
        val className = payload["class_name"]?.jsonPrimitive?.contentOrNull ?: return
        val body = payload["body"]?.jsonPrimitive?.contentOrNull ?: bodyPreview
        val parentIds = NotifyRecipients.parentsOfClass(schoolId, className)

        for (parentId in parentIds) {
            runCatching {
                sendInConversation(
                    senderId = teacherId,
                    senderSchoolId = schoolId,
                    body = body,
                    threadId = null,
                    recipientId = parentId,
                    senderName = teacherName,
                    senderRole = "Teacher",
                    senderImageUrl = null,
                    iconName = null,
                )
            }
        }
        if (parentIds.isNotEmpty()) {
            Notify.toUsers(
                userIds = parentIds,
                category = "message",
                title = "Message from $teacherName",
                body = body.take(120),
                schoolId = schoolId,
                actorId = teacherId,
                deepLink = "parent/messages",
                refType = "scheduled_message",
                refId = "teacher_broadcast",
            )
        }
    }

    private suspend fun dispatchAdminBroadcast(
        schoolId: UUID,
        adminId: UUID,
        adminName: String,
        payload: JsonObject,
        title: String,
        bodyPreview: String,
        audienceType: String
    ) {
        val body = payload["body"]?.jsonPrimitive?.contentOrNull ?: bodyPreview
        val classNames = audienceStrList(payload, "class_names") + audienceStr(payload, "class_name")
        val subjects = audienceStrList(payload, "subjects") + audienceStr(payload, "subject")
        val studentCodes = audienceStrList(payload, "student_codes") + audienceStr(payload, "student_code")

        val recipients = NotifyRecipients.parentsForAudience(
            schoolId = schoolId,
            audienceType = audienceType,
            classNames = classNames,
            subjects = subjects,
            studentCodes = studentCodes,
        )

        for (parentId in recipients) {
            runCatching {
                sendInConversation(
                    senderId = adminId,
                    senderSchoolId = schoolId,
                    body = body,
                    threadId = null,
                    recipientId = parentId,
                    senderName = adminName,
                    senderRole = "School Admin",
                    senderImageUrl = null,
                    iconName = null,
                )
            }
        }
        if (recipients.isNotEmpty()) {
            Notify.toUsers(
                userIds = recipients,
                category = "announcement",
                title = title,
                body = body.take(140),
                schoolId = schoolId,
                actorId = adminId,
                deepLink = "announcements",
                refType = "scheduled_message",
                refId = "admin_broadcast",
            )
        }
    }

    private fun audienceStrList(filter: JsonObject, key: String): List<String> =
        filter[key]?.let {
            runCatching { it.jsonArray.mapNotNull { e -> e.jsonPrimitive.contentOrNull } }.getOrNull()
        } ?: emptyList()

    private fun audienceStr(filter: JsonObject, key: String): List<String> =
        filter[key]?.jsonPrimitive?.contentOrNull?.let { listOf(it) } ?: emptyList()

    private fun calendarTypeForAnnouncement(type: String): String = when (type.lowercase()) {
        "holiday", "holidays" -> "HOLIDAY"
        "ptm" -> "PTM"
        "event", "events" -> "SCHOOL_EVENT"
        else -> "SCHOOL_EVENT"
    }
}

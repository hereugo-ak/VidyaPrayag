/*
 * File: Notify.kt
 * Module: feature.notifications
 *
 * The single write-path for the cross-user notification spine (RA-41/42/46/50).
 * Every trigger that should reach a user — student absent → parent, marks
 * published → parent, homework assigned → class parents, announcement posted →
 * parents+teachers, leave applied/decided, link-child decided, fee status
 * change — calls Notify.* so notifications are created consistently, with the
 * originating school_id carried for tenant isolation.
 *
 * MULTI-TENANCY: every row stores `schoolId`. Recipients are resolved by the
 * caller (which already holds a school-scoped context) and we never fan out
 * across schools — the caller passes only in-tenant recipient ids.
 */
package com.littlebridge.enrollplus.feature.notifications

import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.NotificationPreferencesTable
import com.littlebridge.enrollplus.db.NotificationsTable
import com.littlebridge.enrollplus.feature.notification.dto.SendNotificationRequest
import com.littlebridge.enrollplus.feature.notification.repository.DeviceTokenRepository
import com.littlebridge.enrollplus.feature.notification.service.NotificationService
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.selectAll
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

object Notify {

    private val deviceTokenRepository = DeviceTokenRepository()
    private val pushService = NotificationService(deviceTokenRepository)

    // Rate limits (§6.13)
    private const val MAX_PER_USER_PER_DAY = 50
    private const val MAX_PER_CATEGORY_PER_HOUR = 10
    private const val MAX_OTP_PER_HOUR = 3

    /**
     * Create one notification per recipient. No-op when [userIds] is empty.
     * Safe to call inside or outside an existing transaction (opens its own).
     */
    suspend fun toUsers(
        userIds: Collection<UUID>,
        category: String,
        title: String,
        body: String = "",
        schoolId: UUID? = null,
        actorId: UUID? = null,
        deepLink: String? = null,
        refType: String? = null,
        refId: String? = null,
    ) {
        val recipients = userIds.distinct()
        if (recipients.isEmpty()) return
        val now = Instant.now()

        // --- Preferences filtering: drop recipients who disabled this category ---
        val filteredRecipients = filterByPreferences(recipients, category)
        if (filteredRecipients.isEmpty()) {
            println("NOTIFY: all ${recipients.size} recipients opted out of category=$category")
            return
        }

        // --- Rate limiting (§6.13) ---
        val rateLimitedRecipients = filterByRateLimit(filteredRecipients, category, now)
        if (rateLimitedRecipients.isEmpty()) {
            println("NOTIFY: all ${filteredRecipients.size} recipients rate-limited for category=$category")
            return
        }

        val finalRecipients = rateLimitedRecipients
        dbQuery {
            NotificationsTable.batchInsert(finalRecipients) { uid ->
                this[NotificationsTable.userId] = uid
                this[NotificationsTable.schoolId] = schoolId
                this[NotificationsTable.category] = category
                this[NotificationsTable.title] = title
                this[NotificationsTable.body] = body
                this[NotificationsTable.deepLink] = deepLink
                this[NotificationsTable.actorId] = actorId
                this[NotificationsTable.refType] = refType
                this[NotificationsTable.refId] = refId
                this[NotificationsTable.isRead] = false
                this[NotificationsTable.createdAt] = now
            }
        }

        // Push bridge: fire-and-forget FCM dispatch. In-app notification is
        // already persisted above; push is best-effort and never blocks or
        // crashes the caller. data["type"] = category so the client's
        // FirebaseMessagingService routes to the correct notification channel.
        runCatching {
            pushService.send(SendNotificationRequest(
                title = title,
                body = body,
                userIds = finalRecipients.map { it.toString() },
                deepLink = deepLink,
                data = buildMap {
                    put("type", category)
                    refType?.let { put("refType", it) }
                    refId?.let { put("refId", it) }
                    schoolId?.let { put("schoolId", it.toString()) }
                    actorId?.let { put("actorId", it.toString()) }
                }
            ))
        }.onFailure { e ->
            println("NOTIFY: push bridge failed for ${finalRecipients.size} recipients: ${e.message}")
        }
    }

    // ------------------------------------------------------------------
    // Preferences + Rate limiting
    // ------------------------------------------------------------------

    /** Drop recipients who have disabled notifications for this category. */
    private suspend fun filterByPreferences(
        recipients: List<UUID>,
        category: String
    ): List<UUID> {
        if (recipients.isEmpty()) return emptyList()
        val disabled = dbQuery {
            NotificationPreferencesTable.selectAll()
                .where {
                    (NotificationPreferencesTable.userId inList recipients) and
                    (NotificationPreferencesTable.category eq category) and
                    (NotificationPreferencesTable.enabled eq false)
                }
                .map { it[NotificationPreferencesTable.userId] }
                .toSet()
        }
        return recipients.filter { it !in disabled }
    }

    /** Drop recipients who exceed the per-user daily or per-category hourly limit. */
    private suspend fun filterByRateLimit(
        recipients: List<UUID>,
        category: String,
        now: Instant
    ): List<UUID> {
        if (recipients.isEmpty()) return emptyList()
        val oneHourAgo = now.minus(1, ChronoUnit.HOURS)
        val oneDayAgo = now.minus(1, ChronoUnit.DAYS)

        // Count notifications per user in the last 24h (all categories).
        val dailyCountExpr = NotificationsTable.id.count()
        val dailyCounts = dbQuery {
            NotificationsTable
                .select(NotificationsTable.userId, dailyCountExpr)
                .where {
                    (NotificationsTable.userId inList recipients) and
                    (NotificationsTable.createdAt greater oneDayAgo)
                }
                .groupBy(NotificationsTable.userId)
                .associate { it[NotificationsTable.userId] to it[dailyCountExpr].toInt() }
        }

        // Count notifications per user for this category in the last 1h.
        val hourlyCountExpr = NotificationsTable.id.count()
        val hourlyCounts = dbQuery {
            NotificationsTable
                .select(NotificationsTable.userId, hourlyCountExpr)
                .where {
                    (NotificationsTable.userId inList recipients) and
                    (NotificationsTable.category eq category) and
                    (NotificationsTable.createdAt greater oneHourAgo)
                }
                .groupBy(NotificationsTable.userId)
                .associate { it[NotificationsTable.userId] to it[hourlyCountExpr].toInt() }
        }

        return recipients.filter { uid ->
            val daily = dailyCounts[uid] ?: 0
            val hourly = hourlyCounts[uid] ?: 0
            daily < MAX_PER_USER_PER_DAY && hourly < MAX_PER_CATEGORY_PER_HOUR
        }
    }

    /** Convenience for a single recipient. */
    suspend fun toUser(
        userId: UUID,
        category: String,
        title: String,
        body: String = "",
        schoolId: UUID? = null,
        actorId: UUID? = null,
        deepLink: String? = null,
        refType: String? = null,
        refId: String? = null,
    ) = toUsers(listOf(userId), category, title, body, schoolId, actorId, deepLink, refType, refId)
}

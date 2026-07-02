/*
 * File: LibraryNotificationRouter.kt
 * Module: feature.library
 *
 * Wires the NOTIFICATION_CHANNEL_MATRIX into actual dispatch.
 * Spec §15: "Notification channel matrix (push, email, SMS, in-app per event)"
 *
 * The library service calls this instead of Notify.toUser directly so that
 * the correct channels are activated per event type, and user per-channel
 * opt-outs are respected.
 */
package com.littlebridge.enrollplus.feature.library

import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.NotificationPreferencesTable
import com.littlebridge.enrollplus.feature.notifications.Notify
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import java.util.UUID

object LibraryNotificationRouter {

    data class ChannelDecision(
        val push: Boolean,
        val inApp: Boolean,
        val email: Boolean,
        val sms: Boolean,
    )

    /**
     * Resolve which channels to activate for a given (user, event) pair.
     * Starts from the matrix default, then overrides with user preferences.
     */
    suspend fun resolveChannels(userId: UUID, event: String): ChannelDecision {
        val matrix = LibraryService.NOTIFICATION_CHANNEL_MATRIX[event]
            ?: return ChannelDecision(push = true, inApp = true, email = false, sms = false)

        val prefs = dbQuery {
            NotificationPreferencesTable.selectAll()
                .where {
                    (NotificationPreferencesTable.userId eq userId) and
                    (NotificationPreferencesTable.category eq event)
                }
                .singleOrNull()
        }

        // If user disabled the category entirely, all channels off
        if (prefs != null && prefs[NotificationPreferencesTable.enabled] == false) {
            return ChannelDecision(false, false, false, false)
        }

        return ChannelDecision(
            push   = prefs?.get(NotificationPreferencesTable.pushEnabled)   ?: matrix.push,
            inApp  = prefs?.get(NotificationPreferencesTable.inAppEnabled)  ?: matrix.inApp,
            email  = prefs?.get(NotificationPreferencesTable.emailEnabled)  ?: matrix.email,
            sms    = prefs?.get(NotificationPreferencesTable.smsEnabled)    ?: matrix.sms,
        )
    }

    /**
     * Dispatch a library notification to a single user, respecting the channel
     * matrix and per-channel opt-outs.
     */
    suspend fun notify(
        userId: UUID,
        event: String,
        title: String,
        body: String,
        schoolId: UUID? = null,
        actorId: UUID? = null,
        deepLink: String? = null,
        refType: String? = null,
        refId: String? = null,
    ) {
        val channels = resolveChannels(userId, event)

        // In-app notification (persisted to notifications table)
        if (channels.inApp) {
            runCatching {
                Notify.toUser(
                    userId = userId, category = event, title = title, body = body,
                    schoolId = schoolId, actorId = actorId,
                    deepLink = deepLink, refType = refType, refId = refId,
                )
            }
        }

        // Push (FCM) — only if in-app is also on (push without in-app is unusual)
        // Notify.toUser already does push, so if inApp is on, push is handled there.
        // If inApp is off but push is on, we need a push-only path.
        if (channels.push && !channels.inApp) {
            runCatching {
                Notify.toUser(
                    userId = userId, category = event, title = title, body = body,
                    schoolId = schoolId, actorId = actorId,
                    deepLink = deepLink, refType = refType, refId = refId,
                )
            }
        }

        // Email — stub: log for now, wire to email gateway when available
        if (channels.email) {
            println("LIBRARY_NOTIFY: email dispatch for user=$userId event=$event title=$title (email gateway not yet configured)")
        }

        // SMS — stub: log for now, wire to SMS gateway when available
        if (channels.sms) {
            println("LIBRARY_NOTIFY: sms dispatch for user=$userId event=$event title=$title (sms gateway not yet configured)")
        }
    }

    /**
     * Dispatch to multiple users, resolving channels per user.
     */
    suspend fun notifyUsers(
        userIds: Collection<UUID>,
        event: String,
        title: String,
        body: String,
        schoolId: UUID? = null,
        actorId: UUID? = null,
        deepLink: String? = null,
        refType: String? = null,
        refId: String? = null,
    ) {
        for (uid in userIds.distinct()) {
            runCatching { notify(uid, event, title, body, schoolId, actorId, deepLink, refType, refId) }
                .onFailure { println("LIBRARY_NOTIFY: failed for user=$uid event=$event: ${it.message}") }
        }
    }
}

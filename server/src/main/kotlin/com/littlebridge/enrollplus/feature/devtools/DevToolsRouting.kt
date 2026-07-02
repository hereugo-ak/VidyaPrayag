/*
 * File: DevToolsRouting.kt
 * Module: feature.devtools
 *
 * Super-admin-only endpoints for developer operations: changing OTP provider
 * at runtime, manually triggering weekly pulse generation, and sending
 * ad-hoc notifications to any user from the web dashboard.
 *
 * All endpoints require the authenticated user's role (read from the DB, not
 * the JWT claim) to be "super_admin". Any other role gets 403.
 *
 * Endpoints
 * ---------
 *   GET  /api/v1/admin/dev/otp-providers
 *     → lists all known OTP providers with their configured status and the
 *       current runtime override (if any).
 *
 *   PUT  /api/v1/admin/dev/otp-provider
 *     body: { "provider": "fast2sms" | "msg91" | "twilio" | "whatsapp_cloud" | "smtp" | "console" | "auto" }
 *     → sets a runtime override for OTP_PROVIDER stored in app_config. Pass
 *       "auto" to clear the override and fall back to the env var chain.
 *
 *   POST /api/v1/admin/dev/trigger-pulse
 *     → immediately runs the weekly pulse batch generation for the current
 *       week and sends push notifications to all parents who receive a pulse.
 *
 *   POST /api/v1/admin/dev/send-notification
 *     body: { "user_id": "...", "title": "...", "body": "...", "deep_link"?: "...", "category"?: "..." }
 *     → sends an in-app + push notification to a single user via Notify.toUser.
 *
 *   POST /api/v1/admin/dev/trigger-pews
 *     → immediately runs the PEWS pipeline (Sense → Reason → Act) for all
 *       active schools and returns the count of schools processed + at-risk
 *       snapshots found.
 */
package com.littlebridge.enrollplus.feature.devtools

import com.littlebridge.enrollplus.core.fail
import com.littlebridge.enrollplus.core.ok
import com.littlebridge.enrollplus.core.principalUserUuid
import com.littlebridge.enrollplus.db.AppConfigTable
import com.littlebridge.enrollplus.db.AppUsersTable
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.feature.auth.delivery.OtpDeliveryDispatcher
import com.littlebridge.enrollplus.feature.auth.delivery.OtpEnv
import com.littlebridge.enrollplus.feature.notifications.Notify
import com.littlebridge.enrollplus.feature.pulse.ParentPulseService
import com.littlebridge.enrollplus.feature.pulse.PulseWeeklyJob
import com.littlebridge.enrollplus.feature.pews.PewsDailyJob
import com.littlebridge.enrollplus.feature.pews.PewsSnapshotService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import io.ktor.server.request.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

// ── DTOs ──────────────────────────────────────────────────────────────────

@Serializable
data class OtpProviderInfo(
    val name: String,
    val channel: String,
    val configured: Boolean,
)

@Serializable
data class OtpProvidersResponse(
    val providers: List<OtpProviderInfo>,
    val envPinnedProvider: String,
    val runtimeOverride: String?,
    val effectiveProvider: String,
)

@Serializable
data class UpdateOtpProviderRequest(
    val provider: String,
)

@Serializable
data class UpdateOtpProviderResponse(
    val provider: String,
    val isOverride: Boolean,
)

@Serializable
data class TriggerPulseResponse(
    val weekStart: String,
    val pulsesGenerated: Int,
)

@Serializable
data class DevSendNotificationRequest(
    val user_id: String,
    val title: String,
    val body: String,
    val deep_link: String? = null,
    val category: String = "dev_tools",
    val school_id: String? = null,
)

@Serializable
data class DevSendNotificationResponse(
    val sent: Boolean,
)

@Serializable
data class TriggerPewsResponse(
    val schools_processed: Int,
    val at_risk_count: Int,
)

// ── Guard ──────────────────────────────────────────────────────────────────

private suspend fun ApplicationCall.requireSuperAdmin(): UUID? {
    val uid = principalUserUuid() ?: run {
        fail("Invalid token", HttpStatusCode.Unauthorized, "UNAUTHORIZED")
        return null
    }
    val role = dbQuery {
        AppUsersTable.selectAll().where { AppUsersTable.id eq uid }
            .singleOrNull()?.get(AppUsersTable.role)
    }
    if (role == null) {
        fail("User not found", HttpStatusCode.NotFound, "USER_NOT_FOUND")
        return null
    }
    if (role != "super_admin") {
        fail("This action requires a super admin account.", HttpStatusCode.Forbidden, "SUPER_ADMIN_REQUIRED")
        return null
    }
    return uid
}

// ── AppConfig helpers ──────────────────────────────────────────────────────

private const val OTP_PROVIDER_OVERRIDE_KEY = "otp_provider_override"

private suspend fun readOtpProviderOverride(): String? = dbQuery {
    AppConfigTable.selectAll()
        .where { AppConfigTable.key eq OTP_PROVIDER_OVERRIDE_KEY }
        .singleOrNull()
        ?.get(AppConfigTable.value)
        ?.takeIf { it.isNotBlank() }
}

private suspend fun writeOtpProviderOverride(value: String) = dbQuery {
    val existing = AppConfigTable.selectAll()
        .where { AppConfigTable.key eq OTP_PROVIDER_OVERRIDE_KEY }
        .singleOrNull()
    val now = Instant.now()
    if (existing != null) {
        AppConfigTable.update({ AppConfigTable.key eq OTP_PROVIDER_OVERRIDE_KEY }) {
            it[AppConfigTable.value] = value
            it[AppConfigTable.updatedAt] = now
        }
    } else {
        AppConfigTable.insert {
            it[AppConfigTable.key] = OTP_PROVIDER_OVERRIDE_KEY
            it[AppConfigTable.value] = value
            it[AppConfigTable.updatedAt] = now
        }
    }
}

private suspend fun clearOtpProviderOverride() = dbQuery {
    AppConfigTable.deleteWhere { AppConfigTable.key eq OTP_PROVIDER_OVERRIDE_KEY }
}

// ── Routing ────────────────────────────────────────────────────────────────

fun Route.devToolsRouting() {
    authenticate("jwt") {
        route("/api/v1/admin/dev") {

        // ----- OTP providers list + current config -----
        get("/otp-providers") {
            if (call.requireSuperAdmin() == null) return@get

            val providers = OtpDeliveryDispatcher.knownProviders.map {
                OtpProviderInfo(
                    name = it.name,
                    channel = it.channel.wireName,
                    configured = runCatching { it.isConfigured() }.getOrDefault(false),
                )
            }
            val envPinned = OtpEnv.get("OTP_PROVIDER") ?: ""
            val runtimeOverride = readOtpProviderOverride()
            val effective = runtimeOverride ?: envPinned

            call.ok(
                OtpProvidersResponse(
                    providers = providers,
                    envPinnedProvider = envPinned,
                    runtimeOverride = runtimeOverride,
                    effectiveProvider = effective,
                ),
                message = "OTP provider configuration",
            )
        }

        // ----- Change OTP provider at runtime -----
        put("/otp-provider") {
            if (call.requireSuperAdmin() == null) return@put

            val body = runCatching { call.receive<UpdateOtpProviderRequest>() }.getOrNull()
                ?: run { call.fail("Invalid body: { \"provider\": \"...\" }"); return@put }

            val requested = body.provider.trim().lowercase()
            if (requested.isEmpty()) {
                call.fail("provider is required"); return@put
            }

            if (requested == "auto" || requested == "chain" || requested == "default") {
                clearOtpProviderOverride()
                call.ok(
                    UpdateOtpProviderResponse(provider = "auto", isOverride = false),
                    message = "OTP provider override cleared — using env chain",
                )
                return@put
            }

            val known = OtpDeliveryDispatcher.knownProviders.any { it.name.equals(requested, ignoreCase = true) }
            if (!known) {
                call.fail(
                    "Unknown provider '$requested'. Known: ${OtpDeliveryDispatcher.knownProviders.joinToString(", ") { it.name }}",
                    HttpStatusCode.BadRequest,
                    "UNKNOWN_PROVIDER",
                )
                return@put
            }

            writeOtpProviderOverride(requested)
            call.ok(
                UpdateOtpProviderResponse(provider = requested, isOverride = true),
                message = "OTP provider override set to '$requested'",
            )
        }

        // ----- Manually trigger weekly pulse generation -----
        post("/trigger-pulse") {
            if (call.requireSuperAdmin() == null) return@post

            val weekStart = ParentPulseService.currentWeekStart()
            val count = PulseWeeklyJob.runNow(weekStart)

            call.ok(
                TriggerPulseResponse(
                    weekStart = weekStart.toString(),
                    pulsesGenerated = count,
                ),
                message = "Pulse generation triggered for week $weekStart",
            )
        }

        // ----- Send ad-hoc notification to a user -----
        post("/send-notification") {
            if (call.requireSuperAdmin() == null) return@post

            val body = runCatching { call.receive<DevSendNotificationRequest>() }.getOrNull()
                ?: run { call.fail("Invalid body"); return@post }

            if (body.user_id.isBlank()) {
                call.fail("user_id is required"); return@post
            }
            if (body.title.isBlank()) {
                call.fail("title is required"); return@post
            }
            if (body.body.isBlank()) {
                call.fail("body is required"); return@post
            }

            val userId = runCatching { UUID.fromString(body.user_id) }.getOrNull()
                ?: run { call.fail("Invalid user_id format"); return@post }

            val schoolId = body.school_id?.let { runCatching { UUID.fromString(it) }.getOrNull() }

            runCatching {
                Notify.toUser(
                    userId = userId,
                    category = body.category,
                    title = body.title,
                    body = body.body,
                    schoolId = schoolId,
                    deepLink = body.deep_link,
                    refType = "dev_tools",
                    refId = null,
                )
            }.onFailure {
                call.fail("Failed to send notification: ${it.message}", HttpStatusCode.InternalServerError, "NOTIFY_FAILED")
                return@post
            }

            call.ok(
                DevSendNotificationResponse(sent = true),
                message = "Notification sent to user $userId",
            )
        }

        // ----- Manually trigger PEWS pipeline for all schools -----
        post("/trigger-pews") {
            if (call.requireSuperAdmin() == null) return@post

            val runDate = java.time.LocalDate.now()
            val schoolIds = PewsSnapshotService().activeSchoolIds()
            val atRiskCount = PewsDailyJob.runAll(runDate)

            call.ok(
                TriggerPewsResponse(
                    schools_processed = schoolIds.size,
                    at_risk_count = atRiskCount,
                ),
                message = "PEWS pipeline triggered — $atRiskCount at-risk snapshots across ${schoolIds.size} schools",
            )
        }
        }
    }
}

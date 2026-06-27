/*
 * File: PulseRouting.kt
 * Module: feature.pulse
 *
 * API endpoints for the Parent Pulse feature (PARENT_PULSE_SPEC.md §6).
 *
 *   GET /api/v1/parent/pulse/latest/{childId}    — latest pulse for a child
 *   GET /api/v1/parent/pulse/history/{childId}   — pulse history (last 12 weeks)
 *
 * Both endpoints require JWT authentication. The childId is the children.id
 * (the parent's child record), validated against the authenticated parent.
 */
package com.littlebridge.enrollplus.feature.pulse

import com.littlebridge.enrollplus.core.fail
import com.littlebridge.enrollplus.core.ok
import com.littlebridge.enrollplus.core.principalUserId
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class PulseHistoryResponse(
    val pulses: List<PulseDto>,
)

fun Route.pulseRouting() {
    authenticate("jwt") {
        route("/api/v1/parent/pulse") {

            // ---- GET /latest/{childId} ----
            get("/latest/{childId}") {
                val uid = call.principalUserId()?.let { runCatching { UUID.fromString(it) }.getOrNull() } ?: run {
                    call.fail("Invalid token", HttpStatusCode.Unauthorized); return@get
                }
                val childId = call.parameters["childId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() } ?: run {
                    call.fail("Invalid child id"); return@get
                }

                val service = ParentPulseService()
                val pulse = service.getLatestPulse(uid, childId)
                if (pulse == null) {
                    call.fail("No pulse available yet", HttpStatusCode.NotFound, "PULSE_NOT_FOUND")
                } else {
                    call.ok(pulse, "Latest pulse")
                }
            }

            // ---- GET /history/{childId}?weeks=12 ----
            get("/history/{childId}") {
                val uid = call.principalUserId()?.let { runCatching { UUID.fromString(it) }.getOrNull() } ?: run {
                    call.fail("Invalid token", HttpStatusCode.Unauthorized); return@get
                }
                val childId = call.parameters["childId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() } ?: run {
                    call.fail("Invalid child id"); return@get
                }
                val weeks = (call.request.queryParameters["weeks"]?.toIntOrNull() ?: 12).coerceIn(1, 52)

                val service = ParentPulseService()
                val pulses = service.getPulseHistory(uid, childId, weeks)
                call.ok(PulseHistoryResponse(pulses = pulses), "Pulse history (${pulses.size} weeks)")
            }
        }
    }
}

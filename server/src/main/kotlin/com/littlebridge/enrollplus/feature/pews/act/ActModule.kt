/*
 * File: ActModule.kt
 * Module: feature.pews.act
 *
 * PEWS 2.0 — Tier 3 Act Module.
 *
 * Owns managed casework: sequenced plans, SLA, escalation ladder,
 * follow-up checkpoints, calendar-aware suppression, and the
 * one-tap parent draft endpoint.
 *
 * Kill-switched under module name "act".
 *
 * Spec: PEWS_2.0_AGENTIC_REDESIGN.md §7
 */
package com.littlebridge.enrollplus.feature.pews.act

import com.littlebridge.enrollplus.core.fail
import com.littlebridge.enrollplus.core.ok
import com.littlebridge.enrollplus.core.requireTeacherContext
import com.littlebridge.enrollplus.feature.pews.core.PEWSModule
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.util.UUID

object ActModule : PEWSModule {
    override val moduleName = "act"
    private val log = LoggerFactory.getLogger("ActModule")

    val caseworkService = ManagedCaseworkService()
    val parentDraftService = ParentDraftService()

    override fun registerRoutes(parent: Route) {
        log.info("ActModule registered (kill-switch: {})", moduleName)

        parent.authenticate("jwt") {
            route("/api/v1/teacher/pews/interventions") {

                // One-tap parent draft generation
                post("/{id}/draft-message") {
                    val ctx = call.requireTeacherContext() ?: return@post
                    val interventionId = call.parameters["id"]?.let {
                        runCatching { UUID.fromString(it) }.getOrNull()
                    } ?: run { call.fail("invalid intervention id"); return@post }

                    val language = call.request.queryParameters["lang"] ?: "hi"

                    val result = parentDraftService.generateDraft(ctx.schoolId, interventionId, language)
                    if (result.ok) {
                        call.ok(
                            DraftMessageResponse(language = result.language, body = result.body ?: ""),
                            "Parent draft generated"
                        )
                    } else {
                        call.fail(result.errorMessage ?: "draft generation failed")
                    }
                }

                // Send parent message via messaging system + mark intervention done
                post("/{id}/send-parent-message") {
                    val ctx = call.requireTeacherContext() ?: return@post
                    val interventionId = call.parameters["id"]?.let {
                        runCatching { UUID.fromString(it) }.getOrNull()
                    } ?: run { call.fail("invalid intervention id"); return@post }

                    val result = parentDraftService.sendParentMessage(
                        schoolId = ctx.schoolId,
                        interventionId = interventionId,
                        senderId = ctx.userId,
                        senderName = ctx.fullName,
                    )
                    if (result.ok) {
                        call.ok(
                            SendParentMessageResponse(sent_count = result.sentCount),
                            "Message sent to ${result.sentCount} parent(s)"
                        )
                    } else {
                        call.fail(result.errorMessage ?: "failed to send message")
                    }
                }
            }
        }
    }
}

@Serializable
data class DraftMessageResponse(
    val language: String,
    val body: String,
)

@Serializable
data class SendParentMessageResponse(
    @SerialName("sent_count") val sentCount: Int,
)

/*
 * File: LearnModule.kt
 * Module: feature.pews.learn
 *
 * PEWS 2.0 — Tier 4 Learn Module.
 *
 * Owns the flywheel: auto-outcome measurement, effectiveness priors,
 * and the prescriptive admin effectiveness view.
 *
 * Kill-switched under module name "learn".
 *
 * Spec: PEWS_2.0_AGENTIC_REDESIGN.md §8
 */
package com.littlebridge.enrollplus.feature.pews.learn

import com.littlebridge.enrollplus.core.fail
import com.littlebridge.enrollplus.core.ok
import com.littlebridge.enrollplus.core.requireSchoolAdmin
import com.littlebridge.enrollplus.feature.pews.core.PEWSModule
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.slf4j.LoggerFactory

object LearnModule : PEWSModule {
    override val moduleName = "learn"
    private val log = LoggerFactory.getLogger("LearnModule")

    val service = LearnService()

    override fun registerRoutes(parent: Route) {
        log.info("LearnModule registered (kill-switch: {})", moduleName)

        parent.authenticate("jwt") {
            route("/api/v1/school/pews/effectiveness") {

                // Prescriptive effectiveness view for admins
                get {
                    val ctx = call.requireSchoolAdmin() ?: return@get
                    val result = runCatching { service.prescriptiveEffectiveness(ctx.schoolId) }
                        .onFailure { log.warn("Effectiveness view failed: {}", it.message) }
                        .getOrNull()

                    if (result != null) {
                        call.ok(result, "Effectiveness report")
                    } else {
                        call.fail("Failed to generate effectiveness report")
                    }
                }

                // Rebuild priors from scratch (admin action)
                post("/rebuild") {
                    val ctx = call.requireSchoolAdmin() ?: return@post
                    val count = runCatching { service.rebuildPriors(ctx.schoolId) }
                        .onFailure { log.warn("Rebuild priors failed: {}", it.message) }
                        .getOrNull()

                    if (count != null) {
                        call.ok(mapOf("priors_rebuilt" to count), "Rebuilt $count effectiveness priors")
                    } else {
                        call.fail("Failed to rebuild priors")
                    }
                }
            }
        }
    }
}

package com.littlebridge.enrollplus.feature.pews.triage

import com.littlebridge.enrollplus.feature.pews.core.PEWSModule
import io.ktor.server.routing.Route
import org.slf4j.LoggerFactory

/**
 * PEWS 2.0 Triage Module — Tier 1.
 *
 * Owns the CLASSIFY prefilter and cohort dedup logic. Takes the full
 * at-risk cohort from Sense and filters it down to the cases that need
 * deep Tier-2 caseworker review.
 *
 * Kill-switched under module name "triage".
 */
object TriageModule : PEWSModule {
    override val moduleName = "triage"
    private val log = LoggerFactory.getLogger("TriageModule")
    val service = TriageService()

    override fun registerRoutes(parent: Route) {
        // Triage is invoked internally by the PEWS pipeline, not exposed
        // as a direct API endpoint. Route registration is a no-op for now.
        log.info("TriageModule registered (kill-switch: {})", moduleName)
    }
}

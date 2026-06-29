package com.littlebridge.enrollplus.feature.pews.caseworker

import com.littlebridge.enrollplus.feature.pews.core.PEWSModule
import io.ktor.server.routing.Route
import org.slf4j.LoggerFactory

/**
 * PEWS 2.0 Caseworker Module — Tier 2.
 *
 * Owns the tool-using caseworker agent that produces structured Case Files
 * for students flagged by Triage as needing a deep look.
 *
 * Kill-switched under module name "caseworker".
 */
object CaseworkerModule : PEWSModule {
    override val moduleName = "caseworker"
    private val log = LoggerFactory.getLogger("CaseworkerModule")
    val service = CaseworkerService()

    override fun registerRoutes(parent: Route) {
        // Caseworker is invoked internally by the PEWS pipeline, not exposed
        // as a direct API endpoint. Route registration is a no-op for now.
        log.info("CaseworkerModule registered (kill-switch: {})", moduleName)
    }
}

// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/reportcard/triage/TriageModule.kt
package com.littlebridge.enrollplus.feature.reportcard.triage

import com.littlebridge.enrollplus.feature.reportcard.core.ReportCardConstants
import com.littlebridge.enrollplus.feature.reportcard.core.ReportCardModule
import io.ktor.server.routing.Route
import org.slf4j.LoggerFactory

/**
 * Module registration for Tier 1 — Triage / Cohort-dedup.
 *
 * The triage service is called internally by the batch job; it does not
 * expose its own HTTP routes. This module exists for kill-switch registration
 * and to expose the service singleton.
 *
 * Kill-switched under module name "reportcard_triage".
 *
 * SOLID: S (one responsibility: triage/cohort-dedup).
 */
object TriageModule : ReportCardModule {
    override val moduleName = ReportCardConstants.MODULE_TRIAGE
    private val log = LoggerFactory.getLogger("TriageModule")

    val service = ReportTriageService()

    override fun registerRoutes(parent: Route) {
        log.info("TriageModule registered (kill-switch: {})", moduleName)
        // No HTTP routes — triage is called internally by the batch job.
    }
}

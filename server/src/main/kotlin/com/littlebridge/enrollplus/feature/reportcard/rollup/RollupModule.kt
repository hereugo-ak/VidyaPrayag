// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/reportcard/rollup/RollupModule.kt
package com.littlebridge.enrollplus.feature.reportcard.rollup

import com.littlebridge.enrollplus.feature.reportcard.core.ReportCardModule
import com.littlebridge.enrollplus.feature.reportcard.core.ReportCardConstants
import io.ktor.server.routing.Route
import org.slf4j.LoggerFactory

/**
 * Module registration for Tier 0 — Rollup.
 *
 * The rollup service is called internally by the assembly/batch job; it does
 * not expose its own HTTP routes. This module exists for kill-switch
 * registration and to expose the service singleton.
 *
 * Kill-switched under module name "reportcard_rollup".
 *
 * SOLID: S (one responsibility: deterministic rollup), D (repositories injected).
 */
object RollupModule : ReportCardModule {
    override val moduleName = ReportCardConstants.MODULE_ROLLUP
    private val log = LoggerFactory.getLogger("RollupModule")

    val service = ReportRollupService()

    override fun registerRoutes(parent: Route) {
        log.info("RollupModule registered (kill-switch: {})", moduleName)
        // No HTTP routes — rollup is called internally by the batch job.
    }
}

// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/reportcard/narrator/NarratorModule.kt
package com.littlebridge.enrollplus.feature.reportcard.narrator

import com.littlebridge.enrollplus.feature.reportcard.core.ReportCardConstants
import com.littlebridge.enrollplus.feature.reportcard.core.ReportCardModule
import io.ktor.server.routing.Route
import org.slf4j.LoggerFactory

/**
 * Module registration for Tier 2 — Narrator Agent.
 *
 * The narrator service is called internally by the batch job; it does not
 * expose its own HTTP routes. This module exists for kill-switch registration
 * and to expose the service singleton.
 *
 * Kill-switched under module name "reportcard_narrator".
 *
 * SOLID: S (one responsibility: narration), D (AiService + tools + guard injected).
 */
object NarratorModule : ReportCardModule {
    override val moduleName = ReportCardConstants.MODULE_NARRATOR
    private val log = LoggerFactory.getLogger("NarratorModule")

    val service = NarratorService()

    override fun registerRoutes(parent: Route) {
        log.info("NarratorModule registered (kill-switch: {})", moduleName)
        // No HTTP routes — narrator is called internally by the batch job.
    }
}

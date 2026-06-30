// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/reportcard/assemble/AssembleModule.kt
package com.littlebridge.enrollplus.feature.reportcard.assemble

import com.littlebridge.enrollplus.feature.reportcard.core.ReportCardConstants
import com.littlebridge.enrollplus.feature.reportcard.core.ReportCardModule
import io.ktor.server.routing.Route
import org.slf4j.LoggerFactory

/**
 * Module registration for Tier 3 — Assembly & Publish.
 *
 * Exposes HTTP routes for batch generation, review queue, edit, approve,
 * and publish. Kill-switched under module name "reportcard_assemble".
 *
 * SOLID: S (one responsibility: assembly & publish), D (service injected).
 */
object AssembleModule : ReportCardModule {
    override val moduleName = ReportCardConstants.MODULE_ASSEMBLE
    private val log = LoggerFactory.getLogger("AssembleModule")

    val service = ReportAssemblyService()

    override fun registerRoutes(parent: Route) {
        parent.assembleRouting(service)
        log.info("AssembleModule routes registered (kill-switch: {})", moduleName)
    }
}

// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/reportcard/core/ReportCardRouter.kt
package com.littlebridge.enrollplus.feature.reportcard.core

import com.littlebridge.enrollplus.feature.reportcard.assemble.AssembleModule
import com.littlebridge.enrollplus.feature.reportcard.ecosystem.ecosystemRouting
import com.littlebridge.enrollplus.feature.reportcard.learn.LearnModule
import com.littlebridge.enrollplus.feature.reportcard.narrator.NarratorModule
import com.littlebridge.enrollplus.feature.reportcard.rollup.RollupModule
import com.littlebridge.enrollplus.feature.reportcard.triage.TriageModule
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import org.slf4j.LoggerFactory

/**
 * Aggregator that registers all AI Report Card 2.0 modules with the
 * [ReportCardModuleRegistry] and mounts their routes.
 *
 * Mirrors [com.littlebridge.enrollplus.feature.pews.core.PEWSRouter] but for
 * report card modules. Called from [Application.module] at boot time.
 *
 * SOLID:
 *   O → New modules added by registering here (or self-registering) — no
 *        changes needed to existing module code.
 *   D → Depends on [ReportCardModule] abstraction via the registry.
 */
private val log = LoggerFactory.getLogger("ReportCardRouter")

/** Register all report card modules with the registry. Call at boot time. */
fun registerReportCardModules() {
    log.info("Registering AI Report Card 2.0 modules…")
    ReportCardModuleRegistry.register(RollupModule)
    ReportCardModuleRegistry.register(TriageModule)
    ReportCardModuleRegistry.register(NarratorModule)
    ReportCardModuleRegistry.register(AssembleModule)
    ReportCardModuleRegistry.register(LearnModule)
    log.info("AI Report Card 2.0 modules registered: {}", ReportCardModuleRegistry.all().size)
}

/** Mount all registered report card module routes under /api/v1. */
fun Route.reportCardRouting() {
    route("/api/v1") {
        ReportCardModuleRegistry.installRoutes(this)
        ecosystemRouting()
    }
    log.info("AI Report Card 2.0 routes mounted (incl. ecosystem)")
}

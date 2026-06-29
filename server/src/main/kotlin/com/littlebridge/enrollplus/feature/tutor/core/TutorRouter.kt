// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/tutor/core/TutorRouter.kt
package com.littlebridge.enrollplus.feature.tutor.core

import com.littlebridge.enrollplus.feature.tutor.agent.AgentModule
import com.littlebridge.enrollplus.feature.tutor.sense.SenseModule
import com.littlebridge.enrollplus.feature.tutor.triage.TriageModule
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import org.slf4j.LoggerFactory

/**
 * Boot-time wiring: register all AI Tutor 2.0 modules with the
 * [TutorModuleRegistry] and mount their routes.
 *
 * Mirrors [com.littlebridge.enrollplus.feature.reportcard.core.ReportCardRouter]
 * but for AI Tutor modules. Called from [Application.module] at boot time.
 *
 * To add a new module: implement [TutorModule] and add one line in
 * [registerTutorModules]. This is the ONLY place module wiring is centralized —
 * modules themselves don't know about each other (SOLID: Dependency Inversion).
 *
 * SOLID:
 *   O → New modules added by registering here — no changes to existing code.
 *   D → Depends on [TutorModule] abstraction via the registry.
 */
private val log = LoggerFactory.getLogger("TutorRouter")

/**
 * Register all AI Tutor 2.0 modules with the registry. Call at boot time.
 *
 * Modules are registered incrementally as ticks are completed. Currently
 * no modules are registered — they will be added in TICK 03+ as each
 * vertical slice is built. The router and registry are ready now.
 */
fun registerTutorModules() {
    log.info("Registering AI Tutor 2.0 modules…")
    TutorModuleRegistry.register(SenseModule)
    TutorModuleRegistry.register(TriageModule)
    TutorModuleRegistry.register(AgentModule)
    // Modules will be registered here as they are built in TICK 06+:
    //   TutorModuleRegistry.register(ActModule)
    //   TutorModuleRegistry.register(LearnModule)
    //   TutorModuleRegistry.register(IngestModule)
    //   TutorModuleRegistry.register(TeacherHeatmapModule)
    //   TutorModuleRegistry.register(ParentProgressModule)
    //   TutorModuleRegistry.register(AdminEfficacyModule)
    //   TutorModuleRegistry.register(RagModule)
    log.info("AI Tutor 2.0 modules registered: {}", TutorModuleRegistry.all().size)
}

/** Mount all registered AI Tutor module routes under /api/v1. */
fun Route.tutorRouting() {
    route("/api/v1") {
        TutorModuleRegistry.installRoutes(this)
    }
    log.info("AI Tutor 2.0 routes mounted")
}

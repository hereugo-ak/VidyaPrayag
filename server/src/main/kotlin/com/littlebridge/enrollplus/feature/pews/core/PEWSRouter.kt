// FILE: src/main/kotlin/com/littlebridge/enrollplus/feature/pews/core/PEWSRouter.kt
package com.littlebridge.enrollplus.feature.pews.core

import com.littlebridge.enrollplus.feature.pews.act.ActModule
import com.littlebridge.enrollplus.feature.pews.caseworker.CaseworkerModule
import com.littlebridge.enrollplus.feature.pews.insights.MockInsightsModule
import com.littlebridge.enrollplus.feature.pews.learn.LearnModule
import com.littlebridge.enrollplus.feature.pews.sense.SenseModule
import com.littlebridge.enrollplus.feature.pews.triage.TriageModule
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("PEWSRouter")

/**
 * Boot-time wiring: register all PEWS 2.0 modules with the [ModuleRegistry].
 * Called once at startup before [pewsModuleRouting] mounts the routes.
 *
 * To add a new module: implement [PEWSModule] and add one line here.
 * This is the ONLY place module wiring is centralized — modules themselves
 * don't know about each other (SOLID: Dependency Inversion).
 *
 * The 5 core PEWS 2.0 modules:
 *   1. Sense     — Tier 0: deterministic 15-signal feature vector
 *   2. Triage    — Tier 1: CLASSIFY prefilter + cohort dedup
 *   3. Caseworker — Tier 2: tool-using agent + Case File + grounding
 *   4. Act       — Tier 3: managed casework, SLA, escalation, parent draft
 *   5. Learn     — Tier 4: flywheel, auto-outcomes, effectiveness priors
 */
fun registerPewsModules() {
    ModuleRegistry.register(SenseModule)
    ModuleRegistry.register(TriageModule)
    ModuleRegistry.register(CaseworkerModule)
    ModuleRegistry.register(ActModule)
    ModuleRegistry.register(LearnModule)
    ModuleRegistry.register(MockInsightsModule)
    log.info("PEWS 2.0: {} modules registered", ModuleRegistry.all().size)
}

/**
 * Aggregator that mounts all registered PEWS 2.0 module routes in one call.
 * Replaces the v1 `pewsRouting()` function — instead of hard-wiring every
 * route in a single function, modules self-register via [ModuleRegistry]
 * and this function simply calls [ModuleRegistry.installAll].
 *
 * Usage in Application.kt:
 * ```
 * routing {
 *     pewsModuleRouting()
 * }
 * ```
 *
 * SOLID:
 *   - O (Open/Closed): adding a module requires zero changes here.
 *   - D (Dependency Inversion): depends on [ModuleRegistry] (abstraction),
 *     not on any concrete module.
 */
fun Route.pewsModuleRouting() {
    authenticate("jwt") {
        ModuleRegistry.installAll(this)
    }
}

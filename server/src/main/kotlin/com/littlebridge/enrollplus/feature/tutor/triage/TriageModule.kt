// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/tutor/triage/TriageModule.kt
package com.littlebridge.enrollplus.feature.tutor.triage

import com.littlebridge.enrollplus.feature.tutor.core.TutorConstants
import com.littlebridge.enrollplus.feature.tutor.core.TutorModule
import io.ktor.server.routing.Route

/**
 * Module singleton for Tier 1 — Triage.
 *
 * Registers the triage route under JWT auth.
 *
 * SOLID:
 *   S → One responsibility: wiring the triage tier's routes.
 *   D → Depends on [TutorModule] abstraction.
 *
 * Spec: AI_TUTOR_2.0_AGENTIC_REDESIGN.md §6.1
 */
object TriageModule : TutorModule {
    override val moduleName: String = TutorConstants.MODULE_TRIAGE

    override fun registerRoutes(parent: Route) {
        parent.triageRouting()
    }
}

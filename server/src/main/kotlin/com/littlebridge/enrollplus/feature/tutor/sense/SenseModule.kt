// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/tutor/sense/SenseModule.kt
package com.littlebridge.enrollplus.feature.tutor.sense

import com.littlebridge.enrollplus.feature.tutor.core.TutorConstants
import com.littlebridge.enrollplus.feature.tutor.core.TutorModule
import io.ktor.server.routing.Route

/**
 * Module singleton for Tier 0 — Sense (LearnerBundleBuilder).
 *
 * Registers the learner-bundle route under JWT auth.
 *
 * SOLID:
 *   S → One responsibility: wiring the sense tier's routes.
 *   O → New sense-tier routes added here without touching other modules.
 *   D → Depends on [TutorModule] abstraction.
 *
 * Spec: AI_TUTOR_2.0_AGENTIC_REDESIGN.md §5
 */
object SenseModule : TutorModule {
    override val moduleName: String = TutorConstants.MODULE_SENSE

    override fun registerRoutes(parent: Route) {
        parent.senseRouting()
    }
}

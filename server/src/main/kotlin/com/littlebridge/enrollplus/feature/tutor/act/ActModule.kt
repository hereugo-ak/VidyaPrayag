// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/tutor/act/ActModule.kt
package com.littlebridge.enrollplus.feature.tutor.act

import com.littlebridge.enrollplus.feature.tutor.core.TutorConstants
import com.littlebridge.enrollplus.feature.tutor.core.TutorModule
import io.ktor.server.routing.Route

/**
 * Module singleton for Tier 3 — Act.
 *
 * Registers the practice (auto-grade) and plan (FSRS review) routes.
 *
 * SOLID:
 *   S → One responsibility: wiring the act tier's routes.
 *   D → Depends on [TutorModule] abstraction.
 *
 * Spec: AI_TUTOR_2.0_AGENTIC_REDESIGN.md §7
 */
object ActModule : TutorModule {
    override val moduleName: String = TutorConstants.MODULE_ACT

    override fun registerRoutes(parent: Route) {
        parent.actRouting()
    }
}

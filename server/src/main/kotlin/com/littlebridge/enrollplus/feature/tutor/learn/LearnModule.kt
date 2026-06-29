// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/tutor/learn\LearnModule.kt
package com.littlebridge.enrollplus.feature.tutor.learn

import com.littlebridge.enrollplus.feature.tutor.core.TutorConstants
import com.littlebridge.enrollplus.feature.tutor.core.TutorModule
import io.ktor.server.routing.Route

/**
 * Module singleton for Tier 4 — Learn.
 *
 * Registers the efficacy route.
 *
 * SOLID:
 *   S → One responsibility: wiring the learn tier's routes.
 *   D → Depends on [TutorModule] abstraction.
 *
 * Spec: AI_TUTOR_2.0_AGENTIC_REDESIGN.md §8
 */
object LearnModule : TutorModule {
    override val moduleName: String = TutorConstants.MODULE_LEARN

    override fun registerRoutes(parent: Route) {
        parent.learnRouting()
    }
}

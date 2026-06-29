// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/tutor/parent/ParentProgressModule.kt
package com.littlebridge.enrollplus.feature.tutor.parent

import com.littlebridge.enrollplus.feature.tutor.core.TutorConstants
import com.littlebridge.enrollplus.feature.tutor.core.TutorModule
import io.ktor.server.routing.Route

/**
 * Module singleton for Parent Progress (cross-role).
 *
 * Spec: AI_TUTOR_2.0_AGENTIC_REDESIGN.md §10 (Parent progress card)
 */
object ParentProgressModule : TutorModule {
    override val moduleName: String = TutorConstants.MODULE_PARENT_PROGRESS

    override fun registerRoutes(parent: Route) {
        parent.parentProgressRouting()
    }
}

// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/tutor/admin/AdminEfficacyModule.kt
package com.littlebridge.enrollplus.feature.tutor.admin

import com.littlebridge.enrollplus.feature.tutor.core.TutorConstants
import com.littlebridge.enrollplus.feature.tutor.core.TutorModule
import io.ktor.server.routing.Route

/**
 * Module singleton for Admin Efficacy (cross-role).
 *
 * Spec: AI_TUTOR_2.0_AGENTIC_REDESIGN.md §10 (Admin efficacy view)
 */
object AdminEfficacyModule : TutorModule {
    override val moduleName: String = TutorConstants.MODULE_ADMIN_EFFICACY

    override fun registerRoutes(parent: Route) {
        parent.adminEfficacyRouting()
    }
}

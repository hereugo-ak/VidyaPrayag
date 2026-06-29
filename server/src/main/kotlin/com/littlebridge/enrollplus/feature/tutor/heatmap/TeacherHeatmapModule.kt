// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/tutor/heatmap/TeacherHeatmapModule.kt
package com.littlebridge.enrollplus.feature.tutor.heatmap

import com.littlebridge.enrollplus.feature.tutor.core.TutorConstants
import com.littlebridge.enrollplus.feature.tutor.core.TutorModule
import io.ktor.server.routing.Route

/**
 * Module singleton for Teacher Heatmap (cross-role).
 *
 * Registers the teacher heatmap route.
 *
 * SOLID:
 *   S → One responsibility: wiring the heatmap routes.
 *   D → Depends on [TutorModule] abstraction.
 *
 * Spec: AI_TUTOR_2.0_AGENTIC_REDESIGN.md §10 (Teacher heatmap)
 */
object TeacherHeatmapModule : TutorModule {
    override val moduleName: String = TutorConstants.MODULE_TEACHER_HEATMAP

    override fun registerRoutes(parent: Route) {
        parent.heatmapRouting()
    }
}

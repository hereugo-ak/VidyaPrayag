// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/tutor/ingest/IngestModule.kt
package com.littlebridge.enrollplus.feature.tutor.ingest

import com.littlebridge.enrollplus.feature.tutor.core.TutorConstants
import com.littlebridge.enrollplus.feature.tutor.core.TutorModule
import io.ktor.server.routing.Route

/**
 * Module singleton for Ingest (OCR + Voice).
 *
 * Registers the photo-doubt and voice-doubt upload routes.
 *
 * SOLID:
 *   S → One responsibility: wiring the ingest routes.
 *   D → Depends on [TutorModule] abstraction.
 *
 * Spec: AI_TUTOR_2.0_AGENTIC_REDESIGN.md §6.4 step 1
 */
object IngestModule : TutorModule {
    override val moduleName: String = TutorConstants.MODULE_INGEST

    override fun registerRoutes(parent: Route) {
        parent.ingestRouting()
    }
}

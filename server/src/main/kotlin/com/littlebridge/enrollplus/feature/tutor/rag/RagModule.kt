// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/tutor/rag/RagModule.kt
package com.littlebridge.enrollplus.feature.tutor.rag

import com.littlebridge.enrollplus.feature.tutor.core.TutorConstants
import com.littlebridge.enrollplus.feature.tutor.core.TutorModule
import io.ktor.server.routing.Route

/**
 * Module singleton for RAG (Retrieval-Augmented Generation).
 *
 * Registers the knowledge retrieval route.
 *
 * Spec: AI_TUTOR_2.0_AGENTIC_REDESIGN.md §8.4 (The RAG on-ramp)
 */
object RagModule : TutorModule {
    override val moduleName: String = TutorConstants.MODULE_RAG

    override fun registerRoutes(parent: Route) {
        parent.ragRouting()
    }
}

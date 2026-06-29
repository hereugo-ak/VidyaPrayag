// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/tutor/agent/AgentModule.kt
package com.littlebridge.enrollplus.feature.tutor.agent

import com.littlebridge.enrollplus.feature.tutor.core.TutorConstants
import com.littlebridge.enrollplus.feature.tutor.core.TutorModule
import io.ktor.server.routing.Route

/**
 * Module singleton for Tier 2 — Agent (TutorAgentService).
 *
 * Registers the doubt-resolution route under JWT auth.
 *
 * SOLID:
 *   S → One responsibility: wiring the agent tier's routes.
 *   D → Depends on [TutorModule] abstraction.
 *
 * Spec: AI_TUTOR_2.0_AGENTIC_REDESIGN.md §6
 */
object AgentModule : TutorModule {
    override val moduleName: String = TutorConstants.MODULE_AGENT

    override fun registerRoutes(parent: Route) {
        parent.agentRouting()
    }
}

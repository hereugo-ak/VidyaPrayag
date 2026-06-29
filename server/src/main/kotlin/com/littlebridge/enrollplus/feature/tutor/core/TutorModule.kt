// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/tutor/core/TutorModule.kt
package com.littlebridge.enrollplus.feature.tutor.core

import io.ktor.server.routing.Route

/**
 * Contract every AI Tutor 2.0 module implements. A module is an isolated feature
 * slice with its own routes, services, repositories, models, and DTOs.
 *
 * Mirrors [com.littlebridge.enrollplus.feature.reportcard.core.ReportCardModule]
 * and [com.littlebridge.enrollplus.feature.pews.core.PEWSModule] — same shape,
 * own package to avoid cross-module coupling.
 *
 * Plug & Play: to add a new module, implement this interface and register it
 * via [TutorModuleRegistry.register]. Zero changes to existing modules.
 *
 * SOLID:
 *   S → Each module has one responsibility (one tier of the tutor pipeline).
 *   O → New modules added by implementing this interface — no existing code changes.
 *   I → Feature-scoped, not a god-interface.
 *   D → Registry depends on this abstraction, not on concrete module classes.
 */
interface TutorModule {

    /** Unique module name — used as the kill-switch key and in audit logs. */
    val moduleName: String

    /**
     * Register this module's Ktor routes onto the given [Route] parent.
     * Called by [TutorModuleRegistry.installRoutes] at boot.
     */
    fun registerRoutes(parent: Route)
}

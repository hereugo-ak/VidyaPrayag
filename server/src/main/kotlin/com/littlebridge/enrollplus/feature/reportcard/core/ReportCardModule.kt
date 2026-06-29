// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/reportcard/core/ReportCardModule.kt
package com.littlebridge.enrollplus.feature.reportcard.core

import io.ktor.server.routing.Route

/**
 * Contract every AI Report Card 2.0 module must implement.
 *
 * This mirrors [com.littlebridge.enrollplus.feature.pews.core.PEWSModule] but
 * lives in its own package to avoid cross-module coupling. A module that
 * implements this interface can be registered with [ReportCardModuleRegistry]
 * and its routes will be automatically mounted under JWT authentication.
 *
 * SOLID:
 *   S → Each module has one responsibility (one tier of the report card pipeline).
 *   O → New modules added by implementing this interface — no existing code changes.
 *   D → Registry depends on this abstraction, not concrete modules.
 */
interface ReportCardModule {
    /** Unique module name used for kill-switch lookup and logging. */
    val moduleName: String

    /** Register this module's routes under the given parent [Route]. */
    fun registerRoutes(parent: Route)
}

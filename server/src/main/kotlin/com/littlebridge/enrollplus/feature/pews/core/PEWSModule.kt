// FILE: src/main/kotlin/com/littlebridge/enrollplus/feature/pews/core/PEWSModule.kt
package com.littlebridge.enrollplus.feature.pews.core

import io.ktor.server.routing.Route

/**
 * Contract every PEWS 2.0 module implements. A module is an isolated feature
 * slice with its own routes, services, repositories, models, and DTOs.
 *
 * Plug & Play: to add a new module, implement this interface and register it
 * via [ModuleRegistry.register]. Zero changes to existing modules.
 *
 * SOLID:
 *   - O (Open/Closed): new modules added via extension, not modification.
 *   - I (Interface Segregation): feature-scoped, not a god-interface.
 *   - D (Dependency Inversion): modules depend on this abstraction, not on
 *     concrete implementations of other modules.
 */
interface PEWSModule {

    /** Unique module name — used as the kill-switch key and in audit logs. */
    val moduleName: String

    /**
     * Register this module's Ktor routes onto the given [Route] parent.
     * Called by [ModuleRegistry.installAll] at boot.
     */
    fun registerRoutes(parent: Route)
}

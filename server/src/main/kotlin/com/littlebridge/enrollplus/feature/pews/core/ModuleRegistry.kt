// FILE: src/main/kotlin/com/littlebridge/enrollplus/feature/pews/core/ModuleRegistry.kt
package com.littlebridge.enrollplus.feature.pews.core

import io.ktor.server.routing.Route
import org.slf4j.LoggerFactory
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Central registry for all PEWS 2.0 modules. Modules register themselves at boot
 * (or are registered by a wiring function), and [installAll] mounts every
 * module's routes onto a Ktor [Route] parent in a single call.
 *
 * Plug & Play: adding a new module = implement [PEWSModule] + call [register].
 * No existing code is modified.
 *
 * SOLID:
 *   - O (Open/Closed): new modules registered, not wired into existing code.
 *   - D (Dependency Inversion): the registry depends on the [PEWSModule]
 *     abstraction, never on concrete module classes.
 */
object ModuleRegistry {
    private val log = LoggerFactory.getLogger("ModuleRegistry")

    private val modules = CopyOnWriteArrayList<PEWSModule>()

    /**
     * Register a module. Idempotent — a second registration of the same
     * [PEWSModule.moduleName] replaces the first (supports hot-reload of
     * module implementations during development).
     */
    fun register(module: PEWSModule) {
        modules.removeAll { it.moduleName == module.moduleName }
        modules.add(module)
        log.info("ModuleRegistry: registered '{}' (total={})", module.moduleName, modules.size)
    }

    /**
     * All registered modules (immutable snapshot).
     */
    fun all(): List<PEWSModule> = modules.toList()

    /**
     * Mount every registered module's routes onto the given [parent] Route.
     * Called once at boot from the PEWS router aggregator.
     */
    fun installAll(parent: Route) {
        for (module in modules) {
            log.debug("ModuleRegistry: installing routes for '{}'", module.moduleName)
            module.registerRoutes(parent)
        }
    }

    /**
     * Clear all registrations (for testing).
     */
    fun clear() {
        modules.clear()
    }
}

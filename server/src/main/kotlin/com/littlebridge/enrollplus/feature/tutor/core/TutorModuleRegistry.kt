// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/tutor/core/TutorModuleRegistry.kt
package com.littlebridge.enrollplus.feature.tutor.core

import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe registry for [TutorModule] instances.
 *
 * Mirrors [com.littlebridge.enrollplus.feature.reportcard.core.ReportCardModuleRegistry]
 * but scoped to AI Tutor modules only — no cross-package dependency.
 *
 * Plug & Play: a new module is added by calling [register] with a module
 * instance. Zero changes to existing modules or the registry itself.
 *
 * SOLID:
 *   O → Open for extension (register new modules), closed for modification.
 *   D → Depends on [TutorModule] abstraction, not concrete classes.
 */
object TutorModuleRegistry {
    private val log = LoggerFactory.getLogger("TutorModuleRegistry")
    private val modules = ConcurrentHashMap<String, TutorModule>()

    /** Register a module. Idempotent — re-registering replaces the old instance. */
    fun register(module: TutorModule) {
        modules[module.moduleName] = module
        log.info("TutorModule registered: {}", module.moduleName)
    }

    /** Get a registered module by name, or null. */
    fun get(name: String): TutorModule? = modules[name]

    /** All registered modules. */
    fun all(): List<TutorModule> = modules.values.toList()

    /** Install all registered modules' routes under the given parent [Route]. */
    fun installRoutes(parent: Route) {
        parent.authenticate("jwt") {
            modules.values.forEach { module ->
                runCatching { module.registerRoutes(this) }
                    .onFailure { log.error("Failed to register routes for {}: {}", module.moduleName, it.message, it) }
            }
        }
        log.info("TutorModuleRegistry: {} modules' routes installed", modules.size)
    }
}

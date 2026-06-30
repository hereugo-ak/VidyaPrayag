// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/reportcard/core/ReportCardModuleRegistry.kt
package com.littlebridge.enrollplus.feature.reportcard.core

import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe registry for [ReportCardModule] instances.
 *
 * Mirrors [com.littlebridge.enrollplus.feature.pews.core.ModuleRegistry] but
 * scoped to report card modules only — no cross-package dependency.
 *
 * Plug & Play: a new module is added by calling [register] with a module
 * instance. Zero changes to existing modules or the registry itself.
 *
 * SOLID:
 *   O → Open for extension (register new modules), closed for modification.
 *   D → Depends on [ReportCardModule] abstraction, not concrete classes.
 */
object ReportCardModuleRegistry {
    private val log = LoggerFactory.getLogger("ReportCardModuleRegistry")
    private val modules = ConcurrentHashMap<String, ReportCardModule>()

    /** Register a module. Idempotent — re-registering replaces the old instance. */
    fun register(module: ReportCardModule) {
        modules[module.moduleName] = module
        log.info("ReportCardModule registered: {}", module.moduleName)
    }

    /** Get a registered module by name, or null. */
    fun get(name: String): ReportCardModule? = modules[name]

    /** All registered modules. */
    fun all(): List<ReportCardModule> = modules.values.toList()

    /** Install all registered modules' routes under the given parent [Route]. */
    fun installRoutes(parent: Route) {
        parent.authenticate("jwt") {
            modules.values.forEach { module ->
                runCatching { module.registerRoutes(this) }
                    .onFailure { log.error("Failed to register routes for {}: {}", module.moduleName, it.message, it) }
            }
        }
        log.info("ReportCardModuleRegistry: {} modules' routes installed", modules.size)
    }
}

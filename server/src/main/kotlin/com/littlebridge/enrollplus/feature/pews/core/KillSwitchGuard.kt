// FILE: src/main/kotlin/com/littlebridge/enrollplus/feature/pews/core/KillSwitchGuard.kt
package com.littlebridge.enrollplus.feature.pews.core

/**
 * Thrown when a PEWS module is disabled by the kill switch.
 * Caught by the global StatusPages handler and mapped to HTTP 503 with
 * `{"pews":"disabled","module":"<name>"}`.
 */
class PewsDisabledException(
    val moduleName: String,
) : RuntimeException("PEWS module '$moduleName' is disabled by kill switch")

/**
 * Guard that every PEWS module MUST call at its service entry point before
 * executing any business logic. If the module (or the global PEWS kill switch)
 * is active, a [PewsDisabledException] is thrown — which the global error
 * handler maps to a 503 response.
 *
 * Usage in a service:
 * ```
 * class SenseService(...) {
 *     suspend fun recompute(...) {
 *         KillSwitchGuard.require("sense")
 *         // ... business logic
 *     }
 * }
 * ```
 */
object KillSwitchGuard {

    /**
     * @throws PewsDisabledException if the module or global PEWS is killed.
     */
    fun require(moduleName: String) {
        if (KillSwitchConfig.isKilled(moduleName)) {
            throw PewsDisabledException(moduleName)
        }
    }

    /**
     * Non-throwing variant for guard checks in routes where the caller wants
     * to handle the disabled state explicitly.
     */
    fun isDisabled(moduleName: String): Boolean =
        KillSwitchConfig.isKilled(moduleName)
}

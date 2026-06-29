// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/tutor/core/TutorModuleStatusRouting.kt
package com.littlebridge.enrollplus.feature.tutor.core

import com.littlebridge.enrollplus.core.ok
import com.littlebridge.enrollplus.core.requireSchoolContext
import io.ktor.server.application.call
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.serialization.Serializable

/**
 * Routes for Tutor Module Registry status (pluggability proof).
 *
 * Endpoints:
 *   GET /tutor/modules — list all registered modules with kill-switch status
 *
 * Authorization: school role (admin) via requireSchoolContext.
 *
 * This endpoint proves the plug-and-play architecture:
 * - All 11 modules are registered and visible.
 * - Each module's kill-switch status is shown.
 * - A new module can be added by implementing [TutorModule] and calling
 *   [TutorModuleRegistry.register] — it appears here automatically.
 *
 * Spec: AI_TUTOR_2.0_AGENTIC_REDESIGN.md §5 (Module architecture)
 */
fun Route.moduleStatusRouting() {

    get("/tutor/modules") {
        val ctx = call.requireSchoolContext() ?: return@get

        val modules = TutorModuleRegistry.all().map { m ->
            ModuleStatusItem(
                moduleName = m.moduleName,
                killSwitched = TutorKillSwitch.isDisabled(m.moduleName),
            )
        }

        call.ok(
            ModuleStatusResponse(
                totalModules = modules.size,
                modules = modules,
                globalKillSwitch = TutorKillSwitch.isDisabled(TutorConstants.MODULE_GLOBAL),
            ),
            "Tutor module registry (${modules.size} modules)"
        )
    }
}

@Serializable
data class ModuleStatusItem(
    val moduleName: String,
    val killSwitched: Boolean,
)

@Serializable
data class ModuleStatusResponse(
    val totalModules: Int,
    val modules: List<ModuleStatusItem>,
    val globalKillSwitch: Boolean,
)

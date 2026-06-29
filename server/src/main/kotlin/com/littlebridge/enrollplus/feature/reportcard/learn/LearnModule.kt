// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/reportcard/learn/LearnModule.kt
package com.littlebridge.enrollplus.feature.reportcard.learn

import com.littlebridge.enrollplus.feature.reportcard.core.ReportCardConstants
import com.littlebridge.enrollplus.feature.reportcard.core.ReportCardModule
import io.ktor.server.routing.Route
import org.slf4j.LoggerFactory

/**
 * Module registration for Tier 4 — Learn (Flywheel).
 *
 * Exposes admin routes for running the flywheel, viewing effectiveness
 * priors, and measuring projection accuracy.
 *
 * Kill-switched under module name "reportcard_learn".
 *
 * SOLID: S (one responsibility: learning & effectiveness), D (service injected).
 */
object LearnModule : ReportCardModule {
    override val moduleName = ReportCardConstants.MODULE_LEARN
    private val log = LoggerFactory.getLogger("LearnModule")

    val service = ReportLearnService()

    override fun registerRoutes(parent: Route) {
        parent.learnRouting(service)
        log.info("LearnModule routes registered (kill-switch: {})", moduleName)
    }
}

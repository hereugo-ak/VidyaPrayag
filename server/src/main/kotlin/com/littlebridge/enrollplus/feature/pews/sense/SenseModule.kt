package com.littlebridge.enrollplus.feature.pews.sense

import com.littlebridge.enrollplus.feature.pews.core.PEWSModule
import com.littlebridge.enrollplus.feature.pews.core.KillSwitchGuard
import com.littlebridge.enrollplus.feature.pews.PewsSnapshotService
import io.ktor.server.routing.Route
import org.slf4j.LoggerFactory

/**
 * PEWS 2.0 Sense Module — Tier 0.
 *
 * Owns the deterministic 15-signal feature vector computation and the
 * snapshot read API (cohort, student detail, history). The write path
 * (recomputeSchool) is called by the daily job or the manual /pews/run
 * endpoint; the read paths are mounted here.
 *
 * Kill-switched under module name "sense".
 */
object SenseModule : PEWSModule {
    override val moduleName = "sense"
    private val log = LoggerFactory.getLogger("SenseModule")
    val service = PewsSnapshotService()

    override fun registerRoutes(parent: Route) {
        // Routes are still mounted by the existing pewsRouting() function.
        // In a future tick, route registration will be migrated here.
        // For now, the module exists for kill-switch registration and
        // to expose the service singleton to other modules.
        log.info("SenseModule registered (kill-switch: {})", moduleName)
    }
}

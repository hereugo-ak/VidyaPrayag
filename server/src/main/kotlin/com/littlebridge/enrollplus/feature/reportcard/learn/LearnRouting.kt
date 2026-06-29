// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/reportcard/learn/LearnRouting.kt
package com.littlebridge.enrollplus.feature.reportcard.learn

import com.littlebridge.enrollplus.core.fail
import com.littlebridge.enrollplus.core.ok
import com.littlebridge.enrollplus.core.requireSchoolContext
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Routes for Tier 4 — Learn (Flywheel).
 *
 * Endpoints:
 *   POST  /report-card/learn/flywheel        — run flywheel measurement
 *   GET   /report-card/learn/effectiveness    — get effectiveness priors
 *   GET   /report-card/learn/projection-accuracy — get projection accuracy
 *   GET   /report-card/learn/focus-priors     — get focus priors for PEWS
 *
 * SOLID: S (routes only — no business logic), D (service injected).
 */
fun Route.learnRouting(service: ReportLearnService) {

    // ── Admin: run flywheel ────────────────────────────────────────────
    post("/report-card/learn/flywheel") {
        val ctx = call.requireSchoolContext() ?: return@post
        val currentTerm = call.request.queryParameters["currentTerm"]
            ?: return@post call.fail("currentTerm parameter required", HttpStatusCode.BadRequest)
        val previousTerm = call.request.queryParameters["previousTerm"]
            ?: return@post call.fail("previousTerm parameter required", HttpStatusCode.BadRequest)
        val academicYearId = call.request.queryParameters["academicYearId"]?.let {
            runCatching { UUID.fromString(it) }.getOrNull()
        }

        val reports = service.runFlywheel(ctx.schoolId, currentTerm, previousTerm, academicYearId)
        call.ok(reports, "Flywheel complete (${reports.size} focus areas)")
    }

    // ── Admin: get effectiveness priors ────────────────────────────────
    get("/report-card/learn/effectiveness") {
        val ctx = call.requireSchoolContext() ?: return@get
        val priors = service.getEffectivenessPriors(ctx.schoolId)
        call.ok(priors, "Effectiveness priors (${priors.size})")
    }

    // ── Admin: get projection accuracy ─────────────────────────────────
    get("/report-card/learn/projection-accuracy") {
        val ctx = call.requireSchoolContext() ?: return@get
        val currentTerm = call.request.queryParameters["currentTerm"]
            ?: return@get call.fail("currentTerm parameter required", HttpStatusCode.BadRequest)
        val previousTerm = call.request.queryParameters["previousTerm"]
            ?: return@get call.fail("previousTerm parameter required", HttpStatusCode.BadRequest)
        val academicYearId = call.request.queryParameters["academicYearId"]?.let {
            runCatching { UUID.fromString(it) }.getOrNull()
        }

        val report = service.measureProjectionAccuracy(ctx.schoolId, currentTerm, previousTerm, academicYearId)
        call.ok(report, "Projection accuracy report")
    }

    // ── Admin/PEWS: get focus priors for PEWS flywheel integration ─────
    get("/report-card/learn/focus-priors") {
        val ctx = call.requireSchoolContext() ?: return@get
        val priors = service.getFocusPriorsForPews(ctx.schoolId)
        call.ok(priors, "Focus priors (${priors.size})")
    }
}

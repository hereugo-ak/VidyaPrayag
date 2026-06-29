/*
 * File: EcosystemRouting.kt
 * Module: feature.reportcard.ecosystem
 *
 * Routes for ecosystem features:
 *   GET  /report-card/patterns           — admin: cohort pattern analysis
 *   GET  /report-card/conference-pack    — parent: conference-ready summary
 *
 * SOLID: S (routes only — no business logic), D (services injected).
 */
package com.littlebridge.enrollplus.feature.reportcard.ecosystem

import com.littlebridge.enrollplus.core.fail
import com.littlebridge.enrollplus.core.ok
import com.littlebridge.enrollplus.core.principalUserUuid
import com.littlebridge.enrollplus.core.requireSchoolContext
import com.littlebridge.enrollplus.db.ChildrenTable
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import java.util.UUID

fun Route.ecosystemRouting(
    termPatternService: TermPatternService = TermPatternService(),
    conferencePackService: ParentConferencePackService = ParentConferencePackService(),
) {
    // ── Admin: cohort pattern analysis ─────────────────────────────────
    get("/report-card/patterns") {
        val ctx = call.requireSchoolContext() ?: return@get
        val term = call.request.queryParameters["term"]
            ?: return@get call.fail("term parameter required", HttpStatusCode.BadRequest)
        val academicYearId = call.request.queryParameters["academicYearId"]?.let {
            runCatching { UUID.fromString(it) }.getOrNull()
        }
        val report = termPatternService.analyzePatterns(ctx.schoolId, term, academicYearId)
        call.ok(report, "Cohort patterns (${report.totalStudents} students)")
    }

    // ── Parent: conference pack ────────────────────────────────────────
    get("/report-card/conference-pack") {
        val uid = call.principalUserUuid() ?: run {
            call.fail("Invalid token", HttpStatusCode.Unauthorized); return@get
        }
        val childId = call.request.queryParameters["childId"]?.let {
            runCatching { UUID.fromString(it) }.getOrNull()
        } ?: return@get call.fail("childId parameter required", HttpStatusCode.BadRequest)

        // Resolve schoolId from child record
        val schoolId = dbQuery {
            ChildrenTable.selectAll().where {
                (ChildrenTable.id eq childId) and
                (ChildrenTable.parentId eq uid) and
                (ChildrenTable.isActive eq true)
            }.firstOrNull()?.get(ChildrenTable.schoolId)
        } ?: return@get call.fail("Child not found or not linked to your account", HttpStatusCode.NotFound)

        val pack = conferencePackService.buildPack(schoolId, uid, childId)
            ?: return@get call.fail("No published report card found for this child", HttpStatusCode.NotFound)
        call.ok(pack, "Conference pack ready")
    }
}

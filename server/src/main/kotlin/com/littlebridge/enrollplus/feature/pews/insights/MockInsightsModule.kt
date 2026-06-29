/*
 * File: MockInsightsModule.kt
 * Module: feature.pews.insights
 *
 * PEWS 2.0 — Mock 11th module for plug-and-play verification.
 *
 * This is a minimal, self-contained module that proves the ModuleRegistry
 * contract works: implement PEWSModule, register in registerPewsModules(),
 * and the routes appear without touching any existing code.
 *
 * It exposes a single read-only endpoint that returns a summary of the
 * school's PEWS cohort — useful as a smoke-test endpoint for integration
 * tests and as a template for future module authors.
 *
 * Kill-switched under module name "insights".
 */
package com.littlebridge.enrollplus.feature.pews.insights

import com.littlebridge.enrollplus.core.fail
import com.littlebridge.enrollplus.core.ok
import com.littlebridge.enrollplus.core.requireSchoolAdmin
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.PewsInterventionsTable
import com.littlebridge.enrollplus.db.PewsRiskSnapshotsTable
import com.littlebridge.enrollplus.feature.pews.core.PEWSModule
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.slf4j.LoggerFactory
import java.time.LocalDate

object MockInsightsModule : PEWSModule {
    override val moduleName = "insights"
    private val log = LoggerFactory.getLogger("MockInsightsModule")

    override fun registerRoutes(parent: Route) {
        log.info("MockInsightsModule registered (kill-switch: {})", moduleName)

        parent.authenticate("jwt") {
            route("/api/v1/school/pews/insights") {

                // Cohort summary — a lightweight smoke-test endpoint
                get {
                    val ctx = call.requireSchoolAdmin() ?: return@get
                    val today = LocalDate.now()

                    val summary = dbQuery {
                        val snapshots = PewsRiskSnapshotsTable.selectAll().where {
                            (PewsRiskSnapshotsTable.schoolId eq ctx.schoolId) and
                                (PewsRiskSnapshotsTable.runDate eq today)
                        }.toList()

                        val interventions = PewsInterventionsTable.selectAll().where {
                            (PewsInterventionsTable.schoolId eq ctx.schoolId) and
                                (PewsInterventionsTable.status inList listOf("open", "in_progress"))
                        }.toList()

                        CohortSummary(
                            runDate = today.toString(),
                            totalStudents = snapshots.size,
                            highRisk = snapshots.count { it[PewsRiskSnapshotsTable.riskLevel] == "high" },
                            mediumRisk = snapshots.count { it[PewsRiskSnapshotsTable.riskLevel] == "medium" },
                            watch = snapshots.count { it[PewsRiskSnapshotsTable.riskLevel] == "watch" },
                            openInterventions = interventions.size,
                            escalated = interventions.count { it[PewsInterventionsTable.escalationLevel] >= 2 },
                        )
                    }

                    call.ok(summary, "Cohort summary")
                }
            }
        }
    }
}

@Serializable
data class CohortSummary(
    val runDate: String,
    val totalStudents: Int,
    val highRisk: Int,
    val mediumRisk: Int,
    val watch: Int,
    val openInterventions: Int,
    val escalated: Int,
)

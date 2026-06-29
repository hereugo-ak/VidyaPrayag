/*
 * File: LearnService.kt
 * Module: feature.pews.learn
 *
 * PEWS 2.0 — Tier 4 Learn: the flywheel that compounds.
 *
 * Three responsibilities:
 *   1. Auto-measure outcomes: on each daily run, compare the student's
 *      current snapshot to the snapshot when the intervention was opened.
 *      attendance/marks deltas → auto_outcome ∈ {improved, unchanged, worsened}.
 *      Combine with the owner's manual tag for a trusted outcome.
 *   2. Update effectiveness priors: per school, per cause-family, per action-type,
 *      maintain {n_tried, n_improved, improve_rate, avg_days_to_improve}.
 *      This is what get_similar_resolved_cases() reads — the recommendation
 *      becomes evidence-based for THIS school.
 *   3. Prescriptive effectiveness view for admins: instead of "12 done / 4 improved",
 *      show "Parent calls resolve attendance dips in ~5 days here; remedial classes
 *      don't move attendance — use them for marks."
 *
 * Kill-switched under module name "learn".
 *
 * Spec: PEWS_2.0_AGENTIC_REDESIGN.md §8
 */
package com.littlebridge.enrollplus.feature.pews.learn

import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.PewsEffectivenessPriorsTable
import com.littlebridge.enrollplus.db.PewsInterventionsTable
import com.littlebridge.enrollplus.db.PewsRiskSnapshotsTable
import com.littlebridge.enrollplus.feature.pews.core.KillSwitchGuard
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

class LearnService {
    private val log = LoggerFactory.getLogger("LearnService")
    private val json = Json { ignoreUnknownKeys = true }

    // ── Outcome thresholds ─────────────────────────────────────────────────

    private val ATTENDANCE_IMPROVE_THRESHOLD = 5    // +5% attendance = improved
    private val ATTENDANCE_WORSEN_THRESHOLD = -3   // -3% attendance = worsened
    private val MARKS_IMPROVE_THRESHOLD = 5        // +5% marks = improved
    private val MARKS_WORSEN_THRESHOLD = -5       // -5% marks = worsened

    // ── Public types ───────────────────────────────────────────────────────

    @Serializable
    data class AutoOutcome(
        val studentCode: String,
        @Contextual val interventionId: UUID,
        val autoOutcome: String,       // improved|unchanged|worsened
        val trustedOutcome: String?,   // manual tag if present, else auto
        val attendanceDelta: Int?,
        val marksDelta: Int?,
        val daysOpen: Long,
    )

    @Serializable
    data class PriorRow(
        val causeFamily: String,
        val actionType: String,
        val nTried: Int,
        val nImproved: Int,
        val improveRate: Double,
        val avgDaysToImprove: Double,
    )

    @Serializable
    data class PrescriptiveEffectiveness(
        val totalOpen: Int,
        val totalDone: Int,
        val totalImproved: Int,
        val totalUnchanged: Int,
        val totalWorsened: Int,
        val priors: List<PriorRow>,
        val insights: List<String>,
        val unownedPastSla: Int,
    )

    // ── 1. Auto-measure outcomes ───────────────────────────────────────────

    /**
     * For all open/in_progress interventions, compare the student's current
     * snapshot to the snapshot when the intervention was opened. Set the
     * auto_outcome and, if no manual tag exists, the trusted outcome.
     *
     * Returns the list of measured outcomes.
     */
    suspend fun measureOutcomes(schoolId: UUID, runDate: LocalDate): List<AutoOutcome> {
        KillSwitchGuard.require("learn")

        val openInterventions = dbQuery {
            PewsInterventionsTable.selectAll().where {
                (PewsInterventionsTable.schoolId eq schoolId) and
                    (PewsInterventionsTable.status inList listOf("open", "in_progress"))
            }.toList()
        }

        if (openInterventions.isEmpty()) return emptyList()

        val results = mutableListOf<AutoOutcome>()

        for (intervention in openInterventions) {
            val studentCode = intervention[PewsInterventionsTable.studentCode]
            val snapshotId = intervention[PewsInterventionsTable.snapshotId]
            val openedAt = intervention[PewsInterventionsTable.openedAt]
            val manualOutcome = intervention[PewsInterventionsTable.outcome]
            val causeFamily = intervention[PewsInterventionsTable.causeFamily]
            val actionType = intervention[PewsInterventionsTable.actionType]
            val interventionId = intervention[PewsInterventionsTable.id].value
            val daysOpen = ChronoUnit.DAYS.between(openedAt, Instant.now())

            // Get the snapshot at intervention open time
            val openSnapshot = if (snapshotId != null) {
                dbQuery {
                    PewsRiskSnapshotsTable.selectAll().where {
                        PewsRiskSnapshotsTable.id eq snapshotId
                    }.singleOrNull()
                }
            } else null

            // Get the latest snapshot (current run)
            val currentSnapshot = dbQuery {
                PewsRiskSnapshotsTable.selectAll().where {
                    (PewsRiskSnapshotsTable.schoolId eq schoolId) and
                        (PewsRiskSnapshotsTable.studentCode eq studentCode) and
                        (PewsRiskSnapshotsTable.runDate eq runDate)
                }.singleOrNull()
            } ?: continue  // no current snapshot for this student — skip

            // Compute deltas
            val attendanceDelta = computeDelta(
                openSnapshot?.get(PewsRiskSnapshotsTable.attendancePct),
                currentSnapshot[PewsRiskSnapshotsTable.attendancePct]
            )
            val marksDelta = computeDelta(
                openSnapshot?.get(PewsRiskSnapshotsTable.marksPct),
                currentSnapshot[PewsRiskSnapshotsTable.marksPct]
            )

            // Determine auto outcome
            val autoOutcome = determineOutcome(attendanceDelta, marksDelta)

            // Trusted outcome: manual tag takes precedence, else auto
            val trustedOutcome = manualOutcome ?: autoOutcome

            // Update the intervention with auto outcome if it changed
            // (we don't overwrite manual outcomes)
            if (manualOutcome == null) {
                dbQuery {
                    PewsInterventionsTable.update({
                        PewsInterventionsTable.id eq interventionId
                    }) {
                        it[PewsInterventionsTable.outcome] = autoOutcome
                    }
                }
            }

            // If auto outcome is improved or worsened, and the intervention
            // has been open long enough, consider auto-resolving
            if (autoOutcome == "improved" && daysOpen >= 3) {
                dbQuery {
                    PewsInterventionsTable.update({
                        PewsInterventionsTable.id eq interventionId
                    }) {
                        it[PewsInterventionsTable.status] = "done"
                        it[PewsInterventionsTable.resolvedAt] = Instant.now()
                        it[PewsInterventionsTable.outcome] = manualOutcome ?: "improved"
                    }
                }
                log.info("Learn: auto-resolved intervention for {} as improved ({} days open)", studentCode, daysOpen)

                // Update priors
                updatePrior(schoolId, causeFamily, actionType, "improved", daysOpen)
            } else if (autoOutcome == "worsened" && daysOpen >= 7) {
                // Worsened — bump urgency, don't auto-close
                dbQuery {
                    PewsInterventionsTable.update({
                        PewsInterventionsTable.id eq interventionId
                    }) {
                        it[PewsInterventionsTable.urgency] = "high"
                    }
                }
                log.info("Learn: bumped urgency to high for {} (worsened after {} days)", studentCode, daysOpen)
            }

            results.add(AutoOutcome(
                studentCode = studentCode,
                interventionId = interventionId,
                autoOutcome = autoOutcome,
                trustedOutcome = trustedOutcome,
                attendanceDelta = attendanceDelta,
                marksDelta = marksDelta,
                daysOpen = daysOpen,
            ))
        }

        log.info("Learn: measured {} outcomes for school {} ({} improved, {} unchanged, {} worsened)",
            results.size, schoolId,
            results.count { it.autoOutcome == "improved" },
            results.count { it.autoOutcome == "unchanged" },
            results.count { it.autoOutcome == "worsened" })

        return results
    }

    // ── 2. Update effectiveness priors ─────────────────────────────────────

    /**
     * Update (or insert) the effectiveness prior for a school+cause+action.
     * Called when an intervention is resolved with a known outcome.
     */
    suspend fun updatePrior(
        schoolId: UUID,
        causeFamily: String?,
        actionType: String,
        outcome: String,
        daysOpen: Long,
    ) {
        if (causeFamily == null) return
        KillSwitchGuard.require("learn")

        val existing = dbQuery {
            PewsEffectivenessPriorsTable.selectAll().where {
                (PewsEffectivenessPriorsTable.schoolId eq schoolId) and
                    (PewsEffectivenessPriorsTable.causeFamily eq causeFamily) and
                    (PewsEffectivenessPriorsTable.actionType eq actionType)
            }.singleOrNull()
        }

        if (existing == null) {
            // Insert new prior
            dbQuery {
                PewsEffectivenessPriorsTable.insert {
                    it[PewsEffectivenessPriorsTable.schoolId] = schoolId
                    it[PewsEffectivenessPriorsTable.causeFamily] = causeFamily
                    it[PewsEffectivenessPriorsTable.actionType] = actionType
                    it[PewsEffectivenessPriorsTable.nTried] = 1
                    it[PewsEffectivenessPriorsTable.nImproved] = if (outcome == "improved") 1 else 0
                    it[PewsEffectivenessPriorsTable.improveRate] = if (outcome == "improved") 1.0 else 0.0
                    it[PewsEffectivenessPriorsTable.avgDaysToImprove] = if (outcome == "improved") daysOpen.toDouble() else 0.0
                    it[PewsEffectivenessPriorsTable.updatedAt] = Instant.now()
                }
            }
        } else {
            // Update existing prior
            val nTried = existing[PewsEffectivenessPriorsTable.nTried] + 1
            val nImproved = existing[PewsEffectivenessPriorsTable.nImproved] + if (outcome == "improved") 1 else 0
            val newImproveRate = nImproved.toDouble() / nTried
            val prevAvgDays = existing[PewsEffectivenessPriorsTable.avgDaysToImprove]
            val prevImproved = existing[PewsEffectivenessPriorsTable.nImproved]
            val newAvgDays = if (outcome == "improved" && nImproved > 0) {
                ((prevAvgDays * prevImproved) + daysOpen.toDouble()) / nImproved
            } else {
                prevAvgDays
            }

            dbQuery {
                PewsEffectivenessPriorsTable.update({
                    (PewsEffectivenessPriorsTable.id eq existing[PewsEffectivenessPriorsTable.id])
                }) {
                    it[PewsEffectivenessPriorsTable.nTried] = nTried
                    it[PewsEffectivenessPriorsTable.nImproved] = nImproved
                    it[PewsEffectivenessPriorsTable.improveRate] = newImproveRate
                    it[PewsEffectivenessPriorsTable.avgDaysToImprove] = newAvgDays
                    it[PewsEffectivenessPriorsTable.updatedAt] = Instant.now()
                }
            }
        }

        log.debug("Learn: updated prior for school={} cause={} action={} outcome={}",
            schoolId, causeFamily, actionType, outcome)
    }

    /**
     * Rebuild priors from scratch by scanning all resolved interventions.
     * Useful for backfill or correction.
     */
    suspend fun rebuildPriors(schoolId: UUID): Int {
        KillSwitchGuard.require("learn")

        // Clear existing priors for this school
        dbQuery {
            PewsEffectivenessPriorsTable.deleteWhere {
                PewsEffectivenessPriorsTable.schoolId eq schoolId
            }
        }

        // Scan all resolved interventions
        val resolved = dbQuery {
            PewsInterventionsTable.selectAll().where {
                (PewsInterventionsTable.schoolId eq schoolId) and
                    (PewsInterventionsTable.status inList listOf("done", "dismissed")) and
                    (PewsInterventionsTable.outcome neq null)
            }.toList()
        }

        // Group by cause+action
        val grouped = resolved.groupBy {
            Pair(it[PewsInterventionsTable.causeFamily], it[PewsInterventionsTable.actionType])
        }

        var inserted = 0
        for ((key, rows) in grouped) {
            val causeFamily = key.first ?: continue
            val actionType = key.second
            val nTried = rows.size
            val improved = rows.count { it[PewsInterventionsTable.outcome] == "improved" }
            val improveRate = if (nTried > 0) improved.toDouble() / nTried else 0.0

            val improvedRows = rows.filter { it[PewsInterventionsTable.outcome] == "improved" }
            val avgDays = if (improvedRows.isNotEmpty()) {
                improvedRows.map { row ->
                    ChronoUnit.DAYS.between(
                        row[PewsInterventionsTable.openedAt],
                        row[PewsInterventionsTable.resolvedAt] ?: Instant.now()
                    ).toDouble()
                }.average()
            } else 0.0

            dbQuery {
                PewsEffectivenessPriorsTable.insert {
                    it[PewsEffectivenessPriorsTable.schoolId] = schoolId
                    it[PewsEffectivenessPriorsTable.causeFamily] = causeFamily
                    it[PewsEffectivenessPriorsTable.actionType] = actionType
                    it[PewsEffectivenessPriorsTable.nTried] = nTried
                    it[PewsEffectivenessPriorsTable.nImproved] = improved
                    it[PewsEffectivenessPriorsTable.improveRate] = improveRate
                    it[PewsEffectivenessPriorsTable.avgDaysToImprove] = avgDays
                    it[PewsEffectivenessPriorsTable.updatedAt] = Instant.now()
                }
            }
            inserted++
        }

        log.info("Learn: rebuilt {} priors for school {} from {} resolved interventions", inserted, schoolId, resolved.size)
        return inserted
    }

    // ── 3. Prescriptive effectiveness view ─────────────────────────────────

    /**
     * Get the prescriptive effectiveness view for admins.
     * Returns priors + generated insights + unowned-past-SLA count.
     */
    suspend fun prescriptiveEffectiveness(schoolId: UUID): PrescriptiveEffectiveness {
        KillSwitchGuard.require("learn")

        val allInterventions = dbQuery {
            PewsInterventionsTable.selectAll().where {
                PewsInterventionsTable.schoolId eq schoolId
            }.toList()
        }

        val priors = dbQuery {
            PewsEffectivenessPriorsTable.selectAll().where {
                PewsEffectivenessPriorsTable.schoolId eq schoolId
            }.map { row ->
                PriorRow(
                    causeFamily = row[PewsEffectivenessPriorsTable.causeFamily],
                    actionType = row[PewsEffectivenessPriorsTable.actionType],
                    nTried = row[PewsEffectivenessPriorsTable.nTried],
                    nImproved = row[PewsEffectivenessPriorsTable.nImproved],
                    improveRate = row[PewsEffectivenessPriorsTable.improveRate],
                    avgDaysToImprove = row[PewsEffectivenessPriorsTable.avgDaysToImprove],
                )
            }.sortedByDescending { it.nTried }
        }

        // Count unowned past SLA
        val now = Instant.now()
        val unownedPastSla = allInterventions.count { row ->
            row[PewsInterventionsTable.status] in listOf("open", "in_progress") &&
                row[PewsInterventionsTable.escalationLevel] >= 2
        }

        // Generate insights from priors
        val insights = generateInsights(priors)

        return PrescriptiveEffectiveness(
            totalOpen = allInterventions.count { it[PewsInterventionsTable.status] in listOf("open", "in_progress") },
            totalDone = allInterventions.count { it[PewsInterventionsTable.status] == "done" },
            totalImproved = allInterventions.count { it[PewsInterventionsTable.outcome] == "improved" },
            totalUnchanged = allInterventions.count { it[PewsInterventionsTable.outcome] == "unchanged" },
            totalWorsened = allInterventions.count { it[PewsInterventionsTable.outcome] == "worsened" },
            priors = priors,
            insights = insights,
            unownedPastSla = unownedPastSla,
        )
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun computeDelta(old: Int?, new: Int?): Int? {
        if (old == null || new == null) return null
        return new - old
    }

    private fun determineOutcome(attendanceDelta: Int?, marksDelta: Int?): String {
        // Attendance takes priority (it's the leading indicator)
        val attScore = when {
            attendanceDelta == null -> 0
            attendanceDelta >= ATTENDANCE_IMPROVE_THRESHOLD -> 1
            attendanceDelta <= ATTENDANCE_WORSEN_THRESHOLD -> -1
            else -> 0
        }

        val marksScore = when {
            marksDelta == null -> 0
            marksDelta >= MARKS_IMPROVE_THRESHOLD -> 1
            marksDelta <= MARKS_WORSEN_THRESHOLD -> -1
            else -> 0
        }

        val total = attScore + marksScore
        return when {
            total > 0 -> "improved"
            total < 0 -> "worsened"
            else -> "unchanged"
        }
    }

    private fun generateInsights(priors: List<PriorRow>): List<String> {
        val insights = mutableListOf<String>()

        // Group by cause family and find best action
        val byCause = priors.filter { it.nTried >= 2 }.groupBy { it.causeFamily }
        for ((cause, actions) in byCause) {
            val best = actions.maxByOrNull { it.improveRate }
            val worst = actions.minByOrNull { it.improveRate }
            if (best != null && best.improveRate > 0.0) {
                val daysStr = if (best.avgDaysToImprove > 0) {
                    " in ~${best.avgDaysToImprove.toInt()} days"
                } else ""
                insights.add("For $cause: ${best.actionType} works ${String.format("%.0f", best.improveRate * 100)}%$daysStr.")
            }
            if (worst != null && worst != best && worst.nTried >= 2 && worst.improveRate < 0.3) {
                insights.add("For $cause: ${worst.actionType} rarely works (${String.format("%.0f", worst.improveRate * 100)}%) — try ${best?.actionType ?: "other approaches"} first.")
            }
        }

        // Highlight actions with high volume but low effectiveness
        val lowEffectiveness = priors.filter { it.nTried >= 3 && it.improveRate < 0.3 }
        for (prior in lowEffectiveness) {
            insights.add("${prior.actionType} for ${prior.causeFamily}: tried ${prior.nTried}×, only ${String.format("%.0f", prior.improveRate * 100)}% improved — consider alternatives.")
        }

        return insights
    }
}

// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/reportcard/learn/ReportLearnService.kt
package com.littlebridge.enrollplus.feature.reportcard.learn

import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.ReportCardDraftsTable
import com.littlebridge.enrollplus.feature.pews.core.AuditLogger
import com.littlebridge.enrollplus.feature.reportcard.core.ReportCardConstants
import com.littlebridge.enrollplus.feature.reportcard.core.ReportCardKillSwitch
import com.littlebridge.enrollplus.feature.reportcard.data.ReportFocusEffectivenessRepository
import com.littlebridge.enrollplus.feature.reportcard.rollup.ReportFactBundle
import com.littlebridge.enrollplus.feature.reportcard.rollup.ReportFactBundle.Companion.fromJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * Tier 4 — Learn (Flywheel).
 *
 * Measures the effectiveness of focus-area recommendations by comparing
 * projections from the previous term against actual results in the current
 * term. Updates the [ReportFocusEffectivenessTable] with rolling statistics.
 *
 * Also provides admin views for effectiveness priors and projection accuracy.
 *
 * Graceful degradation:
 *   - No previous term data → "insufficient data" response
 *   - No current term data → skips, no update
 *
 * SOLID:
 *   S → Single responsibility: learning & effectiveness measurement.
 *   D → Repository injected.
 *
 * Kill switch: [KillSwitchGuard.require] at entry with "reportcard_learn".
 */
class ReportLearnService(
    private val effectivenessRepo: ReportFocusEffectivenessRepository = ReportFocusEffectivenessRepository(),
) {
    private val log = LoggerFactory.getLogger("ReportLearnService")
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    data class EffectivenessReport(
        val focusArea: String,
        val studentsTargeted: Int,
        val studentsImproved: Int,
        val effectivenessScore: Double,
        val confidence: String,
    )

    @Serializable
    data class ProjectionAccuracyReport(
        val totalProjections: Int,
        val accurateWithin5Pct: Int,
        val accuracyRate: Double,
        val avgDelta: Double,
        val confidence: String,
    )

    /**
     * Run the flywheel: compare previous term projections against current
     * term results. Updates effectiveness priors.
     *
     * @param schoolId       School UUID
     * @param currentTerm    Current term label
     * @param previousTerm   Previous term label
     * @param academicYearId Academic year UUID
     */
    suspend fun runFlywheel(
        schoolId: UUID,
        currentTerm: String,
        previousTerm: String,
        academicYearId: UUID?,
    ): List<EffectivenessReport> {
        ReportCardKillSwitch.require(ReportCardConstants.MODULE_LEARN)
        log.info("Learn: running flywheel for school={}, current={}, previous={}", schoolId, currentTerm, previousTerm)

        // 1) Get all published drafts from the previous term
        val previousDrafts = dbQuery {
            ReportCardDraftsTable.selectAll().where {
                (ReportCardDraftsTable.schoolId eq schoolId) and
                (ReportCardDraftsTable.term eq previousTerm) and
                (ReportCardDraftsTable.status eq "published") and
                (if (academicYearId != null)
                    ReportCardDraftsTable.academicYearId eq academicYearId
                else
                    ReportCardDraftsTable.academicYearId.isNull())
            }.map { row ->
                PreviousDraft(
                    studentId = row[ReportCardDraftsTable.studentId],
                    factBundle = fromJson(row[ReportCardDraftsTable.factBundle]),
                    aiDraft = row[ReportCardDraftsTable.aiDraft],
                )
            }
        }

        if (previousDrafts.isEmpty()) {
            log.info("Learn: no previous term drafts — nothing to measure")
            return emptyList()
        }

        // 2) Get current term fact bundles to compare
        val currentDrafts = dbQuery {
            ReportCardDraftsTable.selectAll().where {
                (ReportCardDraftsTable.schoolId eq schoolId) and
                (ReportCardDraftsTable.term eq currentTerm) and
                (if (academicYearId != null)
                    ReportCardDraftsTable.academicYearId eq academicYearId
                else
                    ReportCardDraftsTable.academicYearId.isNull())
            }.associate { it[ReportCardDraftsTable.studentId] to fromJson(it[ReportCardDraftsTable.factBundle]) }
        }

        if (currentDrafts.isEmpty()) {
            log.info("Learn: no current term data — skipping flywheel")
            return emptyList()
        }

        // 3) Group previous projections by focus area
        val byFocusArea = previousDrafts.groupBy { draft ->
            draft.factBundle.projection?.focusAreas ?: emptyList()
        }

        val reports = mutableListOf<EffectivenessReport>()

        // 4) For each focus area, measure how many students improved
        val focusAreaStats = mutableMapOf<String, MutableList<Boolean>>()

        for (prevDraft in previousDrafts) {
            val prevProjection = prevDraft.factBundle.projection ?: continue
            val prevFocusAreas = prevProjection.focusAreas
            val currentBundle = currentDrafts[prevDraft.studentId] ?: continue

            val prevOverall = prevDraft.factBundle.overallPct ?: continue
            val currentOverall = currentBundle.overallPct ?: continue
            val delta = currentOverall - prevOverall
            val improved = delta >= 3.0  // 3% improvement threshold

            for (focusArea in prevFocusAreas) {
                focusAreaStats.getOrPut(focusArea) { mutableListOf() }.add(improved)
            }
        }

        // 5) Update effectiveness priors
        for ((focusArea, outcomes) in focusAreaStats) {
            val targeted = outcomes.size
            val improved = outcomes.count { it }
            val score = if (targeted > 0) improved.toDouble() / targeted else 0.0
            val avgDelta = previousDrafts.mapNotNull { pd ->
                val curr = currentDrafts[pd.studentId]?.overallPct
                val prev = pd.factBundle.overallPct
                if (curr != null && prev != null) curr - prev else null
            }.averageOrNull() ?: 0.0

            val confidence = when {
                targeted >= 20 -> ReportCardConstants.Confidence.HIGH
                targeted >= 10 -> ReportCardConstants.Confidence.MEDIUM
                targeted >= 3 -> ReportCardConstants.Confidence.LOW
                else -> ReportCardConstants.Confidence.INSUFFICIENT
            }

            effectivenessRepo.upsert(
                schoolId = schoolId,
                focusArea = focusArea,
                term = currentTerm,
                academicYearId = academicYearId,
                studentsTargeted = targeted,
                studentsImproved = improved,
                avgDelta = avgDelta,
                effectivenessScore = score,
                sampleSize = targeted,
                confidence = confidence,
            )

            reports.add(EffectivenessReport(focusArea, targeted, improved, score, confidence))
        }

        // 6) Audit log
        AuditLogger.log(
            schoolId = schoolId,
            module = ReportCardConstants.MODULE_LEARN,
            action = "flywheel_run",
            entityType = "term",
            entityId = currentTerm,
            details = mapOf(
                "previousTerm" to previousTerm,
                "focusAreas" to focusAreaStats.size,
                "students" to previousDrafts.size,
            ),
        )

        log.info("Learn: flywheel complete — {} focus areas measured", focusAreaStats.size)
        return reports.sortedByDescending { it.effectivenessScore }
    }

    /**
     * Get effectiveness priors for a school (admin view).
     */
    suspend fun getEffectivenessPriors(schoolId: UUID): List<EffectivenessReport> {
        ReportCardKillSwitch.require(ReportCardConstants.MODULE_LEARN)
        return effectivenessRepo.findBySchool(schoolId).map { row ->
            EffectivenessReport(
                focusArea = row.focusArea,
                studentsTargeted = row.studentsTargeted,
                studentsImproved = row.studentsImproved,
                effectivenessScore = row.effectivenessScore,
                confidence = row.confidence,
            )
        }
    }

    /**
     * Measure projection accuracy: how close were previous term projections
     * to actual current term results?
     */
    suspend fun measureProjectionAccuracy(
        schoolId: UUID,
        currentTerm: String,
        previousTerm: String,
        academicYearId: UUID?,
    ): ProjectionAccuracyReport {
        ReportCardKillSwitch.require(ReportCardConstants.MODULE_LEARN)

        val previousDrafts = dbQuery {
            ReportCardDraftsTable.selectAll().where {
                (ReportCardDraftsTable.schoolId eq schoolId) and
                (ReportCardDraftsTable.term eq previousTerm) and
                (ReportCardDraftsTable.status eq "published") and
                (if (academicYearId != null)
                    ReportCardDraftsTable.academicYearId eq academicYearId
                else
                    ReportCardDraftsTable.academicYearId.isNull())
            }.map { row ->
                PreviousDraft(
                    studentId = row[ReportCardDraftsTable.studentId],
                    factBundle = fromJson(row[ReportCardDraftsTable.factBundle]),
                    aiDraft = row[ReportCardDraftsTable.aiDraft],
                )
            }
        }

        val currentDrafts = dbQuery {
            ReportCardDraftsTable.selectAll().where {
                (ReportCardDraftsTable.schoolId eq schoolId) and
                (ReportCardDraftsTable.term eq currentTerm) and
                (if (academicYearId != null)
                    ReportCardDraftsTable.academicYearId eq academicYearId
                else
                    ReportCardDraftsTable.academicYearId.isNull())
            }.associate { it[ReportCardDraftsTable.studentId] to fromJson(it[ReportCardDraftsTable.factBundle]) }
        }

        var totalProjections = 0
        var accurateWithin5Pct = 0
        var totalDelta = 0.0

        for (prevDraft in previousDrafts) {
            val prevProjection = prevDraft.factBundle.projection ?: continue
            val currentBundle = currentDrafts[prevDraft.studentId] ?: continue
            val currentOverall = currentBundle.overallPct ?: continue
            val prevOverall = prevDraft.factBundle.overallPct ?: continue

            // Projected = prevOverall + slope
            val slope = prevDraft.factBundle.marksSlope ?: 0.0
            val projected = prevOverall + slope
            val delta = kotlin.math.abs(projected - currentOverall)

            totalProjections++
            totalDelta += delta
            if (delta <= 5.0) accurateWithin5Pct++
        }

        val accuracyRate = if (totalProjections > 0) accurateWithin5Pct.toDouble() / totalProjections else 0.0
        val avgDelta = if (totalProjections > 0) totalDelta / totalProjections else 0.0
        val confidence = when {
            totalProjections >= 30 -> ReportCardConstants.Confidence.HIGH
            totalProjections >= 15 -> ReportCardConstants.Confidence.MEDIUM
            totalProjections >= 5 -> ReportCardConstants.Confidence.LOW
            else -> ReportCardConstants.Confidence.INSUFFICIENT
        }

        return ProjectionAccuracyReport(
            totalProjections = totalProjections,
            accurateWithin5Pct = accurateWithin5Pct,
            accuracyRate = accuracyRate,
            avgDelta = avgDelta,
            confidence = confidence,
        )
    }

    /**
     * Get effectiveness priors formatted for PEWS consumption.
     * Returns focus areas with their effectiveness scores so PEWS
     * can prioritize interventions on areas that have historically
     * been effective.
     */
    suspend fun getFocusPriorsForPews(schoolId: UUID): List<FocusPriorDto> {
        ReportCardKillSwitch.require(ReportCardConstants.MODULE_LEARN)
        return effectivenessRepo.findBySchool(schoolId).map { row ->
            FocusPriorDto(
                focusArea = row.focusArea,
                effectivenessScore = row.effectivenessScore,
                studentsTargeted = row.studentsTargeted,
                studentsImproved = row.studentsImproved,
                confidence = row.confidence,
                term = row.term,
            )
        }.sortedByDescending { it.effectivenessScore }
    }

    @Serializable
    data class FocusPriorDto(
        val focusArea: String,
        val effectivenessScore: Double,
        val studentsTargeted: Int,
        val studentsImproved: Int,
        val confidence: String,
        val term: String,
    )

    private data class PreviousDraft(
        val studentId: UUID,
        val factBundle: ReportFactBundle,
        val aiDraft: String?,
    )

    private fun List<Double>.averageOrNull(): Double? = if (isEmpty()) null else average()
}

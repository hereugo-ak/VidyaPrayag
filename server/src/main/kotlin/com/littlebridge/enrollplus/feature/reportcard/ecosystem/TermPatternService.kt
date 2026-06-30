/*
 * File: TermPatternService.kt
 * Module: feature.reportcard.ecosystem
 *
 * ECO 1: Term Pattern Agent — analyzes cohort-level patterns across a term
 * for school admins. Aggregates movement patterns, common focus areas,
 * class-level trends, and subject-level insights from published report cards.
 *
 * This is a read-only analytics service — no AI calls, pure data aggregation
 * from existing report_card_drafts and fact_bundles.
 *
 * SOLID: S (single responsibility: cohort pattern analysis), D (uses dbQuery).
 */
package com.littlebridge.enrollplus.feature.reportcard.ecosystem

import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.ReportCardDraftsTable
import com.littlebridge.enrollplus.feature.reportcard.core.ReportCardConstants
import com.littlebridge.enrollplus.feature.reportcard.core.ReportCardKillSwitch
import com.littlebridge.enrollplus.feature.reportcard.rollup.ReportFactBundle
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import java.util.UUID

class TermPatternService {

    @Serializable
    data class CohortPatternReport(
        val schoolId: String,
        val term: String,
        val totalStudents: Int,
        val movementDistribution: Map<String, Int>,
        val topFocusAreas: List<FocusAreaStat>,
        val topStrengths: List<String>,
        val topImprovementAreas: List<String>,
        val classBreakdown: List<ClassPatternRow>,
        val avgOverallPct: Double?,
        val confidenceLevel: String,
    )

    @Serializable
    data class FocusAreaStat(
        val focusArea: String,
        val count: Int,
        val avgPct: Double,
    )

    @Serializable
    data class ClassPatternRow(
        val className: String,
        val section: String,
        val studentCount: Int,
        val avgPct: Double?,
        val improvedCount: Int,
        val slidCount: Int,
        val steadyCount: Int,
        val topFocusAreas: List<String>,
    )

    /**
     * Analyze cohort patterns for a term across all classes in a school.
     */
    suspend fun analyzePatterns(
        schoolId: UUID,
        term: String,
        academicYearId: UUID?,
    ): CohortPatternReport {
        ReportCardKillSwitch.require(ReportCardConstants.MODULE_ASSEMBLE)

        val drafts = dbQuery {
            ReportCardDraftsTable.selectAll().where {
                (ReportCardDraftsTable.schoolId eq schoolId) and
                (ReportCardDraftsTable.term eq term) and
                (if (academicYearId != null)
                    ReportCardDraftsTable.academicYearId eq academicYearId
                else
                    ReportCardDraftsTable.academicYearId.isNull())
            }.map { row ->
                val bundle = ReportFactBundle.fromJson(row[ReportCardDraftsTable.factBundle])
                Triple(
                    row[ReportCardDraftsTable.className],
                    row[ReportCardDraftsTable.section],
                    bundle,
                )
            }
        }

        if (drafts.isEmpty()) {
            return CohortPatternReport(
                schoolId = schoolId.toString(),
                term = term,
                totalStudents = 0,
                movementDistribution = emptyMap(),
                topFocusAreas = emptyList(),
                topStrengths = emptyList(),
                topImprovementAreas = emptyList(),
                classBreakdown = emptyList(),
                avgOverallPct = null,
                confidenceLevel = ReportCardConstants.Confidence.INSUFFICIENT,
            )
        }

        // Aggregate movement patterns
        val movementCounts = mutableMapOf<String, Int>()
        val focusAreaCounts = mutableMapOf<String, MutableList<Double>>()
        val strengthsCounts = mutableMapOf<String, Int>()
        val improvementCounts = mutableMapOf<String, Int>()
        val overallPcts = mutableListOf<Double>()

        // Per-class aggregation
        val classData = mutableMapOf<String, MutableList<ReportFactBundle>>()

        for ((className, section, bundle) in drafts) {
            val key = "$className-$section"
            classData.getOrPut(key) { mutableListOf() }.add(bundle)

            val movement = bundle.trajectoryLabel
            movementCounts[movement] = movementCounts.getOrDefault(movement, 0) + 1

            bundle.projection?.focusAreas?.forEach { fa ->
                focusAreaCounts.getOrPut(fa) { mutableListOf() }.add(bundle.overallPct ?: 0.0)
            }

            // Aggregate subject-level movements as strengths/improvement areas
            for (subject in bundle.subjects) {
                if (subject.percentage != null && subject.percentage >= 75.0) {
                    strengthsCounts[subject.subject] = strengthsCounts.getOrDefault(subject.subject, 0) + 1
                }
                if (subject.percentage != null && subject.percentage < 50.0) {
                    improvementCounts[subject.subject] = improvementCounts.getOrDefault(subject.subject, 0) + 1
                }
            }

            bundle.overallPct?.let { overallPcts.add(it) }
        }

        val topFocusAreas = focusAreaCounts.map { (area, pcts) ->
            FocusAreaStat(
                focusArea = area,
                count = pcts.size,
                avgPct = pcts.average(),
            )
        }.sortedByDescending { it.count }.take(10)

        val topStrengths = strengthsCounts.entries
            .sortedByDescending { it.value }
            .take(10)
            .map { it.key }

        val topImprovementAreas = improvementCounts.entries
            .sortedByDescending { it.value }
            .take(10)
            .map { it.key }

        val classBreakdown = classData.map { (key, bundles) ->
            val parts = key.split("-")
            val className = parts.getOrElse(0) { "Unknown" }
            val section = parts.getOrElse(1) { "A" }
            val avgPct = bundles.mapNotNull { it.overallPct }.takeIf { it.isNotEmpty() }?.average()
            val improved = bundles.count { it.trajectoryLabel == ReportCardConstants.MovementPattern.IMPROVED }
            val slid = bundles.count { it.trajectoryLabel == ReportCardConstants.MovementPattern.SLID }
            val steady = bundles.count { it.trajectoryLabel == ReportCardConstants.MovementPattern.STEADY }
            val fas = bundles.flatMap { it.projection?.focusAreas ?: emptyList() }
                .groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(5).map { it.key }

            ClassPatternRow(className, section, bundles.size, avgPct, improved, slid, steady, fas)
        }.sortedBy { it.className + "-" + it.section }

        val avgOverall = overallPcts.takeIf { it.isNotEmpty() }?.average()
        val confidence = when {
            drafts.size >= 50 -> ReportCardConstants.Confidence.HIGH
            drafts.size >= 20 -> ReportCardConstants.Confidence.MEDIUM
            drafts.size >= 5 -> ReportCardConstants.Confidence.LOW
            else -> ReportCardConstants.Confidence.INSUFFICIENT
        }

        return CohortPatternReport(
            schoolId = schoolId.toString(),
            term = term,
            totalStudents = drafts.size,
            movementDistribution = movementCounts,
            topFocusAreas = topFocusAreas,
            topStrengths = topStrengths,
            topImprovementAreas = topImprovementAreas,
            classBreakdown = classBreakdown,
            avgOverallPct = avgOverall,
            confidenceLevel = confidence,
        )
    }
}

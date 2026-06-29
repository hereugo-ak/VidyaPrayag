/*
 * File: ReportCardModels.kt
 * Module: feature.reportcard.domain.model
 *
 * Serializable DTOs for the AI Report Card 2.0 feature.
 * Mirrors server-side DTOs from AssembleRouting.kt, LearnRouting.kt, EcosystemRouting.kt.
 */
package com.littlebridge.enrollplus.feature.reportcard.domain.model

import kotlinx.serialization.Serializable

object ReportCardModels {

    @Serializable
    data class GenerateRequest(
        val className: String,
        val section: String = "A",
        val term: String,
        val academicYearId: String? = null,
        val language: String? = null,
    )

    @Serializable
    data class BatchResult(
        val jobId: String,
        val totalStudents: Int,
        val completed: Int,
        val failed: Int,
        val grounded: Int,
        val flagged: Int,
        val fallbackUsed: Int,
        val errors: List<String> = emptyList(),
    )

    @Serializable
    data class JobStatus(
        val jobId: String,
        val status: String,
        val totalItems: Int,
        val completedItems: Int,
        val result: String? = null,
        val createdAt: String,
        val completedAt: String? = null,
    )

    @Serializable
    data class DraftDto(
        val id: String,
        val studentId: String,
        val className: String,
        val section: String,
        val term: String,
        val academicYearId: String? = null,
        val aiDraft: String? = null,
        val classContext: String? = null,
        val status: String,
        val aiProviderUsed: String? = null,
        val tokensUsed: Int = 0,
        val language: String = "en",
        val groundingFlags: String? = null,
        val createdAt: String,
        val updatedAt: String,
    )

    @Serializable
    data class EditDraftRequest(
        val draftJson: String,
    )

    @Serializable
    data class RegenerateResult(
        val draftId: String,
        val studentId: String,
        val success: Boolean,
        val grounded: Boolean,
        val fallbackUsed: Boolean,
        val error: String? = null,
    )

    @Serializable
    data class BulkApproveRequest(
        val draftIds: List<String>,
    )

    @Serializable
    data class BulkApproveResult(
        val approved: Int,
    )

    @Serializable
    data class PublishRequest(
        val className: String,
        val section: String,
        val term: String,
        val academicYearId: String? = null,
    )

    @Serializable
    data class PublishResult(
        val published: Int,
    )

    @Serializable
    data class ClassOversightRow(
        val className: String,
        val section: String,
        val term: String,
        val totalDrafts: Int,
        val draftCount: Int,
        val flaggedCount: Int,
        val approvedCount: Int,
        val publishedCount: Int,
    )

    @Serializable
    data class OversightSummary(
        val schoolId: String,
        val classes: List<ClassOversightRow>,
    )

    @Serializable
    data class TermConfig(
        val currentTerm: String? = null,
        val termWindowDays: Int = 7,
        val enabled: Boolean = true,
        val batchConcurrency: Int = 5,
        val fallbackOnAiFail: Boolean = true,
    )

    @Serializable
    data class EffectivenessReport(
        val focusArea: String,
        val studentsTargeted: Int,
        val studentsImproved: Int,
        val effectivenessScore: Double,
        val confidence: String,
    )

    @Serializable
    data class ProjectionAccuracy(
        val totalProjections: Int,
        val accurateWithin5Pct: Int,
        val accuracyRate: Double,
        val avgDelta: Double,
        val confidence: String,
    )

    @Serializable
    data class CohortPatternReport(
        val schoolId: String,
        val term: String,
        val totalStudents: Int,
        val movementDistribution: Map<String, Int> = emptyMap(),
        val topFocusAreas: List<FocusAreaStat> = emptyList(),
        val topStrengths: List<String> = emptyList(),
        val topImprovementAreas: List<String> = emptyList(),
        val classBreakdown: List<ClassPatternRow> = emptyList(),
        val avgOverallPct: Double? = null,
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
        val avgPct: Double? = null,
        val improvedCount: Int,
        val slidCount: Int,
        val steadyCount: Int,
        val topFocusAreas: List<String> = emptyList(),
    )

    @Serializable
    data class ParentReport(
        val id: String,
        val term: String,
        val className: String,
        val section: String,
        val aiDraft: String? = null,
        val classContext: String? = null,
        val language: String = "en",
        val publishedAt: String? = null,
        val groundingFlags: String? = null,
    )

    @Serializable
    data class ConferencePack(
        val studentName: String,
        val className: String,
        val section: String,
        val term: String,
        val overallPct: Double? = null,
        val overallGrade: String? = null,
        val attendancePct: Int? = null,
        val movementLabel: String,
        val focusAreas: List<String> = emptyList(),
        val strengths: List<String> = emptyList(),
        val improvementAreas: List<String> = emptyList(),
        val parentSummary: String = "",
        val teacherNote: String = "",
        val projectionNote: String = "",
        val pewsRiskLevel: String? = null,
        val subjectHighlights: List<SubjectHighlight> = emptyList(),
        val conferenceTips: List<String> = emptyList(),
    )

    @Serializable
    data class SubjectHighlight(
        val subject: String,
        val percentage: Double? = null,
        val grade: String? = null,
        val movement: String,
    )
}

// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/reportcard/triage/ReportTriageService.kt
package com.littlebridge.enrollplus.feature.reportcard.triage

import com.littlebridge.enrollplus.feature.ai.AiLane
import com.littlebridge.enrollplus.feature.ai.AiService
import com.littlebridge.enrollplus.feature.ai.LlmMessage
import com.littlebridge.enrollplus.feature.reportcard.core.ReportCardConfig
import com.littlebridge.enrollplus.feature.reportcard.core.ReportCardConstants
import com.littlebridge.enrollplus.feature.reportcard.core.ReportCardKillSwitch
import com.littlebridge.enrollplus.feature.reportcard.rollup.ReportFactBundle
import com.littlebridge.enrollplus.feature.reportcard.rollup.SubjectFact
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * Tier 1 — Triage / Cohort-dedup.
 *
 * Buckets per-subject movement into patterns (improved/steady/slid/volatile)
 * and generates ONE shared class-context paragraph per class-subject cluster
 * via a cheap CLASSIFY call (non-PII lane → high-TPM providers).
 *
 * This drastically reduces LLM calls: instead of narrating each student
 * independently, the narrator agent receives pre-computed class context and
 * only needs to personalize it.
 *
 * Graceful degradation:
 *   - If AI is unavailable → deterministic class-context from slopes alone.
 *   - If no students → empty result.
 *
 * SOLID:
 *   S → Single responsibility: triage bucketing + class-context phrasing.
 *   O → New bucketing strategies via extension without modifying this class.
 *
 * Kill switch: [KillSwitchGuard.require] at entry with "reportcard_triage".
 */
class ReportTriageService {
    private val log = LoggerFactory.getLogger("ReportTriageService")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Serializable
    data class SubjectCluster(
        val subject: String,
        val pattern: String,           // improved|steady|slid|volatile
        val studentCount: Int,
        val avgPercentage: Double,
        val avgDelta: Double,
        val classContext: String,      // shared paragraph for this cluster
    )

    @Serializable
    data class TriageResult(
        val clusters: List<SubjectCluster>,
        val overallClassContext: String,
    )

    /**
     * Run triage on a batch of fact bundles (one class).
     *
     * @param schoolId   School UUID
     * @param bundles    All student fact bundles for this class/term
     * @return Triage result with per-subject clusters and class context
     */
    suspend fun triage(
        schoolId: UUID,
        bundles: List<ReportFactBundle>,
    ): TriageResult {
        ReportCardKillSwitch.require(ReportCardConstants.MODULE_TRIAGE)
        log.info("Triage: school={}, students={}", schoolId, bundles.size)

        if (bundles.isEmpty()) {
            return TriageResult(emptyList(), "No students to analyze.")
        }

        // 1) Group all subject facts by subject
        val bySubject: Map<String, List<SubjectFact>> = bundles
            .flatMap { it.subjects }
            .groupBy { it.subject }

        // 2) Build clusters
        val clusters = bySubject.map { (subject, facts) ->
            val avgPct = facts.mapNotNull { it.percentage }.averageOrNull() ?: 0.0
            val avgDelta = facts.mapNotNull { f ->
                f.percentage?.let { curr -> f.previousPercentage?.let { prev -> curr - prev } }
            }.averageOrNull() ?: 0.0

            val pattern = dominantPattern(facts)
            val studentCount = facts.size

            // 3) Generate class-context paragraph (one CLASSIFY call per cluster)
            val classContext = generateClassContext(
                schoolId, subject, pattern, studentCount, avgPct, avgDelta
            )

            SubjectCluster(subject, pattern, studentCount, avgPct, avgDelta, classContext)
        }.sortedBy { it.subject }

        // 4) Overall class context
        val overallContext = generateOverallClassContext(schoolId, clusters)

        log.info("Triage complete: {} clusters, {} patterns", clusters.size,
            clusters.groupBy { it.pattern }.mapValues { it.value.size })

        return TriageResult(clusters, overallContext)
    }

    // ── Private helpers ────────────────────────────────────────────────

    private fun dominantPattern(facts: List<SubjectFact>): String {
        val patterns = facts.groupBy { it.movement }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
        return patterns.firstOrNull()?.first ?: ReportCardConstants.MovementPattern.STEADY
    }

    private suspend fun generateClassContext(
        schoolId: UUID,
        subject: String,
        pattern: String,
        studentCount: Int,
        avgPct: Double,
        avgDelta: Double,
    ): String {
        val prompt = buildString {
            append("Write a single-paragraph class context note for a report card. ")
            append("Subject: $subject. ")
            append("Class pattern: $pattern. ")
            append("Students: $studentCount. ")
            append("Class average: ${avgPct.toInt()}%. ")
            if (avgDelta != 0.0) {
                append("Average change from last term: ${if (avgDelta > 0) "+" else ""}${avgDelta.toInt()}%. ")
            }
            append("Write in a professional, encouraging tone. Max 2 sentences. ")
            append("Do NOT mention specific student names or numbers — this is shared class context. ")
            append("Respond with just the paragraph, no preamble.")
        }

        // Try AI (non-PII lane → high-TPM providers)
        val result = AiService.complete(
            feature = ReportCardConstants.AI_FEATURE_TAG,
            lane = AiLane.CLASSIFY,
            messages = listOf(
                LlmMessage(role = "system", content = "You are a concise report card assistant. Write professional, encouraging class context notes."),
                LlmMessage(role = "user", content = prompt),
            ),
            containsPii = false,
            schoolId = schoolId,
            temperature = 0.3,
            maxTokens = 150,
            cache = true,
            cacheTtlMin = ReportCardConfig.cacheTtlMinutes,
        )

        if (result.ok && !result.content.isNullOrBlank()) {
            return result.content.trim()
        }

        // Graceful fallback: deterministic context from pattern
        return deterministicClassContext(subject, pattern, avgPct, avgDelta)
    }

    private suspend fun generateOverallClassContext(
        schoolId: UUID,
        clusters: List<SubjectCluster>,
    ): String {
        val improved = clusters.count { it.pattern == ReportCardConstants.MovementPattern.IMPROVED }
        val slid = clusters.count { it.pattern == ReportCardConstants.MovementPattern.SLID }
        val steady = clusters.count { it.pattern == ReportCardConstants.MovementPattern.STEADY }

        val prompt = buildString {
            append("Write a single-paragraph overall class context note for a report card. ")
            append("$improved subjects improved, $steady remained steady, $slid showed decline. ")
            append("Write in a professional, encouraging tone. Max 3 sentences. ")
            append("Do NOT mention specific numbers. Respond with just the paragraph.")
        }

        val result = AiService.complete(
            feature = ReportCardConstants.AI_FEATURE_TAG,
            lane = AiLane.CLASSIFY,
            messages = listOf(
                LlmMessage(role = "system", content = "You are a concise report card assistant."),
                LlmMessage(role = "user", content = prompt),
            ),
            containsPii = false,
            schoolId = schoolId,
            temperature = 0.3,
            maxTokens = 200,
            cache = true,
            cacheTtlMin = ReportCardConfig.cacheTtlMinutes,
        )

        if (result.ok && !result.content.isNullOrBlank()) {
            return result.content.trim()
        }

        // Fallback
        return buildString {
            append("This term, the class showed ")
            if (improved > steady && improved > slid) append("encouraging improvement across several subjects. ")
            else if (slid > improved) append("some areas needing attention alongside steady performance. ")
            else append("consistent performance across subjects. ")
            append("Teachers are working to support continued growth.")
        }
    }

    private fun deterministicClassContext(
        subject: String,
        pattern: String,
        avgPct: Double,
        avgDelta: Double,
    ): String = when (pattern) {
        ReportCardConstants.MovementPattern.IMPROVED ->
            "In $subject this term, the class showed encouraging improvement, building well on previous work."
        ReportCardConstants.MovementPattern.SLID ->
            "In $subject this term, the class faced some challenges. With focused support, students can recover."
        ReportCardConstants.MovementPattern.VOLATILE ->
            "In $subject this term, performance varied across the class. Individual attention will help stabilize outcomes."
        else ->
            "In $subject this term, the class maintained steady performance at ${avgPct.toInt()}% average."
    }

    private fun List<Double>.averageOrNull(): Double? =
        if (isEmpty()) null else average()
}

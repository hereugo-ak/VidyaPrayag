/*
 * File: TriageService.kt
 * Module: feature.pews.triage
 *
 * PEWS 2.0 — TIER 1 TRIAGE.
 *
 * Takes the full at-risk cohort from Sense and:
 *   1. Deterministic cohort dedup — groups students with identical signal
 *      signatures (same cause_family + risk level + signal kinds) into
 *      clusters. One representative per cluster is reasoned about, not
 *      N near-identical students. This is the "12 kids in 9-B with the
 *      same attendance dip get ONE reasoning" optimisation.
 *   2. Batched CLASSIFY prefilter — sends compact summaries of cluster
 *      representatives to a fast 8B model (Groq/Cerebras via CLASSIFY lane)
 *      in a SINGLE call. The model decides which clusters need a deep
 *      Tier-2 caseworker review vs which can be handled with a standard
 *      playbook action.
 *
 * This filters ~400 flagged students → ~30 deep cases, saving expensive
 * REASON calls and fixing the 23.5s/429 problem.
 *
 * Graceful degradation: if no AI provider is available, falls back to
 * deterministic triage (medium+ → deep look, watch → playbook).
 *
 * Kill-switched under module name "triage".
 */
package com.littlebridge.enrollplus.feature.pews.triage

import com.littlebridge.enrollplus.feature.ai.AiLane
import com.littlebridge.enrollplus.feature.ai.AiService
import com.littlebridge.enrollplus.feature.ai.LlmMessage
import com.littlebridge.enrollplus.feature.pews.PewsSnapshot
import com.littlebridge.enrollplus.feature.pews.core.KillSwitchGuard
import org.slf4j.LoggerFactory
import java.util.UUID

class TriageService {
    private val log = LoggerFactory.getLogger("TriageService")

    // ── Public types ───────────────────────────────────────────────────────

    /** Per-student triage decision after CLASSIFY + dedup. */
    data class TriageDecision(
        val studentCode: String,
        val needsDeepLook: Boolean,
        val causeFamily: String,
        val clusterKey: String,
        val isClusterRep: Boolean,
        val clusterSize: Int,
        val triageConfidence: Double? = null,
    )

    /** Aggregate result for the whole cohort. */
    data class TriageResult(
        val decisions: List<TriageDecision>,
        val deepLookCount: Int,
        val clusterCount: Int,
        val totalStudents: Int,
        val skippedByDedup: Int,
        val modelUsed: Boolean,
    )

    // ── Internal types ─────────────────────────────────────────────────────

    private data class Cluster(
        val key: String,
        val causeFamily: String,
        val members: List<PewsSnapshot>,
        val rep: PewsSnapshot,
    )

    // ── Main entry point ───────────────────────────────────────────────────

    /**
     * Triage the full at-risk cohort. Returns per-student decisions indicating
     * which need deep Tier-2 caseworker review and which can be handled with
     * standard playbook actions.
     */
    suspend fun triage(schoolId: UUID, snapshots: List<PewsSnapshot>): TriageResult {
        KillSwitchGuard.require("triage")

        if (snapshots.isEmpty()) {
            return TriageResult(emptyList(), 0, 0, 0, 0, false)
        }

        // Step 1: deterministic cohort dedup
        val clusters = buildClusters(snapshots)
        log.info("PEWS triage: school={} → {} students in {} clusters ({} deduped)",
            schoolId, snapshots.size, clusters.size, snapshots.size - clusters.size)

        // Step 2: batched CLASSIFY on cluster representatives
        val classifyResult = classifyBatch(schoolId, clusters)

        // Step 3: map back to all students
        val decisions = clusters.flatMap { cluster ->
            val repDecision = classifyResult[cluster.rep.studentCode]
            val needsDeep = repDecision?.first ?: defaultDeepLook(cluster.rep)
            val confidence = repDecision?.second

            cluster.members.mapIndexed { idx, snap ->
                TriageDecision(
                    studentCode = snap.studentCode,
                    needsDeepLook = needsDeep,
                    causeFamily = cluster.causeFamily,
                    clusterKey = cluster.key,
                    isClusterRep = idx == 0,
                    clusterSize = cluster.members.size,
                    triageConfidence = confidence,
                )
            }
        }

        val deepCount = decisions.count { it.needsDeepLook }
        return TriageResult(
            decisions = decisions,
            deepLookCount = deepCount,
            clusterCount = clusters.size,
            totalStudents = snapshots.size,
            skippedByDedup = snapshots.size - clusters.size,
            modelUsed = classifyResult.isNotEmpty(),
        )
    }

    /** Convenience: return only the student codes that need deep Tier-2 review. */
    suspend fun deepLookCodes(schoolId: UUID, snapshots: List<PewsSnapshot>): List<String> {
        val result = triage(schoolId, snapshots)
        return result.decisions.filter { it.needsDeepLook }.map { it.studentCode }
    }

    // ── Cohort dedup (deterministic) ───────────────────────────────────────

    /**
     * Group students into clusters by (causeFamily + riskLevel + sorted signal kinds).
     * Students in the same cluster get the same reasoning — no near-identical
     * paragraphs. The representative is the highest-risk, most-signaled student.
     */
    private fun buildClusters(snapshots: List<PewsSnapshot>): List<Cluster> {
        val grouped = snapshots.groupBy { snap ->
            val signalKinds = snap.signals.map { it.kind }.sorted().joinToString(",")
            "${snap.causeFamily ?: "unknown"}|${snap.riskLevel}|$signalKinds"
        }
        return grouped.map { (key, members) ->
            val rep = members.sortedWith(
                compareByDescending<PewsSnapshot> { it.riskScore }
                    .thenByDescending { it.signals.size }
            ).first()
            Cluster(
                key = key,
                causeFamily = rep.causeFamily ?: "unknown",
                members = members.sortedByDescending { it.riskScore },
                rep = rep,
            )
        }.sortedByDescending { it.rep.riskScore }
    }

    // ── Fallback (no AI) ───────────────────────────────────────────────────

    /**
     * Deterministic fallback when no AI provider is available.
     * Medium+ → deep look; watch → playbook (no deep look).
     */
    private fun defaultDeepLook(snap: PewsSnapshot): Boolean =
        snap.riskLevel == "medium" || snap.riskLevel == "high"

    // ── Batched CLASSIFY call ──────────────────────────────────────────────

    private val triageSystemPrompt = """
        You are a triage assistant for a school early-warning system. You receive
        batched summaries of student risk clusters. For each cluster, decide whether
        it warrants a deep caseworker review (Tier 2) or can be handled with a
        standard playbook action.

        STRICT RULES:
        - Use ONLY the data provided. NEVER invent signals, scores, or student details.
        - needs_deep_look=true when: risk is high, trends are declining, multiple
          signal types are present, leading indicators suggest escalation, or
          the cluster represents a large group (systemic issue).
        - needs_deep_look=false when: single minor signal, stable or improving,
          watch-level with no leading indicators.
        - Respond as a JSON array with one entry per cluster, exactly:
          {"index": <int>, "needs_deep_look": <true|false>, "confidence": <0.0-1.0>}
        - The index must match the input order.
    """.trimIndent()

    private suspend fun classifyBatch(
        schoolId: UUID, clusters: List<Cluster>,
    ): Map<String, Pair<Boolean, Double?>> {
        if (clusters.isEmpty()) return emptyMap()
        if (!AiService.anyProviderConfigured()) {
            log.info("PEWS triage: no AI provider configured for school {} — using deterministic fallback", schoolId)
            return emptyMap()
        }

        val summaries = clusters.mapIndexed { idx, cluster ->
            val rep = cluster.rep
            buildString {
                append("{\"index\": $idx, ")
                append("\"student_code\": \"${rep.studentCode}\", ")
                append("\"risk_score\": ${rep.riskScore}, ")
                append("\"risk_level\": \"${rep.riskLevel}\", ")
                append("\"cause_family\": \"${rep.causeFamily ?: "unknown"}\", ")
                append("\"cluster_size\": ${cluster.members.size}, ")
                append("\"signals\": [${rep.signals.joinToString(",") { "\"${it.kind}\"" }}], ")
                append("\"leading_score\": ${rep.leadingScore ?: 0}, ")
                append("\"confidence\": ${rep.confidence ?: 0.0}")
                append("}")
            }
        }

        val userPrompt = buildString {
            appendLine("Triage the following student clusters. Respond with a JSON array.")
            appendLine()
            appendLine("[")
            append(summaries.joinToString(",\n"))
            appendLine("]")
        }

        val result = AiService.complete(
            feature = "pews_triage",
            lane = AiLane.CLASSIFY,
            messages = listOf(
                LlmMessage("system", triageSystemPrompt),
                LlmMessage("user", userPrompt),
            ),
            containsPii = true,
            schoolId = schoolId,
            temperature = 0.2,
            maxTokens = 800,
            cache = true,
        )

        if (!result.ok || result.content.isNullOrBlank()) {
            log.warn("PEWS triage CLASSIFY failed for school {} ({}:{}) — falling back to deterministic",
                schoolId, result.routingDecision, result.errorMessage)
            return emptyMap()
        }

        val parsed = parseTriageResponse(result.content, clusters)
        log.info("PEWS triage: school={} CLASSIFY returned {} decisions via {} ({})",
            schoolId, parsed.size, result.providerUsed, result.routingDecision)
        return parsed
    }

    /**
     * Tolerant parsing of the CLASSIFY response. Extracts {index, needs_deep_look,
     * confidence} from each JSON object in the array. Handles models that wrap
     * JSON in prose or code fences.
     */
    private fun parseTriageResponse(
        raw: String, clusters: List<Cluster>,
    ): Map<String, Pair<Boolean, Double?>> {
        val out = HashMap<String, Pair<Boolean, Double?>>()
        val objRegex = Regex("""\{[^{}]*"index"\s*:\s*(\d+)[^{}]*\}""")
        for (match in objRegex.findAll(raw)) {
            val objText = match.value
            val index = match.groupValues[1].toIntOrNull() ?: continue
            val needsDeep = Regex(
                """"needs_deep_look"\s*:\s*(true|false)""", RegexOption.IGNORE_CASE
            ).find(objText)?.groupValues?.get(1)?.lowercase() == "true"
            val confidence = Regex(
                """"confidence"\s*:\s*([\d.]+)"""
            ).find(objText)?.groupValues?.get(1)?.toDoubleOrNull()

            if (index in clusters.indices) {
                out[clusters[index].rep.studentCode] = needsDeep to confidence
            }
        }
        return out
    }
}

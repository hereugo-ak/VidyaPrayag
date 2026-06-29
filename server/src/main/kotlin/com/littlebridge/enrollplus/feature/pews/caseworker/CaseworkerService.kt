/*
 * File: CaseworkerService.kt
 * Module: feature.pews.caseworker
 *
 * PEWS 2.0 — Tier 2 Caseworker Agent Service.
 *
 * For each student flagged by Triage as needing a deep look:
 *   1. Builds a system prompt with the deterministic snapshot bundle.
 *   2. Calls AiService.runAgent() with the 6 caseworker tools.
 *   3. The agent calls tools (student history, past interventions, similar
 *      resolved cases, calendar context, parent responsiveness, homework detail)
 *      to gather context, then produces a structured Case File JSON.
 *   4. GroundingGuard verifies the Case File against the deterministic bundle.
 *   5. Persists the Case File JSON to the snapshot's aiNarrative/aiCause/aiRecommendation
 *      fields (v1 compat) plus the full Case File JSON.
 *
 * Graceful degradation: if no AI provider, produces a deterministic Case File
 * from the snapshot alone (no tools, no LLM).
 *
 * Kill-switched under module name "caseworker".
 *
 * Spec: PEWS_2.0_AGENTIC_REDESIGN.md §6.3–6.4
 */
package com.littlebridge.enrollplus.feature.pews.caseworker

import com.littlebridge.enrollplus.feature.ai.AiLane
import com.littlebridge.enrollplus.feature.ai.AiService
import com.littlebridge.enrollplus.feature.pews.PewsSnapshot
import com.littlebridge.enrollplus.feature.pews.core.KillSwitchGuard
import org.slf4j.LoggerFactory
import java.util.UUID

class CaseworkerService {
    private val log = LoggerFactory.getLogger("CaseworkerService")

    // ── Public types ───────────────────────────────────────────────────────

    data class CaseworkerResult(
        val studentCode: String,
        val caseFile: CaseFile,
        val modelUsed: Boolean,
        val providerUsed: String? = null,
        val toolCallsMade: Int = 0,
        val stepsTaken: Int = 0,
        val grounded: Boolean = true,
    )

    // ── Main entry point ───────────────────────────────────────────────────

    /**
     * Run the caseworker agent for a single student snapshot.
     * Returns a structured Case File, grounded against the deterministic bundle.
     */
    suspend fun review(schoolId: UUID, snapshot: PewsSnapshot): CaseworkerResult {
        KillSwitchGuard.require("caseworker")

        // Try the agentic path (LLM + tools)
        val agentResult = runAgent(schoolId, snapshot)

        if (agentResult != null) {
            val caseFile = CaseFileCodec.parse(agentResult.content!!)
            if (caseFile != null) {
                val grounded = GroundingGuard.verify(caseFile, snapshot)
                val wasGrounded = grounded.narrative == caseFile.narrative &&
                    grounded.hypotheses == caseFile.hypotheses
                return CaseworkerResult(
                    studentCode = snapshot.studentCode,
                    caseFile = grounded,
                    modelUsed = true,
                    providerUsed = agentResult.providerUsed,
                    toolCallsMade = agentResult.toolCallsMade,
                    stepsTaken = agentResult.stepsTaken,
                    grounded = wasGrounded,
                )
            } else {
                log.warn("Caseworker: model output failed Case File parse for {} — falling back to deterministic", snapshot.studentCode)
            }
        }

        // Fallback: deterministic Case File (no AI)
        val detCaseFile = CaseFileCodec.deterministic(
            studentName = snapshot.studentName,
            riskLevel = snapshot.riskLevel,
            causeFamily = snapshot.causeFamily,
            topSignalLabel = snapshot.signals.maxByOrNull { it.severity }?.label ?: "needs attention",
        )
        return CaseworkerResult(
            studentCode = snapshot.studentCode,
            caseFile = detCaseFile,
            modelUsed = false,
        )
    }

    /**
     * Run the caseworker agent for a batch of students (the deep-look set
     * from Triage). Returns one Case File per student.
     */
    suspend fun reviewBatch(
        schoolId: UUID,
        snapshots: List<PewsSnapshot>,
    ): List<CaseworkerResult> {
        KillSwitchGuard.require("caseworker")
        if (snapshots.isEmpty()) return emptyList()

        val results = mutableListOf<CaseworkerResult>()
        for (snap in snapshots) {
            runCatching { review(schoolId, snap) }
                .onSuccess { results.add(it) }
                .onFailure { e ->
                    log.warn("Caseworker: review failed for {}: {}", snap.studentCode, e.message)
                    // Still produce a deterministic fallback
                    results.add(CaseworkerResult(
                        studentCode = snap.studentCode,
                        caseFile = CaseFileCodec.deterministic(
                            studentName = snap.studentName,
                            riskLevel = snap.riskLevel,
                            causeFamily = snap.causeFamily,
                            topSignalLabel = snap.signals.maxByOrNull { it.severity }?.label ?: "needs attention",
                        ),
                        modelUsed = false,
                    ))
                }
        }
        log.info("Caseworker: reviewed {} students for school {} ({} model-generated, {} deterministic)",
            results.size, schoolId, results.count { it.modelUsed }, results.count { !it.modelUsed })
        return results
    }

    // ── Agent prompt + execution ───────────────────────────────────────────

    private val systemPrompt = """
        You are a school caseworker AI assistant. You analyze at-risk students
        and produce a structured Case File with a sequenced intervention plan.

        STRICT RULES:
        - Use ONLY data from the provided snapshot and tool results. NEVER invent
          numbers, names, or facts. Every figure in your output must come from
          the deterministic data.
        - Call tools to gather context before producing your plan. Use
          get_student_history to see trends, get_past_interventions to avoid
          repeating failed actions, get_similar_resolved_cases to learn what
          worked, get_calendar_context to time interventions, and
          get_parent_responsiveness to choose the communication channel.
        - Produce a Case File as JSON with this exact schema:
        {
          "narrative": "1-2 grounded sentences explaining the situation",
          "hypotheses": [{"cause": "...", "confidence": 0.0-1.0, "evidence": ["..."]}],
          "plan": [{"step": 1, "action": "...", "owner": "class_teacher", "sla_days": N, "rationale": "...", "condition": "optional"}],
          "parent_draft": {"language": "hi", "tone": "warm, non-clinical", "body": "vernacular message to parent"},
          "urgency": "low|medium|high",
          "skip_reason": null or "reason to defer"
        }
        - The plan must be a SEQUENCE (2-4 steps), not one action. Each step
          should have a clear owner and SLA.
        - The parent_draft body must be in vernacular (Hindi by default), warm
          and non-clinical. Never mention "risk", "score", or "PEWS". Include
          a concrete next step.
        - If calendar context shows exam week or holiday, set skip_reason
          appropriately and defer parent contact.
        - Condition later steps on earlier outcomes (e.g. "if no improvement
          after step 1").
    """.trimIndent()

    private suspend fun runAgent(
        schoolId: UUID, snapshot: PewsSnapshot,
    ): AiService.AgentResult? {
        if (!AiService.anyProviderConfigured()) {
            log.debug("Caseworker: no AI provider — using deterministic for {}", snapshot.studentCode)
            return null
        }

        val userPrompt = buildUserPrompt(snapshot)
        val tools = CaseworkerTools.allTools()

        val result = AiService.runAgent(
            feature = "pews_caseworker",
            lane = AiLane.REASON,
            systemPrompt = systemPrompt,
            userPrompt = userPrompt,
            tools = tools,
            schoolId = schoolId,
            containsPii = true,
            maxSteps = 6,
            temperature = 0.3,
            maxTokens = 2048,
        )

        if (!result.ok) {
            log.warn("Caseworker: agent failed for {} — {}", snapshot.studentCode, result.errorMessage)
            return null
        }

        log.info("Caseworker: agent completed for {} ({} steps, {} tool calls, {}+{} tokens, provider={})",
            snapshot.studentCode, result.stepsTaken, result.toolCallsMade,
            result.totalInputTokens, result.totalOutputTokens, result.providerUsed)
        return result
    }

    private fun buildUserPrompt(snap: PewsSnapshot): String = buildString {
        appendLine("Student: ${snap.studentName} (${snap.studentCode})")
        appendLine("Class: ${snap.className}-${snap.section}")
        appendLine("Run date: ${snap.runDate}")
        appendLine("Risk score: ${snap.riskScore} (${snap.riskLevel})")
        appendLine("Attendance: ${snap.attendancePct ?: "N/A"}%")
        appendLine("Marks: ${snap.marksPct ?: "N/A"}%")
        appendLine("Leave count: ${snap.leaveCount}")
        if (snap.attendanceSlope != null) appendLine("Attendance trend: ${String.format("%.2f", snap.attendanceSlope)}/day")
        if (snap.marksSlope != null) appendLine("Marks trend: ${String.format("%.2f", snap.marksSlope)}/day")
        if (snap.causeFamily != null) appendLine("Cause family: ${snap.causeFamily}")
        if (snap.leadingScore != null) appendLine("Leading score: ${snap.leadingScore}")
        if (snap.confidence != null) appendLine("Data confidence: ${snap.confidence}")
        appendLine()
        appendLine("Signals:")
        for (s in snap.signals) {
            appendLine("  - [${s.kind}] ${s.label} (severity ${s.severity})" +
                if (s.isLeading) " [LEADING]" else "")
        }
        appendLine()
        appendLine("Analyze this student and produce a Case File. Call tools to gather context first.")
    }
}

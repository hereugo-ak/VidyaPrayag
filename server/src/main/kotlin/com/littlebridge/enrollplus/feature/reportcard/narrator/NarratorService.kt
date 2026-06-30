// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/reportcard/narrator/NarratorService.kt
package com.littlebridge.enrollplus.feature.reportcard.narrator

import com.littlebridge.enrollplus.feature.ai.AiLane
import com.littlebridge.enrollplus.feature.ai.AiService
import com.littlebridge.enrollplus.feature.reportcard.core.ReportCardConfig
import com.littlebridge.enrollplus.feature.reportcard.core.ReportCardConstants
import com.littlebridge.enrollplus.feature.reportcard.core.ReportCardKillSwitch
import com.littlebridge.enrollplus.feature.reportcard.rollup.ReportFactBundle
import com.littlebridge.enrollplus.feature.reportcard.rollup.ReportFactBundle.Companion.toJson
import com.littlebridge.enrollplus.feature.reportcard.triage.ReportTriageService
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * Tier 2 — Narrator Agent.
 *
 * Runs the tool-using agent loop via [AiService.runAgent] with 6 read-only
 * tools. The agent gathers context, then produces a structured [ReportDraft]
 * JSON. The [ReportGroundingGuard] verifies every number/grade/name against
 * the deterministic [ReportFactBundle]. Ungrounded fields are dropped and
 * the draft is flagged for review.
 *
 * Graceful degradation:
 *   - AI unavailable → deterministic draft from fact bundle alone.
 *   - JSON parse failure → one repair retry, then deterministic fallback.
 *   - Grounding failure → fields dropped, draft flagged for review.
 *
 * Per-student cache: the cache key includes fact_hash + template_version +
 * language so editing a template or changing language triggers re-narration.
 *
 * SOLID:
 *   S → Single responsibility: narration.
 *   D → AiService + tools + guard are dependencies, not hardcoded.
 *   L → Tools substitutable for AgentTool interface.
 *
 * Kill switch: [KillSwitchGuard.require] at entry with "reportcard_narrator".
 */
class NarratorService(
    private val triageService: ReportTriageService = ReportTriageService(),
) {
    private val log = LoggerFactory.getLogger("NarratorService")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    data class NarrationResult(
        val draft: ReportDraft,
        val draftJson: String,
        val providerUsed: String?,
        val modelUsed: String?,
        val tokensUsed: Int,
        val groundingFlags: List<String>,
        val groundingPassed: Boolean,
        val usedFallback: Boolean,
    )

    /**
     * Generate a narrated report draft for a single student.
     *
     * @param schoolId     School UUID
     * @param bundle       Tier-0 deterministic fact bundle
     * @param classContext  Tier-1 class-context paragraph (nullable)
     * @param language     Narrative language (default "en")
     */
    suspend fun narrate(
        schoolId: UUID,
        bundle: ReportFactBundle,
        classContext: String?,
        language: String = "en",
    ): NarrationResult {
        ReportCardKillSwitch.require(ReportCardConstants.MODULE_NARRATOR)
        log.info("Narrator: student={}, term={}, confidence={}", bundle.studentName, bundle.term, bundle.dataConfidence)

        // If data is insufficient, go straight to deterministic
        if (bundle.dataConfidence == ReportCardConstants.Confidence.INSUFFICIENT) {
            log.info("Narrator: insufficient data — using deterministic fallback")
            return deterministicResult(bundle, language)
        }

        // Build the system prompt
        val systemPrompt = buildSystemPrompt(bundle.board, language)

        // Build the user prompt with the fact bundle
        val userPrompt = buildUserPrompt(bundle, classContext, language)

        // Run the agent with tools and exponential backoff retry
        val agentResult = runWithRetry {
            AiService.runAgent(
                feature = ReportCardConstants.AI_FEATURE_TAG,
                lane = AiLane.REASON,
                systemPrompt = systemPrompt,
                userPrompt = userPrompt,
                tools = NarratorTools.allTools(),
                schoolId = schoolId,
                containsPii = true,
                maxSteps = ReportCardConfig.narratorMaxSteps,
                temperature = ReportCardConfig.narratorTemperature,
                maxTokens = ReportCardConfig.narratorMaxTokens,
            )
        }

        if (!agentResult.ok || agentResult.content.isNullOrBlank()) {
            log.warn("Narrator: AI unavailable ({}), using deterministic fallback", agentResult.errorMessage)
            return deterministicResult(bundle, language)
        }

        // Parse the JSON response
        var draft = ReportDraft.fromJson(agentResult.content)
        if (draft == null) {
            // One repair retry: ask the model to fix the JSON
            log.warn("Narrator: JSON parse failed, attempting repair")
            val repairResult = AiService.complete(
                feature = ReportCardConstants.AI_FEATURE_TAG,
                lane = AiLane.REASON,
                messages = listOf(
                    com.littlebridge.enrollplus.feature.ai.LlmMessage("system", "You are a JSON repair tool. Fix the JSON and return only valid JSON."),
                    com.littlebridge.enrollplus.feature.ai.LlmMessage("user", "Fix this JSON to match the ReportDraft schema: ${agentResult.content}"),
                ),
                containsPii = true,
                schoolId = schoolId,
                temperature = 0.0,
                maxTokens = ReportCardConfig.narratorMaxTokens,
                cache = false,
            )
            draft = if (repairResult.ok && !repairResult.content.isNullOrBlank()) {
                ReportDraft.fromJson(repairResult.content)
            } else null

            if (draft == null) {
                log.warn("Narrator: JSON repair failed, using deterministic fallback")
                return deterministicResult(bundle, language)
            }
        }

        // Run grounding guard
        val groundingResult = ReportGroundingGuard.verify(draft, bundle)
        val finalDraft = groundingResult.draft
        val draftJson = ReportDraft.toJson(finalDraft)

        val status = if (groundingResult.passed) {
            ReportCardConstants.DraftStatus.DRAFT
        } else {
            ReportCardConstants.DraftStatus.FLAGGED
        }

        log.info("Narrator: success, grounding={}, flags={}", groundingResult.passed, groundingResult.flags.size)

        return NarrationResult(
            draft = finalDraft,
            draftJson = draftJson,
            providerUsed = agentResult.providerUsed,
            modelUsed = agentResult.modelUsed,
            tokensUsed = agentResult.totalInputTokens + agentResult.totalOutputTokens,
            groundingFlags = groundingResult.flags,
            groundingPassed = groundingResult.passed,
            usedFallback = false,
        )
    }

    // ── Private helpers ────────────────────────────────────────────────

    private suspend fun runWithRetry(block: suspend () -> AiService.AgentResult): AiService.AgentResult {
        var lastResult: AiService.AgentResult = AiService.AgentResult.unavailable("not attempted")
        for (attempt in 0..ReportCardConfig.retryBackoffMs.size) {
            lastResult = block()
            if (lastResult.ok) return lastResult
            if (attempt < ReportCardConfig.retryBackoffMs.size) {
                val delayMs = ReportCardConfig.retryBackoffMs[attempt]
                log.info("Narrator: retry {} after {}ms (last error: {})", attempt + 1, delayMs, lastResult.errorMessage)
                delay(delayMs)
            }
        }
        return lastResult
    }

    private fun buildSystemPrompt(board: String, language: String): String = """
        You are a professional report card narrator for an Indian school following the $board board.
        Your job is to write honest, specific, and encouraging narrative comments for a student's report card.

        CRITICAL RULES (LAW 6 — Grounding):
        1. Every number, percentage, and grade you mention MUST come from the fact bundle or tool results.
        2. NEVER invent statistics, scores, or grades that are not in the data.
        3. If data is missing, say so honestly — do not fabricate.
        4. Use the student's real name from the fact bundle.
        5. Keep narratives to 2-3 sentences per subject.
        6. The overall summary should be 3-4 sentences.
        7. The parent summary should be in ${languageName(language)} and simple, encouraging, and actionable.
        8. Be honest about areas needing improvement — don't sugarcoat, but be constructive.

        You have access to tools that can fetch additional context. Use them if you need
        to check past performance, PEWS risk data, or board rubric details.

        Return your response as JSON matching this schema:
        {
          "studentName": "string",
          "className": "string",
          "section": "string",
          "term": "string",
          "subjects": [{"subject": "string", "grade": "string", "percentage": 0.0, "narrative": "string", "movement": "string"}],
          "overallSummary": "string",
          "parentSummary": "string",
          "projectionNote": "string",
          "focusAreas": ["string"],
          "strengths": ["string"],
          "improvementAreas": ["string"],
          "teacherNote": "string"
        }
    """.trimIndent()

    private fun buildUserPrompt(bundle: ReportFactBundle, classContext: String?, language: String): String {
        val sb = StringBuilder()
        sb.append("Generate a report card narrative for the following student.\n\n")
        sb.append("FACT BUNDLE (deterministic — all numbers here are verified):\n")
        sb.append(toJson(bundle)).append("\n\n")

        if (classContext != null) {
            sb.append("CLASS CONTEXT (shared for all students in this class):\n")
            sb.append(classContext).append("\n\n")
        }

        sb.append("Language for parent summary: ${languageName(language)}\n")
        sb.append("Board: ${bundle.board}\n")
        sb.append("Data confidence: ${bundle.dataConfidence}\n")

        if (bundle.projection != null) {
            sb.append("Deterministic projection: ${bundle.projection.likelyGrade} ")
            sb.append("(${bundle.projection.likelyPercentageRange}%), ")
            sb.append("at-risk=${bundle.projection.atRisk}\n")
            sb.append("Projection basis: ${bundle.projection.basis}\n")
        }

        sb.append("\nUse the tools if you need more context. ")
        sb.append("Return ONLY the JSON — no preamble, no markdown fences.")

        return sb.toString()
    }

    private fun languageName(code: String): String = when (code.lowercase()) {
        "hi" -> "Hindi"
        "en" -> "English"
        "ta" -> "Tamil"
        "te" -> "Telugu"
        "kn" -> "Kannada"
        "mr" -> "Marathi"
        "bn" -> "Bengali"
        "gu" -> "Gujarati"
        "pa" -> "Punjabi"
        "ur" -> "Urdu"
        else -> "English"
    }

    private fun deterministicResult(bundle: ReportFactBundle, language: String): NarrationResult {
        val draft = deterministicDraft(bundle, language)
        return NarrationResult(
            draft = draft,
            draftJson = ReportDraft.toJson(draft),
            providerUsed = null,
            modelUsed = null,
            tokensUsed = 0,
            groundingFlags = emptyList(),
            groundingPassed = true,
            usedFallback = true,
        )
    }
}

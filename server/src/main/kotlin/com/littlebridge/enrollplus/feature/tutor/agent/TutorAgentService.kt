// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/tutor/agent/TutorAgentService.kt
package com.littlebridge.enrollplus.feature.tutor.agent

import com.littlebridge.enrollplus.feature.ai.AiLane
import com.littlebridge.enrollplus.feature.ai.AiService
import com.littlebridge.enrollplus.feature.tutor.core.TutorConstants
import com.littlebridge.enrollplus.feature.tutor.core.TutorKillSwitch
import com.littlebridge.enrollplus.feature.tutor.data.TutorSessionRepository
import com.littlebridge.enrollplus.feature.tutor.sense.LearnerBundleBuilder
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * TIER 2 — Tutor Agent Service.
 *
 * Runs AiService.runAgent() with the Tutor toolset, parses the structured
 * TutorTurn, passes it through TutorGroundingGuard, and persists the session.
 *
 * Graceful degradation: if no AI provider, produces a deterministic TutorTurn
 * (Socratic step) from the bundle alone (no tools, no LLM).
 *
 * Kill-switched under module name "tutor_agent".
 *
 * SOLID:
 *   S → Single responsibility: orchestrates the agent loop.
 *   D → Depends on AiService abstraction, not a specific provider.
 *
 * Spec: AI_TUTOR_2.0_AGENTIC_REDESIGN.md §6.4
 */
class TutorAgentService(
    private val sessionRepo: TutorSessionRepository = TutorSessionRepository(),
    private val bundleBuilder: LearnerBundleBuilder = LearnerBundleBuilder(),
) {
    private val log = LoggerFactory.getLogger("TutorAgentService")

    data class TutorResult(
        val sessionId: UUID?,
        val turn: TutorTurn,
        val modelUsed: Boolean,
        val providerUsed: String? = null,
        val toolCallsMade: Int = 0,
        val stepsTaken: Int = 0,
        val grounded: Boolean = true,
        val safetyFlag: String? = null,
    )

    /**
     * Resolve a doubt: run the agent loop, ground the output, persist the session.
     *
     * @param schoolId  tenant scope (from JWT, never from the model)
     * @param childId   the child's UUID
     * @param subjectId the subject's UUID
     * @param question  the child's doubt text
     * @return a grounded TutorTurn wrapped in a TutorResult
     */
    suspend fun resolveDoubt(
        schoolId: UUID,
        childId: UUID,
        subjectId: UUID?,
        question: String,
    ): TutorResult {
        TutorKillSwitch.require(TutorConstants.MODULE_AGENT)

        // Build the deterministic bundle (Tier 0) — null subjectId is OK,
        // bundle will be null and the agent runs in DIAGNOSTIC mode
        val bundle = if (subjectId != null) bundleBuilder.build(childId, subjectId) else null

        // Try the agentic path (LLM + tools)
        val agentResult = runAgent(schoolId, childId, subjectId, question, bundle)

        if (agentResult != null) {
            val turn = TutorTurnCodec.parse(agentResult.content!!)
            if (turn != null) {
                // Ground the turn against the deterministic bundle
                val grounded = if (bundle != null) {
                    TutorGroundingGuard.verify(turn, bundle)
                } else {
                    turn // No bundle → can't ground, but serve with a warning
                }

                val wasGrounded = grounded != null && grounded.studentFacing.text == turn.studentFacing.text
                val finalTurn = grounded ?: TutorTurnCodec.deterministic(question)

                // Persist the session
                val sessionId = persistSession(
                    schoolId, childId, subjectId, finalTurn,
                    agentResult.providerUsed, agentResult.toolCallsMade,
                    wasGrounded,
                )

                return TutorResult(
                    sessionId = sessionId,
                    turn = finalTurn,
                    modelUsed = true,
                    providerUsed = agentResult.providerUsed,
                    toolCallsMade = agentResult.toolCallsMade,
                    stepsTaken = agentResult.stepsTaken,
                    grounded = wasGrounded,
                )
            } else {
                log.warn("TutorAgent: model output failed TutorTurn parse — falling back to deterministic")
            }
        }

        // Fallback: deterministic TutorTurn (no AI)
        val detTurn = TutorTurnCodec.deterministic(question)
        val sessionId = persistSession(
            schoolId, childId, subjectId, detTurn,
            null, 0, true,
        )
        return TutorResult(
            sessionId = sessionId,
            turn = detTurn,
            modelUsed = false,
        )
    }

    // ── Agent prompt + execution ───────────────────────────────────────────

    private val systemPrompt = """
        You are an AI Tutor for school students. You help children understand
        concepts and solve problems using the SOCRATIC method — you guide,
        you do NOT solve problems for them.

        STRICT RULES:
        - Use ONLY data from the provided learner bundle and tool results.
          NEVER invent numbers, scores, or facts. Every figure in your output
          must come from the deterministic data (LAW 6).
        - Call tools to gather context before producing your response. Use
          get_learner_bundle for the full picture, get_weak_topics to see
          where the child struggles, get_syllabus_position to check what's
          been covered (NEVER teach ahead), get_due_reviews for FSRS state,
          get_homework_context for homework load.
        - If the child's doubt reveals a misconception, call log_misconception
          to record it. This is the ONLY write tool.
        - Produce a TutorTurn as JSON with this exact schema:
        {
          "mode": "SOCRATIC_STEP | HINT | EXPLANATION | PRACTICE_SET | PLAN_UPDATE | ESCALATE",
          "groundedRefs": [{"topicId": "...", "source": "MARKS|SYLLABUS|NCERT|RAG", "value": "..."}],
          "studentFacing": {"text": "...", "mathBlocks": ["LaTeX..."], "nextPrompt": "..."},
          "practice": [{"questionId": "...", "stem": "...", "options": ["..."], "answerKey": "...", "topicId": "...", "difficulty": "easy|medium|hard"}],
          "planDelta": {"addReviews": [{"topicId": "...", "priority": "high|medium|low"}], "adjustDifficulty": "..."},
          "teacherFlag": {"topicId": "...", "reason": "...", "severity": "low|medium|high"},
          "misconception": {"topicId": "...", "type": "...", "evidence": "..."}
        }
        - Mode SOCRATIC_STEP: guide the child with a question that helps them
          think. "What do you get when you find a common denominator first?"
        - Mode HINT: give a targeted hint, not the answer.
        - Mode EXPLANATION: explain a concept (only if the child is stuck after
          hints). Keep it grounded in the NCERT framing.
        - Mode PRACTICE_SET: generate 2-3 practice questions targeted at the
          child's weakest covered topic. Include answer keys.
        - Mode ESCALATE: if the child asks for the answer repeatedly, switch to
          "let's break it down" and set teacherFlag.
        - NEVER just hand over the full worked solution. That is a last resort,
          gated, and logged.
        - If the doubt is off-syllabus (not in coveredTopicIds), decline and
          offer to flag it for the teacher.
        - mathBlocks: use LaTeX notation for any math expressions.
    """.trimIndent()

    private suspend fun runAgent(
        schoolId: UUID,
        childId: UUID,
        subjectId: UUID?,
        question: String,
        bundle: com.littlebridge.enrollplus.feature.tutor.sense.LearnerBundle?,
    ): AiService.AgentResult? {
        if (!AiService.anyProviderConfigured()) {
            log.debug("TutorAgent: no AI provider — using deterministic for child {}", childId)
            return null
        }

        val userPrompt = buildUserPrompt(childId, subjectId, question, bundle)
        val tools = TutorTools.allTools()

        val result = AiService.runAgent(
            feature = "ai_tutor",
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
            log.warn("TutorAgent: agent failed for child {} — {}", childId, result.errorMessage)
            return null
        }

        log.info("TutorAgent: agent completed for child {} ({} steps, {} tool calls, {}+{} tokens, provider={})",
            childId, result.stepsTaken, result.toolCallsMade,
            result.totalInputTokens, result.totalOutputTokens, result.providerUsed)
        return result
    }

    private fun buildUserPrompt(
        childId: UUID,
        subjectId: UUID?,
        question: String,
        bundle: com.littlebridge.enrollplus.feature.tutor.sense.LearnerBundle?,
    ): String = buildString {
        appendLine("Child ID: $childId")
        appendLine("Subject ID: $subjectId")
        appendLine()
        if (bundle != null) {
            appendLine("Learner Bundle Summary:")
            appendLine("  Has marks: ${bundle.dataConfidence.hasMarks}")
            appendLine("  Has syllabus: ${bundle.dataConfidence.hasSyllabus}")
            appendLine("  Has homework: ${bundle.dataConfidence.hasHomework}")
            appendLine("  Weak topics: ${bundle.weakTopics.size}")
            appendLine("  Covered topics: ${bundle.syllabusPosition.coveredTopicIds.size}")
            appendLine("  Due reviews: ${bundle.reviewQueue.size}")
            if (bundle.weakTopics.isNotEmpty()) {
                appendLine("  Weakest topic: ${bundle.weakTopics.first().topicId} (${bundle.weakTopics.first().pct}%)")
            }
        } else {
            appendLine("Learner Bundle: NOT AVAILABLE (child may not be linked to school)")
            appendLine("Run in DIAGNOSTIC mode — ask gentle placement questions.")
        }
        appendLine()
        appendLine("Child's doubt: $question")
        appendLine()
        appendLine("Analyze this doubt and produce a TutorTurn. Call tools to gather context first.")
    }

    private suspend fun persistSession(
        schoolId: UUID,
        childId: UUID,
        subjectId: UUID?,
        turn: TutorTurn,
        providerUsed: String?,
        toolCallsMade: Int,
        grounded: Boolean,
    ): UUID? = runCatching {
        sessionRepo.insert(
            schoolId = schoolId,
            childId = childId,
            subjectId = subjectId,
            mode = "DOUBT",
            turns = TutorTurnCodec.encode(turn),
            groundedRefs = turn.groundedRefs.joinToString(",") { "${it.source}:${it.value}" },
            providerUsed = providerUsed,
            tokensUsed = 0,
            cacheHit = false,
            safetyFlag = if (turn.mode == "ESCALATE") "repeated_answer_request" else null,
        )
    }.onFailure { log.warn("TutorAgent: failed to persist session — {}", it.message) }.getOrNull()
}

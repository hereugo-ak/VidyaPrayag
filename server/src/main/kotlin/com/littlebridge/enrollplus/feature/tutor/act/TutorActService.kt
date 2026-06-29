// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/tutor/act/TutorActService.kt
package com.littlebridge.enrollplus.feature.tutor.act

import com.littlebridge.enrollplus.feature.tutor.agent.PracticeQuestion
import com.littlebridge.enrollplus.feature.tutor.agent.TutorTurn
import com.littlebridge.enrollplus.feature.tutor.core.TutorConstants
import com.littlebridge.enrollplus.feature.tutor.core.TutorKillSwitch
import com.littlebridge.enrollplus.feature.tutor.data.TutorMasteryRepository
import com.littlebridge.enrollplus.feature.tutor.data.TutorMisconceptionRepository
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * TIER 3 — Act Service.
 *
 * Deterministic effects + safety rails. Nothing the model says reaches a
 * child or a record until Tier 3 has run:
 * - GroundingGuard.verify (already done in Tier 2 — TutorAgentService)
 * - SafetyClassifier on the child's input
 * - Auto-grade practice answers + FSRS update
 * - Escalation rails (deterministic, never the model's call)
 * - Mastery update
 *
 * Kill-switched under module name "tutor_act".
 *
 * SOLID:
 *   S → Single responsibility: deterministic effects and safety.
 *   D → Depends on [FsrsScheduler], [SafetyClassifier], and repository abstractions.
 *
 * Spec: AI_TUTOR_2.0_AGENTIC_REDESIGN.md §7
 */
class TutorActService(
    private val fsrsScheduler: FsrsScheduler = FsrsScheduler(),
    private val masteryRepo: TutorMasteryRepository = TutorMasteryRepository(),
    private val misconceptionRepo: TutorMisconceptionRepository = TutorMisconceptionRepository(),
) {
    private val log = LoggerFactory.getLogger("TutorActService")

    data class SafetyCheckResult(
        val tripped: Boolean,
        val category: String?,
        val action: String,
    )

    /**
     * Run the safety classifier on the child's input text.
     * If tripped, the caller MUST NOT proceed with AI tutoring —
     * escalate to safeguarding contact immediately.
     */
    fun checkSafety(childInput: String): SafetyCheckResult {
        val result = SafetyClassifier.classify(childInput)
        return SafetyCheckResult(
            tripped = result.tripped,
            category = result.category,
            action = result.action,
        )
    }

    /**
     * Auto-grade a practice answer and update FSRS + mastery.
     *
     * @param schoolId  tenant scope
     * @param childId   the child's UUID
     * @param question  the practice question
     * @param childAnswer  the child's answer
     * @return the grade (0-100) and whether it was correct
     */
    suspend fun autoGrade(
        schoolId: UUID,
        childId: UUID,
        subjectId: UUID,
        question: PracticeQuestion,
        childAnswer: String,
    ): AutoGradeResult {
        TutorKillSwitch.require(TutorConstants.MODULE_ACT)

        val isCorrect = gradeAnswer(question.answerKey, childAnswer)
        val gradePct = if (isCorrect) 100 else 0

        // Update FSRS state for the topic
        val topicUuid = runCatching { UUID.fromString(question.topicId) }.getOrNull()
        if (topicUuid != null) {
            fsrsScheduler.review(schoolId, childId, topicUuid, gradePct)

            // Update mastery
            masteryRepo.upsert(
                schoolId = schoolId,
                childId = childId,
                subjectId = subjectId,
                topicId = topicUuid,
                mastery = gradePct.toDouble(),
                source = "PRACTICE",
                attempts = 1,
                correct = if (isCorrect) 1 else 0,
            )
        }

        log.info("TutorAct: auto-graded answer for child {} on topic {} — correct={}",
            childId, question.topicId, isCorrect)

        return AutoGradeResult(
            correct = isCorrect,
            gradePct = gradePct,
            feedback = if (isCorrect) "Correct!" else "Not quite — let's review this.",
        )
    }

    /**
     * Check if the child is repeatedly asking for the answer (escalation rail).
     * If so, switch to "let's break it down" mode and optionally flag for teacher.
     *
     * @param sessionTurns  the list of previous turns in this session
     * @param currentQuestion  the current question
     * @return true if escalation is needed
     */
    fun checkEscalation(sessionTurns: List<TutorTurn>, currentQuestion: String): EscalationResult {
        val answerKeywords = listOf("just tell me the answer", "give me the answer",
            "what is the answer", "solution please", "solve it for me")
        val lower = currentQuestion.lowercase()

        val isAnswerRequest = answerKeywords.any { lower.contains(it) }
        if (!isAnswerRequest) return EscalationResult(escalate = false)

        // Count how many times the child has asked for the answer in this session
        val previousEscalations = sessionTurns.count { it.mode == "ESCALATE" }
        if (previousEscalations >= 2) {
            log.warn("TutorAct: child has asked for answer {} times — escalating to teacher", previousEscalations + 1)
            return EscalationResult(
                escalate = true,
                reason = "repeated_answer_request",
                notifyTeacher = true,
            )
        }

        return EscalationResult(
            escalate = true,
            reason = "answer_request",
            notifyTeacher = false,
        )
    }

    // ── Internal ──────────────────────────────────────────────────────────

    data class AutoGradeResult(
        val correct: Boolean,
        val gradePct: Int,
        val feedback: String,
    )

    data class EscalationResult(
        val escalate: Boolean,
        val reason: String? = null,
        val notifyTeacher: Boolean = false,
    )

    /**
     * Simple exact-match grading (case-insensitive, trimmed).
     * In production this could be enhanced with fuzzy matching or LLM grading,
     * but the deterministic baseline is exact match.
     */
    private fun gradeAnswer(answerKey: String, childAnswer: String): Boolean {
        val normalized1 = answerKey.trim().lowercase().replace("\\s+".toRegex(), " ")
        val normalized2 = childAnswer.trim().lowercase().replace("\\s+".toRegex(), " ")
        return normalized1 == normalized2
    }
}

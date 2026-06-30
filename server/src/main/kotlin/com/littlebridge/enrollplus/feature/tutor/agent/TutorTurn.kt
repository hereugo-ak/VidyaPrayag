// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/tutor/agent/TutorTurn.kt
package com.littlebridge.enrollplus.feature.tutor.agent

import kotlinx.serialization.Serializable

/**
 * The structured TutorTurn — replaces the free-text chat blob.
 *
 * The agent never returns raw prose to the client. It returns a typed object
 * (the CaseFile analogue). Every fact in `studentFacing` must cite a
 * `groundedRef` — verified by [TutorGroundingGuard] before the child sees it.
 *
 * Spec: AI_TUTOR_2.0_AGENTIC_REDESIGN.md §6.3
 */
@Serializable
data class TutorTurn(
    val mode: String,                    // SOCRATIC_STEP | HINT | EXPLANATION | PRACTICE_SET | PLAN_UPDATE | ESCALATE
    val groundedRefs: List<GroundedRef> = emptyList(),
    val studentFacing: StudentFacing,
    val practice: List<PracticeQuestion>? = null,
    val planDelta: PlanDelta? = null,
    val teacherFlag: TeacherFlag? = null,
    val misconception: MisconceptionLog? = null,
)

@Serializable
data class GroundedRef(
    val topicId: String,
    val source: String,                  // MARKS | SYLLABUS | NCERT | RAG
    val value: String,
)

@Serializable
data class StudentFacing(
    val text: String,
    val mathBlocks: List<String> = emptyList(),  // LaTeX strings
    val nextPrompt: String? = null,
)

@Serializable
data class PracticeQuestion(
    val questionId: String,
    val stem: String,
    val options: List<String>? = null,
    val answerKey: String,
    val topicId: String,
    val difficulty: String,              // easy | medium | hard
)

@Serializable
data class PlanDelta(
    val addReviews: List<AddReview> = emptyList(),
    val adjustDifficulty: String? = null,
)

@Serializable
data class AddReview(
    val topicId: String,
    val priority: String,                // high | medium | low
)

@Serializable
data class TeacherFlag(
    val topicId: String,
    val reason: String,
    val severity: String,                // low | medium | high
)

@Serializable
data class MisconceptionLog(
    val topicId: String,
    val type: String,
    val evidence: String,
)

// ──────────────────────────────────────────────────────────────────────────
// Codec (parse + deterministic fallback)
// ──────────────────────────────────────────────────────────────────────────

object TutorTurnCodec {
    private val json = kotlinx.serialization.json.Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        encodeDefaults = true
    }

    /** Parse a model's JSON output into a TutorTurn. Returns null on failure. */
    fun parse(raw: String): TutorTurn? = try {
        var cleaned = raw.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```")
            .trim()
        if (!cleaned.startsWith("{")) {
            val firstBrace = cleaned.indexOf('{')
            val lastBrace = cleaned.lastIndexOf('}')
            if (firstBrace >= 0 && lastBrace > firstBrace) {
                cleaned = cleaned.substring(firstBrace, lastBrace + 1)
            }
        }
        json.decodeFromString(TutorTurn.serializer(), cleaned)
    } catch (e: Exception) {
        null
    }

    /** Serialize a TutorTurn to JSON for storage. */
    fun encode(turn: TutorTurn): String =
        json.encodeToString(TutorTurn.serializer(), turn)

    /**
     * Deterministic fallback when no AI is available or the model output
     * fails parsing. Guides the child to break the problem into steps.
     */
    fun deterministic(question: String): TutorTurn = TutorTurn(
        mode = "SOCRATIC_STEP",
        groundedRefs = emptyList(),
        studentFacing = StudentFacing(
            text = "Let's break this down step by step. What part of the problem " +
                "do you think we should look at first?",
            nextPrompt = "Try identifying what's given and what we need to find.",
        ),
    )
}

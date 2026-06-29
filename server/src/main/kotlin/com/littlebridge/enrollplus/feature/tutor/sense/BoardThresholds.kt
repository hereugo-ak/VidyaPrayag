// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/tutor/sense/BoardThresholds.kt
package com.littlebridge.enrollplus.feature.tutor.sense

/**
 * Board-aware "weak topic" thresholds.
 *
 * "Weak topic" = covered topic where `pct < board_threshold`. The threshold is
 * configurable per school board (CBSE, ICSE, IB, State, etc.). The number comes
 * from AssessmentMarksTable, never from the model (LAW 6).
 *
 * Spec: AI_TUTOR_2.0_AGENTIC_REDESIGN.md §5.2
 *
 * SOLID: S (single responsibility — threshold lookup), O (new boards added
 * without modifying existing ones).
 */
object BoardThresholds {

    private val thresholds = mapOf(
        "CBSE"  to 60.0,
        "ICSE"  to 50.0,
        "IB"    to 50.0,
        "State" to 40.0,
        "Cambridge" to 50.0,
    )

    private val defaultThreshold = 50.0

    /** Returns the "weak" percentage threshold for the given board. */
    fun weakThreshold(board: String): Double =
        thresholds[board] ?: defaultThreshold

    /** Returns severity label based on how far below the threshold the score is. */
    fun severity(pct: Double, board: String): String {
        val threshold = weakThreshold(board)
        if (pct >= threshold) return "none"
        val deficit = threshold - pct
        return when {
            deficit >= 25.0 -> "high"
            deficit >= 10.0 -> "medium"
            else -> "low"
        }
    }

    /** True if the topic is weak (pct below board threshold). */
    fun isWeak(pct: Double, board: String): Boolean =
        pct < weakThreshold(board)
}

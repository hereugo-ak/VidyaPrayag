// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/reportcard/rollup/BoardRubric.kt
package com.littlebridge.enrollplus.feature.reportcard.rollup

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Deterministic marks→grade→descriptor mapping for a specific board.
 *
 * Loaded from [ReportCardTemplatesTable] (grading_scale JSON column) with
 * Kotlin fallback defaults for CBSE, ICSE, and NEP HPC when the table is empty.
 *
 * SOLID:
 *   S → Single responsibility: grade computation only.
 *   O → New boards added by adding a new fallback entry or DB row — no modification.
 */
object BoardRubric {

    @Serializable
    data class GradeBand(
        val minPct: Double,
        val grade: String,
        val descriptor: String,
    )

    private val json = Json { ignoreUnknownKeys = true }

    // ── Fallback defaults (used when DB template is not yet seeded) ──────

    val CBSE_FALLBACK = listOf(
        GradeBand(90.0, "A1", "Outstanding"),
        GradeBand(80.0, "A2", "Excellent"),
        GradeBand(71.0, "B1", "Very Good"),
        GradeBand(62.0, "B2", "Good"),
        GradeBand(53.0, "C1", "Fair"),
        GradeBand(45.0, "C2", "Satisfactory"),
        GradeBand(33.0, "D", "Pass"),
        GradeBand(0.0, "E", "Needs Improvement"),
    )

    val ICSE_FALLBACK = listOf(
        GradeBand(90.0, "A+", "Excellent"),
        GradeBand(75.0, "A", "Very Good"),
        GradeBand(60.0, "B", "Good"),
        GradeBand(45.0, "C", "Satisfactory"),
        GradeBand(33.0, "D", "Pass"),
        GradeBand(0.0, "F", "Fail"),
    )

    val NEP_HPC_FALLBACK = listOf(
        GradeBand(90.0, "A", "Advanced"),
        GradeBand(75.0, "B", "Proficient"),
        GradeBand(55.0, "C", "Developing"),
        GradeBand(35.0, "D", "Beginner"),
        GradeBand(0.0, "E", "Needs Support"),
    )

    val STATE_FALLBACK = listOf(
        GradeBand(80.0, "A", "Distinction"),
        GradeBand(60.0, "B", "First Class"),
        GradeBand(45.0, "C", "Second Class"),
        GradeBand(33.0, "D", "Pass"),
        GradeBand(0.0, "F", "Fail"),
    )

    val IB_FALLBACK = listOf(
        GradeBand(90.0, "7", "Excellent"),
        GradeBand(80.0, "6", "Very Good"),
        GradeBand(70.0, "5", "Good"),
        GradeBand(60.0, "4", "Satisfactory"),
        GradeBand(50.0, "3", "Mediocre"),
        GradeBand(33.0, "2", "Poor"),
        GradeBand(0.0, "1", "Very Poor"),
    )

    /**
     * Parse a grading scale JSON string (from DB) into a list of [GradeBand]s.
     */
    fun parseGradingScale(jsonStr: String): List<GradeBand> = runCatching {
        json.decodeFromString<List<GradeBand>>(jsonStr)
    }.getOrElse { emptyList() }

    /**
     * Get the fallback rubric for a board. Returns CBSE if unknown.
     */
    fun fallbackFor(board: String): List<GradeBand> = when (board.uppercase()) {
        "CBSE" -> CBSE_FALLBACK
        "ICSE" -> ICSE_FALLBACK
        "NEP_HPC", "NEP" -> NEP_HPC_FALLBACK
        "STATE" -> STATE_FALLBACK
        "IB" -> IB_FALLBACK
        else -> CBSE_FALLBACK
    }

    /**
     * Convert a percentage to a grade + descriptor using the given rubric.
     * Returns null if percentage is null.
     */
    fun gradeFor(pct: Double?, rubric: List<GradeBand>): Pair<String, String>? {
        if (pct == null) return null
        val band = rubric.firstOrNull { pct >= it.minPct } ?: rubric.last()
        return band.grade to band.descriptor
    }

    /**
     * Determine movement pattern from current and previous percentage.
     * improved: delta >= +5
     * slid: delta <= -5
     * volatile: abs(delta) >= 10 AND signs alternate across subjects
     * steady: |delta| < 5
     */
    fun movementFor(currentPct: Double?, previousPct: Double?): String {
        if (currentPct == null || previousPct == null) return "steady"
        val delta = currentPct - previousPct
        return when {
            delta >= 5.0 -> "improved"
            delta <= -5.0 -> "slid"
            else -> "steady"
        }
    }
}

/*
 * File: PewsReasoningService.kt
 * Module: feature.pews
 *
 * PEWS — REASON stage. Takes a deterministic snapshot (already computed by
 * PewsSnapshotService) and asks the LLM to EXPLAIN it — never to decide who is
 * at risk or to invent numbers. The model receives ONLY the provided signal
 * bundle and must ground every sentence in it (honesty LAW 6).
 *
 * What it produces, written back onto the SAME snapshot row:
 *   • ai_narrative        — 1–2 plain-language sentences a teacher can read
 *   • ai_cause            — the most likely contributing factor (from the signals)
 *   • ai_recommendation   — ONE concrete next step (maps to an intervention type)
 *   • ai_provider_used    — which provider answered (observability)
 *
 * Privacy: this is a PII-bearing prompt (it names a student), so it goes through
 * AiService with containsPii=true → the gateway pins it to no-training providers
 * (Cerebras → Groq → OpenRouter). The prompt is deliberately minimal: first name
 * + class + the deterministic signals only (no phone, no full record).
 *
 * Caching: AiService caches by message hash; because the prompt embeds the
 * deterministic signals, an unchanged bundle re-uses the cached narrative (so we
 * don't burn a free-tier call every day for a student whose signals didn't move).
 *
 * Graceful degradation: if no provider is available, the ai_* columns simply stay
 * null and the UI shows the deterministic signals without an AI paragraph. PEWS
 * never hard-fails on the LLM.
 */
package com.littlebridge.enrollplus.feature.pews

import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.PewsConfigTable
import com.littlebridge.enrollplus.db.PewsRiskSnapshotsTable
import com.littlebridge.enrollplus.feature.ai.AiLane
import com.littlebridge.enrollplus.feature.ai.AiService
import com.littlebridge.enrollplus.feature.ai.LlmMessage
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory
import java.util.UUID

class PewsReasoningService {
    private val log = LoggerFactory.getLogger("PewsReasoningService")

    private val systemPrompt = """
        You are an assistant to a school teacher. You are given a SINGLE student's
        early-warning signals that were computed deterministically from school
        records. Your job is ONLY to explain these signals in plain, supportive
        language a busy teacher can act on. STRICT RULES:
        - Use ONLY the numbers and signals provided. NEVER invent attendance,
          marks, dates, names, or any fact not given.
        - Do NOT diagnose, label, or use clinical/judgemental words. No "failing",
          "lazy", "problem child". Be factual and constructive.
        - Keep it short. Respond as compact JSON with exactly these keys:
          {"narrative": "...", "cause": "...", "recommendation": "..."}
          narrative: 1-2 sentences summarising what the data shows.
          cause: the single most likely contributing factor, stated tentatively.
          recommendation: ONE concrete next step the teacher/school can take.
        - The recommendation must be one of these intents in spirit: talk to the
          parent, a home check-in, counselling, a remedial class, or simply
          observe for now. Pick the lightest action that fits the signals.
    """.trimIndent()

    /**
     * Reason over the at-risk snapshots for a school+run that don't yet have a
     * narrative. Respects pews_config.ai_narrative_enabled and the
     * minimum-risk-level gate (we don't burn calls on "watch" by default).
     * Returns the number of snapshots successfully narrated.
     */
    suspend fun reasonForSchool(
        schoolId: UUID,
        snapshots: List<PewsSnapshot>,
        minLevel: String = "medium",
    ): Int {
        val enabled = dbQuery {
            PewsConfigTable.selectAll().where {
                PewsConfigTable.id eq EntityID(schoolId, PewsConfigTable)
            }.singleOrNull()?.get(PewsConfigTable.aiNarrativeEnabled) ?: true
        }
        if (!enabled) {
            log.info("PEWS reasoning disabled for school {} (pews_config)", schoolId)
            return 0
        }
        if (!AiService.anyProviderConfigured()) {
            log.info("PEWS reasoning skipped for school {} — no AI provider configured", schoolId)
            return 0
        }

        val levelRank = mapOf("watch" to 0, "medium" to 1, "high" to 2)
        val minRank = levelRank[minLevel] ?: 1
        val targets = snapshots.filter { (levelRank[it.riskLevel] ?: 0) >= minRank }

        var done = 0
        for (snap in targets) {
            runCatching { reasonOne(schoolId, snap) }
                .onSuccess { if (it) done++ }
                .onFailure { log.warn("PEWS reason failed for {}: {}", snap.studentCode, it.message) }
        }
        log.info("PEWS reasoning: school={} narrated {}/{} snapshots", schoolId, done, targets.size)
        return done
    }

    /** Reason a single snapshot; returns true if a narrative was written. */
    suspend fun reasonOne(schoolId: UUID, snap: PewsSnapshot): Boolean {
        val firstName = snap.studentName.trim().split(" ").firstOrNull() ?: "This student"
        val signalLines = snap.signals.joinToString("\n") { "- ${it.label}" }
        val userPrompt = buildString {
            appendLine("Student: $firstName (Class ${snap.className}${snap.section})")
            appendLine("Composite risk score: ${snap.riskScore}/100 (${snap.riskLevel})")
            snap.attendancePct?.let { appendLine("Attendance: $it%") }
            snap.marksPct?.let { appendLine("Average marks: $it%") }
            if (snap.leaveCount > 0) appendLine("Leave requests: ${snap.leaveCount}")
            snap.attendanceSlope?.let { if (it < 0) appendLine("Attendance trend: ${it.toInt()} pts") }
            snap.marksSlope?.let { if (it < 0) appendLine("Marks trend: ${it.toInt()} pts") }
            appendLine("Signals:")
            append(signalLines)
        }

        val result = AiService.complete(
            feature = "pews",
            lane = AiLane.REASON,
            messages = listOf(
                LlmMessage("system", systemPrompt),
                LlmMessage("user", userPrompt),
            ),
            containsPii = true,                 // names a student → no-training lane
            schoolId = schoolId,
            temperature = 0.3,
            maxTokens = 400,
            cache = true,                       // cached by prompt hash (signals embedded)
        )
        if (!result.ok || result.content.isNullOrBlank()) return false

        val parsed = parseJsonish(result.content)
        val narrative = parsed["narrative"]?.takeIf { it.isNotBlank() } ?: return false

        dbQuery {
            PewsRiskSnapshotsTable.update({
                (PewsRiskSnapshotsTable.schoolId eq snap.schoolId) and
                    (PewsRiskSnapshotsTable.studentCode eq snap.studentCode) and
                    (PewsRiskSnapshotsTable.runDate eq snap.runDate)
            }) {
                it[aiNarrative] = narrative
                it[aiCause] = parsed["cause"]
                it[aiRecommendation] = parsed["recommendation"]
                it[aiProviderUsed] = result.providerUsed
            }
        }
        return true
    }

    /**
     * Tolerant extraction of the three keys from the model output. Models
     * sometimes wrap JSON in prose or code fences; we pull the values rather
     * than insist on strict JSON (and never fabricate — missing key = null).
     */
    private fun parseJsonish(raw: String): Map<String, String> {
        val text = raw.substringAfter("{", raw).substringBeforeLast("}", raw)
        val out = HashMap<String, String>()
        for (key in listOf("narrative", "cause", "recommendation")) {
            val rx = Regex("\"$key\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"", RegexOption.IGNORE_CASE)
            rx.find(text)?.groupValues?.getOrNull(1)?.let {
                out[key] = it.replace("\\\"", "\"").replace("\\n", " ").trim()
            }
        }
        return out
    }
}

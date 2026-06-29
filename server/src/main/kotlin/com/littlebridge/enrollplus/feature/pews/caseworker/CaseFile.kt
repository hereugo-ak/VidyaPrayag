/*
 * File: CaseFile.kt
 * Module: feature.pews.caseworker
 *
 * PEWS 2.0 — Structured Case File schema.
 *
 * Replaces the v1 3-string output (narrative + cause + recommendation) with
 * a structured, sequenced plan that is conditioned on the student's history,
 * what worked for similar cases, calendar context, and parent responsiveness.
 *
 * This is the anti-generic mechanism: two children with the same raw signals
 * but different histories get different plans. No template paste.
 *
 * Spec: PEWS_2.0_AGENTIC_REDESIGN.md §6.3
 */
package com.littlebridge.enrollplus.feature.pews.caseworker

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// ──────────────────────────────────────────────────────────────────────────
// Case File schema
// ──────────────────────────────────────────────────────────────────────────

@Serializable
data class CaseFile(
    val narrative: String? = null,
    val hypotheses: List<Hypothesis> = emptyList(),
    val plan: List<PlanStep> = emptyList(),
    val parentDraft: ParentDraft? = null,
    val urgency: String = "medium",        // low|medium|high
    val skipReason: String? = null,        // e.g. "exam week — defer parent contact"
)

@Serializable
data class Hypothesis(
    val cause: String,
    val confidence: Double = 0.0,
    val evidence: List<String> = emptyList(),
)

@Serializable
data class PlanStep(
    val step: Int,
    val action: String,                     // parent_call|home_visit|counselling|remedial_class|parent_message|observe|mentor_pairing|fee_counselling
    val owner: String = "class_teacher",    // class_teacher|school_admin|mentor
    @SerialName("sla_days") val slaDays: Int = 3,
    val rationale: String? = null,
    val condition: String? = null,          // e.g. "if no improvement after step 1"
)

@Serializable
data class ParentDraft(
    val language: String = "hi",            // ISO 639-1 code
    val tone: String = "warm, non-clinical",
    val body: String,
)

// ──────────────────────────────────────────────────────────────────────────
// Serialization helpers
// ──────────────────────────────────────────────────────────────────────────

object CaseFileCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        encodeDefaults = true
    }

    /** Parse a model's JSON output into a CaseFile. Returns null on failure. */
    fun parse(raw: String): CaseFile? = try {
        // Strip code fences if present
        val cleaned = raw.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```")
            .trim()
        json.decodeFromString(CaseFile.serializer(), cleaned)
    } catch (e: Exception) {
        null
    }

    /** Serialize a CaseFile to JSON for storage. */
    fun encode(caseFile: CaseFile): String =
        json.encodeToString(CaseFile.serializer(), caseFile)

    /** Create a minimal deterministic Case File when no AI is available. */
    fun deterministic(
        studentName: String,
        riskLevel: String,
        causeFamily: String?,
        topSignalLabel: String,
    ): CaseFile {
        val urgency = when (riskLevel) {
            "high" -> "high"
            "medium" -> "medium"
            else -> "low"
        }
        return CaseFile(
            narrative = "$studentName is showing ${topSignalLabel.lowercase()}. " +
                "Risk level: $riskLevel. Cause family: ${causeFamily ?: "unknown"}.",
            hypotheses = listOf(
                Hypothesis(
                    cause = causeFamily ?: "unknown",
                    confidence = 0.5,
                    evidence = listOf(topSignalLabel),
                )
            ),
            plan = listOf(
                PlanStep(
                    step = 1,
                    action = defaultAction(causeFamily),
                    owner = "class_teacher",
                    slaDays = if (urgency == "high") 2 else 5,
                    rationale = "Standard first response for $causeFamily cases.",
                )
            ),
            urgency = urgency,
            skipReason = null,
        )
    }

    private fun defaultAction(causeFamily: String?): String = when (causeFamily) {
        "attendance" -> "parent_call"
        "academic" -> "remedial_class"
        "disengagement" -> "mentor_pairing"
        "wellbeing" -> "counselling"
        "financial" -> "fee_counselling"
        "external" -> "parent_call"
        else -> "observe"
    }
}

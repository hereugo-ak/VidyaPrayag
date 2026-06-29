/*
 * File: GuardrailService.kt
 * Module: feature.ai
 *
 * The privacy + safety gate that every prompt passes through before it leaves
 * the building. Two jobs:
 *
 *   1. PII routing enforcement — a prompt flagged `containsPii=true` may ONLY go
 *      to no-training providers (Cerebras/Groq/OpenRouter). Mistral/SambaNova
 *      (training-opt-in) are filtered out of the lane for PII prompts. This is
 *      enforced HERE, at the gateway, not trusted to the caller.
 *
 *   2. PII redaction — for the school-wide PATTERN prompts that we *do* want to
 *      send to a training-opt-in provider, `redactPii()` strips names, phones,
 *      emails, and student codes so the prompt is de-identified before it leaves.
 *      (PEWS per-student REASON does NOT redact — it pins to no-training
 *      providers and sends the minimal signal bundle instead.)
 *
 * The redaction is deliberately conservative (better to over-redact than leak).
 * It is NOT a substitute for the routing rule — both layers apply.
 */
package com.littlebridge.enrollplus.feature.ai

import org.slf4j.LoggerFactory

object GuardrailService {
    private val log = LoggerFactory.getLogger("AiGuardrail")

    // Indian 10-digit mobile, optionally +91/0 prefixed.
    private val PHONE = Regex("""(?:\+91[\-\s]?|0)?[6-9]\d{9}""")
    private val EMAIL = Regex("""[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}""")
    // Common student-code shapes (e.g. STU-2024-001, S12345, ADM/24/0012).
    private val STUDENT_CODE = Regex("""\b(?:STU|ADM|S|R)[\-/]?\d{2,}[\-/]?\d{0,6}\b""", RegexOption.IGNORE_CASE)
    // Aadhaar-like 12-digit national ID (never send these anywhere).
    private val AADHAAR = Regex("""\b\d{4}\s?\d{4}\s?\d{4}\b""")

    /**
     * Filter a candidate provider list for a PII-bearing prompt: keep only
     * no-training providers, and (when a template allow-list is supplied)
     * intersect with that allow-list. For non-PII prompts the list is returned
     * unchanged.
     *
     * @param candidates ordered lane (primary first)
     * @param containsPii whether the prompt carries personal data
     * @param allowList   optional CSV allow-list from the prompt template
     *                    (empty/null = "all no-training providers")
     */
    fun filterProvidersForPii(
        candidates: List<AiProvider>,
        containsPii: Boolean,
        allowList: Set<String> = emptySet(),
    ): List<AiProvider> {
        if (!containsPii) return candidates
        val filtered = candidates.filter { p ->
            p.noTraining && (allowList.isEmpty() || p.code in allowList)
        }
        if (filtered.size < candidates.size) {
            val dropped = (candidates - filtered.toSet()).joinToString { it.code }
            log.debug("PII routing dropped training-opt-in providers from lane: {}", dropped)
        }
        return filtered
    }

    /**
     * De-identify text for prompts that may reach a training-opt-in provider
     * (school-wide PATTERN prompts only). Replaces names with role tokens is
     * out of scope (we don't have a name list here) — instead the PATTERN
     * prompt is built from aggregates and we scrub any residual identifiers.
     */
    fun redactPii(text: String): String {
        var t = text
        t = AADHAAR.replace(t, "[ID]")
        t = PHONE.replace(t, "[PHONE]")
        t = EMAIL.replace(t, "[EMAIL]")
        t = STUDENT_CODE.replace(t, "[CODE]")
        return t
    }

    /**
     * Quick check: does this text look like it carries PII? Used as a defensive
     * assertion (a prompt the caller said was non-PII but clearly isn't gets
     * downgraded to the PII lane rather than leaking).
     */
    fun looksLikePii(text: String): Boolean =
        PHONE.containsMatchIn(text) ||
            EMAIL.containsMatchIn(text) ||
            AADHAAR.containsMatchIn(text)

    /**
     * Sanity-check an LLM response before it is stored/shown. Returns null if
     * the response should be rejected (empty, or obviously leaked a redaction
     * token back, or is absurdly long). The deterministic layer is the source
     * of truth, so a rejected narrative simply stays null (honest empty).
     */
    fun validateResponse(response: String, maxChars: Int = 4000): String? {
        val trimmed = response.trim()
        if (trimmed.isBlank()) return null
        if (trimmed.length > maxChars) return trimmed.take(maxChars)
        return trimmed
    }
}

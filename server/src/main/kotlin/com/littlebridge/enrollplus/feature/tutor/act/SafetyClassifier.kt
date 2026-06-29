// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/tutor/act/SafetyClassifier.kt
package com.littlebridge.enrollplus.feature.tutor.act

import org.slf4j.LoggerFactory

/**
 * TIER 3 — Safety Classifier (deterministic, never the model's call).
 *
 * Scans the child's doubt text for self-harm, abuse, distress, or other
 * safeguarding signals. If tripped, the AI does NOT counsel — the event
 * is immediately surfaced to the school's safeguarding contact and the
 * parent. This is a hard, audited path.
 *
 * This is a deterministic keyword + pattern classifier (no LLM) — it must
 * be reliable and fast. The model is never trusted with safety decisions.
 *
 * SOLID:
 *   S → Single responsibility: safety signal detection only.
 *   D → No external dependencies; pure function.
 *
 * Spec: AI_TUTOR_2.0_AGENTIC_REDESIGN.md §7 (Safety classifier)
 */
object SafetyClassifier {
    private val log = LoggerFactory.getLogger("SafetyClassifier")

    data class SafetyResult(
        val tripped: Boolean,
        val category: String? = null,    // self_harm | abuse | distress | bullying | violence
        val matchedPhrase: String? = null,
        val action: String = "none",     // escalate_safeguarding | none
    )

    // Keyword sets per category. These are deliberately broad — false positives
    // are acceptable (a teacher/parent gets notified); false negatives are not.
    private val selfHarmKeywords = listOf(
        "kill myself", "end my life", "suicide", "suicidal", "want to die",
        "hurt myself", "cutting", "self-harm", "no reason to live",
        "better off dead", "can't go on", "give up on life",
    )

    private val abuseKeywords = listOf(
        "abuse", "abused", "being hurt", "touching me", "hitting me",
        "beating", "scared of", "afraid to go home", "unsafe at home",
        "dad hits", "mom hits", "father hits", "mother hits",
    )

    private val distressKeywords = listOf(
        "depressed", "anxiety", "panic attack", "can't breathe", "crying",
        "alone", "nobody cares", "helpless", "hopeless", "worthless",
        "hate myself", "can't sleep", "nightmares",
    )

    private val bullyingKeywords = listOf(
        "bullied", "bullying", "being teased", "making fun of me",
        "nobody talks to me", "excluded", "left out", "cyberbullying",
    )

    private val violenceKeywords = listOf(
        "threatened", "weapon", "gun", "knife", "fight",
        "going to hurt", "going to kill",
    )

    /**
     * Classify the child's input text for safety signals.
     * Returns a [SafetyResult] — if [SafetyResult.tripped] is true, the
     * caller MUST escalate immediately (surface to safeguarding contact).
     */
    fun classify(text: String): SafetyResult {
        val lower = text.lowercase().trim()

        // Check each category in priority order
        for (kw in selfHarmKeywords) {
            if (lower.contains(kw)) {
                log.warn("SafetyClassifier: SELF_HARM signal detected — phrase='{}'", kw)
                return SafetyResult(
                    tripped = true,
                    category = "self_harm",
                    matchedPhrase = kw,
                    action = "escalate_safeguarding",
                )
            }
        }

        for (kw in abuseKeywords) {
            if (lower.contains(kw)) {
                log.warn("SafetyClassifier: ABUSE signal detected — phrase='{}'", kw)
                return SafetyResult(
                    tripped = true,
                    category = "abuse",
                    matchedPhrase = kw,
                    action = "escalate_safeguarding",
                )
            }
        }

        for (kw in violenceKeywords) {
            if (lower.contains(kw)) {
                log.warn("SafetyClassifier: VIOLENCE signal detected — phrase='{}'", kw)
                return SafetyResult(
                    tripped = true,
                    category = "violence",
                    matchedPhrase = kw,
                    action = "escalate_safeguarding",
                )
            }
        }

        for (kw in bullyingKeywords) {
            if (lower.contains(kw)) {
                log.warn("SafetyClassifier: BULLYING signal detected — phrase='{}'", kw)
                return SafetyResult(
                    tripped = true,
                    category = "bullying",
                    matchedPhrase = kw,
                    action = "escalate_safeguarding",
                )
            }
        }

        for (kw in distressKeywords) {
            if (lower.contains(kw)) {
                log.warn("SafetyClassifier: DISTRESS signal detected — phrase='{}'", kw)
                return SafetyResult(
                    tripped = true,
                    category = "distress",
                    matchedPhrase = kw,
                    action = "escalate_safeguarding",
                )
            }
        }

        return SafetyResult(tripped = false)
    }
}

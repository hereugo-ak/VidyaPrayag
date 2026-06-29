/*
 * File: ParentDraftService.kt
 * Module: feature.pews.act
 *
 * PEWS 2.0 — One-tap parent draft generation.
 *
 * Generates a vernacular, warm, non-clinical message to the parent based on
 * the student's Case File. The teacher reviews and edits before sending via
 * the existing messaging system. NEVER auto-sent.
 *
 * Endpoint: POST /teacher/pews/interventions/{id}/draft-message
 *
 * Spec: PEWS_2.0_AGENTIC_REDESIGN.md §7 (One-tap parent draft)
 */
package com.littlebridge.enrollplus.feature.pews.act

import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.PewsInterventionsTable
import com.littlebridge.enrollplus.db.PewsRiskSnapshotsTable
import com.littlebridge.enrollplus.db.StudentsTable
import com.littlebridge.enrollplus.feature.ai.AiLane
import com.littlebridge.enrollplus.feature.ai.AiService
import com.littlebridge.enrollplus.feature.ai.LlmMessage
import com.littlebridge.enrollplus.feature.pews.caseworker.CaseFileCodec
import com.littlebridge.enrollplus.feature.pews.core.KillSwitchGuard
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.slf4j.LoggerFactory
import java.util.UUID

class ParentDraftService {
    private val log = LoggerFactory.getLogger("ParentDraftService")

    data class DraftResult(
        val ok: Boolean,
        val language: String = "hi",
        val body: String? = null,
        val errorMessage: String? = null,
    )

    private val systemPrompt = """
        You write short, warm messages from teachers to parents about their child.
        Rules:
        - Write in the parent's vernacular language (Hindi by default, unless specified).
        - Tone: warm, caring, non-clinical. You are a teacher who cares, not a system.
        - NEVER mention "risk", "score", "PEWS", "early warning", or any system term.
        - NEVER share numbers like attendance percentage or test scores.
        - Keep it to 2-3 sentences. Include one concrete next step (e.g. "please call me",
          "let's meet at PTM", "please ensure homework is done").
        - Address the parent respectfully (e.g. "नमस्ते" / "प्रणाम").
        - Use the child's first name, not their roll number or code.
        - Output ONLY the message body, nothing else. No JSON, no quotes, no preamble.
    """.trimIndent()

    /**
     * Generate a parent draft message for an intervention.
     * The teacher reviews, edits, and sends via the existing messaging system.
     */
    suspend fun generateDraft(
        schoolId: UUID,
        interventionId: UUID,
        language: String = "hi",
    ): DraftResult {
        KillSwitchGuard.require("act")

        // Load the intervention
        val intervention = dbQuery {
            PewsInterventionsTable.selectAll().where {
                (PewsInterventionsTable.id eq interventionId) and
                    (PewsInterventionsTable.schoolId eq schoolId)
            }.singleOrNull()
        } ?: return DraftResult(ok = false, errorMessage = "intervention not found")

        val studentCode = intervention[PewsInterventionsTable.studentCode]
        val planJson = intervention[PewsInterventionsTable.planJson]
        val actionType = intervention[PewsInterventionsTable.actionType]
        val urgency = intervention[PewsInterventionsTable.urgency] ?: "medium"

        // Load student identity
        val student = dbQuery {
            StudentsTable.selectAll().where {
                (StudentsTable.schoolId eq schoolId) and
                    (StudentsTable.studentCode eq studentCode)
            }.singleOrNull()
        } ?: return DraftResult(ok = false, errorMessage = "student not found")

        val studentName = student[StudentsTable.fullName]
        val className = student[StudentsTable.className]
        val section = student[StudentsTable.section]
        val firstName = studentName.trim().split(" ").firstOrNull() ?: studentName

        // Load latest snapshot for context
        val snapshot = dbQuery {
            PewsRiskSnapshotsTable.selectAll().where {
                (PewsRiskSnapshotsTable.schoolId eq schoolId) and
                    (PewsRiskSnapshotsTable.studentCode eq studentCode)
            }.orderBy(PewsRiskSnapshotsTable.runDate, org.jetbrains.exposed.sql.SortOrder.DESC)
                .firstOrNull()
        }

        // Parse Case File if available
        val caseFile = planJson?.let { CaseFileCodec.parse(it) }
        val existingDraft = caseFile?.parentDraft

        // If the Case File already has a parent draft in the right language, use it
        if (existingDraft != null && existingDraft.language == language) {
            return DraftResult(ok = true, language = language, body = existingDraft.body)
        }

        // Build the prompt for LLM generation
        val topSignal = snapshot?.get(PewsRiskSnapshotsTable.signalsJson)
            ?.let { runCatching { CaseFileCodec.parse(it) }.getOrNull() }
        val narrative = caseFile?.narrative
            ?: snapshot?.get(PewsRiskSnapshotsTable.aiNarrative)
            ?: "$firstName has been showing some concerns in class."

        val userPrompt = buildString {
            appendLine("Child: $firstName (Class $className-$section)")
            appendLine("Language: $language")
            appendLine("Action: $actionType")
            appendLine("Context: $narrative")
            if (caseFile != null && caseFile.plan.isNotEmpty()) {
                appendLine("Plan step 1: ${caseFile.plan.first().action} — ${caseFile.plan.first().rationale ?: ""}")
            }
            appendLine()
            appendLine("Write a warm message to $firstName's parent. Do NOT mention risk, scores, or system terms.")
        }

        // Generate via AI
        if (!AiService.anyProviderConfigured()) {
            // Fallback: deterministic template
            val fallbackBody = deterministicDraft(firstName, language, actionType)
            return DraftResult(ok = true, language = language, body = fallbackBody)
        }

        val result = AiService.complete(
            feature = "pews_parent_draft",
            lane = AiLane.FAST_CHAT,
            messages = listOf(
                LlmMessage("system", systemPrompt),
                LlmMessage("user", userPrompt),
            ),
            containsPii = true,
            schoolId = schoolId,
            temperature = 0.5,
            maxTokens = 200,
            cache = true,
        )

        if (!result.ok || result.content.isNullOrBlank()) {
            log.warn("ParentDraft: AI generation failed for {} — using deterministic", studentCode)
            val fallbackBody = deterministicDraft(firstName, language, actionType)
            return DraftResult(ok = true, language = language, body = fallbackBody)
        }

        return DraftResult(
            ok = true,
            language = language,
            body = result.content.trim(),
        )
    }

    private fun deterministicDraft(firstName: String, language: String, actionType: String): String {
        // Simple vernacular templates as fallback
        return when (language) {
            "hi" -> when (actionType) {
                "parent_call" -> "नमस्ते, $firstName के बारे में बात करनी है। कृपया सुविधानुसार कॉल करें। धन्यवाद।"
                "parent_message" -> "नमस्ते, $firstName की कक्षा में प्रगति पर बात करना चाहता हूँ। कृपया संदेश का उत्तर दें।"
                "home_visit" -> "नमस्ते, $firstName के बारे में चर्चा के लिए मैं आपसे मिलना चाहता/चाहती हूँ। समय बताएँ।"
                "remedial_class" -> "नमस्ते, $firstName को अतिरिक्त सहायता देने के लिए हम विशेष कक्षा का आयोजन कर रहे हैं। कृपया सहयोग दें।"
                else -> "नमस्ते, $firstName के बारे में बात करनी है। कृपया संपर्क करें। धन्यवाद।"
            }
            else -> when (actionType) {
                "parent_call" -> "Hello, I'd like to discuss $firstName. Please call at your convenience. Thank you."
                "parent_message" -> "Hello, I'd like to talk about $firstName's progress in class. Please reply to this message."
                "home_visit" -> "Hello, I'd like to meet with you to discuss $firstName. Please let me know a suitable time."
                "remedial_class" -> "Hello, we're arranging extra support classes for $firstName. Your cooperation is appreciated."
                else -> "Hello, I'd like to discuss $firstName. Please get in touch. Thank you."
            }
        }
    }
}

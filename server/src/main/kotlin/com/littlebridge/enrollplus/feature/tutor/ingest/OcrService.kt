// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/tutor/ingest/OcrService.kt
package com.littlebridge.enrollplus.feature.tutor.ingest

import com.littlebridge.enrollplus.feature.ai.AiLane
import com.littlebridge.enrollplus.feature.ai.AiService
import com.littlebridge.enrollplus.feature.ai.LlmMessage
import com.littlebridge.enrollplus.feature.tutor.core.TutorConstants
import com.littlebridge.enrollplus.feature.tutor.core.TutorKillSwitch
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.util.Base64
import java.util.UUID

/**
 * TIER 0 — Ingest: OCR Service.
 *
 * Converts a photo of a problem (uploaded by the child) into text.
 * Two paths:
 *   1. Vision-capable LLM (Gemini) — send the image as base64 with a
 *      "extract the problem text" prompt. Handles handwriting, diagrams.
 *   2. Fallback: return empty string — the caller can ask the child to type.
 *
 * PII note: images may contain the child's name/handwriting. We use the
 * REASON lane which routes to PII-safe providers when containsPii=true.
 *
 * Kill-switched under module name "tutor_ingest".
 *
 * SOLID:
 *   S → Single responsibility: photo → text extraction.
 *   D → Depends on AiService abstraction, not a specific provider.
 *
 * Spec: AI_TUTOR_2.0_AGENTIC_REDESIGN.md §6.4 step 1 (Capture — Photo → OCR)
 */
class OcrService {
    private val log = LoggerFactory.getLogger("OcrService")

    @Serializable
    data class OcrResult(
        val text: String,
        val providerUsed: String? = null,
        val modelUsed: Boolean = false,
        val error: String? = null,
    )

    /**
     * Extract text from a photo using a vision-capable LLM.
     *
     * @param schoolId    tenant scope
     * @param imageBytes  raw image bytes (JPEG/PNG)
     * @param mimeType    image MIME type (e.g. "image/jpeg")
     */
    suspend fun extractText(
        schoolId: UUID,
        imageBytes: ByteArray,
        mimeType: String = "image/jpeg",
    ): OcrResult {
        TutorKillSwitch.require(TutorConstants.MODULE_INGEST)

        if (!AiService.anyProviderConfigured()) {
            log.debug("OcrService: no AI provider — returning empty")
            return OcrResult(text = "", error = "No AI provider configured")
        }

        // Convert image to base64 data URL
        val base64 = Base64.getEncoder().encodeToString(imageBytes)
        val dataUrl = "data:$mimeType;base64,$base64"

        // Use the REASON lane — Gemini supports vision via the OpenAI-compatible endpoint.
        // The image is sent as a content part in the user message.
        val systemPrompt = """
            You are an OCR assistant for a school tutoring system. Extract the
            text from the provided image of a math or science problem. Return
            ONLY the extracted text, preserving:
            - Numbers, equations, and symbols exactly
            - Line breaks where they appear
            - Any multiple-choice options (a, b, c, d)
            Do not add explanations or commentary. If the image is unclear,
            return your best guess of the text.
        """.trimIndent()

        val userContent = buildString {
            appendLine("Extract the problem text from this image:")
            appendLine()
            appendLine("[IMAGE: $dataUrl]")
        }

        val result = AiService.complete(
            feature = "ai_tutor_ocr",
            lane = AiLane.REASON,
            messages = listOf(
                LlmMessage("system", systemPrompt),
                LlmMessage("user", userContent),
            ),
            containsPii = true,
            schoolId = schoolId,
            temperature = 0.1,
            maxTokens = 512,
            cache = false,
        )

        if (!result.ok || result.content == null) {
            log.warn("OcrService: OCR failed — {}", result.errorMessage)
            return OcrResult(text = "", error = result.errorMessage)
        }

        log.info("OcrService: extracted {} chars (provider={})", result.content.length, result.providerUsed)
        return OcrResult(
            text = result.content.trim(),
            providerUsed = result.providerUsed,
            modelUsed = true,
        )
    }
}

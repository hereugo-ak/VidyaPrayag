// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/tutor/ingest/VoiceService.kt
package com.littlebridge.enrollplus.feature.tutor.ingest

import com.littlebridge.enrollplus.feature.ai.AiProvider
import com.littlebridge.enrollplus.feature.ai.KeyVault
import com.littlebridge.enrollplus.feature.tutor.core.TutorConstants
import com.littlebridge.enrollplus.feature.tutor.core.TutorKillSwitch
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * TIER 0 — Ingest: Voice Service.
 *
 * Transcribes a child's voice doubt using Whisper via Groq.
 * Groq hosts the Whisper Large v3 model at `POST /openai/v1/audio/transcriptions`.
 * Handles Hinglish (Hindi + English mix) per the spec.
 *
 * Kill-switched under module name "tutor_ingest".
 *
 * SOLID:
 *   S → Single responsibility: voice → text transcription.
 *   D → Depends on KeyVault for API key resolution, not hardcoded credentials.
 *
 * Spec: AI_TUTOR_2.0_AGENTIC_REDESIGN.md §6.4 step 1 (Capture — Voice → Whisper via Groq)
 */
class VoiceService {
    private val log = LoggerFactory.getLogger("VoiceService")

    private val client = HttpClient(CIO) {
        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            requestTimeoutMillis = 60_000
            socketTimeoutMillis = 60_000
        }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    @Serializable
    data class TranscriptionResult(
        val text: String,
        val providerUsed: String? = null,
        val modelUsed: Boolean = false,
        val error: String? = null,
    )

    /**
     * Transcribe audio using Whisper via Groq.
     *
     * @param schoolId    tenant scope (for logging, not sent to Groq)
     * @param audioBytes  raw audio bytes (MP3, WAV, M4A, etc.)
     * @param mimeType    audio MIME type (e.g. "audio/mpeg")
     */
    suspend fun transcribe(
        schoolId: UUID,
        audioBytes: ByteArray,
        mimeType: String = "audio/mpeg",
    ): TranscriptionResult {
        TutorKillSwitch.require(TutorConstants.MODULE_INGEST)

        val apiKey = KeyVault.keyFor(AiProvider.GROQ)
        if (apiKey == null) {
            log.debug("VoiceService: Groq API key not configured — cannot transcribe")
            return TranscriptionResult(text = "", error = "Groq API key not configured")
        }

        val baseUrl = KeyVault.baseUrlFor(AiProvider.GROQ) ?: "https://api.groq.com/openai/v1"
        val model = "whisper-large-v3"

        return try {
            val response: HttpResponse = client.submitFormWithBinaryData(
                url = "$baseUrl/audio/transcriptions",
                formData = formData {
                    append("model", model)
                    append("file", audioBytes, Headers.build {
                        append(HttpHeaders.ContentType, mimeType)
                        append(HttpHeaders.ContentDisposition, "filename=audio.webm")
                    })
                    append("language", "hi")
                    append("response_format", "json")
                },
            ) {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
            }

            if (!response.status.isSuccess()) {
                val errorBody = response.body<String>()
                log.warn("VoiceService: Groq Whisper returned {} — {}", response.status, errorBody.take(200))
                return TranscriptionResult(text = "", error = "Whisper API error: ${response.status}")
            }

            val jsonBody: JsonObject = response.body()
            val text = jsonBody["text"]?.jsonPrimitive?.content ?: ""

            log.info("VoiceService: transcribed {} chars via Groq Whisper", text.length)
            TranscriptionResult(
                text = text.trim(),
                providerUsed = "groq",
                modelUsed = true,
            )
        } catch (e: Exception) {
            log.warn("VoiceService: transcription failed — {}", e.message)
            TranscriptionResult(text = "", error = e.message)
        }
    }

    /**
     * Check if voice transcription is available (Groq key configured).
     */
    fun isAvailable(): Boolean =
        KeyVault.isConfigured(AiProvider.GROQ)
}

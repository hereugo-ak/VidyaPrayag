package com.littlebridge.enrollplus.feature.school

import com.littlebridge.enrollplus.core.fail
import com.littlebridge.enrollplus.core.ok
import com.littlebridge.enrollplus.core.requireSchoolContext
import com.littlebridge.enrollplus.feature.ai.AiLane
import com.littlebridge.enrollplus.feature.ai.AiService
import com.littlebridge.enrollplus.feature.ai.LlmMessage
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

// ──────────────────────────────────────────────────────────────────────────
// Timetable AI Import — OCR (vision) + text parsing via the AI gateway
// ──────────────────────────────────────────────────────────────────────────

@Serializable
data class TimetableImportOcrRequest(
    val image: String,          // base64-encoded image (no data: prefix)
    @SerialName("mime_type") val mimeType: String = "image/jpeg",
)

@Serializable
data class TimetableImportTextRequest(
    val text: String,
)

@Serializable
data class TimetableImportResponse(
    val slots: List<SchoolDaySlotDto>,
    val name: String = "",
    @SerialName("ai_used") val aiUsed: Boolean = false,
    @SerialName("raw_text") val rawText: String? = null,
)

private val TIME_REGEX = Regex("(\\d{1,2}[:.]\\d{2})\\s*[-\u2013to ]+\\s*(\\d{1,2}[:.]\\d{2})")
private val BREAK_KEYWORDS = setOf("break", "recess", "lunch", "interval", "assembly", "prayer")

private fun normalizeTime(time: String): String {
    val parts = time.split(":")
    if (parts.size == 2) {
        val h = parts[0].padStart(2, '0')
        val m = parts[1].padStart(2, '0')
        return "$h:$m"
    }
    return time
}

private fun parseTimetableText(text: String): Pair<List<SchoolDaySlotDto>, String> {
    val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
    val slots = mutableListOf<SchoolDaySlotDto>()
    var name = ""

    for ((idx, line) in lines.withIndex()) {
        val match = TIME_REGEX.find(line)
        if (match != null) {
            val startRaw = match.groupValues[1].replace('.', ':')
            val endRaw = match.groupValues[2].replace('.', ':')
            val startTime = normalizeTime(startRaw)
            val endTime = normalizeTime(endRaw)

            val afterMatch = line.substring(match.range.last + 1).trim()
                .removePrefix("-").removePrefix("\u2013").trim()
            val beforeMatch = line.substring(0, match.range.first).trim()
                .removeSuffix("-").removeSuffix("\u2013").trim()

            val label = when {
                afterMatch.isNotBlank() -> afterMatch
                beforeMatch.isNotBlank() -> beforeMatch
                else -> "Slot ${idx + 1}"
            }

            val lowerLabel = label.lowercase()
            val slotType = when {
                BREAK_KEYWORDS.any { lowerLabel.contains(it) } -> {
                    if (lowerLabel.contains("assembly") || lowerLabel.contains("prayer")) "ASSEMBLY"
                    else "BREAK"
                }
                lowerLabel.contains("lab") -> "LAB"
                else -> "TEACHING"
            }

            slots.add(SchoolDaySlotDto(idx, slotType, label, startTime, endTime))
        }
    }

    for (line in lines) {
        if (TIME_REGEX.find(line) == null && line.length in 3..40) {
            name = line
            break
        }
    }

    return slots to name
}

private const val OCR_SYSTEM_PROMPT = """You are a timetable OCR assistant. Extract the school schedule from the provided image.
For each time slot, output exactly one line in the format: HH:MM-HH:MM Label
Rules:
- Use 24-hour time format (e.g. 08:00, 14:30)
- Include ALL periods, breaks, assemblies, and lunch
- Keep labels short (e.g. "Period 1", "Maths", "Short Break", "Lunch Break", "Assembly")
- Output ONLY the schedule lines, no preamble or explanation
- If the image is not a timetable, output: ERROR_NOT_A_TIMETABLE"""

private const val TEXT_PARSE_SYSTEM_PROMPT = """You are a timetable parser. Convert the provided timetable text into a structured format.
For each time slot, output exactly one line in the format: HH:MM-HH:MM Label
Rules:
- Use 24-hour time format (e.g. 08:00, 14:30)
- Convert 12-hour format (AM/PM) to 24-hour
- Include ALL periods, breaks, assemblies, and lunch
- Keep labels short (e.g. "Period 1", "Maths", "Short Break", "Lunch Break")
- Output ONLY the schedule lines, no preamble or explanation
- If the text is not a timetable, output: ERROR_NOT_A_TIMETABLE"""

fun Route.timetableImportRouting() {
    authenticate("jwt") {

        // ── POST /api/v1/school/timetable/import-ocr — AI vision OCR ──────────
        post("/api/v1/school/timetable/import-ocr") {
            val ctx = call.requireSchoolContext() ?: return@post
            val req = call.receive<TimetableImportOcrRequest>()

            if (req.image.isBlank()) {
                call.fail("Image data is required", HttpStatusCode.BadRequest, "VALIDATION")
                return@post
            }

            // Size guard: ~10MB base64 limit (≈13.3M chars)
            if (req.image.length > 13_300_000) {
                call.fail("Image too large (max 10MB). Please use a smaller image.", HttpStatusCode.BadRequest, "IMAGE_TOO_LARGE")
                return@post
            }

            // Mime type whitelist
            val allowedMimes = setOf("image/jpeg", "image/png", "image/webp", "image/gif")
            if (req.mimeType !in allowedMimes) {
                call.fail("Unsupported image format: ${req.mimeType}. Supported: JPEG, PNG, WebP, GIF.", HttpStatusCode.BadRequest, "INVALID_MIME")
                return@post
            }

            val aiResult = AiService.completeWithVision(
                feature = "timetable_import",
                systemPrompt = OCR_SYSTEM_PROMPT,
                userText = "Extract the timetable schedule from this image.",
                imageBase64 = req.image,
                imageMimeType = req.mimeType,
                schoolId = ctx.schoolId,
                temperature = 0.2,
                maxTokens = 2048,
            )

            if (!aiResult.ok || aiResult.content.isNullOrBlank()) {
                call.fail(
                    aiResult.errorMessage ?: "AI OCR failed — no provider available",
                    HttpStatusCode.ServiceUnavailable,
                    "AI_UNAVAILABLE",
                )
                return@post
            }

            val rawText = aiResult.content!!
            if (rawText.contains("ERROR_NOT_A_TIMETABLE")) {
                call.fail("The image does not appear to be a timetable.", HttpStatusCode.BadRequest, "NOT_A_TIMETABLE")
                return@post
            }

            val (slots, name) = parseTimetableText(rawText)
            if (slots.isEmpty()) {
                call.fail("Could not parse any slots from the AI output.", HttpStatusCode.UnprocessableEntity, "PARSE_FAILED")
                return@post
            }

            call.ok(TimetableImportResponse(slots = slots, name = name, aiUsed = true, rawText = rawText),
                message = "Timetable imported via AI OCR")
        }

        // ── POST /api/v1/school/timetable/import-text — AI text parsing ──────
        post("/api/v1/school/timetable/import-text") {
            val ctx = call.requireSchoolContext() ?: return@post
            val req = call.receive<TimetableImportTextRequest>()

            if (req.text.isBlank()) {
                call.fail("Text is required", HttpStatusCode.BadRequest, "VALIDATION")
                return@post
            }

            val messages = listOf(
                LlmMessage(role = "system", content = TEXT_PARSE_SYSTEM_PROMPT),
                LlmMessage(role = "user", content = req.text),
            )

            val aiResult = AiService.complete(
                feature = "timetable_import",
                lane = AiLane.REASON,
                messages = messages,
                schoolId = ctx.schoolId,
                temperature = 0.2,
                maxTokens = 2048,
                cache = false,
            )

            if (!aiResult.ok || aiResult.content.isNullOrBlank()) {
                // Fallback to regex parser
                val (fallbackSlots, fallbackName) = parseTimetableText(req.text)
                if (fallbackSlots.isEmpty()) {
                    call.fail(
                        aiResult.errorMessage ?: "AI parsing failed and regex fallback found no slots",
                        HttpStatusCode.ServiceUnavailable,
                        "AI_UNAVAILABLE",
                    )
                    return@post
                }
                call.ok(TimetableImportResponse(slots = fallbackSlots, name = fallbackName, aiUsed = false),
                    message = "Timetable parsed with regex fallback (AI unavailable)")
                return@post
            }

            val rawText = aiResult.content!!
            if (rawText.contains("ERROR_NOT_A_TIMETABLE")) {
                call.fail("The text does not appear to be a timetable.", HttpStatusCode.BadRequest, "NOT_A_TIMETABLE")
                return@post
            }

            val (slots, name) = parseTimetableText(rawText)
            if (slots.isEmpty()) {
                // Fallback to regex on original text
                val (fallbackSlots, fallbackName) = parseTimetableText(req.text)
                if (fallbackSlots.isEmpty()) {
                    call.fail("Could not parse any slots from the AI output or original text.", HttpStatusCode.UnprocessableEntity, "PARSE_FAILED")
                    return@post
                }
                call.ok(TimetableImportResponse(slots = fallbackSlots, name = fallbackName, aiUsed = false),
                    message = "Timetable parsed with regex fallback")
                return@post
            }

            call.ok(TimetableImportResponse(slots = slots, name = name, aiUsed = true, rawText = rawText),
                message = "Timetable parsed via AI")
        }
    }
}

/*
 * File: LlmClient.kt
 * Module: feature.ai
 *
 * ONE OpenAI-compatible chat-completions client, with a swappable
 * (baseUrl, apiKey, model) per call. Every free-tier provider we use
 * (Cerebras, Groq, Groq-Fast, SambaNova, Mistral, OpenRouter, Gemini) speaks the same
 * `POST {baseUrl}/chat/completions` shape, so a single client covers all seven
 * — the gateway picks the lane and hands this client the right triple.
 *
 * Mirrors the codebase's HTTP-client-per-concern pattern (OtpHttpClient,
 * SupabaseStorage): a single lazily-built CIO HttpClient with bounded timeouts
 * and lenient JSON. We DO NOT enable Ktor's built-in retry — failover across
 * providers is the gateway's job (AiService), not a blind hammer of one dead
 * endpoint.
 *
 * This file owns ONLY transport + (de)serialization. No routing, no caching,
 * no circuit-breaking, no key handling — those live in AiService / CircuitBreaker
 * / KeyVault. Keeping it dumb makes it trivially testable and reusable for the
 * next AI features.
 */
package com.littlebridge.enrollplus.feature.ai

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

// ──────────────────────────────────────────────────────────────────────────
// Wire DTOs (OpenAI chat-completions shape)
// ──────────────────────────────────────────────────────────────────────────

/** OpenAI-style function tool definition sent in the request. */
@Serializable
data class ToolDefinition(
    val type: String = "function",
    val function: ToolFunction,
)

@Serializable
data class ToolFunction(
    val name: String,
    val description: String,
    val parameters: kotlinx.serialization.json.JsonElement,
)

/** A tool call returned by the model in the response. */
@Serializable
data class ToolCall(
    val id: String,
    val type: String = "function",
    val function: ToolCallFunction,
)

@Serializable
data class ToolCallFunction(
    val name: String,
    val arguments: String,  // JSON string of arguments
)

@Serializable
data class LlmMessage(
    val role: String,        // system | user | assistant | tool
    val content: String? = null,
    @SerialName("tool_calls") val toolCalls: List<ToolCall>? = null,
    @SerialName("tool_call_id") val toolCallId: String? = null,
)

@Serializable
private data class ChatCompletionRequest(
    val model: String,
    val messages: List<LlmMessage>,
    val temperature: Double = 0.4,
    @SerialName("max_tokens") val maxTokens: Int = 1024,
    val stream: Boolean = false,
    val tools: List<ToolDefinition>? = null,
    @SerialName("tool_choice") val toolChoice: String? = null,
)

@Serializable
private data class ChatCompletionResponse(
    val choices: List<Choice> = emptyList(),
    val usage: Usage? = null,
    val model: String? = null,
) {
    @Serializable data class Choice(
        val message: LlmMessage? = null,
        @SerialName("finish_reason") val finishReason: String? = null,
    )
    @Serializable data class Usage(
        @SerialName("prompt_tokens") val promptTokens: Int = 0,
        @SerialName("completion_tokens") val completionTokens: Int = 0,
        @SerialName("total_tokens") val totalTokens: Int = 0,
    )
}

// ──────────────────────────────────────────────────────────────────────────
// Result type (transport-level outcome; the gateway maps this to routing)
// ──────────────────────────────────────────────────────────────────────────

/**
 * The outcome of ONE provider call. `ok=true` carries [content] + token counts;
 * `ok=false` carries a classified [errorKind] + [httpStatus] so the gateway can
 * decide whether to fail over (5xx / 429 / network) or surface (4xx other).
 */
data class LlmResult(
    val ok: Boolean,
    val content: String? = null,
    val inputTokens: Int = 0,
    val outputTokens: Int = 0,
    val modelUsed: String? = null,
    val errorKind: LlmErrorKind? = null,
    val httpStatus: Int? = null,
    val errorMessage: String? = null,
    val toolCalls: List<ToolCall>? = null,
    val finishReason: String? = null,
) {
    companion object {
        fun success(
            content: String?, inTok: Int, outTok: Int, model: String?,
            toolCalls: List<ToolCall>? = null, finishReason: String? = null,
        ) = LlmResult(
            ok = true, content = content, inputTokens = inTok, outputTokens = outTok,
            modelUsed = model, toolCalls = toolCalls, finishReason = finishReason,
        )

        fun failure(kind: LlmErrorKind, status: Int? = null, message: String? = null) =
            LlmResult(ok = false, errorKind = kind, httpStatus = status, errorMessage = message)
    }
}

/** Classified failure so the gateway can route (retryable vs terminal). */
enum class LlmErrorKind {
    RATE_LIMITED,   // 429 — back off, try next provider
    SERVER_ERROR,   // 5xx — provider down, fail over
    AUTH_ERROR,     // 401/403 — bad/expired key, fail over (and flag for admin)
    BAD_REQUEST,    // 4xx other — our prompt is wrong, surface (don't blindly retry)
    EMPTY_RESPONSE, // 200 but no usable content
    NETWORK,        // timeout / connection refused — fail over
    UNKNOWN,
}

/**
 * Stateless transport. One CIO client shared across all providers (Ktor clients
 * are heavy). Construct once as a module singleton in AiService.
 */
class LlmClient(
    private val client: HttpClient = defaultClient,
) {
    private val log = LoggerFactory.getLogger("AiLlmClient")

    /**
     * Call `{baseUrl}/chat/completions` with the given key + model. Returns a
     * classified [LlmResult] — never throws for an expected provider failure
     * (timeouts, 4xx/5xx are mapped to [LlmResult.failure]); only truly
     * unexpected programmer errors propagate.
     */
    suspend fun complete(
        baseUrl: String,
        apiKey: String,
        model: String,
        messages: List<LlmMessage>,
        temperature: Double = 0.4,
        maxTokens: Int = 1024,
        extraHeaders: Map<String, String> = emptyMap(),
        tools: List<ToolDefinition>? = null,
        toolChoice: String? = null,
    ): LlmResult {
        val url = "${baseUrl.trimEnd('/')}/chat/completions"
        val payload = ChatCompletionRequest(
            model = model,
            messages = messages,
            temperature = temperature,
            maxTokens = maxTokens,
            tools = tools,
            toolChoice = toolChoice,
        )

        val resp: HttpResponse = try {
            client.post(url) {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                // OpenRouter likes attribution headers; harmless elsewhere.
                extraHeaders.forEach { (k, v) -> header(k, v) }
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
        } catch (e: Exception) {
            log.warn("LLM transport error to {}: {}", baseUrl, e.message)
            return LlmResult.failure(LlmErrorKind.NETWORK, message = e.message)
        }

        if (!resp.status.isSuccess()) {
            val status = resp.status.value
            val bodyText = runCatching { resp.bodyAsText() }.getOrDefault("")
            val kind = when {
                status == 429 -> LlmErrorKind.RATE_LIMITED
                status == 401 || status == 403 -> LlmErrorKind.AUTH_ERROR
                status in 500..599 -> LlmErrorKind.SERVER_ERROR
                status in 400..499 -> LlmErrorKind.BAD_REQUEST
                else -> LlmErrorKind.UNKNOWN
            }
            log.warn("LLM provider {} returned {} ({}): {}", baseUrl, status, kind,
                bodyText.take(300))
            return LlmResult.failure(kind, status = status, message = bodyText.take(500))
        }

        val parsed = runCatching { resp.body<ChatCompletionResponse>() }.getOrElse {
            log.warn("LLM response parse failed from {}: {}", baseUrl, it.message)
            return LlmResult.failure(LlmErrorKind.EMPTY_RESPONSE, message = "parse failed")
        }

        val choice = parsed.choices.firstOrNull()
        val message = choice?.message
        val content = message?.content?.trim()
        val toolCalls = message?.toolCalls
        val finishReason = choice?.finishReason

        if (content.isNullOrBlank() && toolCalls.isNullOrEmpty()) {
            return LlmResult.failure(LlmErrorKind.EMPTY_RESPONSE, status = 200,
                message = "no content or tool_calls in choices")
        }

        val usage = parsed.usage
        return LlmResult.success(
            content = content,
            inTok = usage?.promptTokens ?: 0,
            outTok = usage?.completionTokens ?: 0,
            model = parsed.model ?: model,
            toolCalls = toolCalls,
            finishReason = finishReason,
        )
    }

    companion object {
        /** Lenient JSON — every provider has minor schema quirks. */
        private val jsonCodec = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
            explicitNulls = false
        }

        /**
         * One shared client. Generous request timeout (LLM completions are
         * slower than OTP), but still bounded so a hung provider fails over
         * instead of blocking a scheduled batch forever.
         */
        val defaultClient: HttpClient by lazy {
            val codec = jsonCodec
            HttpClient(CIO) {
                expectSuccess = false   // we classify statuses ourselves
                install(HttpTimeout) {
                    connectTimeoutMillis = 8_000
                    requestTimeoutMillis = 60_000
                    socketTimeoutMillis = 60_000
                }
                install(ContentNegotiation) { json(codec) }
                install(Logging) { level = LogLevel.NONE }  // never echo prompts/keys
            }
        }
    }
}

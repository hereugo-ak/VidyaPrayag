/*
 * File: OtpDeliveryProvider.kt
 * Module: feature.auth
 *
 * Pluggable OTP delivery layer.
 *
 * Today we ship a single MOCK provider that logs the code to stdout — this
 * is what runs out of the box on local dev (and the dev-mode API response
 * echoes the code back so Postman tests are zero-friction).
 *
 * In production you swap OTP_PROVIDER to one of the real implementations
 * (TODO).  The contract is a single suspend function:
 *
 *     deliver(identifier, identifierType, code, purpose) → DeliveryOutcome
 *
 * Suggested production providers for the Indian market:
 *   - MSG91         (best DLT compliance, cheapest for SMS)
 *   - Gupshup       (WhatsApp + SMS hybrid)
 *   - Twilio        (global, more expensive)
 *   - AWS SNS       (fallback)
 *
 * Once you wire one in, expose it via:
 *
 *     OTP_PROVIDER=msg91
 *     MSG91_AUTH_KEY=...
 *     MSG91_TEMPLATE_ID=...
 *
 * …and dispatch from `deliver(...)` based on `System.getenv("OTP_PROVIDER")`.
 */
package com.littlebridge.vidyaprayag.feature.auth

import org.slf4j.LoggerFactory

data class DeliveryOutcome(
    val ok: Boolean,
    val providerMessageId: String? = null,
    val reason: String? = null
)

object OtpDeliveryProvider {

    private val log = LoggerFactory.getLogger("OtpDeliveryProvider")

    suspend fun deliver(
        identifier: String,
        identifierType: String,
        code: String,
        purpose: String
    ): DeliveryOutcome {
        val provider = System.getenv("OTP_PROVIDER")?.takeIf { it.isNotBlank() } ?: "mock"
        return when (provider.lowercase()) {
            "mock" -> deliverMock(identifier, code, purpose)
            // "msg91"   -> deliverMsg91(identifier, code, purpose)
            // "twilio"  -> deliverTwilio(identifier, code, purpose)
            // "gupshup" -> deliverGupshup(identifier, code, purpose)
            else -> {
                log.warn("Unknown OTP_PROVIDER='{}', falling back to mock", provider)
                deliverMock(identifier, code, purpose)
            }
        }
    }

    private fun deliverMock(identifier: String, code: String, purpose: String): DeliveryOutcome {
        // Important: This is the ONLY place in the code-base where the plain
        // OTP touches a log line — and only in mock mode.  In production
        // with a real provider we never log the plain code.
        log.info("[MOCK-OTP] identifier={} purpose={} code={}", identifier, purpose, code)
        println(">>> [MOCK-OTP] $identifier ($purpose) → CODE: $code  (valid 10 min)")
        return DeliveryOutcome(ok = true, providerMessageId = "mock-${System.currentTimeMillis()}")
    }
}

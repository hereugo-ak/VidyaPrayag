/*
 * File: EncryptionService.kt
 * Module: feature.ai
 *
 * AES-256-GCM encryption for provider API keys at rest (AI_INFRASTRUCTURE_SPEC
 * §8.13). JVM-native javax.crypto only — NO external crypto dependency.
 *
 *   - Key: derived from AI_ENCRYPTION_KEY env var. Accepts either a 64-char hex
 *     string (32 bytes) or any other string (SHA-256 → 32 bytes) so an operator
 *     can paste `openssl rand -hex 32` OR a passphrase and it still works.
 *   - Each encrypt() generates a random 12-byte IV, prepended to the ciphertext,
 *     and the whole thing is Base64-encoded for storage in a TEXT column.
 *   - decrypt() reads the IV from the first 12 bytes.
 *
 * If AI_ENCRYPTION_KEY is unset, the service runs in a DEV passthrough mode
 * (keys stored as `plain:<key>`), so a developer can run the gateway locally
 * without configuring encryption. Production MUST set AI_ENCRYPTION_KEY.
 */
package com.littlebridge.enrollplus.feature.ai

import org.slf4j.LoggerFactory
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class EncryptionService(
    rawEnvKey: String? = System.getenv("AI_ENCRYPTION_KEY")?.takeIf { it.isNotBlank() }
) {
    private val log = LoggerFactory.getLogger("AiEncryptionService")

    private val secretKey: SecretKeySpec? = rawEnvKey?.let { deriveKey(it) }
    /** True when a real key is configured (production). False = DEV passthrough. */
    val isConfigured: Boolean get() = secretKey != null

    init {
        if (secretKey == null) {
            log.warn(
                "AI_ENCRYPTION_KEY not set — provider keys will be stored in DEV " +
                    "passthrough mode (NOT encrypted). Set AI_ENCRYPTION_KEY in production."
            )
        }
    }

    /** Encrypt plaintext → Base64(IV||ciphertext) or `plain:<text>` in dev mode. */
    fun encrypt(plaintext: String): String {
        val key = secretKey ?: return "$PLAIN_PREFIX$plaintext"
        val iv = ByteArray(IV_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        }
        val ct = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val combined = iv + ct
        return Base64.getEncoder().encodeToString(combined)
    }

    /** Decrypt Base64(IV||ciphertext) (or `plain:<text>`); returns null on failure. */
    fun decrypt(stored: String): String? {
        if (stored.isBlank()) return null
        if (stored.startsWith(PLAIN_PREFIX)) return stored.removePrefix(PLAIN_PREFIX)
        val key = secretKey ?: run {
            log.warn("Cannot decrypt a ciphertext without AI_ENCRYPTION_KEY")
            return null
        }
        return runCatching {
            val combined = Base64.getDecoder().decode(stored)
            if (combined.size <= IV_BYTES) return null
            val iv = combined.copyOfRange(0, IV_BYTES)
            val ct = combined.copyOfRange(IV_BYTES, combined.size)
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            }
            String(cipher.doFinal(ct), Charsets.UTF_8)
        }.onFailure { log.warn("Provider key decryption failed: ${it.message}") }.getOrNull()
    }

    /** Mask a key for safe display in API responses / logs. */
    fun mask(plaintext: String?): String {
        if (plaintext.isNullOrBlank()) return ""
        if (plaintext.length <= 8) return "****"
        return plaintext.take(4) + "…" + plaintext.takeLast(4)
    }

    private fun deriveKey(raw: String): SecretKeySpec {
        val bytes = if (raw.length == 64 && raw.all { it.isHexDigit() }) {
            raw.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        } else {
            // Any other string → SHA-256 to a deterministic 32-byte key.
            MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        }
        return SecretKeySpec(bytes, "AES")
    }

    private fun Char.isHexDigit() = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val GCM_TAG_BITS = 128
        const val PLAIN_PREFIX = "plain:"
    }
}

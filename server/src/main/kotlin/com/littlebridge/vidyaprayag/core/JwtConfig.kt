/*
 * File: JwtConfig.kt
 * Module: core
 * Purpose:
 *   Centralised HMAC256 JWT issuance + verification for VidyaPrayag.
 *   Wraps com.auth0:java-jwt (transitively pulled in by ktor-server-auth-jwt).
 *
 * Reads (with safe dev defaults):
 *   - JWT_SECRET       → HMAC signing key (REQUIRED in production)
 *   - JWT_ISSUER       → default "vidyaprayag-api"
 *   - JWT_AUDIENCE     → default "vidyaprayag-app"
 *   - JWT_REALM        → default "vidyaprayag"
 *   - JWT_EXPIRY_SECS  → default 7 days for access token
 *
 * Token claims:
 *   - sub        : userId (UUID string)
 *   - role       : ADMIN | PARENT | TEACHER
 *   - name       : display name (convenience)
 *
 * Used by:
 *   - feature/auth/AuthRouting.kt    (signup, login → issues token)
 *   - core/SecurityModule.kt         (installs Ktor JWT auth)
 *   - any handler that does `call.principalUserId()`
 *
 * NOTE FOR DEVOPS (manual step you must do):
 *   Set JWT_SECRET to a strong random 256-bit value in production .env.
 *   The dev fallback ("vidyaprayag-dev-secret-change-me") is INSECURE and is
 *   only there so the server boots on a fresh clone.
 */
package com.littlebridge.vidyaprayag.core

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date

object JwtConfig {
    private fun env(name: String, default: String): String =
        System.getenv(name)?.takeIf { it.isNotBlank() } ?: default

    val secret: String   by lazy { env("JWT_SECRET", "vidyaprayag-dev-secret-change-me") }
    val issuer: String   by lazy { env("JWT_ISSUER", "vidyaprayag-api") }
    val audience: String by lazy { env("JWT_AUDIENCE", "vidyaprayag-app") }
    val realm: String    by lazy { env("JWT_REALM", "vidyaprayag") }
    val expirySecs: Long by lazy { env("JWT_EXPIRY_SECS", "604800").toLong() } // 7 days

    private val algorithm by lazy { Algorithm.HMAC256(secret) }

    val verifier: com.auth0.jwt.JWTVerifier by lazy {
        JWT.require(algorithm)
            .withIssuer(issuer)
            .withAudience(audience)
            .build()
    }

    /** Issue a signed access token. */
    fun issueToken(userId: String, role: String, name: String): String =
        JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withSubject(userId)
            .withClaim("role", role)
            .withClaim("name", name)
            .withIssuedAt(Date())
            .withExpiresAt(Date(System.currentTimeMillis() + expirySecs * 1000))
            .sign(algorithm)

    /** Issue an opaque refresh token. In production, persist + rotate it. */
    fun issueRefreshToken(userId: String): String =
        JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withSubject(userId)
            .withClaim("type", "refresh")
            .withIssuedAt(Date())
            .withExpiresAt(Date(System.currentTimeMillis() + 30L * 24 * 3600 * 1000)) // 30 days
            .sign(algorithm)
}

/*
 * File: SecurityModule.kt
 * Module: core
 * Purpose:
 *   Installs the Ktor Authentication plugin with a "jwt" provider configured
 *   from JwtConfig. Also exposes the small helpers used by routes:
 *     - call.principalUserId()
 *     - call.principalRole()
 *
 * Used by:
 *   - Application.kt → install(Authentication) { configureJwt() }
 *   - Every protected route wraps its block in `authenticate("jwt") { ... }`.
 *
 * Spec ref:
 *   - vidya_prayag_api_spec.artifact.md §Authentication "Bearer JWT"
 *   - vidya_prayag_api_spec2.artifact.md §Headers "Authorization: Bearer ..."
 */
package com.littlebridge.vidyaprayag.core

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*

/** Apply to `install(Authentication) { configureJwt() }`. */
fun AuthenticationConfig.configureJwt(name: String = "jwt") {
    jwt(name) {
        realm = JwtConfig.realm
        verifier(JwtConfig.verifier)
        validate { credential ->
            if (credential.payload.subject.isNullOrBlank()) null
            else JWTPrincipal(credential.payload)
        }
        challenge { _, _ ->
            call.respond(
                HttpStatusCode.Unauthorized,
                ApiError(message = "Session expired, please login again", errorCode = "UNAUTHORIZED")
            )
        }
    }
}

/** Returns the authenticated user's UUID string from JWT `sub`, or null if absent. */
fun ApplicationCall.principalUserId(): String? =
    principal<JWTPrincipal>()?.payload?.subject

/** Returns the authenticated user's role claim, or null. */
fun ApplicationCall.principalRole(): String? =
    principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString()

/** Returns the authenticated user's display name claim, or null. */
fun ApplicationCall.principalName(): String? =
    principal<JWTPrincipal>()?.payload?.getClaim("name")?.asString()

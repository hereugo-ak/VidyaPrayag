// FILE: src/main/kotlin/com/littlebridge/enrollplus/feature/pews/core/ErrorMapper.kt
package com.littlebridge.enrollplus.feature.pews.core

import com.littlebridge.enrollplus.core.ApiError
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond

/**
 * Shared error-mapping utility for PEWS 2.0 modules. Centralizes the mapping
 * of PEWS-specific exceptions to HTTP responses so modules don't duplicate
 * error-handling boilerplate (REUSABILITY mandate).
 *
 * The global [PewsDisabledException] is also handled by StatusPages
 * (ErrorHandling.kt) so routes that let it propagate still produce the
 * correct 503 response. This utility is for routes that want explicit
 * control over error responses.
 *
 * SOLID:
 *   - S: one responsibility — mapping PEWS errors to HTTP responses.
 *   - D: depends on Ktor's ApplicationCall, not on any PEWS module.
 */
object ErrorMapper {

    /**
     * Respond to a [PewsDisabledException] with the canonical 503 body:
     * `{"pews":"disabled","module":"<name>"}`.
     */
    suspend fun ApplicationCall.respondPewsDisabled(moduleName: String) {
        respond(
            HttpStatusCode.ServiceUnavailable,
            PewsDisabledResponse(module = moduleName),
        )
    }

    /**
     * Respond with a generic PEWS error (non-503). Used for validation
     * failures, not-found, etc. where the module wants a structured body.
     */
    suspend fun ApplicationCall.respondPewsError(
        message: String,
        status: HttpStatusCode = HttpStatusCode.BadRequest,
        errorCode: String? = null,
    ) {
        respond(status, ApiError(success = false, message = message, errorCode = errorCode))
    }
}

/**
 * The canonical 503 body when a PEWS module is killed.
 * Matches the spec: `{"pews":"disabled","module":"<name>"}`.
 */
@kotlinx.serialization.Serializable
data class PewsDisabledResponse(
    val pews: String = "disabled",
    val module: String,
)

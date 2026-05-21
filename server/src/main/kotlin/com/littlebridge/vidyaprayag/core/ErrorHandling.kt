/*
 * File: ErrorHandling.kt
 * Module: core
 * Purpose:
 *   Installs Ktor's StatusPages so that any uncaught exception from a route
 *   becomes a well-formed { success:false, message } envelope instead of an
 *   HTML stack trace. Keeps mobile clients happy and matches the spec's
 *   error response shape.
 *
 * Maps:
 *   - BadRequestException (parent of ContentTransformationException in Ktor 3.x)
 *                                    -> 400 Bad Request ("Invalid request body")
 *   - IllegalArgumentException       -> 400 Bad Request (message preserved)
 *   - NotFoundException              -> 404 Not Found
 *   - Throwable (catch-all)          -> 500 Internal Server Error
 *
 * Used by:
 *   - Application.kt -> install(StatusPages) { configureErrorHandling() }
 */
package com.littlebridge.vidyaprayag.core

import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.plugins.statuspages.StatusPagesConfig
import io.ktor.server.request.uri
import io.ktor.server.response.respond

fun StatusPagesConfig.configureErrorHandling() {
    // BadRequestException is the parent of Ktor's ContentTransformationException,
    // so this also catches malformed JSON / missing fields during deserialization.
    exception<BadRequestException> { call, cause ->
        call.respond(
            HttpStatusCode.BadRequest,
            ApiError(message = "Invalid request: ${cause.message ?: "malformed body"}")
        )
    }
    exception<IllegalArgumentException> { call, cause ->
        call.respond(
            HttpStatusCode.BadRequest,
            ApiError(message = cause.message ?: "Bad request")
        )
    }
    exception<NotFoundException> { call, cause ->
        call.respond(
            HttpStatusCode.NotFound,
            ApiError(message = cause.message ?: "Resource not found")
        )
    }
    exception<Throwable> { call, cause ->
        // Log to stderr; production should pipe this to a real logger / Sentry.
        System.err.println("[VidyaPrayag] Unhandled error on ${call.request.uri}: ${cause.message}")
        cause.printStackTrace()
        call.respond(
            HttpStatusCode.InternalServerError,
            ApiError(message = "Something went wrong. Please try again later.")
        )
    }

    status(HttpStatusCode.NotFound) { call, _ ->
        call.respond(
            HttpStatusCode.NotFound,
            ApiError(message = "Endpoint not found: ${call.request.uri}")
        )
    }
}

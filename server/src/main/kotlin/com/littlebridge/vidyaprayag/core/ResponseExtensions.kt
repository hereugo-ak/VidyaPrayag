/*
 * File: ResponseExtensions.kt
 * Module: core
 * Purpose:
 *   Tiny DSL on top of Ktor's `ApplicationCall.respond` that guarantees every
 *   handler emits the canonical { success, message, data } envelope without
 *   boilerplate.
 *
 * Provides:
 *   - call.ok(data, message, status)          → 200 success envelope
 *   - call.created(data, message)             → 201 Created envelope
 *   - call.accepted(data, message)            → 202 Accepted envelope (used by /sync-whatsapp)
 *   - call.fail(message, status, errorCode)   → error envelope w/ chosen HTTP code
 *
 * Used by: every *Routing.kt file under the feature package.
 */
package com.littlebridge.vidyaprayag.core

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*

suspend inline fun <reified T : Any> ApplicationCall.ok(
    data: T,
    message: String = "OK",
    status: HttpStatusCode = HttpStatusCode.OK
) = respond(status, ApiResponse(success = true, message = message, data = data))

suspend inline fun <reified T : Any> ApplicationCall.created(
    data: T,
    message: String = "Created"
) = respond(HttpStatusCode.Created, ApiResponse(success = true, message = message, data = data))

suspend inline fun <reified T : Any> ApplicationCall.accepted(
    data: T,
    message: String = "Accepted"
) = respond(HttpStatusCode.Accepted, ApiResponse(success = true, message = message, data = data))

suspend fun ApplicationCall.okMessage(
    message: String,
    status: HttpStatusCode = HttpStatusCode.OK
) = respond(status, ApiResponse<Unit>(success = true, message = message, data = null))

suspend fun ApplicationCall.fail(
    message: String,
    status: HttpStatusCode = HttpStatusCode.BadRequest,
    errorCode: String? = null
) = respond(status, ApiError(success = false, message = message, errorCode = errorCode))

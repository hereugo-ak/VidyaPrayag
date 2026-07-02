/*
 * File: LibraryExceptions.kt
 * Module: feature.library
 *
 * Custom exception hierarchy for the library feature. Mapped to HTTP
 * status codes in LibraryRouting.kt via toHttpStatusCode().
 */
package com.littlebridge.enrollplus.feature.library

sealed class LibraryException(message: String) : RuntimeException(message)

class LibraryValidationException(val field: String, message: String) : LibraryException(message)
class LibraryConflictException(message: String) : LibraryException(message)
class LibraryPermissionException(message: String) : LibraryException(message)
class LibraryNotFoundException(entity: String, id: String) : LibraryException("$entity not found: $id")
class LibraryConfigurationException(message: String) : LibraryException(message)

fun LibraryException.toHttpStatusCode(): io.ktor.http.HttpStatusCode = when (this) {
    is LibraryValidationException -> io.ktor.http.HttpStatusCode.BadRequest
    is LibraryConflictException -> io.ktor.http.HttpStatusCode.Conflict
    is LibraryPermissionException -> io.ktor.http.HttpStatusCode.Forbidden
    is LibraryNotFoundException -> io.ktor.http.HttpStatusCode.NotFound
    is LibraryConfigurationException -> io.ktor.http.HttpStatusCode.InternalServerError
}

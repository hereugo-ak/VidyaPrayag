package com.littlebridge.vidyaprayag.feature.auth.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val contact: String,
    val password: String,
    val role: String // "ADMIN" or "PARENT"
)

@Serializable
data class SignupRequest(
    val name: String,
    val contact: String,
    val password: String,
    val role: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val userId: String,
    val name: String,
    val role: String
)

@Serializable
data class ErrorResponse(
    val message: String
)

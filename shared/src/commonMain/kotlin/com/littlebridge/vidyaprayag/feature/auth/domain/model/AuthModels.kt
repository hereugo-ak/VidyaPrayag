package com.littlebridge.vidyaprayag.feature.auth.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class AuthFlow {
    LOGIN_EMAIL,
    SIGNUP_EMAIL,
    LOGIN_PHONE,
    SIGNUP_PHONE
}

@Serializable
data class CheckUserRequest(
    val identifier: String
)

@Serializable
data class UserFlowResponse(
    val flow: AuthFlow
)

@Serializable
data class LoginRequest(
    val contact: String,
    val password: String? = null,
    val otp: String? = null,
    val role: String // "ADMIN" or "PARENT"
)

@Serializable
data class SignupRequest(
    val name: String,
    val contact: String,
    val password: String? = null,
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

@Serializable
data class OtpRequest(
    val contact: String
)

@Serializable
data class OtpResponse(
    val message: String
)

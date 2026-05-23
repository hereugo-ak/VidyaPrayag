package com.littlebridge.vidyaprayag.feature.auth.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

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
    @SerialName("is_new_user") val isNewUser: Boolean,
    @SerialName("auth_method_required") val authMethodRequired: String,
    val message: String
)

@Serializable
data class LoginRequest(
    val identifier: String,
    val password: String? = null,
    val otp: String? = null,
    val role: String // "ADMIN" or "PARENT"
)

@Serializable
data class SignupRequest(
    val name: String,
    val identifier: String,
    val password: String? = null,
    val otp: String? = null,
    val role: String
)

@Serializable
data class AuthResponse(
    val token: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("user_id") val userId: String,
    val name: String,
    val role: String,
    @SerialName("profile_completed") val profileCompleted: Boolean
)

@Serializable
data class ErrorResponse(
    val message: String
)

@Serializable
data class OtpRequest(
    val identifier: String,
    val purpose: String? = null
)

@Serializable
data class OtpResponse(
    val message: String
)

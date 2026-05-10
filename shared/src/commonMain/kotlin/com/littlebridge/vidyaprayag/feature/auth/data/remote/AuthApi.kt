package com.littlebridge.vidyaprayag.feature.auth.data.remote

import com.littlebridge.vidyaprayag.feature.auth.domain.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class AuthApi(
    private val client: HttpClient,
    private val baseUrl: String = "https://vidyaprayag-1.onrender.com"
) {
    suspend fun checkUser(identifier: String): UserFlowResponse {
        return client.post("$baseUrl/auth/check-user") {
            contentType(ContentType.Application.Json)
            setBody(CheckUserRequest(identifier))
        }.body()
    }

    suspend fun signup(request: SignupRequest): AuthResponse {
        return client.post("$baseUrl/auth/signup") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun login(request: LoginRequest): AuthResponse {
        return client.post("$baseUrl/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun sendOtp(contact: String): OtpResponse {
        return client.post("$baseUrl/auth/send-otp") {
            contentType(ContentType.Application.Json)
            setBody(OtpRequest(contact))
        }.body()
    }
}

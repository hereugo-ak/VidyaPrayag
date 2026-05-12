package com.littlebridge.vidyaprayag.feature.auth.data.remote

import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.core.network.safeApiCall
import com.littlebridge.vidyaprayag.feature.auth.domain.model.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*

class AuthApi(
    private val client: HttpClient,
    private val baseUrl: String
) {
    suspend fun checkUser(identifier: String): NetworkResult<UserFlowResponse> {
        return safeApiCall {
            client.post("$baseUrl/auth/check-user") {
                contentType(ContentType.Application.Json)
                setBody(CheckUserRequest(identifier))
            }
        }
    }

    suspend fun signup(request: SignupRequest): NetworkResult<AuthResponse> {
        return safeApiCall {
            client.post("$baseUrl/auth/signup") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    suspend fun login(request: LoginRequest): NetworkResult<AuthResponse> {
        return safeApiCall {
            client.post("$baseUrl/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    suspend fun sendOtp(contact: String): NetworkResult<OtpResponse> {
        return safeApiCall {
            client.post("$baseUrl/auth/send-otp") {
                contentType(ContentType.Application.Json)
                setBody(OtpRequest(contact))
            }
        }
    }
}

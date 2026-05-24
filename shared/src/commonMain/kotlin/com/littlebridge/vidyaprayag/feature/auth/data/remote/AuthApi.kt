package com.littlebridge.vidyaprayag.feature.auth.data.remote

import com.littlebridge.vidyaprayag.core.model.ApiResponse
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
    private fun getUrl(path: String): String {
        val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val cleanPath = if (path.startsWith("/")) path.substring(1) else path
        return "$base$cleanPath"
    }

    suspend fun checkUser(identifier: String): NetworkResult<ApiResponse<UserFlowResponse>> {
        return safeApiCall {
            client.post(getUrl("api/v1/auth/check-user")) {
                contentType(ContentType.Application.Json)
                setBody(CheckUserRequest(identifier))
            }
        }
    }

    suspend fun signup(request: SignupRequest): NetworkResult<ApiResponse<AuthResponse>> {
        return safeApiCall {
            client.post(getUrl("api/v1/auth/signup")) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    suspend fun login(request: LoginRequest): NetworkResult<ApiResponse<AuthResponse>> {
        return safeApiCall {
            client.post(getUrl("api/v1/auth/login")) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    suspend fun sendOtp(identifier: String, purpose: String? = null): NetworkResult<ApiResponse<OtpResponse>> {
        return safeApiCall {
            client.post(getUrl("api/v1/auth/send-otp")) {
                contentType(ContentType.Application.Json)
                setBody(OtpRequest(identifier, purpose))
            }
        }
    }

    suspend fun verifyOtp(identifier: String, code: String, purpose: String? = null): NetworkResult<ApiResponse<OtpResponse>> {
        return safeApiCall {
            client.post(getUrl("api/v1/auth/verify-otp")) {
                contentType(ContentType.Application.Json)
                setBody(mapOf("identifier" to identifier, "code" to code, "purpose" to purpose))
            }
        }
    }

    suspend fun getUserDetails(token: String): NetworkResult<UserDetailsResponse> {
        return safeApiCall {
            client.get(getUrl("api/v1/user/details")) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }
    }
}

package com.littlebridge.vidyaprayag.feature.auth.domain.repository

import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.feature.auth.domain.model.*

interface AuthRepository {
    suspend fun checkUser(identifier: String): NetworkResult<AuthFlow>
    suspend fun signup(request: SignupRequest): NetworkResult<AuthResponse>
    suspend fun login(request: LoginRequest): NetworkResult<AuthResponse>
    suspend fun sendOtp(identifier: String, purpose: String? = null): NetworkResult<String>
    suspend fun verifyOtp(identifier: String, code: String, purpose: String? = null): NetworkResult<Boolean>
    suspend fun saveSession(response: AuthResponse)
    suspend fun getSession(): AuthResponse?
    suspend fun logout()
    suspend fun getUserDetails(token: String): NetworkResult<UserDetailsResponse>
}

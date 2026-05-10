package com.littlebridge.vidyaprayag.feature.auth.domain.repository

import com.littlebridge.vidyaprayag.feature.auth.domain.model.*

interface AuthRepository {
    suspend fun checkUser(identifier: String): Result<AuthFlow>
    suspend fun signup(request: SignupRequest): Result<AuthResponse>
    suspend fun login(request: LoginRequest): Result<AuthResponse>
    suspend fun sendOtp(contact: String): Result<String>
    suspend fun saveSession(response: AuthResponse)
    suspend fun getSession(): AuthResponse?
    suspend fun logout()
}

package com.littlebridge.vidyaprayag.feature.auth.data.repository

import com.littlebridge.vidyaprayag.feature.auth.data.remote.AuthApi
import com.littlebridge.vidyaprayag.feature.auth.domain.model.*
import com.littlebridge.vidyaprayag.feature.auth.domain.repository.AuthRepository

class AuthRepositoryImpl(
    private val api: AuthApi
) : AuthRepository {
    private var cachedSession: AuthResponse? = null

    override suspend fun checkUser(identifier: String): Result<AuthFlow> = try {
        Result.success(api.checkUser(identifier).flow)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun signup(request: SignupRequest): Result<AuthResponse> = try {
        val response = api.signup(request)
        saveSession(response)
        Result.success(response)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun login(request: LoginRequest): Result<AuthResponse> = try {
        val response = api.login(request)
        saveSession(response)
        Result.success(response)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun sendOtp(contact: String): Result<String> = try {
        Result.success(api.sendOtp(contact).message)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun saveSession(response: AuthResponse) {
        cachedSession = response
        // In a real app, save to DataStore/SharedPreferences here
    }

    override suspend fun getSession(): AuthResponse? {
        return cachedSession
    }

    override suspend fun logout() {
        cachedSession = null
    }
}

package com.littlebridge.vidyaprayag.feature.auth.data.repository

import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.core.prefs.PreferenceRepository
import com.littlebridge.vidyaprayag.feature.auth.data.remote.AuthApi
import com.littlebridge.vidyaprayag.feature.auth.domain.model.*
import com.littlebridge.vidyaprayag.feature.auth.domain.repository.AuthRepository

class AuthRepositoryImpl(
    private val api: AuthApi,
    private val preferenceRepository: PreferenceRepository
) : AuthRepository {
    private var cachedSession: AuthResponse? = null

    override suspend fun checkUser(identifier: String): NetworkResult<AuthFlow> {
        return when (val result = api.checkUser(identifier)) {
            is NetworkResult.Success -> {
                val data = result.data.data ?: return NetworkResult.Error("No data in response")
                val isEmail = identifier.contains("@")
                val flow = when {
                    isEmail && data.isNewUser -> AuthFlow.SIGNUP_EMAIL
                    isEmail && !data.isNewUser -> AuthFlow.LOGIN_EMAIL
                    !isEmail && data.isNewUser -> AuthFlow.SIGNUP_PHONE
                    else -> AuthFlow.LOGIN_PHONE
                }
                NetworkResult.Success(flow)
            }
            is NetworkResult.Error -> NetworkResult.Error(result.message, result.code)
            is NetworkResult.ConnectionError -> NetworkResult.ConnectionError
        }
    }

    override suspend fun signup(request: SignupRequest): NetworkResult<AuthResponse> {
        return when (val result = api.signup(request)) {
            is NetworkResult.Success -> {
                val data = result.data.data ?: return NetworkResult.Error("No data in response")
                saveSession(data)
                NetworkResult.Success(data)
            }
            is NetworkResult.Error -> NetworkResult.Error(result.message, result.code)
            is NetworkResult.ConnectionError -> NetworkResult.ConnectionError
        }
    }

    override suspend fun login(request: LoginRequest): NetworkResult<AuthResponse> {
        return when (val result = api.login(request)) {
            is NetworkResult.Success -> {
                val data = result.data.data ?: return NetworkResult.Error("No data in response")
                saveSession(data)
                NetworkResult.Success(data)
            }
            is NetworkResult.Error -> NetworkResult.Error(result.message, result.code)
            is NetworkResult.ConnectionError -> NetworkResult.ConnectionError
        }
    }

    override suspend fun sendOtp(identifier: String, purpose: String?): NetworkResult<String> {
        return when (val result = api.sendOtp(identifier, purpose)) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.data?.message ?: result.data.message)
            is NetworkResult.Error -> NetworkResult.Error(result.message, result.code)
            is NetworkResult.ConnectionError -> NetworkResult.ConnectionError
        }
    }

    override suspend fun verifyOtp(identifier: String, code: String, purpose: String?): NetworkResult<Boolean> {
        return when (val result = api.verifyOtp(identifier, code, purpose)) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.success)
            is NetworkResult.Error -> NetworkResult.Error(result.message, result.code)
            is NetworkResult.ConnectionError -> NetworkResult.ConnectionError
        }
    }

    override suspend fun saveSession(response: AuthResponse) {
        cachedSession = response
        preferenceRepository.setUserToken(response.token)
        preferenceRepository.setUserRole(response.role)
    }

    override suspend fun getSession(): AuthResponse? = cachedSession

    override suspend fun logout() {
        cachedSession = null
        preferenceRepository.clearSession()
    }

    override suspend fun getUserDetails(token: String): NetworkResult<UserDetailsResponse> {
        return api.getUserDetails(token)
    }
}

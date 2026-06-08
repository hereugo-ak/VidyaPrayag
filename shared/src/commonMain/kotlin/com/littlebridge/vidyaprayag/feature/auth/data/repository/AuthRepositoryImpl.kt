package com.littlebridge.vidyaprayag.feature.auth.data.repository

import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.core.prefs.PreferenceRepository
import com.littlebridge.vidyaprayag.feature.auth.data.remote.AuthApi
import com.littlebridge.vidyaprayag.feature.auth.domain.model.*
import com.littlebridge.vidyaprayag.feature.auth.domain.repository.AuthRepository
import kotlinx.coroutines.flow.first

class AuthRepositoryImpl(
    private val api: AuthApi,
    private val preferenceRepository: PreferenceRepository
) : AuthRepository {
    // RA-29: there is NO in-memory session cache. `prefs` is the single source
    // of truth — the same store the Ktor `Auth` plugin's `refreshTokens` writes
    // the rotated access/refresh tokens into. A previous `cachedSession` field
    // was written once on login but never updated on refresh, so `getSession()`
    // could hand back a stale (pre-rotation) token until the next cold start.
    // Reading prefs every time keeps the repository and the plugin in lock-step.

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
        // Persist the FULL session so it survives an app restart (audit §3.4).
        // Previously only token+role were stored and userId/refreshToken/
        // profileCompleted lived only in the in-memory cache → unrecoverable
        // after a cold start, and the server's refresh route was unreachable.
        preferenceRepository.setUserToken(response.token)
        preferenceRepository.setUserRole(response.role)
        preferenceRepository.setUserId(response.userId)
        preferenceRepository.setRefreshToken(response.refreshToken)
        preferenceRepository.setProfileCompleted(response.profileCompleted)
    }

    override suspend fun getSession(): AuthResponse? {
        // RA-29: always read the live prefs (single source of truth). After a
        // 401-triggered token refresh, the Auth plugin rewrites the token here,
        // so this reflects the rotated token immediately rather than a stale one.
        val token = preferenceRepository.getUserToken().first() ?: return null
        val refreshToken = preferenceRepository.getRefreshToken().first() ?: ""
        val userId = preferenceRepository.getUserId().first() ?: ""
        val role = preferenceRepository.getUserRole().first()
        val profileCompleted = preferenceRepository.getProfileCompleted().first() ?: false
        return AuthResponse(
            token = token,
            refreshToken = refreshToken,
            userId = userId,
            name = "",
            role = role,
            profileCompleted = profileCompleted
        )
    }

    override suspend fun refresh(): NetworkResult<AuthResponse> {
        val refreshToken = preferenceRepository.getRefreshToken().first()
            ?: return NetworkResult.Error("No refresh token stored")
        return when (val result = api.refresh(refreshToken)) {
            is NetworkResult.Success -> {
                val data = result.data.data ?: return NetworkResult.Error("No data in response")
                saveSession(data)
                NetworkResult.Success(data)
            }
            is NetworkResult.Error -> NetworkResult.Error(result.message, result.code)
            is NetworkResult.ConnectionError -> NetworkResult.ConnectionError
        }
    }

    override suspend fun logout() {
        // Best-effort server-side revocation (audit §3.6) before clearing local
        // state, so the refresh token cannot be reused for 30 days.
        val token = preferenceRepository.getUserToken().first()
        val refreshToken = preferenceRepository.getRefreshToken().first()
        if (token != null) {
            runCatching { api.logout(token, refreshToken) }
        }
        preferenceRepository.clearSession()
    }

    override suspend fun getUserDetails(token: String): NetworkResult<UserDetailsResponse> {
        return api.getUserDetails(token)
    }
}

package com.littlebridge.enrollplus.core.prefs

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class InMemoryPreferenceManager : PreferenceRepository {
    private val themeName = MutableStateFlow("LIGHT")
    private val userRole = MutableStateFlow("GUEST")
    private val userToken = MutableStateFlow<String?>(null)
    private val userId = MutableStateFlow<String?>(null)
    private val refreshToken = MutableStateFlow<String?>(null)
    private val profileCompleted = MutableStateFlow<Boolean?>(null)
    private val userName = MutableStateFlow<String?>(null)
    private val fcmToken = MutableStateFlow<String?>(null)
    private val notificationsDeclined = MutableStateFlow(false)

    override fun getThemeName(): Flow<String> {
        return themeName
    }

    override suspend fun setThemeName(name: String) {
        themeName.value = name
    }

    override fun getUserRole(): Flow<String> {
        return userRole
    }

    override suspend fun setUserRole(role: String) {
        userRole.value = role
    }

    override fun getUserToken(): Flow<String?> {
        return userToken
    }

    override suspend fun setUserToken(token: String?) {
        userToken.value = token
    }

    override fun getUserId(): Flow<String?> = userId
    override suspend fun setUserId(userId: String?) { this.userId.value = userId }

    override fun getRefreshToken(): Flow<String?> = refreshToken
    override suspend fun setRefreshToken(token: String?) { refreshToken.value = token }

    override fun getProfileCompleted(): Flow<Boolean?> = profileCompleted
    override suspend fun setProfileCompleted(completed: Boolean?) { profileCompleted.value = completed }

    override fun getUserName(): Flow<String?> = userName
    override suspend fun setUserName(name: String?) { userName.value = name?.takeIf { it.isNotBlank() } }

    override fun getFcmToken(): Flow<String?> = fcmToken
    override suspend fun setFcmToken(token: String?) { fcmToken.value = token }

    override fun getNotificationsDeclined(): Flow<Boolean> = notificationsDeclined
    override suspend fun setNotificationsDeclined(declined: Boolean) { notificationsDeclined.value = declined }

    override suspend fun clearSession() {
        userRole.value = "GUEST"
        userToken.value = null
        userId.value = null
        refreshToken.value = null
        profileCompleted.value = null
        userName.value = null
        fcmToken.value = null
        notificationsDeclined.value = false
    }
}

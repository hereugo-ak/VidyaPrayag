package com.littlebridge.vidyaprayag.core.prefs

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class InMemoryPreferenceManager : PreferenceRepository {
    private val themeName = MutableStateFlow("LIGHT")
    private val userRole = MutableStateFlow("GUEST")
    private val userToken = MutableStateFlow<String?>(null)

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

    override suspend fun clearSession() {
        userRole.value = "GUEST"
        userToken.value = null
    }
}

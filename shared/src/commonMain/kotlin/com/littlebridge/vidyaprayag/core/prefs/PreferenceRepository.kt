package com.littlebridge.vidyaprayag.core.prefs

import kotlinx.coroutines.flow.Flow

interface PreferenceRepository {
    fun getThemeName(): Flow<String>
    suspend fun setThemeName(name: String)
    
    fun getUserRole(): Flow<String>
    suspend fun setUserRole(role: String)

    fun getUserToken(): Flow<String?>
    suspend fun setUserToken(token: String?)

    suspend fun clearSession()
}

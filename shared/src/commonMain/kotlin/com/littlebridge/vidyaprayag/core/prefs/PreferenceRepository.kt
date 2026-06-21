package com.littlebridge.vidyaprayag.core.prefs

import kotlinx.coroutines.flow.Flow

interface PreferenceRepository {
    fun getThemeName(): Flow<String>
    suspend fun setThemeName(name: String)
    
    fun getUserRole(): Flow<String>
    suspend fun setUserRole(role: String)

    fun getUserToken(): Flow<String?>
    suspend fun setUserToken(token: String?)

    // --- session persistence (audit §3.4, finding F) ---
    // These three were previously only held in an in-memory cache and were
    // permanently lost on app restart, making the full session unrecoverable
    // and the server's refresh infrastructure unreachable.
    fun getUserId(): Flow<String?>
    suspend fun setUserId(userId: String?)

    fun getRefreshToken(): Flow<String?>
    suspend fun setRefreshToken(token: String?)

    fun getProfileCompleted(): Flow<Boolean?>
    suspend fun setProfileCompleted(completed: Boolean?)

    // RA-S03: the user's display name, persisted at sign-in and refreshed from
    // GET /user/details, so the portal headers/avatars can greet the real user
    // instead of hardcoding "Parent". (school_id is intentionally NOT persisted
    // — the server authoritatively derives it from the JWT.)
    fun getUserName(): Flow<String?>
    suspend fun setUserName(name: String?)

    suspend fun clearSession()
}

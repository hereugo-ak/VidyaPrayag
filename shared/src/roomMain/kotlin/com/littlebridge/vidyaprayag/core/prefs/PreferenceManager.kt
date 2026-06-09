package com.littlebridge.vidyaprayag.core.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath

class PreferenceManager(
    private val dataStore: DataStore<Preferences>
) : PreferenceRepository {

    private val THEME_NAME_KEY = stringPreferencesKey("theme_name")
    private val USER_ROLE_KEY = stringPreferencesKey("user_role")
    private val USER_TOKEN_KEY = stringPreferencesKey("user_token")
    private val USER_ID_KEY = stringPreferencesKey("user_id")
    private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
    private val PROFILE_COMPLETED_KEY = booleanPreferencesKey("profile_completed")
    private val USER_NAME_KEY = stringPreferencesKey("user_name")

    override fun getThemeName(): Flow<String> {
        return dataStore.data.map { preferences ->
            preferences[THEME_NAME_KEY] ?: "LIGHT"
        }
    }

    override suspend fun setThemeName(name: String) {
        dataStore.edit { preferences ->
            preferences[THEME_NAME_KEY] = name
        }
    }

    override fun getUserRole(): Flow<String> {
        return dataStore.data.map { preferences ->
            preferences[USER_ROLE_KEY] ?: "GUEST"
        }
    }

    override suspend fun setUserRole(role: String) {
        dataStore.edit { preferences ->
            preferences[USER_ROLE_KEY] = role
        }
    }

    override fun getUserToken(): Flow<String?> {
        return dataStore.data.map { preferences ->
            preferences[USER_TOKEN_KEY]
        }
    }

    override suspend fun setUserToken(token: String?) {
        dataStore.edit { preferences ->
            if (token == null) {
                preferences.remove(USER_TOKEN_KEY)
            } else {
                preferences[USER_TOKEN_KEY] = token
            }
        }
    }

    override fun getUserId(): Flow<String?> {
        return dataStore.data.map { preferences ->
            preferences[USER_ID_KEY]
        }
    }

    override suspend fun setUserId(userId: String?) {
        dataStore.edit { preferences ->
            if (userId == null) preferences.remove(USER_ID_KEY)
            else preferences[USER_ID_KEY] = userId
        }
    }

    override fun getRefreshToken(): Flow<String?> {
        return dataStore.data.map { preferences ->
            preferences[REFRESH_TOKEN_KEY]
        }
    }

    override suspend fun setRefreshToken(token: String?) {
        dataStore.edit { preferences ->
            if (token == null) preferences.remove(REFRESH_TOKEN_KEY)
            else preferences[REFRESH_TOKEN_KEY] = token
        }
    }

    override fun getProfileCompleted(): Flow<Boolean?> {
        return dataStore.data.map { preferences ->
            preferences[PROFILE_COMPLETED_KEY]
        }
    }

    override suspend fun setProfileCompleted(completed: Boolean?) {
        dataStore.edit { preferences ->
            if (completed == null) preferences.remove(PROFILE_COMPLETED_KEY)
            else preferences[PROFILE_COMPLETED_KEY] = completed
        }
    }

    override fun getUserName(): Flow<String?> {
        return dataStore.data.map { preferences ->
            preferences[USER_NAME_KEY]
        }
    }

    override suspend fun setUserName(name: String?) {
        dataStore.edit { preferences ->
            if (name.isNullOrBlank()) preferences.remove(USER_NAME_KEY)
            else preferences[USER_NAME_KEY] = name
        }
    }

    override suspend fun clearSession() {
        dataStore.edit { preferences ->
            preferences.remove(USER_ROLE_KEY)
            preferences.remove(USER_TOKEN_KEY)
            preferences.remove(USER_ID_KEY)
            preferences.remove(REFRESH_TOKEN_KEY)
            preferences.remove(PROFILE_COMPLETED_KEY)
            preferences.remove(USER_NAME_KEY)
        }
    }
}

fun createDataStore(producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        produceFile = { producePath().toPath() }
    )

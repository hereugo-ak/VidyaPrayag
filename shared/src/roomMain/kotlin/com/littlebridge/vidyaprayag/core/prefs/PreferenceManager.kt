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
}

fun createDataStore(producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        produceFile = { producePath().toPath() }
    )

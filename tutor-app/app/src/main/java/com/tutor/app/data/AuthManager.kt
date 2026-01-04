package com.tutor.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth")

class AuthManager(private val context: Context) {

    companion object {
        private val TOKEN_KEY = stringPreferencesKey("auth_token")
        private val USERNAME_KEY = stringPreferencesKey("username")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
    }

    val isLoggedIn: Flow<Boolean> = context.authDataStore.data.map { preferences ->
        !preferences[TOKEN_KEY].isNullOrEmpty()
    }

    val token: Flow<String?> = context.authDataStore.data.map { preferences ->
        preferences[TOKEN_KEY]
    }

    val username: Flow<String?> = context.authDataStore.data.map { preferences ->
        preferences[USERNAME_KEY]
    }

    suspend fun saveLoginInfo(token: String, username: String, userId: String) {
        context.authDataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
            preferences[USERNAME_KEY] = username
            preferences[USER_ID_KEY] = userId
        }
    }

    suspend fun clearAuth() {
        context.authDataStore.edit { preferences ->
            preferences.clear()
        }
    }

    suspend fun getTokenSync(): String? {
        var token: String? = null
        context.authDataStore.data.map { preferences ->
            token = preferences[TOKEN_KEY]
        }
        return token
    }
}

package com.tutor.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {
    
    companion object {
        private val BASE_URL_KEY = stringPreferencesKey("base_url")
        const val DEFAULT_BASE_URL = "http://172.20.10.4:8080/"
    }
    
    val baseUrl: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[BASE_URL_KEY] ?: DEFAULT_BASE_URL
    }
    
    suspend fun setBaseUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[BASE_URL_KEY] = url
        }
    }
}

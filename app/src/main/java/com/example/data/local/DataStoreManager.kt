package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class DataStoreManager(private val context: Context) {

    companion object {
        private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        private val DEFAULT_LOGO_KEY = stringPreferencesKey("default_logo")
        private val DEFAULT_FONT_KEY = stringPreferencesKey("default_font")
        private val DEFAULT_RESOLUTION_KEY = stringPreferencesKey("default_res")
    }

    val darkModeFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DARK_MODE_KEY] ?: true
    }

    val defaultLogoFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[DEFAULT_LOGO_KEY] ?: ""
    }

    val defaultFontFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[DEFAULT_FONT_KEY] ?: "Noto Sans Bangla"
    }

    val defaultResolutionFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[DEFAULT_RESOLUTION_KEY] ?: "1080P"
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = enabled
        }
    }

    suspend fun setDefaultLogo(path: String) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_LOGO_KEY] = path
        }
    }

    suspend fun setDefaultFont(font: String) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_FONT_KEY] = font
        }
    }

    suspend fun setDefaultResolution(res: String) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_RESOLUTION_KEY] = res
        }
    }
}

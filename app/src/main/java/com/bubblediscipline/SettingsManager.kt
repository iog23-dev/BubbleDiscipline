package com.bubblediscipline

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {

    companion object {
        val BUBBLE_COLOR = stringPreferencesKey("bubble_color")
        val BUBBLE_SPEED = floatPreferencesKey("bubble_speed")
        val APP_THEME = stringPreferencesKey("app_theme")
        
        const val DEFAULT_COLOR = "#FF4081"
        const val DEFAULT_SPEED = 12f
        const val DEFAULT_THEME = "Rosa"
    }

    val bubbleColorFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[BUBBLE_COLOR] ?: DEFAULT_COLOR
    }

    val bubbleSpeedFlow: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[BUBBLE_SPEED] ?: DEFAULT_SPEED
    }

    val appThemeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[APP_THEME] ?: DEFAULT_THEME
    }

    suspend fun setBubbleColor(color: String) {
        context.dataStore.edit { preferences ->
            preferences[BUBBLE_COLOR] = color
        }
    }

    suspend fun setBubbleSpeed(speed: Float) {
        context.dataStore.edit { preferences ->
            preferences[BUBBLE_SPEED] = speed
        }
    }

    suspend fun setAppTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[APP_THEME] = theme
        }
    }
}
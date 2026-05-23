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
        val VACATION_UNTIL = longPreferencesKey("vacation_until")
        val WHATSAPP_CONTACT = stringPreferencesKey("whatsapp_contact")
        val SEND_PANIC_MESSAGE = booleanPreferencesKey("send_panic_message")
        
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

    val vacationUntilFlow: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[VACATION_UNTIL] ?: 0L
    }

    val whatsappContactFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[WHATSAPP_CONTACT] ?: ""
    }

    val sendPanicMessageFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SEND_PANIC_MESSAGE] ?: true
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

    suspend fun setVacationUntil(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[VACATION_UNTIL] = timestamp
        }
    }

    suspend fun setWhatsappContact(contact: String) {
        context.dataStore.edit { preferences ->
            preferences[WHATSAPP_CONTACT] = contact
        }
    }

    suspend fun setSendPanicMessage(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SEND_PANIC_MESSAGE] = enabled
        }
    }
}
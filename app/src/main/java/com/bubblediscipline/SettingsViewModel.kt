package com.bubblediscipline

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsManager = SettingsManager(application)

    val bubbleColor: StateFlow<String> = settingsManager.bubbleColorFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsManager.DEFAULT_COLOR
    )

    val bubbleSpeed: StateFlow<Float> = settingsManager.bubbleSpeedFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsManager.DEFAULT_SPEED
    )

    val appTheme: StateFlow<String> = settingsManager.appThemeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsManager.DEFAULT_THEME
    )

    val vacationUntil: StateFlow<Long> = settingsManager.vacationUntilFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0L
    )

    val whatsappContact: StateFlow<String> = settingsManager.whatsappContactFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    val sendPanicMessage: StateFlow<Boolean> = settingsManager.sendPanicMessageFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    fun setBubbleColor(color: String) {
        viewModelScope.launch {
            settingsManager.setBubbleColor(color)
        }
    }

    fun setBubbleSpeed(speed: Float) {
        viewModelScope.launch {
            settingsManager.setBubbleSpeed(speed)
        }
    }

    fun setAppTheme(theme: String) {
        viewModelScope.launch {
            settingsManager.setAppTheme(theme)
        }
    }

    fun setVacationUntil(timestamp: Long) {
        viewModelScope.launch {
            settingsManager.setVacationUntil(timestamp)
        }
    }

    fun setWhatsappContact(contact: String) {
        viewModelScope.launch {
            settingsManager.setWhatsappContact(contact)
        }
    }

    fun setSendPanicMessage(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setSendPanicMessage(enabled)
        }
    }
}
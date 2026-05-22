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
}
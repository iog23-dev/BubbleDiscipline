package com.bubblediscipline

import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

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

    fun exportDatabase(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dbFile = getApplication<Application>().getDatabasePath("bubble_discipline_database")
                if (dbFile.exists()) {
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use { output ->
                        FileInputStream(dbFile).use { input ->
                            input.copyTo(output)
                        }
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(getApplication(), "Base de datos exportada", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Error al exportar: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun importDatabase(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Cerrar DB antes de sobrescribir
                AppDatabase.getDatabase(getApplication()).close()
                
                val dbFile = getApplication<Application>().getDatabasePath("bubble_discipline_database")
                getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(dbFile).use { output ->
                        input.copyTo(output)
                    }
                }
                
                // Reiniciar app o notificar éxito
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Base de datos importada. Reinicia la app.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Error al importar: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
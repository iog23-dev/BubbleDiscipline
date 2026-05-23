package com.bubblediscipline

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val missionId = intent.getIntExtra("MISSION_ID", -1)
        val missionMessage = intent.getStringExtra("MISSION_MESSAGE") ?: "¡Hora de la disciplina!"
        
        val settingsManager = SettingsManager(context)
        
        CoroutineScope(Dispatchers.Main).launch {
            val vacationUntil = settingsManager.vacationUntilFlow.first()
            if (System.currentTimeMillis() < vacationUntil) {
                Log.d("AlarmReceiver", "Modo vacaciones activo. Ignorando alarma.")
                return@launch
            }

            Log.d("BubbleDiscipline", "¡Alarma recibida! ID: $missionId, Mensaje: $missionMessage")
            Toast.makeText(context, "⏰ $missionMessage", Toast.LENGTH_LONG).show()

            val serviceIntent = Intent(context, BubbleForegroundService::class.java).apply {
                putExtra("BUBBLE_TEXT", missionMessage)
                putExtra("MISSION_ID", missionId)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}
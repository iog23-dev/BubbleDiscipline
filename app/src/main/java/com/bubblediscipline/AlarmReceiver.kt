package com.bubblediscipline

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("BubbleDiscipline", "¡Alarma recibida! Despertando el servicio de disciplina...")
        
        Toast.makeText(context, "⏰ ¡Hora de la disciplina!", Toast.LENGTH_LONG).show()

        // Arrancamos el Foreground Service
        val serviceIntent = Intent(context, BubbleForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
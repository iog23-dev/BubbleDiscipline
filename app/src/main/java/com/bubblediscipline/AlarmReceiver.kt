package com.bubblediscipline

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val missionId = intent.getIntExtra("MISSION_ID", -1)
        val missionMessage = intent.getStringExtra("MISSION_MESSAGE") ?: "¡Hora de la disciplina!"
        
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
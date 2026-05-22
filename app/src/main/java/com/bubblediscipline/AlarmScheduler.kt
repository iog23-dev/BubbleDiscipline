package com.bubblediscipline

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(mission: Mission) {
        if (!mission.isEnabled) return

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("MISSION_ID", mission.id)
            putExtra("MISSION_MESSAGE", mission.message)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            mission.id, // ID único para cada alarma
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, mission.hour)
            set(Calendar.MINUTE, mission.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // Si la hora ya pasó hoy, programar para mañana inicialmente
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        // Buscar el próximo día válido según los días seleccionados
        val nextTriggerTime = getNextOccurrence(calendar, mission)

        if (nextTriggerTime != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextTriggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        nextTriggerTime,
                        pendingIntent
                    )
                }
                Log.d("AlarmScheduler", "Alarma programada para mission ${mission.id} a las ${mission.hour}:${mission.minute}")
            } catch (e: SecurityException) {
                Log.e("AlarmScheduler", "Error de seguridad: No se puede programar alarma exacta", e)
            }
        }
    }

    fun cancel(mission: Mission) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            mission.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d("AlarmScheduler", "Alarma cancelada para mission ${mission.id}")
    }

    private fun getNextOccurrence(calendar: Calendar, mission: Mission): Long? {
        // Mapeo de Calendar.DAY_OF_WEEK a los booleanos de la misión
        val daysEnabled = mapOf(
            Calendar.MONDAY to mission.monday,
            Calendar.TUESDAY to mission.tuesday,
            Calendar.WEDNESDAY to mission.wednesday,
            Calendar.THURSDAY to mission.thursday,
            Calendar.FRIDAY to mission.friday,
            Calendar.SATURDAY to mission.saturday,
            Calendar.SUNDAY to mission.sunday
        )

        // Si no hay ningún día marcado, no programar
        if (daysEnabled.values.none { it }) return null

        // Buscamos en los próximos 7 días
        for (i in 0..7) {
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            if (daysEnabled[dayOfWeek] == true) {
                return calendar.timeInMillis
            }
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return null
    }
}
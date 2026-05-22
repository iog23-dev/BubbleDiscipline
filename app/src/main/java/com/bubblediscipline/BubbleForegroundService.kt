package com.bubblediscipline

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat

class BubbleForegroundService : Service() {

    private val CHANNEL_ID = "BubbleDisciplineChannel"
    private val NOTIFICATION_ID = 101

    private lateinit var windowManager: WindowManager
    private var bubbleView: View? = null

    override fun onCreate() {
        super.onCreate()
        Log.d("BubbleService", "Servicio creado")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("BubbleService", "onStartCommand ejecutado")
        
        // 1. Iniciar la notificación persistente obligatoria
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BubbleDiscipline Activo.")
            .setContentText("Custodiando tu pantalla...")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID, 
                    notification, 
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            Log.d("BubbleService", "startForeground llamado con éxito")
        } catch (e: Exception) {
            Log.e("BubbleService", "Error al iniciar startForeground", e)
        }

        // 2. Lanzar la burbuja superpuesta
        showOverlayBubble()

        return START_STICKY
    }

    private fun showOverlayBubble() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // Creamos una vista simple (Un texto dentro de un círculo)
        val textView = TextView(this).apply {
            text = "🫧\n¡Haz la cama!"
            id = View.generateViewId()
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            textSize = 16f
            
            // Diseñamos el círculo programáticamente (Fondo rojo/rosa disciplinario)
            val circle = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#FF4081"))
                setStroke(4, Color.WHITE)
            }
            background = circle
        }

        // Definimos el tamaño de la burbuja (Ej: 120dp x 120dp aproximados en píxeles)
        val sizeInPx = (120 * resources.displayMetrics.density).toInt()

        // Configuración crucial del WindowManager
        val layoutParams = WindowManager.LayoutParams(
            sizeInPx,
            sizeInPx,
            // TYPE_APPLICATION_OVERLAY es obligatorio desde Android 8
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // FLAG_NOT_FOCUSABLE evita que la burbuja robe el teclado del sistema
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            // Posición inicial de la burbuja (Centro de la pantalla)
            gravity = Gravity.CENTER
        }

        bubbleView = textView

        // Acción al pulsar la burbuja: Por ahora se destruye ("explota")
        bubbleView?.setOnClickListener {
            Toast.makeText(this, "💥 ¡Burbuja explotada!", Toast.LENGTH_SHORT).show()
            removeBubble()
            stopSelf() // Detiene el Foreground Service si ya no hay trabajo
        }

        // Añadimos la vista directamente al flujo visual del sistema operativo
        try {
            windowManager.addView(bubbleView, layoutParams)
        } catch (e: Exception) {
            Log.e("BubbleService", "Error al añadir vista al WindowManager", e)
            Toast.makeText(this, "Error al pintar overlay. ¿Falta el permiso?", Toast.LENGTH_LONG).show()
        }
    }

    private fun removeBubble() {
        if (bubbleView != null) {
            try {
                windowManager.removeView(bubbleView)
            } catch (e: Exception) {
                Log.e("BubbleService", "Error al quitar la burbuja", e)
            }
            bubbleView = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeBubble() // Limpieza obligatoria para evitar Memory Leaks
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Servicio de Disciplina",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Canal para las alarmas e interrupciones de BubbleDiscipline"
            }
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
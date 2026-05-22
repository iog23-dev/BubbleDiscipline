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
import android.os.Handler
import android.os.IBinder
import android.os.Looper
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
    private lateinit var layoutParams: WindowManager.LayoutParams

    // Motor de animación
    private val animationHandler = Handler(Looper.getMainLooper())
    private var animationRunnable: Runnable? = null

    // Variables de física de la burbuja
    private var posX = 0f
    private var posY = 0f
    private var speedX = 12f
    private var speedY = 15f
    private var bubbleSize = 0

    override fun onCreate() {
        super.onCreate()
        Log.d("BubbleService", "Servicio creado")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("BubbleService", "onStartCommand ejecutado")
        
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BubbleDiscipline Activo")
            .setContentText("¡La burbuja está patrullando!")
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
        } catch (e: Exception) {
            Log.e("BubbleService", "Error al iniciar startForeground", e)
        }

        showOverlayBubble()
        startBubbleAnimation()

        return START_STICKY
    }

    private fun showOverlayBubble() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        bubbleSize = (120 * resources.displayMetrics.density).toInt()

        val textView = TextView(this).apply {
            text = "🫧\n¡Cázame!"
            id = View.generateViewId()
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            textSize = 16f
            
            val circle = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#FF4081"))
                setStroke(4, Color.WHITE)
            }
            background = circle
        }

        // Es fundamental usar Gravity.TOP or Gravity.START para posicionamiento absoluto
        layoutParams = WindowManager.LayoutParams(
            bubbleSize,
            bubbleSize,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }

        posX = layoutParams.x.toFloat()
        posY = layoutParams.y.toFloat()

        bubbleView = textView
        bubbleView?.setOnClickListener {
            Toast.makeText(this, "💥 ¡Conseguido!", Toast.LENGTH_SHORT).show()
            stopSelf()
        }

        try {
            windowManager.addView(bubbleView, layoutParams)
        } catch (e: Exception) {
            Log.e("BubbleService", "Error al añadir vista", e)
            stopSelf()
        }
    }

    private fun startBubbleAnimation() {
        val metrics = resources.displayMetrics
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels

        animationRunnable = object : Runnable {
            override fun run() {
                if (bubbleView == null) return

                posX += speedX
                posY += speedY

                // Rebote X
                if (posX <= 0) {
                    speedX = Math.abs(speedX)
                    posX = 0f
                } else if (posX >= (screenWidth - bubbleSize)) {
                    speedX = -Math.abs(speedX)
                    posX = (screenWidth - bubbleSize).toFloat()
                }

                // Rebote Y
                if (posY <= 0) {
                    speedY = Math.abs(speedY)
                    posY = 0f
                } else if (posY >= (screenHeight - bubbleSize)) {
                    speedY = -Math.abs(speedY)
                    posY = (screenHeight - bubbleSize).toFloat()
                }

                layoutParams.x = posX.toInt()
                layoutParams.y = posY.toInt()
                
                try {
                    windowManager.updateViewLayout(bubbleView, layoutParams)
                    animationHandler.postDelayed(this, 16)
                } catch (e: Exception) {
                    // Si la vista ya no existe, detenemos la animación
                    animationHandler.removeCallbacks(this)
                }
            }
        }

        animationHandler.post(animationRunnable!!)
    }

    override fun onDestroy() {
        super.onDestroy()
        animationRunnable?.let { animationHandler.removeCallbacks(it) }
        removeBubble()
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
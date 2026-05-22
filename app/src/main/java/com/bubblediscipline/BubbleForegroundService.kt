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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class BubbleInstance(
    val missionId: Int,
    val view: TextView,
    val layoutParams: WindowManager.LayoutParams,
    var posX: Float,
    var posY: Float,
    var speedX: Float,
    var speedY: Float
)

class BubbleForegroundService : Service() {

    private val CHANNEL_ID = "BubbleDisciplineChannel"
    private val NOTIFICATION_ID = 101

    private lateinit var windowManager: WindowManager
    private val activeBubbles = mutableListOf<BubbleInstance>()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val animationHandler = Handler(Looper.getMainLooper())
    private var isAnimationRunning = false

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        observeDatabase()
    }

    private fun observeDatabase() {
        val missionDao = AppDatabase.getDatabase(this).missionDao()
        serviceScope.launch {
            missionDao.getAllMissions().collectLatest { missions ->
                // Cuando la DB cambie, actualizamos el texto de las burbujas activas
                activeBubbles.forEach { bubble ->
                    val updatedMission = missions.find { it.id == bubble.missionId }
                    if (updatedMission != null) {
                        bubble.view.text = "🫧\n${updatedMission.message}"
                    } else {
                        // Si la misión ya no existe, opcionalmente podríamos borrar la burbuja
                        // Pero por ahora solo la dejamos con el texto que tenía
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val bubbleText = intent?.getStringExtra("BUBBLE_TEXT") ?: "¡Cázame!"
        val missionId = intent?.getIntExtra("MISSION_ID", -1) ?: -1
        
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BubbleDiscipline Activo")
            .setContentText("Tienes misiones pendientes")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) { Log.e("BubbleService", "Error FGS", e) }

        createNewBubble(missionId, bubbleText)

        if (!isAnimationRunning) {
            startGlobalAnimationLoop()
        }

        return START_STICKY
    }

    private fun createNewBubble(missionId: Int, text: String) {
        // Evitar duplicar burbujas para la misma misión si ya está en pantalla
        if (activeBubbles.any { it.missionId == missionId && missionId != -1 }) return

        val metrics = resources.displayMetrics
        val bubbleSize = (120 * metrics.density).toInt()
        
        val textView = TextView(this).apply {
            this.text = "🫧\n$text"
            id = View.generateViewId()
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            textSize = 14f
            val circle = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#FF4081"))
                setStroke(4, Color.WHITE)
            }
            background = circle
        }

        val lp = WindowManager.LayoutParams(
            bubbleSize, bubbleSize,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (Math.random() * (metrics.widthPixels - bubbleSize)).toInt()
            y = (Math.random() * (metrics.heightPixels - bubbleSize)).toInt()
        }

        val newBubble = BubbleInstance(
            missionId = missionId,
            view = textView,
            layoutParams = lp,
            posX = lp.x.toFloat(),
            posY = lp.y.toFloat(),
            speedX = (if (Math.random() > 0.5) 1 else -1) * (8..15).random().toFloat(),
            speedY = (if (Math.random() > 0.5) 1 else -1) * (8..15).random().toFloat()
        )

        textView.setOnClickListener {
            removeBubble(newBubble)
            Toast.makeText(this, "💥 ¡Disciplina cumplida!", Toast.LENGTH_SHORT).show()
            if (activeBubbles.isEmpty()) stopSelf()
        }

        try {
            windowManager.addView(textView, lp)
            activeBubbles.add(newBubble)
        } catch (e: Exception) { Log.e("BubbleService", "Error addView", e) }
    }

    private fun startGlobalAnimationLoop() {
        if (isAnimationRunning) return
        isAnimationRunning = true
        
        val metrics = resources.displayMetrics
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels
        val bubbleSize = (120 * metrics.density).toInt()

        val runnable = object : Runnable {
            override fun run() {
                if (activeBubbles.isEmpty()) {
                    isAnimationRunning = false
                    return
                }

                val iterator = activeBubbles.iterator()
                while (iterator.hasNext()) {
                    val bubble = iterator.next()
                    bubble.posX += bubble.speedX
                    bubble.posY += bubble.speedY

                    if (bubble.posX <= 0) { bubble.speedX = Math.abs(bubble.speedX); bubble.posX = 0f }
                    else if (bubble.posX >= (screenWidth - bubbleSize)) { bubble.speedX = -Math.abs(bubble.speedX); bubble.posX = (screenWidth - bubbleSize).toFloat() }

                    if (bubble.posY <= 0) { bubble.speedY = Math.abs(bubble.speedY); bubble.posY = 0f }
                    else if (bubble.posY >= (screenHeight - bubbleSize)) { bubble.speedY = -Math.abs(bubble.speedY); bubble.posY = (screenHeight - bubbleSize).toFloat() }

                    bubble.layoutParams.x = bubble.posX.toInt()
                    bubble.layoutParams.y = bubble.posY.toInt()

                    try {
                        windowManager.updateViewLayout(bubble.view, bubble.layoutParams)
                    } catch (e: Exception) { iterator.remove() }
                }

                animationHandler.postDelayed(this, 16)
            }
        }
        animationHandler.post(runnable)
    }

    private fun removeBubble(bubble: BubbleInstance) {
        try { windowManager.removeView(bubble.view) } catch (e: Exception) { }
        activeBubbles.remove(bubble)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        animationHandler.removeCallbacksAndMessages(null)
        activeBubbles.forEach { try { windowManager.removeView(it.view) } catch (e: Exception) {} }
        activeBubbles.clear()
        isAnimationRunning = false
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Servicio de Disciplina", NotificationManager.IMPORTANCE_HIGH)
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
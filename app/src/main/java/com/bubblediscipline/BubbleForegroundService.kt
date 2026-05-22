package com.bubblediscipline

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class BlockingView(context: Context, private val onEmergencyExit: () -> Unit) : FrameLayout(context) {
    private var volumeDownCount = 0
    private var lastClickTime = 0L

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && event.action == KeyEvent.ACTION_DOWN) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime < 2000) {
                volumeDownCount++
            } else {
                volumeDownCount = 1
            }
            lastClickTime = currentTime

            if (volumeDownCount >= 3) {
                onEmergencyExit()
            }
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    // Gestionar el foco
    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (!hasWindowFocus) {
            // Si perdemos el foco (porque bajaron notificaciones), 
            // intentamos recuperarlo para que el bloqueo sea efectivo.
            requestFocus()
        }
    }
}

data class BubbleInstance(
    val missionId: Int,
    val view: TextView,
    var posX: Float,
    var posY: Float,
    var speedX: Float,
    var speedY: Float
)

class BubbleForegroundService : Service() {

    private val CHANNEL_ID = "BubbleDisciplineChannel"
    private val NOTIFICATION_ID = 101

    private lateinit var windowManager: WindowManager
    private lateinit var blockingContainer: BlockingView
    private val activeBubbles = mutableListOf<BubbleInstance>()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val animationHandler = Handler(Looper.getMainLooper())
    private var isAnimationRunning = false

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        setupBlockingUI()
        observeDatabase()
    }

    private fun setupBlockingUI() {
        blockingContainer = BlockingView(this) {
            Toast.makeText(this, "🚨 CÓDIGO DE EMERGENCIA: Desbloqueando", Toast.LENGTH_LONG).show()
            stopSelf()
        }.apply {
            setBackgroundColor(Color.argb(120, 0, 0, 0))
            isFocusable = true
            isFocusableInTouchMode = true
            isClickable = true
        }

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            // PERMITIR QUE LA VISTA INVADA EL NOTCH Y LOS BORDES REALES
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        try {
            windowManager.addView(blockingContainer, lp)
            blockingContainer.requestFocus()
        } catch (e: Exception) {
            Log.e("BubbleService", "Error al bloquear", e)
            stopSelf()
        }
    }

    private fun observeDatabase() {
        val missionDao = AppDatabase.getDatabase(this).missionDao()
        serviceScope.launch {
            missionDao.getAllMissions().collectLatest { missions ->
                activeBubbles.forEach { bubble ->
                    val updatedMission = missions.find { it.id == bubble.missionId }
                    if (updatedMission != null) {
                        bubble.view.text = "🫧\n${updatedMission.message}"
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val bubbleText = intent?.getStringExtra("BUBBLE_TEXT") ?: "¡Disciplina!"
        val missionId = intent?.getIntExtra("MISSION_ID", -1) ?: -1
        
        startForegroundNotification()
        createNewBubble(missionId, bubbleText)

        if (!isAnimationRunning) startGlobalAnimationLoop()

        return START_STICKY
    }

    private fun startForegroundNotification() {
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BubbleDiscipline BLOQUEADO")
            .setContentText("Pulsa 3 veces 'Bajar Volumen' en emergencia")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNewBubble(missionId: Int, text: String) {
        if (activeBubbles.any { it.missionId == missionId && missionId != -1 }) return

        val realSize = Point()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            realSize.x = bounds.width()
            realSize.y = bounds.height()
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealSize(realSize)
        }

        val metrics = resources.displayMetrics
        val bubbleSize = (120 * metrics.density).toInt()
        
        val textView = TextView(this).apply {
            this.text = "🫧\n$text"
            id = View.generateViewId()
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            textSize = 14f
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#FF4081"))
                setStroke(4, Color.WHITE)
            }
            isClickable = true
        }

        val lp = FrameLayout.LayoutParams(bubbleSize, bubbleSize)
        textView.layoutParams = lp

        val newBubble = BubbleInstance(
            missionId = missionId,
            view = textView,
            posX = (Math.random() * (realSize.x - bubbleSize)).toFloat(),
            posY = (Math.random() * (realSize.y - bubbleSize)).toFloat(),
            speedX = (if (Math.random() > 0.5) 1 else -1) * (10..15).random().toFloat(),
            speedY = (if (Math.random() > 0.5) 1 else -1) * (10..15).random().toFloat()
        )

        textView.setOnClickListener {
            blockingContainer.removeView(textView)
            activeBubbles.remove(newBubble)
            if (activeBubbles.isEmpty()) stopSelf()
        }

        blockingContainer.addView(textView)
        activeBubbles.add(newBubble)
    }

    private fun startGlobalAnimationLoop() {
        if (isAnimationRunning) return
        isAnimationRunning = true
        
        val realSize = Point()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            realSize.x = bounds.width()
            realSize.y = bounds.height()
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealSize(realSize)
        }

        val bubbleSize = (120 * resources.displayMetrics.density).toInt()

        val runnable = object : Runnable {
            override fun run() {
                if (activeBubbles.isEmpty()) {
                    isAnimationRunning = false
                    return
                }

                activeBubbles.forEach { bubble ->
                    bubble.posX += bubble.speedX
                    bubble.posY += bubble.speedY

                    // Rebote usando el TAMAÑO REAL de la pantalla (incluyendo barras)
                    if (bubble.posX <= 0 || bubble.posX >= (realSize.x - bubbleSize)) {
                        bubble.speedX *= -1f
                        bubble.posX = bubble.posX.coerceIn(0f, (realSize.x - bubbleSize).toFloat())
                    }
                    if (bubble.posY <= 0 || bubble.posY >= (realSize.y - bubbleSize)) {
                        bubble.speedY *= -1f
                        bubble.posY = bubble.posY.coerceIn(0f, (realSize.y - bubbleSize).toFloat())
                    }

                    bubble.view.x = bubble.posX
                    bubble.view.y = bubble.posY
                }

                animationHandler.postDelayed(this, 16)
            }
        }
        animationHandler.post(runnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        animationHandler.removeCallbacksAndMessages(null)
        try {
            windowManager.removeView(blockingContainer)
        } catch (e: Exception) {}
        activeBubbles.clear()
        isAnimationRunning = false
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Servicio de Bloqueo", NotificationManager.IMPORTANCE_HIGH)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
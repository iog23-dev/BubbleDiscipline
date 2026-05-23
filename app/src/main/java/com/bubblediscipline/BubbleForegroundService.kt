package com.bubblediscipline

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.util.Log
import android.view.*
import android.widget.*
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first

class BlockingView(context: Context, private val onEmergencyExit: () -> Unit) : FrameLayout(context) {
    private var volumeDownCount = 0
    private var lastClickTime = 0L

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && event.action == KeyEvent.ACTION_DOWN) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime < 2000) volumeDownCount++
            else volumeDownCount = 1
            lastClickTime = currentTime
            if (volumeDownCount >= 3) onEmergencyExit()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        val lp = layoutParams as? WindowManager.LayoutParams
        if (!hasWindowFocus && lp != null && (lp.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE) == 0) {
            requestFocus()
        }
    }
}

data class BubbleInstance(
    val missionId: Int,
    val historyId: Int,
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
    
    private lateinit var settingsManager: SettingsManager
    private var currentBubbleColor = SettingsManager.DEFAULT_COLOR
    private var currentBubbleSpeed = SettingsManager.DEFAULT_SPEED

    private lateinit var historyDao: HistoryDao
    private val ocrManager = OCRManager()

    private var mediaProjection: MediaProjection? = null
    private var verifyButton: Button? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        settingsManager = SettingsManager(this)
        historyDao = AppDatabase.getDatabase(this).historyDao()
        
        createNotificationChannel()
        setupBlockingUI()
        observeDatabase()
        observeSettings()
    }

    private fun setupBlockingUI() {
        blockingContainer = BlockingView(this) { handleEmergencyExit() }.apply {
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        try {
            windowManager.addView(blockingContainer, lp)
            blockingContainer.requestFocus()
        } catch (e: Exception) {
            stopSelf()
        }
    }

    private fun handleEmergencyExit() {
        serviceScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            activeBubbles.forEach { historyDao.markCompleted(it.historyId, now, true) }

            val contact = settingsManager.whatsappContactFlow.first()
            val shouldSendPanic = settingsManager.sendPanicMessageFlow.first()
            
            if (shouldSendPanic && contact.isNotBlank()) {
                withContext(Dispatchers.Main) { showOcrVerificationMode(contact) }
            } else {
                withContext(Dispatchers.Main) { stopSelf() }
            }
        }
    }

    private fun showOcrVerificationMode(contact: String) {
        val message = "He fracasado con algunas de las tareas, soy un vago."
        val url = "https://api.whatsapp.com/send?phone=$contact&text=${android.net.Uri.encode(message)}"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = android.net.Uri.parse(url)
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try { startActivity(intent) } catch (e: Exception) {}

        val lp = blockingContainer.layoutParams as WindowManager.LayoutParams
        lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        blockingContainer.setBackgroundColor(Color.TRANSPARENT)
        windowManager.updateViewLayout(blockingContainer, lp)

        if (verifyButton == null) {
            verifyButton = Button(this).apply {
                text = "VERIFICAR ENVÍO"
                setBackgroundColor(Color.RED)
                setTextColor(Color.WHITE)
                setOnClickListener { requestScreenCapture() }
            }
            
            val btnLp = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                y = 150
            }
            
            try {
                windowManager.addView(verifyButton, btnLp)
            } catch (e: Exception) {
                Log.e("BubbleService", "Error al añadir botón verificar", e)
            }
        }
    }

    private fun requestScreenCapture() {
        val intent = Intent(this, ScreenCaptureActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Aseguramos que la notificación FGS esté siempre activa con los tipos correctos
        startForegroundNotification()

        if (intent?.action == "ACTION_START_CAPTURE") {
            val resultData: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra("RESULT_DATA", Intent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra("RESULT_DATA")
            }
            if (resultData != null) startOcrProcess(resultData)
            return START_STICKY
        }

        val bubbleText = intent?.getStringExtra("BUBBLE_TEXT") ?: "¡Disciplina!"
        val missionId = intent?.getIntExtra("MISSION_ID", -1) ?: -1
        
        serviceScope.launch {
            currentBubbleColor = settingsManager.bubbleColorFlow.first()
            currentBubbleSpeed = settingsManager.bubbleSpeedFlow.first()
            
            val historyId = withContext(Dispatchers.IO) {
                historyDao.insertHistory(MissionHistory(missionId = missionId, missionMessage = bubbleText, activationTime = System.currentTimeMillis())).toInt()
            }
            createNewBubble(missionId, historyId, bubbleText)
            if (!isAnimationRunning) startGlobalAnimationLoop()
        }
        return START_STICKY
    }

    private fun startOcrProcess(resultData: Intent) {
        val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(Activity.RESULT_OK, resultData)
        
        // REQUISITO ANDROID 14: Registrar callback antes de crear VirtualDisplay
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    super.onStop()
                    Log.d("BubbleService", "MediaProjection detenido")
                }
            }, Handler(Looper.getMainLooper()))
        }

        val metrics = resources.displayMetrics
        val imageReader = ImageReader.newInstance(metrics.widthPixels, metrics.heightPixels, PixelFormat.RGBA_8888, 2)
        
        mediaProjection?.createVirtualDisplay(
            "ScreenCapture", metrics.widthPixels, metrics.heightPixels, metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, imageReader.surface, null, null
        )

        Handler(Looper.getMainLooper()).postDelayed({
            val image = imageReader.acquireLatestImage()
            if (image != null) {
                val planes = image.planes
                val buffer = planes[0].buffer
                val pixelStride = planes[0].pixelStride
                val rowStride = planes[0].rowStride
                val rowPadding = rowStride - pixelStride * metrics.widthPixels

                val bitmap = Bitmap.createBitmap(metrics.widthPixels + rowPadding / pixelStride, metrics.heightPixels, Bitmap.Config.ARGB_8888)
                bitmap.copyPixelsFromBuffer(buffer)
                image.close()
                imageReader.close()
                mediaProjection?.stop()

                serviceScope.launch {
                    val success = ocrManager.verifyPunishmentMessage(bitmap)
                    if (success) {
                        Toast.makeText(this@BubbleForegroundService, "¡Verificado! Desbloqueando...", Toast.LENGTH_LONG).show()
                        stopSelf()
                    } else {
                        Toast.makeText(this@BubbleForegroundService, "No veo el mensaje enviado. Envíalo primero.", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Toast.makeText(this, "Error de captura. Inténtalo de nuevo.", Toast.LENGTH_SHORT).show()
            }
        }, 500)
    }

    private fun startForegroundNotification() {
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BubbleDiscipline ACTIVO")
            .setContentText("Pulsa 3 veces 'Volumen Abajo' para emergencia")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID, 
                notification, 
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNewBubble(missionId: Int, historyId: Int, text: String) {
        if (activeBubbles.any { it.missionId == missionId && missionId != -1 }) return
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
                setColor(Color.parseColor(currentBubbleColor))
                setStroke(4, Color.WHITE)
            }
            isClickable = true
        }

        val newBubble = BubbleInstance(
            missionId = missionId, historyId = historyId, view = textView,
            posX = (Math.random() * (metrics.widthPixels - bubbleSize)).toFloat(),
            posY = (Math.random() * (metrics.heightPixels - bubbleSize)).toFloat(),
            speedX = (if (Math.random() > 0.5) 1 else -1) * currentBubbleSpeed,
            speedY = (if (Math.random() > 0.5) 1 else -1) * currentBubbleSpeed
        )

        textView.setOnClickListener {
            val lp = blockingContainer.layoutParams as WindowManager.LayoutParams
            if ((lp.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE) == 0) {
                removeBubbleInstance(newBubble, true)
            }
        }
        blockingContainer.addView(textView, FrameLayout.LayoutParams(bubbleSize, bubbleSize))
        activeBubbles.add(newBubble)
    }

    private fun removeBubbleInstance(bubble: BubbleInstance, updateDb: Boolean) {
        if (updateDb) serviceScope.launch(Dispatchers.IO) { historyDao.markCompleted(bubble.historyId, System.currentTimeMillis(), false) }
        blockingContainer.removeView(bubble.view)
        activeBubbles.remove(bubble)
        if (activeBubbles.isEmpty()) stopSelf()
    }

    private fun startGlobalAnimationLoop() {
        if (isAnimationRunning) return
        isAnimationRunning = true
        val metrics = resources.displayMetrics
        val bubbleSize = (120 * metrics.density).toInt()
        val runnable = object : Runnable {
            override fun run() {
                if (activeBubbles.isEmpty()) { isAnimationRunning = false; return }
                activeBubbles.forEach { bubble ->
                    bubble.posX += bubble.speedX; bubble.posY += bubble.speedY
                    
                    val maxX = (metrics.widthPixels - bubbleSize).toFloat()
                    val maxY = (metrics.heightPixels - bubbleSize).toFloat()

                    // Rebote en eje X con corrección de posición para evitar vibración
                    if (bubble.posX <= 0) {
                        bubble.posX = 0f
                        bubble.speedX = Math.abs(bubble.speedX)
                    } else if (bubble.posX >= maxX) {
                        bubble.posX = maxX
                        bubble.speedX = -Math.abs(bubble.speedX)
                    }

                    // Rebote en eje Y con corrección de posición para evitar vibración
                    if (bubble.posY <= 0) {
                        bubble.posY = 0f
                        bubble.speedY = Math.abs(bubble.speedY)
                    } else if (bubble.posY >= maxY) {
                        bubble.posY = maxY
                        bubble.speedY = -Math.abs(bubble.speedY)
                    }

                    bubble.view.x = bubble.posX; bubble.view.y = bubble.posY
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
        if (verifyButton != null) { try { windowManager.removeView(verifyButton) } catch (e: Exception) {} }
        try { windowManager.removeView(blockingContainer) } catch (e: Exception) {}
        activeBubbles.clear()
        isAnimationRunning = false
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun observeDatabase() {
        val missionDao = AppDatabase.getDatabase(this).missionDao()
        serviceScope.launch {
            missionDao.getAllMissions().collectLatest { missions ->
                activeBubbles.forEach { bubble ->
                    val updatedMission = missions.find { it.id == bubble.missionId }
                    if (updatedMission != null && !updatedMission.isEnabled) removeBubbleInstance(bubble, false)
                }
            }
        }
    }

    private fun observeSettings() {
        serviceScope.launch {
            settingsManager.bubbleColorFlow.collectLatest { colorHex ->
                currentBubbleColor = colorHex
                val color = Color.parseColor(colorHex)
                activeBubbles.forEach { (it.view.background as? GradientDrawable)?.setColor(color) }
            }
        }
        serviceScope.launch {
            settingsManager.bubbleSpeedFlow.collectLatest { speed ->
                currentBubbleSpeed = speed
                activeBubbles.forEach { it.speedX = if (it.speedX > 0) speed else -speed; it.speedY = if (it.speedY > 0) speed else -speed }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Servicio de Bloqueo", NotificationManager.IMPORTANCE_HIGH)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        
        // Cuando cambia la orientación, reajustamos las burbujas para que no se queden fuera
        val metrics = resources.displayMetrics
        val bubbleSize = (120 * metrics.density).toInt()
        
        activeBubbles.forEach { bubble ->
            // Si la burbuja está fuera de los nuevos límites, la movemos dentro
            val maxX = (metrics.widthPixels - bubbleSize).toFloat()
            val maxY = (metrics.heightPixels - bubbleSize).toFloat()
            
            if (bubble.posX > maxX) bubble.posX = maxX
            if (bubble.posY > maxY) bubble.posY = maxY
            
            // Asegurar que no sea negativa
            if (bubble.posX < 0) bubble.posX = 0f
            if (bubble.posY < 0) bubble.posY = 0f
            
            bubble.view.x = bubble.posX
            bubble.view.y = bubble.posY
        }
    }
}
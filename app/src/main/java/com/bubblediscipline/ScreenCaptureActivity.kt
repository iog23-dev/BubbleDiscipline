package com.bubblediscipline

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle

class ScreenCaptureActivity : Activity() {
    private val REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            val serviceIntent = Intent(this, BubbleForegroundService::class.java).apply {
                action = "ACTION_START_CAPTURE"
                putExtra("RESULT_DATA", data)
            }
            startService(serviceIntent)
        }
        finish()
    }
}
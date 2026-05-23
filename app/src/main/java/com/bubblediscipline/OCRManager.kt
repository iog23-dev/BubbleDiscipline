package com.bubblediscipline

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

class OCRManager {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun verifyPunishmentMessage(bitmap: Bitmap): Boolean {
        val image = InputImage.fromBitmap(bitmap, 0)
        return try {
            val result = recognizer.process(image).await()
            val screenHeight = bitmap.height
            
            // Buscamos el texto, pero analizando su posición
            var foundSentMessage = false

            for (block in result.textBlocks) {
                val blockText = block.text.lowercase()
                if (blockText.contains("fracasado") && blockText.contains("algunas") && blockText.contains("vago")) {
                    
                    // Obtener la posición vertical del bloque de texto
                    val boundingBox = block.boundingBox
                    if (boundingBox != null) {
                        // Si el texto está en el 20% inferior de la pantalla, 
                        // probablemente es el cuadro de texto (aún no enviado).
                        // Los mensajes enviados suelen estar por encima de la barra de escritura.
                        val isAtBottom = boundingBox.bottom > (screenHeight * 0.8)
                        
                        if (!isAtBottom) {
                            foundSentMessage = true
                            break
                        }
                    }
                }
            }
            foundSentMessage
        } catch (e: Exception) {
            false
        }
    }
}
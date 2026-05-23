package com.bubblediscipline

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class DisciplineAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Log para depuración
        Log.d("BubbleAccessibility", "Evento detectado: ${AccessibilityEvent.eventTypeToString(event.eventType)}")

        val rootNode = rootInActiveWindow ?: return
        val targetMessage = "He fracasado con algunas de las tareas, soy un vago."

        // 1. Intentar encontrar el botón de enviar por múltiples métodos
        val nodes = rootNode.findAccessibilityNodeInfosByViewId("com.whatsapp:id/send")
        
        // También buscamos nodos que tengan descripción "Enviar" o similar (para diferentes idiomas)
        val allNodes = mutableListOf<AccessibilityNodeInfo>()
        allNodes.addAll(nodes)
        
        // Búsqueda recursiva de botones con descripción de "enviar"
        findSendButtons(rootNode, allNodes)

        for (node in allNodes) {
            if (node.isEnabled && node.isClickable) {
                // Verificar si el mensaje de castigo está presente en la pantalla antes de pulsar
                if (isMessageInScreen(rootNode, targetMessage)) {
                    Log.d("BubbleAccessibility", "¡Castigo detectado! Pulsando enviar...")
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    
                    // Notificar éxito
                    val broadcastIntent = Intent("com.bubblediscipline.PUNISHMENT_COMPLETED").apply {
                        setPackage(packageName) // Asegurar que llega a nuestra app
                    }
                    sendBroadcast(broadcastIntent)
                }
            }
        }
    }

    private fun isMessageInScreen(rootNode: AccessibilityNodeInfo, message: String): Boolean {
        val entryNodes = rootNode.findAccessibilityNodeInfosByViewId("com.whatsapp:id/entry")
        if (entryNodes.any { it.text?.toString()?.contains(message) == true }) return true
        
        // Búsqueda genérica por texto
        val textNodes = rootNode.findAccessibilityNodeInfosByText(message)
        return textNodes.isNotEmpty()
    }

    private fun findSendButtons(node: AccessibilityNodeInfo, list: MutableList<AccessibilityNodeInfo>) {
        if (node.className?.contains("Button") == true || node.isClickable) {
            val desc = node.contentDescription?.toString()?.lowercase()
            if (desc == "enviar" || desc == "send") {
                list.add(node)
            }
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { findSendButtons(it, list) }
        }
    }

    override fun onInterrupt() {}
}
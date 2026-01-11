package com.hydra.client

import android.app.Service
import android.content.Intent
import android.os.IBinder
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
    
    // TEMPORARY: Trust all certificates to bypass "Trust anchor not found"
    private val client = unsafeOkHttpClient()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "Ghost Service Started")
        performCheckIn()
        return START_STICKY
    }

    private fun performCheckIn() {
        val url = "https://10.0.2.2:8443/checkin/ANDROID-HEAD-01?platform=android"
        val body = "{}".toRequestBody("application/json".toMediaType())

        val request = Request.Builder().url(url).post(body).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Check-in failed: ${e.message}")
            }
            override fun onResponse(call: Call, response: Response) {
                Log.i(TAG, "Server Responded: ${response.code}")
                response.close()
            }
        })
    }

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

    override fun onBind(intent: Intent?): IBinder? = null
}
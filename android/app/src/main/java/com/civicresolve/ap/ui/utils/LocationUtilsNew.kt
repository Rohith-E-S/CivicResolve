package com.civicresolve.ap.ui.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class ReverseGeocodeResult(
    val city: String,
    val state: String,
    val landmark: String
)

suspend fun reverseGeocodeNominatim(lat: Double, lng: Double): ReverseGeocodeResult? = withContext(Dispatchers.IO) {
    try {
        val client = OkHttpClient()
        val req = Request.Builder()
            .url("https://nominatim.openstreetmap.org/reverse?format=json&lat=$lat&lon=$lng")
            .header("User-Agent", "com.civicresolve.ap/1.0")
            .build()
        val resp = client.newCall(req).execute()
        val body = resp.body?.string() ?: return@withContext null
        val json = JSONObject(body)
        val addr = json.optJSONObject("address") ?: return@withContext null
        val city = addr.optString("city").ifEmpty { addr.optString("town").ifEmpty { addr.optString("village").ifEmpty { addr.optString("municipality") } } }
        val state = addr.optString("state")
        val landmark = addr.optString("road").ifEmpty { addr.optString("suburb") }
        ReverseGeocodeResult(city, state, landmark)
    } catch (_: Exception) { null }
}

package com.civicresolve.ap.ui.utils

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

fun formatStencil(id: String): String = "CIV-${id.takeLast(6).uppercase()}"

fun formatShortId(id: String): String = "#${id.takeLast(8).uppercase()}"

fun formatDate(iso: String?): String {
    if (iso == null) return "-"
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val date = parser.parse(iso) ?: return iso.take(10)
        SimpleDateFormat("MMM dd, yyyy", Locale.US).format(date)
    } catch (_: Exception) {
        try {
            iso.substring(0, 10)
        } catch (_: Exception) { iso }
    }
}

fun formatDateTime(iso: String?): String {
    if (iso == null) return "-"
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val date = parser.parse(iso) ?: return iso
        SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.US).format(date)
    } catch (_: Exception) { iso }
}

fun formatTime(iso: String?): String {
    if (iso == null) return ""
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val date = parser.parse(iso) ?: return ""
        SimpleDateFormat("hh:mm a", Locale.US).format(date)
    } catch (_: Exception) { "" }
}

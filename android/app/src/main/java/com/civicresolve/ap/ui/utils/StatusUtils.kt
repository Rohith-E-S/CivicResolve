package com.civicresolve.ap.ui.utils

import androidx.compose.ui.graphics.Color
import com.civicresolve.ap.ui.theme.DeflockColors
import com.civicresolve.ap.ui.theme.StatusDotColors
import kotlin.math.*

fun getStatusBadge(status: String): StatusBadgeType {
    val s = status.lowercase()
    return when (s) {
        "resolved", "confirmed_resolved" -> StatusBadgeType.Resolved
        "in_progress", "in progress", "re_opened", "pending_verification", "disputed" -> StatusBadgeType.Progress
        else -> StatusBadgeType.New
    }
}

enum class StatusBadgeType { New, Progress, Resolved }

fun statusDotColor(status: String): Color {
    val s = status.lowercase()
    return when (s) {
        "new", "under_review" -> StatusDotColors.new
        "in_progress", "in progress", "re_opened" -> StatusDotColors.inProgress
        "pending_verification" -> StatusDotColors.pendingVerification
        "disputed" -> StatusDotColors.disputed
        "resolved", "confirmed_resolved" -> StatusDotColors.resolved
        else -> StatusDotColors.default
    }
}

fun deflockColor(status: String): Color {
    val s = status.lowercase()
    return when (s) {
        "new", "under_review" -> DeflockColors.new
        "in_progress", "in progress", "re_opened" -> DeflockColors.active
        "pending_verification" -> DeflockColors.pending
        "disputed" -> DeflockColors.disputed
        "resolved", "confirmed_resolved" -> DeflockColors.resolved
        else -> DeflockColors.default
    }
}

fun haversineDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val r = 6371000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
    return r * 2 * atan2(sqrt(a), sqrt(1 - a))
}

fun categoryDisplayName(category: String?): String =
    category?.replace("_", " ")?.uppercase() ?: "OTHER"

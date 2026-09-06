package com.civicresolve.ap.ui.theme

import androidx.compose.ui.graphics.Color

// ── Light ──
object LightCivicColors {
    val bg = Color(0xFFF6F3EB)
    val surface = Color(0xFFFFFFFF)
    val surfaceMuted = Color(0xFFF1EFE8)
    val border = Color(0xFFD8D2C2)
    val text = Color(0xFF0B0F1A)
    val textMuted = Color(0xFF6B7A90)
    val primary = Color(0xFF0B0F1A)
    val primaryHover = Color(0xFF1A56DB)
    val accent = Color(0xFF1A56DB)
    val accentStrong = Color(0xFFFF6B2B)
    val danger = Color(0xFFB91C1C)
    val success = Color(0xFF0E9F6E)
    val focus = Color(0xFF1A56DB)
    val grid = Color(0x0A0B0F1A) // 4% opacity
    val onPrimary = Color(0xFFFFFFFF)
    val cardShadow = Color(0x0A0B0F1A)
}

// ── Dark ──
object DarkCivicColors {
    val bg = Color(0xFF18181B)
    val surface = Color(0xFF27272A)
    val surfaceMuted = Color(0xFF3F3F46)
    val border = Color(0xFF3F3F46)
    val text = Color(0xFFFAFAFA)
    val textMuted = Color(0xFFA1A1AA)
    val primary = Color(0xFFFAFAFA)
    val primaryHover = Color(0xFFE4E4E7)
    val accent = Color(0xFF60A5FA)
    val accentStrong = Color(0xFFFF6B2B)
    val danger = Color(0xFFF87171)
    val success = Color(0xFF34D399)
    val focus = Color(0xFF60A5FA)
    val grid = Color(0x0FFAFAFA) // 6% opacity
    val onPrimary = Color(0xFF18181B)
    val cardShadow = Color(0x1A000000)
}

// Status dot (ComplaintCard)
object StatusDotColors {
    val new = Color(0xFFE53935)
    val inProgress = Color(0xFFFFB74D)
    val pendingVerification = Color(0xFFE67E22)
    val disputed = Color(0xFF9C27B0)
    val resolved = Color(0xFF0E9F6E)
    val default = Color(0xFF8B95A1)
}

// DeFlock / Map dots
object DeflockColors {
    val new = Color(0xFFFF453A)
    val active = Color(0xFFFF9F0A)
    val pending = Color(0xFFFFCC00)
    val disputed = Color(0xFFAF52DE)
    val resolved = Color(0xFF30D158)
    val default = Color(0xFF8E8E93)
}

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

package com.civicresolve.ap.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightScheme = lightColorScheme(
    primary = LightCivicColors.primary,
    onPrimary = LightCivicColors.onPrimary,
    primaryContainer = LightCivicColors.accent,
    onPrimaryContainer = Color.White,
    secondary = LightCivicColors.accentStrong,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE7D2),
    onSecondaryContainer = Color(0xFF4A2800),
    tertiary = LightCivicColors.success,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDCFCE7),
    onTertiaryContainer = Color(0xFF14532D),
    error = LightCivicColors.danger,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    background = LightCivicColors.bg,
    onBackground = LightCivicColors.text,
    surface = LightCivicColors.surface,
    onSurface = LightCivicColors.text,
    surfaceVariant = LightCivicColors.surfaceMuted,
    onSurfaceVariant = LightCivicColors.textMuted,
    outline = LightCivicColors.border,
    outlineVariant = Color(0xFFE0E3E5),
    surfaceContainer = LightCivicColors.surface,
    surfaceContainerHigh = LightCivicColors.surfaceMuted,
    surfaceContainerHighest = Color(0xFFE8E6DE),
)

private val DarkScheme = darkColorScheme(
    primary = DarkCivicColors.primary,
    onPrimary = DarkCivicColors.onPrimary,
    primaryContainer = DarkCivicColors.accent,
    onPrimaryContainer = Color.White,
    secondary = DarkCivicColors.accentStrong,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF693C00),
    onSecondaryContainer = Color(0xFFFFDCBE),
    tertiary = DarkCivicColors.success,
    onTertiary = Color(0xFF00391F),
    tertiaryContainer = Color(0xFF00522F),
    onTertiaryContainer = Color(0xFF8EF5B5),
    error = DarkCivicColors.danger,
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = DarkCivicColors.bg,
    onBackground = DarkCivicColors.text,
    surface = DarkCivicColors.surface,
    onSurface = DarkCivicColors.text,
    surfaceVariant = DarkCivicColors.surfaceMuted,
    onSurfaceVariant = DarkCivicColors.textMuted,
    outline = DarkCivicColors.border,
    outlineVariant = Color(0xFF414750),
    surfaceContainer = DarkCivicColors.surface,
    surfaceContainerHigh = DarkCivicColors.surfaceMuted,
    surfaceContainerHighest = Color(0xFF3F3F46),
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

fun themeModeToDark(isSystemDark: Boolean, mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
    ThemeMode.SYSTEM -> isSystemDark
}

@Composable
fun CivicResolveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val scheme: ColorScheme = if (darkTheme) DarkScheme else LightScheme
    MaterialTheme(
        colorScheme = scheme,
        typography = CivicTypography,
        shapes = CivicShapes,
        content = content
    )
}

@Composable
fun CivicResolveTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = themeModeToDark(systemDark, themeMode)
    CivicResolveTheme(darkTheme = useDark, content = content)
}

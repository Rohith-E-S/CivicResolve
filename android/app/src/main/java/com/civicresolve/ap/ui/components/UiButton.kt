package com.civicresolve.ap.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.civicresolve.ap.ui.theme.DarkCivicColors
import com.civicresolve.ap.ui.theme.LightCivicColors

enum class UiButtonVariant { Primary, Secondary, Ghost, Accent }

@Composable
fun UiButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: UiButtonVariant = UiButtonVariant.Primary,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    val isDark = MaterialTheme.colorScheme.background == DarkCivicColors.bg
    val colors = when (variant) {
        UiButtonVariant.Primary -> ButtonDefaults.buttonColors(
            containerColor = if (isDark) Color.White else LightCivicColors.primary,
            contentColor = if (isDark) Color(0xFF18181B) else Color.White,
            disabledContainerColor = MaterialTheme.colorScheme.outline,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
        UiButtonVariant.Accent -> ButtonDefaults.buttonColors(
            containerColor = LightCivicColors.accentStrong,
            contentColor = Color.White
        )
        UiButtonVariant.Secondary -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
        UiButtonVariant.Ghost -> ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    }
    val border = when (variant) {
        UiButtonVariant.Secondary, UiButtonVariant.Ghost -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
        else -> null
    }
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        border = border,
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(6.dp))
        }
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun UiButtonSmall(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: UiButtonVariant = UiButtonVariant.Secondary,
    enabled: Boolean = true
) {
    UiButton(text = text, onClick = onClick, modifier = modifier, variant = variant, enabled = enabled)
}

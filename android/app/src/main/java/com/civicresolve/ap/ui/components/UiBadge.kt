package com.civicresolve.ap.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.civicresolve.ap.ui.theme.MonoFontFamily
import com.civicresolve.ap.ui.utils.StatusBadgeType
import com.civicresolve.ap.ui.utils.getStatusBadge

@Composable
fun UiBadge(text: String, type: StatusBadgeType, modifier: Modifier = Modifier) {
    val (bg, fg, border) = when (type) {
        StatusBadgeType.New -> Triple(Color(0xFFDBEAFE), Color(0xFF1E3A8A), Color(0xFFBFDBFE))
        StatusBadgeType.Progress -> Triple(Color(0xFFFFF7ED), Color(0xFF9A3412), Color(0xFFFFEDD5))
        StatusBadgeType.Resolved -> Triple(Color(0xFFDCFCE7), Color(0xFF14532D), Color(0xFFBBF7D0))
    }
    val isDark = MaterialTheme.colorScheme.background.value.toLong() == 0xFF18181B.toLong()
    val (bgD, fgD, borderD) = when (type) {
        StatusBadgeType.New -> Triple(Color(0xFF1E293B), Color(0xFFBFDBFE), Color(0xFF334155))
        StatusBadgeType.Progress -> Triple(Color(0xFF431407), Color(0xFFFFEDD5), Color(0xFF7C2D12))
        StatusBadgeType.Resolved -> Triple(Color(0xFF052E16), Color(0xFFBBF7D0), Color(0xFF14532D))
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = if (isDark) bgD else bg,
        contentColor = if (isDark) fgD else fg,
        border = BorderStroke(1.dp, if (isDark) borderD else border)
    ) {
        Text(
            text = text.uppercase(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            fontFamily = MonoFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 10.sp,
            letterSpacing = 0.6.sp
        )
    }
}

@Composable
fun StatusBadge(status: String, modifier: Modifier = Modifier) {
    UiBadge(text = status.replace("_", " "), type = getStatusBadge(status), modifier = modifier)
}

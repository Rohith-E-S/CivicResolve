package com.civicresolve.ap.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun UiDot(color: Color, modifier: Modifier = Modifier, size: Dp = 10.dp, pulse: Boolean = false) {
    if (!pulse) {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(color)
                .border(2.dp, Color.White, CircleShape)
        )
    } else {
        val infinite = rememberInfiniteTransition(label = "dotPulse")
        val scale by infinite.animateFloat(
            initialValue = 1f,
            targetValue = 1.35f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ), label = "scale"
        )
        val alpha by infinite.animateFloat(
            initialValue = 0.85f,
            targetValue = 0.45f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse
            ), label = "alpha"
        )
        Box(
            modifier = modifier
                .size(size * scale)
                .clip(CircleShape)
                .background(color.copy(alpha = alpha))
                .border(2.dp, Color.White, CircleShape)
        ) {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

package com.example.complaintportal.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.togetherWith

// Spring Specs
val DefaultSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessLow
)

val SnappySpring = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMedium
)

// Bounce Click Modifier (Micro-interaction)
fun Modifier.bounceClick(
    scaleDown: Float = 0.95f,
    onClick: () -> Unit
) = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = DefaultSpring,
        label = "bounceClick"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}

// Shimmer Effect Modifier
fun Modifier.shimmerEffect(): Modifier = composed {
    var size by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "shimmer")
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width.toFloat(),
        targetValue = 2 * size.width.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFB8B5B5),
                Color(0xFF8F8B8B),
                Color(0xFFB8B5B5),
            ),
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
        )
    )
}

@Composable
fun MorphingStatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    androidx.compose.animation.AnimatedContent(
        targetState = status,
        transitionSpec = {
            (androidx.compose.animation.fadeIn(animationSpec = tween(300))
                .togetherWith(androidx.compose.animation.fadeOut(animationSpec = tween(300))))
                .using(androidx.compose.animation.SizeTransform { _, _ ->
                    spring(stiffness = Spring.StiffnessMediumLow)
                })
        },
        label = "statusMorph",
        modifier = modifier
    ) { currentStatus ->
        val (bgColor, textColor) = when (currentStatus.lowercase()) {
            "new" -> Color(0xFFFFEBEE) to Color(0xFFD32F2F)
            "under_review" -> Color(0xFFFFF3E0) to Color(0xFFE65100)
            "in_progress" -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
            "re_opened" -> Color(0xFFFFEBEE) to Color(0xFFD32F2F)
            "pending_verification" -> Color(0xFFFFF3E0) to Color(0xFFE65100)
            "disputed" -> Color(0xFFFFEBEE) to Color(0xFFB71C1C)
            "resolved", "confirmed_resolved" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
            else -> Color.LightGray to Color.DarkGray
        }

        val displayText = when (currentStatus.lowercase()) {
            "re_opened" -> "REOPENED"
            "pending_verification" -> "PENDING VERIFICATION"
            "confirmed_resolved" -> "CONFIRMED RESOLVED"
            else -> currentStatus.replace("_", " ").uppercase()
        }

        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .background(bgColor, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            androidx.compose.material3.Text(
                text = displayText,
                color = textColor,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

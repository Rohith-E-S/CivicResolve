package com.example.complaintportal.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// ── Colors ──────────────────────────────────────────────────────────────────
private val NavyDark   = Color(0xFF0F2244)
private val NavyMid    = Color(0xFF1A3A6E)
private val TealAccent = Color(0xFF7ECFC0)
private val TextLight  = Color(0xFFE8F0FA)
private val TextMuted  = Color(0xFFB8CFE8)
private val RingColor  = Color(0xFF7ECFC0).copy(alpha = 0.15f)

@Composable
fun SplashScreen(onFinished: () -> Unit = {}) {

    // ── Animation states ─────────────────────────────────────────────────────
    var startAnimation by remember { mutableStateOf(false) }

    // Ring pulses
    val ring1Scale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800, delayMillis = 100, easing = EaseOut),
        label = "ring1"
    )
    val ring1Alpha by animateFloatAsState(
        targetValue = if (startAnimation) 0f else 0.6f,
        animationSpec = tween(durationMillis = 800, delayMillis = 100, easing = EaseOut),
        label = "ring1Alpha"
    )
    val ring2Scale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 900, delayMillis = 200, easing = EaseOut),
        label = "ring2"
    )
    val ring2Alpha by animateFloatAsState(
        targetValue = if (startAnimation) 0f else 0.6f,
        animationSpec = tween(durationMillis = 900, delayMillis = 200, easing = EaseOut),
        label = "ring2Alpha"
    )
    val ring3Scale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, delayMillis = 300, easing = EaseOut),
        label = "ring3"
    )
    val ring3Alpha by animateFloatAsState(
        targetValue = if (startAnimation) 0f else 0.6f,
        animationSpec = tween(durationMillis = 1000, delayMillis = 300, easing = EaseOut),
        label = "ring3Alpha"
    )

    // Logo bounce-in
    val logoScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMedium,
        ),
        label = "logoScale"
    )
    val logoAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 400, delayMillis = 200),
        label = "logoAlpha"
    )
    val logoRotation by animateFloatAsState(
        targetValue = if (startAnimation) 0f else -20f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMedium,
        ),
        label = "logoRotation"
    )

    // Title fade-up
    val titleAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 400, delayMillis = 600),
        label = "titleAlpha"
    )
    val titleOffset by animateFloatAsState(
        targetValue = if (startAnimation) 0f else 10f,
        animationSpec = tween(durationMillis = 400, delayMillis = 600),
        label = "titleOffset"
    )

    // Tagline fade-up
    val taglineAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 400, delayMillis = 800),
        label = "taglineAlpha"
    )
    val taglineOffset by animateFloatAsState(
        targetValue = if (startAnimation) 0f else 8f,
        animationSpec = tween(durationMillis = 400, delayMillis = 800),
        label = "taglineOffset"
    )

    // Loader
    val loaderAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 300, delayMillis = 1000),
        label = "loaderAlpha"
    )
    val loaderProgress by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1500, delayMillis = 1000, easing = EaseInOut),
        label = "loaderProgress"
    )

    // Loader label cycles
    val loaderMessages = listOf("fetching issues...", "welcome!")
    var loaderMsgIndex by remember { mutableStateOf(0) }

    // ── Side effects ─────────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        startAnimation = true
        // Cycle loader text
        delay(1000L) // Wait for loader to start
        loaderMsgIndex = 0 // "fetching issues..."
        delay(1500L) // Wait for loader to complete
        loaderMsgIndex = 1 // "welcome!"
        
        delay(1000L) // Short pause before transition
        onFinished()
    }

    // ── UI ───────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyDark),
        contentAlignment = Alignment.Center
    ) {

        // Ripple rings drawn on Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f

            // Ring 1
            drawCircle(
                color  = RingColor.copy(alpha = ring1Alpha),
                radius = 60.dp.toPx() * ring1Scale,
                center = androidx.compose.ui.geometry.Offset(cx, cy),
                style  = Stroke(width = 1.5.dp.toPx())
            )
            // Ring 2
            drawCircle(
                color  = RingColor.copy(alpha = ring2Alpha),
                radius = 90.dp.toPx() * ring2Scale,
                center = androidx.compose.ui.geometry.Offset(cx, cy),
                style  = Stroke(width = 1.5.dp.toPx())
            )
            // Ring 3
            drawCircle(
                color  = RingColor.copy(alpha = ring3Alpha),
                radius = 125.dp.toPx() * ring3Scale,
                center = androidx.compose.ui.geometry.Offset(cx, cy),
                style  = Stroke(width = 1.5.dp.toPx())
            )
        }

        // Center content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {

            Spacer(modifier = Modifier.weight(1f))

            // Logo icon box
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(72.dp)
                    .graphicsLayer {
                        scaleX        = logoScale
                        scaleY        = logoScale
                        alpha         = logoAlpha
                        rotationZ     = logoRotation
                        shadowElevation = 0f
                    }
            ) {
                // Navy rounded square background
                Canvas(modifier = Modifier.size(72.dp)) {
                    drawRoundRect(
                        color       = NavyMid,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx())
                    )
                }
                // Shield + pin icon
                ShieldPinIcon(
                    modifier  = Modifier.size(40.dp),
                    ringColor = TealAccent,
                    pinColor  = TealAccent,
                    arcColor  = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // App title
            Text(
                text      = "CivicResolve",
                fontSize  = 22.sp,
                fontWeight= FontWeight.Medium,
                color     = TextLight,
                letterSpacing = 0.5.sp,
                modifier  = Modifier
                    .alpha(titleAlpha)
                    .offset(y = titleOffset.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Tagline
            Text(
                text      = "REPORT · TRACK · RESOLVE",
                fontSize  = 11.sp,
                fontWeight= FontWeight.Normal,
                color     = TealAccent,
                letterSpacing = 1.5.sp,
                modifier  = Modifier
                    .alpha(taglineAlpha)
                    .offset(y = taglineOffset.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Loader
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .alpha(loaderAlpha)
                    .padding(bottom = 60.dp)
            ) {
                // Progress track
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(3.dp)
                ) {
                    // Track background
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRoundRect(
                            color        = Color.White.copy(alpha = 0.1f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                        )
                        // Fill
                        drawRoundRect(
                            color        = TealAccent,
                            size         = androidx.compose.ui.geometry.Size(size.width * loaderProgress, size.height),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text      = loaderMessages[loaderMsgIndex],
                    fontSize  = 10.sp,
                    color     = TextMuted.copy(alpha = 0.6f),
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

// ── Shield + pin icon drawn with Canvas ──────────────────────────────────────
@Composable
private fun ShieldPinIcon(
    modifier  : Modifier = Modifier,
    ringColor : Color,
    pinColor  : Color,
    arcColor  : Color
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Circle (pin head)
        drawCircle(
            color  = ringColor,
            radius = w * 0.22f,
            center = androidx.compose.ui.geometry.Offset(w / 2f, h * 0.40f),
            style  = Stroke(width = 2.dp.toPx())
        )

        // Pin stem
        drawLine(
            color       = pinColor,
            start       = androidx.compose.ui.geometry.Offset(w / 2f, h * 0.62f),
            end         = androidx.compose.ui.geometry.Offset(w / 2f, h * 0.90f),
            strokeWidth = 2.dp.toPx(),
            cap         = androidx.compose.ui.graphics.StrokeCap.Round
        )

        // Arc above (signal wave)
        val arcPath = Path().apply {
            moveTo(w * 0.28f, h * 0.40f)
            quadraticBezierTo(w / 2f, h * 0.10f, w * 0.72f, h * 0.40f)
        }
        drawPath(
            path  = arcPath,
            color = arcColor,
            style = Stroke(
                width = 1.5.dp.toPx(),
                cap   = androidx.compose.ui.graphics.StrokeCap.Round
            )
        )

        // Inner dot
        drawCircle(
            color  = ringColor,
            radius = w * 0.08f,
            center = androidx.compose.ui.geometry.Offset(w / 2f, h * 0.40f)
        )
    }
}

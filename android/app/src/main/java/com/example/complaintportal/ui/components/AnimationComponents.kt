package com.example.complaintportal.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.*
import com.example.complaintportal.R

@Composable
fun SuccessAnimation(
    modifier: Modifier = Modifier,
    onAnimationFinished: () -> Unit = {}
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.confetti))
    val progress by animateLottieCompositionAsState(
        composition,
        iterations = 1,
        isPlaying = true
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.size(300.dp)
        )
    }

    LaunchedEffect(progress) {
        if (progress == 1f) {
            onAnimationFinished()
        }
    }
}

@Composable
fun LoadingAnimation(
    modifier: Modifier = Modifier
) {
    // Fallback to a nice themed indicator if specific loading JSON isn't available
    // For now we'll just use a CircularProgressIndicator with a nice background
    // Or we can try to find a generic loading animation if we have one.
    // Since we only have confetti.json, I'll stick to a high-end Compose animation.
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            strokeWidth = 4.dp,
            color = androidx.compose.material3.MaterialTheme.colorScheme.primary
        )
    }
}

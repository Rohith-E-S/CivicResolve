package com.civicresolve.ap.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object CivicRounded {
    val sm = 4.dp
    val md = 8.dp
    val lg = 16.dp
    val xl = 24.dp
    val full = 9999.dp
}

val CivicShapes = Shapes(
    extraSmall = RoundedCornerShape(CivicRounded.sm),
    small = RoundedCornerShape(CivicRounded.md),
    medium = RoundedCornerShape(CivicRounded.lg),
    large = RoundedCornerShape(CivicRounded.xl),
    extraLarge = RoundedCornerShape(CivicRounded.xl)
)

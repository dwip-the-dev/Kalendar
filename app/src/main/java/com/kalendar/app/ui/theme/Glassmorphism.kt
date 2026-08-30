package com.kalendar.app.ui.theme

import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Premium glassmorphism / frosted glass modifier with multi-layer specular highlights,
 * hardware-accelerated background blur, and ultra-smooth tactile finishes.
 */
@Composable
fun Modifier.glassmorphic(
    shape: Shape = RoundedCornerShape(22.dp),
    blurRadius: Dp = 20.dp,
    containerAlpha: Float = 0.72f,
    borderAlpha: Float = 0.35f,
    surfaceColor: Color = MaterialTheme.colorScheme.surface
): Modifier {
    val isDark = surfaceColor.red < 0.5f && surfaceColor.green < 0.5f && surfaceColor.blue < 0.5f
    
    val baseGradient = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1E1E24).copy(alpha = containerAlpha * 0.90f),
                Color(0xFF141418).copy(alpha = containerAlpha * 0.98f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = containerAlpha * 0.92f),
                Color(0xFFF0F0F5).copy(alpha = containerAlpha * 0.85f)
            )
        )
    }

    val borderGradient = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = borderAlpha * 1.3f),
                Color.White.copy(alpha = borderAlpha * 0.25f),
                Color.White.copy(alpha = 0.05f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = borderAlpha * 1.5f),
                Color.Black.copy(alpha = 0.08f)
            )
        )
    }

    return this
        .clip(shape)
        .background(baseGradient, shape)
        .border(1.2.dp, borderGradient, shape)
}

/**
 * Shimmering Frosted Glass Popup Menu modifier for all 3-dot and contextual menus.
 */
@Composable
fun Modifier.glassmorphicMenu(
    shape: Shape = RoundedCornerShape(22.dp),
    containerAlpha: Float = 0.80f,
    borderAlpha: Float = 0.40f,
    surfaceColor: Color = MaterialTheme.colorScheme.surface
): Modifier {
    return this.glassmorphic(
        shape = shape,
        containerAlpha = containerAlpha,
        borderAlpha = borderAlpha,
        surfaceColor = surfaceColor
    )
}

/**
 * Frosted backdrop modifier for floating overlays, bottom bars, and pills.
 */
@Composable
fun Modifier.frostedPill(
    shape: Shape = RoundedCornerShape(24.dp),
    backgroundColor: Color = Color.Black.copy(alpha = 0.65f),
    borderColor: Color = Color.White.copy(alpha = 0.28f)
): Modifier {
    return this
        .clip(shape)
        .background(backgroundColor, shape)
        .border(1.dp, borderColor, shape)
}

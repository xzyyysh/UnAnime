package com.unanimetv.android.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import com.unanimetv.android.core.designsystem.theme.BrandFont

@Composable
fun NotAnimeLoader(
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val transition = rememberInfiniteTransition(label = "shine")
    val progress by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing)
        ),
        label = "shine-progress"
    )
    val breathe by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shine-breathe"
    )
    var widthPx by remember { mutableIntStateOf(0) }
    val base = MaterialTheme.colorScheme.onSurfaceVariant
    val highlight = MaterialTheme.colorScheme.primary
    val brush = if (widthPx > 0) {
        val x = widthPx * progress
        Brush.linearGradient(
            colors = listOf(base, highlight, base),
            start = Offset(x - widthPx, 0f),
            end = Offset(x, 0f)
        )
    } else {
        null
    }
    val style = if (compact) {
        MaterialTheme.typography.titleLarge
    } else {
        MaterialTheme.typography.headlineMedium
    }
    val finalStyle = if (brush != null) {
        style.copy(brush = brush)
    } else {
        style.copy(color = base)
    }
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "NotAnime",
            style = finalStyle,
            fontFamily = BrandFont,
            fontWeight = FontWeight.Bold,
            color = if (brush != null) Color.Unspecified else base,
            modifier = Modifier
                .graphicsLayer { alpha = breathe }
                .onSizeChanged { widthPx = it.width }
        )
    }
}

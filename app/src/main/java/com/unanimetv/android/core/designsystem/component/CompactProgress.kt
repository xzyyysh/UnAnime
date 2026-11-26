package com.unanimetv.android.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun CompactProgress(
    fraction: Float,
    modifier: Modifier = Modifier
) {
    val clamped = fraction.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(clamped)
                .height(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

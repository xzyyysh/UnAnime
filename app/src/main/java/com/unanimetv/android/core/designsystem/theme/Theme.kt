package com.unanimetv.android.core.designsystem.theme

import android.app.Activity
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.lerp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.launch

internal val LightColorScheme = lightColorScheme(
    primary = EspressoPrimaryLight,
    onPrimary = EspressoOnPrimaryLight,
    primaryContainer = EspressoPrimaryContainerLight,
    onPrimaryContainer = EspressoOnPrimaryContainerLight,
    secondary = EspressoSecondaryLight,
    onSecondary = EspressoOnSecondaryLight,
    secondaryContainer = EspressoSecondaryContainerLight,
    onSecondaryContainer = EspressoOnSecondaryContainerLight,
    tertiary = EspressoTertiaryLight,
    onTertiary = EspressoOnTertiaryLight,
    tertiaryContainer = EspressoTertiaryContainerLight,
    onTertiaryContainer = EspressoOnTertiaryContainerLight,
    background = EspressoBackgroundLight,
    onBackground = EspressoOnBackgroundLight,
    surface = EspressoSurfaceLight,
    onSurface = EspressoOnSurfaceLight,
    surfaceVariant = EspressoSurfaceVariantLight,
    onSurfaceVariant = EspressoOnSurfaceVariantLight,
    surfaceContainer = EspressoSurfaceContainerLight,
    surfaceContainerLow = Color(0xFFF5EDE3),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerHigh = EspressoSurfaceContainerHighLight,
    surfaceContainerHighest = Color(0xFFE6DACC),
    outline = EspressoOutlineLight,
    outlineVariant = EspressoOutlineVariantLight
)

internal val DarkColorScheme = darkColorScheme(
    primary = EspressoPrimaryDark,
    onPrimary = EspressoOnPrimaryDark,
    primaryContainer = EspressoPrimaryContainerDark,
    onPrimaryContainer = EspressoOnPrimaryContainerDark,
    secondary = EspressoSecondaryDark,
    onSecondary = EspressoOnSecondaryDark,
    secondaryContainer = EspressoSecondaryContainerDark,
    onSecondaryContainer = EspressoOnSecondaryContainerDark,
    tertiary = EspressoTertiaryDark,
    onTertiary = EspressoOnTertiaryDark,
    tertiaryContainer = EspressoTertiaryContainerDark,
    onTertiaryContainer = EspressoOnTertiaryContainerDark,
    background = EspressoBackgroundDark,
    onBackground = EspressoOnBackgroundDark,
    surface = EspressoSurfaceDark,
    onSurface = EspressoOnSurfaceDark,
    surfaceVariant = EspressoSurfaceVariantDark,
    onSurfaceVariant = EspressoOnSurfaceVariantDark,
    surfaceContainer = EspressoSurfaceContainerDark,
    surfaceContainerLow = Color(0xFF1C1815),
    surfaceContainerLowest = Color(0xFF12100D),
    surfaceContainerHigh = EspressoSurfaceContainerHighDark,
    surfaceContainerHighest = Color(0xFF37302A),
    outline = EspressoOutlineDark,
    outlineVariant = EspressoOutlineVariantDark
)

private fun ColorScheme.amoled() = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainer = Color(0xFF0D0D0D),
    surfaceContainerLow = Color(0xFF0A0A0A),
    surfaceContainerLowest = Color.Black,
    surfaceContainerHigh = Color(0xFF141414),
    surfaceContainerHighest = Color(0xFF191412),
    surfaceVariant = Color(0xFF1A1A1A)
)

private fun lerpScheme(from: ColorScheme, to: ColorScheme, fraction: Float): ColorScheme = from.copy(
    primary = lerp(from.primary, to.primary, fraction),
    onPrimary = lerp(from.onPrimary, to.onPrimary, fraction),
    primaryContainer = lerp(from.primaryContainer, to.primaryContainer, fraction),
    onPrimaryContainer = lerp(from.onPrimaryContainer, to.onPrimaryContainer, fraction),
    secondary = lerp(from.secondary, to.secondary, fraction),
    onSecondary = lerp(from.onSecondary, to.onSecondary, fraction),
    secondaryContainer = lerp(from.secondaryContainer, to.secondaryContainer, fraction),
    onSecondaryContainer = lerp(from.onSecondaryContainer, to.onSecondaryContainer, fraction),
    tertiary = lerp(from.tertiary, to.tertiary, fraction),
    onTertiary = lerp(from.onTertiary, to.onTertiary, fraction),
    tertiaryContainer = lerp(from.tertiaryContainer, to.tertiaryContainer, fraction),
    onTertiaryContainer = lerp(from.onTertiaryContainer, to.onTertiaryContainer, fraction),
    background = lerp(from.background, to.background, fraction),
    onBackground = lerp(from.onBackground, to.onBackground, fraction),
    surface = lerp(from.surface, to.surface, fraction),
    onSurface = lerp(from.onSurface, to.onSurface, fraction),
    surfaceVariant = lerp(from.surfaceVariant, to.surfaceVariant, fraction),
    onSurfaceVariant = lerp(from.onSurfaceVariant, to.onSurfaceVariant, fraction),
    surfaceContainer = lerp(from.surfaceContainer, to.surfaceContainer, fraction),
    surfaceContainerHigh = lerp(from.surfaceContainerHigh, to.surfaceContainerHigh, fraction),
    surfaceContainerHighest = lerp(from.surfaceContainerHighest, to.surfaceContainerHighest, fraction),
    surfaceContainerLow = lerp(from.surfaceContainerLow, to.surfaceContainerLow, fraction),
    surfaceContainerLowest = lerp(from.surfaceContainerLowest, to.surfaceContainerLowest, fraction),
    outline = lerp(from.outline, to.outline, fraction),
    outlineVariant = lerp(from.outlineVariant, to.outlineVariant, fraction),
    error = lerp(from.error, to.error, fraction),
    onError = lerp(from.onError, to.onError, fraction)
)

@Composable
fun UnAnimeTheme(
    settings: ThemeSettings,
    content: @Composable () -> Unit
) {
    val darkTheme = when (settings.mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val target = remember(settings, darkTheme) {
        when {
            settings.materialYou && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            settings.style == ThemeStyle.AMOLED && darkTheme -> DarkColorScheme.amoled()
            else -> styleScheme(settings.style, darkTheme)
        }
    }
    var current by remember { mutableStateOf(target) }
    val fraction = remember { Animatable(1f) }
    LocalConfiguration.current
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    LaunchedEffect(target) {
        val from = current
        if (from == target) return@LaunchedEffect
        fraction.snapTo(0f)
        kotlinx.coroutines.coroutineScope {
            this.launch { fraction.animateTo(1f, tween(durationMillis = 450)) }
            androidx.compose.runtime.snapshotFlow { fraction.value }
                .collect { current = lerpScheme(from, target, it) }
        }
        current = target
    }
    MaterialTheme(
        colorScheme = current,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}

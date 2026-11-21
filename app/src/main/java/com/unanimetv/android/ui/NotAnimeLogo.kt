package com.unanimetv.android.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.unanimetv.android.R
import com.unanimetv.android.core.designsystem.theme.BrandFont

private val I_VARIANTS = listOf("i", "í", "1", "!", "|", "ï", "î")

private const val VARIANT_INTERVAL_MS = 1800L

@Composable
fun rememberNotAnimeLogoVariant(): androidx.compose.runtime.MutableIntState {
    val state = remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(VARIANT_INTERVAL_MS)
            state.intValue = (state.intValue + 1) % I_VARIANTS.size
        }
    }
    return state
}

@Composable
fun BrandMark(modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = androidx.compose.ui.unit.Dp.Unspecified) {
    Icon(
        painter = androidx.compose.ui.res.painterResource(R.drawable.ic_brand_mark),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = if (size == androidx.compose.ui.unit.Dp.Unspecified) modifier else modifier.then(Modifier.size(size))
    )
}

@Composable
fun NotAnimeLogo(
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.headlineMedium,
    variantState: androidx.compose.runtime.MutableIntState? = null
) {
    val sharedState = variantState ?: remember { mutableIntStateOf(0) }
    LaunchedEffect(variantState == null) {
        if (variantState == null) {
            while (true) {
                kotlinx.coroutines.delay(VARIANT_INTERVAL_MS)
                sharedState.intValue = (sharedState.intValue + 1) % I_VARIANTS.size
            }
        }
    }
    val variantIndex = sharedState.intValue
    Row(modifier = modifier) {
        Text(
            text = "NotAn",
            style = style,
            fontFamily = BrandFont,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.alignByBaseline()
        )
        AnimatedContent(
            targetState = I_VARIANTS[variantIndex],
            transitionSpec = {
                (fadeIn(tween(250)) + scaleIn(initialScale = 0.4f, animationSpec = tween(250)))
                    .togetherWith(fadeOut(tween(250)) + scaleOut(targetScale = 1.6f, animationSpec = tween(250)))
            },
            label = "i-variant"
        ) { char ->
            Text(
                text = char,
                style = style,
                fontFamily = BrandFont,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.alignByBaseline()
            )
        }
        Text(
            text = "me",
            style = style,
            fontFamily = BrandFont,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.alignByBaseline()
        )
    }
}

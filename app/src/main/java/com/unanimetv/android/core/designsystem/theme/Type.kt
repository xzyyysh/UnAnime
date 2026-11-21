package com.unanimetv.android.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

val AppTypography = Typography().withDefaultFontFamily(FontFamily.SansSerif)

val BrandFont: FontFamily = FontFamily.Monospace

private fun Typography.withDefaultFontFamily(family: FontFamily) = Typography(
    displayLarge = displayLarge.copy(fontFamily = family),
    displayMedium = displayMedium.copy(fontFamily = family),
    displaySmall = displaySmall.copy(fontFamily = family),
    headlineLarge = headlineLarge.copy(fontFamily = family),
    headlineMedium = headlineMedium.copy(fontFamily = family),
    headlineSmall = headlineSmall.copy(fontFamily = family),
    titleLarge = titleLarge.copy(fontFamily = family, fontWeight = FontWeight.SemiBold),
    titleMedium = titleMedium.copy(fontFamily = family, fontWeight = FontWeight.SemiBold),
    titleSmall = titleSmall.copy(fontFamily = family, fontWeight = FontWeight.SemiBold),
    bodyLarge = bodyLarge.copy(fontFamily = family),
    bodyMedium = bodyMedium.copy(fontFamily = family),
    bodySmall = bodySmall.copy(fontFamily = family),
    labelLarge = labelLarge.copy(fontFamily = family, fontWeight = FontWeight.Medium),
    labelMedium = labelMedium.copy(fontFamily = family, fontWeight = FontWeight.Medium),
    labelSmall = labelSmall.copy(fontFamily = family, fontWeight = FontWeight.Medium)
)

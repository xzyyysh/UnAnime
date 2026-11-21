package com.unanimetv.android.core.navigation

import androidx.annotation.DrawableRes
import com.unanimetv.android.R

enum class TopLevelDestination(
    val route: String,
    val label: String,
    @DrawableRes val iconRes: Int
) {
    HOME("home", "Inicio", R.drawable.ic_tabler_home),
    SETTINGS("settings", "Ajustes", R.drawable.ic_tabler_settings)
}

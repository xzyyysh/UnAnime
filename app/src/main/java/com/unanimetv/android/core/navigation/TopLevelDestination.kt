package com.unanimetv.android.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    HOME("home", "Inicio", Icons.Rounded.Home),
    SETTINGS("settings", "Ajustes", Icons.Rounded.Settings)
}

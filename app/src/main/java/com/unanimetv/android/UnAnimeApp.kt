package com.unanimetv.android

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.unanimetv.android.core.designsystem.tokens.Spacing
import com.unanimetv.android.core.network.CloudflareGate
import com.unanimetv.android.R
import com.unanimetv.android.core.navigation.TopLevelDestination
import com.unanimetv.android.core.navigation.UnAnimeNavHost
import com.unanimetv.android.ui.CloudflareChallengeDialog

private val ShellShape = RoundedCornerShape(28.dp)

@Composable
fun UnAnimeApp() {
    val navController = rememberNavController()
    val challengeUrl by CloudflareGate.challengeUrl.collectAsState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isSubpage = currentRoute != null && (
        currentRoute.startsWith("catalog?") ||
            currentRoute.startsWith("title?") ||
            currentRoute.startsWith("episode?")
        )
    val activeRoute = if (isSubpage) TopLevelDestination.HOME.route else currentRoute
    val useRail = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 600
    val RailClearance = 112.dp
    val playerFullscreen by com.unanimetv.android.core.playback.PlayerFullscreen.active.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        UnAnimeNavHost(
            navController,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (useRail && !playerFullscreen) {
                        Modifier.padding(start = RailClearance)
                    } else {
                        Modifier
                    }
                )
        )
        if (useRail && !playerFullscreen) {
            FloatingRail(
                currentRoute = activeRoute,
                onSelect = { destination -> navigateTo(navController, destination.route) },
                modifier = Modifier.align(Alignment.CenterStart)
            )
        }
        if (!useRail && !playerFullscreen) {
            FloatingBar(
                    currentRoute = activeRoute,
                    onSelect = { destination -> navigateTo(navController, destination.route) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = Spacing.lg)
            )
        }
    }

    challengeUrl?.let { url ->
        CloudflareChallengeDialog(
            url = url,
            onDismiss = { CloudflareGate.dismiss() }
        )
    }
}

private fun navigateTo(navController: androidx.navigation.NavHostController, route: String) {
    val current = navController.currentBackStackEntry?.destination?.route
    if (current == route) return
    val isSubpage = current != null && (
        current.startsWith("catalog?") || current.startsWith("title?") || current.startsWith("episode?")
        )
    if (isSubpage && navController.popBackStack(route, false)) return
    navController.navigate(route) {
        popUpTo(navController.graph.startDestinationId) { inclusive = false }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun FloatingBar(
    currentRoute: String?,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = ShellShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 6.dp,
        shadowElevation = 12.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TopLevelDestination.entries.forEach { destination ->
                ShellItem(
                    destination = destination,
                    selected = currentRoute == destination.route,
                    onClick = { onSelect(destination) }
                )
            }
        }
    }
}

@Composable
private fun FloatingRail(
    currentRoute: String?,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(start = Spacing.lg, top = Spacing.xxl, bottom = Spacing.xxl),
        contentAlignment = Alignment.CenterStart
    ) {
        Surface(
            shape = ShellShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier.padding(vertical = Spacing.sm, horizontal = Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TopLevelDestination.entries.forEach { destination ->
                    ShellItemVertical(
                        destination = destination,
                        selected = currentRoute == destination.route,
                        onClick = { onSelect(destination) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ShellItem(
    destination: TopLevelDestination,
    selected: Boolean,
    onClick: () -> Unit
) {
    val pillColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            Color.Transparent
        },
        animationSpec = tween(durationMillis = 250),
        label = "shell-pill"
    )
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val horizontalPadding by animateDpAsState(
        targetValue = if (selected) Spacing.lg else Spacing.md,
        animationSpec = tween(durationMillis = 250),
        label = "shell-pad"
    )
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(pillColor)
            .clickable(onClick = onClick)
            .padding(
                horizontal = horizontalPadding,
                vertical = Spacing.sm
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Icon(
            painter = painterResource(destination.iconRes),
            contentDescription = destination.label,
            tint = contentColor
        )
        AnimatedVisibility(
            visible = selected,
            enter = expandHorizontally(tween(250)) + fadeIn(tween(250)),
            exit = shrinkHorizontally(tween(200)) + fadeOut(tween(150))
        ) {
            Text(
                text = destination.label,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor
            )
        }
    }
}

@Composable
private fun ShellItemVertical(
    destination: TopLevelDestination,
    selected: Boolean,
    onClick: () -> Unit
) {
    val pillColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            Color.Transparent
        },
        animationSpec = tween(durationMillis = 250),
        label = "shell-pill-vertical"
    )
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(pillColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(destination.iconRes),
            contentDescription = destination.label,
            tint = contentColor
        )
    }
}

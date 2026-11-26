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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.unanimetv.android.core.designsystem.tokens.Spacing
import com.unanimetv.android.core.network.CloudflareGate
import com.unanimetv.android.R
import com.unanimetv.android.core.navigation.FAVORITES_ROUTE
import com.unanimetv.android.core.navigation.HISTORY_ROUTE
import com.unanimetv.android.core.navigation.TopLevelDestination
import com.unanimetv.android.core.navigation.UnAnimeNavHost
import com.unanimetv.android.core.navigation.catalogRoute
import com.unanimetv.android.core.update.UpdateChecker
import com.unanimetv.android.ui.AppDrawerScaffold
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import com.unanimetv.android.ui.GateDialog
import com.unanimetv.android.ui.UpdateDialog

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
    val isDrawerLocked = currentRoute != null && (
        currentRoute.startsWith("title?") ||
            currentRoute.startsWith("episode?")
        )
    val activeRoute = if (isSubpage) TopLevelDestination.HOME.route else currentRoute
    val playerFullscreen by com.unanimetv.android.core.playback.PlayerFullscreen.active.collectAsState()
    val context = LocalContext.current
    val pendingUpdate by UpdateChecker.update.collectAsState()
    val updateDownload by UpdateChecker.download.collectAsState()
    val openFromNotification by UpdateChecker.openFromNotification.collectAsState()

    LaunchedEffect(openFromNotification) {
        if (!openFromNotification) return@LaunchedEffect
        delay(5_000)
        if (challengeUrl != null) {
            withTimeoutOrNull(120_000) {
                snapshotFlow { challengeUrl }.first { it == null }
            }
        }
        if (UpdateChecker.update.value == null) {
            UpdateChecker.checkNow(context)
        }
        UpdateChecker.consumeNotificationOpen()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AppDrawerScaffold(
            selectedPath = backStackEntry?.arguments?.getString("url").takeIf {
                currentRoute?.startsWith("catalog?") == true
            },
            onCatalogClick = { path, title -> navigateTo(navController, catalogRoute(path, title)) },
            onFavoritesClick = { navigateTo(navController, FAVORITES_ROUTE) },
            onHistoryClick = { navigateTo(navController, HISTORY_ROUTE) },
            drawerEnabled = !isDrawerLocked,
            modifier = Modifier.fillMaxSize()
        ) {
            UnAnimeNavHost(navController, modifier = Modifier.fillMaxSize())
        }
        if (!playerFullscreen) {
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
        GateDialog(
            url = url,
            onDismiss = { CloudflareGate.dismiss() }
        )
    }

    pendingUpdate?.let { pending ->
        UpdateDialog(
            currentVersion = com.unanimetv.android.core.BuildInfo.version,
            update = pending,
            download = updateDownload,
            onRememberLater = { UpdateChecker.rememberLater(context) },
            onDownload = { UpdateChecker.download(context) }
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


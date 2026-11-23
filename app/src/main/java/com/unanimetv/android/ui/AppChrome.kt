package com.unanimetv.android.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.unanimetv.android.R
import com.unanimetv.android.core.designsystem.tokens.Spacing
import com.unanimetv.android.feature.search.ui.SearchDialog
import kotlinx.coroutines.launch

@Composable
fun AppChrome(
    logoVariant: MutableIntState,
    collapseProgress: () -> Float,
    onTitleClick: (String) -> Unit,
    onCatalogClick: (String, String) -> Unit,
    onFavoritesClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    alwaysSolid: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val drawerState = androidx.compose.material3.rememberDrawerState(androidx.compose.material3.DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var searchOpen by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 600 ||
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isWideScreen) {
        Row(modifier = modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.4f)
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
            ) {
                DrawerContent(
                    logoVariant = logoVariant,
                    onCatalogClick = onCatalogClick,
                    onFavoritesClick = onFavoritesClick
                )
            }
            VerticalDivider()
            Box(modifier = Modifier.weight(1f)) {
                content()
                IconButton(
                    onClick = { searchOpen = true },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(horizontal = Spacing.xs, vertical = Spacing.xs)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_tabler_search),
                            contentDescription = "Buscar",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                AppDrawer(
                    logoVariant = logoVariant,
                    onCatalogClick = { path, title ->
                        scope.launch { drawerState.close() }
                        onCatalogClick(path, title)
                    },
                    onFavoritesClick = {
                        scope.launch { drawerState.close() }
                        onFavoritesClick()
                    }
                )
            }
        ) {
            Box(modifier = modifier.fillMaxSize()) {
                content()
                ChromeTopBar(
                    collapseProgress = collapseProgress,
                    alwaysSolid = alwaysSolid,
                    logoVariant = logoVariant,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onSearchClick = { searchOpen = true }
                )
            }
        }
    }
    if (searchOpen) {
        SearchDialog(
            onDismiss = { searchOpen = false },
            onResultClick = onTitleClick
        )
    }
}

@Composable
private fun ChromeTopBar(
    collapseProgress: () -> Float,
    alwaysSolid: Boolean,
    logoVariant: MutableIntState,
    showMenu: Boolean = true,
    showTitle: Boolean = true,
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit
) {
    val progress = collapseProgress()
    val visibility = if (alwaysSolid) 1f else progress
    val barColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f * visibility)
    val chipAlpha = if (alwaysSolid) 0f else 1f - progress
    val logoAlpha = if (alwaysSolid) 1f else progress
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to barColor,
                        0.6f to barColor,
                        1f to barColor.copy(alpha = 0f)
                    )
                )
            )
            .statusBarsPadding()
            .padding(horizontal = Spacing.xs, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showMenu) {
            IconButton(onClick = onMenuClick) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = chipAlpha)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_tabler_menu_2),
                        contentDescription = "Menú",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.graphicsLayer {
                            scaleX = 1f - 0.1f * progress
                            scaleY = 1f - 0.1f * progress
                        }
                    )
                }
            }
        }
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            if (showTitle) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Spacing.sm),
                    modifier = Modifier.graphicsLayer {
                        alpha = logoAlpha
                        scaleX = 0.8f + 0.2f * logoAlpha
                        scaleY = 0.8f + 0.2f * logoAlpha
                    }
                ) {
                    BrandMark(size = 26.dp)
                    NotAnimeLogo(
                        variantState = logoVariant,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }
        IconButton(onClick = onSearchClick) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = chipAlpha)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_tabler_search),
                    contentDescription = "Buscar",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.graphicsLayer {
                        scaleX = 1f - 0.1f * progress
                        scaleY = 1f - 0.1f * progress
                    }
                )
            }
        }
    }
}

@Composable
private fun AppDrawer(
    logoVariant: MutableIntState,
    onCatalogClick: (String, String) -> Unit,
    onFavoritesClick: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.background
    ) {
        DrawerContent(
            logoVariant = logoVariant,
            onCatalogClick = onCatalogClick,
            onFavoritesClick = onFavoritesClick
        )
    }
}

@Composable
private fun DrawerContent(
    logoVariant: MutableIntState,
    onCatalogClick: (String, String) -> Unit,
    onFavoritesClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(vertical = Spacing.lg)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Spacing.md)
            ) {
                BrandMark(size = 40.dp)
                NotAnimeLogo(variantState = logoVariant)
            }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))
        NavigationDrawerItem(
            icon = { Icon(painterResource(R.drawable.ic_tabler_star), contentDescription = null) },
            label = { Text("Favoritos") },
            selected = false,
            onClick = onFavoritesClick
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))
        DrawerSection("Catálogos")
        NavigationDrawerItem(
            icon = { Icon(painterResource(R.drawable.ic_tabler_player_play), contentDescription = null) },
            label = { Text("Últimos episodios") },
            selected = false,
            onClick = { onCatalogClick("episodio", "Últimos episodios") }
        )
        NavigationDrawerItem(
            icon = { Icon(painterResource(R.drawable.ic_tabler_device_tv), contentDescription = null) },
            label = { Text("Animes") },
            selected = false,
            onClick = { onCatalogClick("online", "Animes") }
        )
        NavigationDrawerItem(
            icon = { Icon(painterResource(R.drawable.ic_tabler_movie), contentDescription = null) },
            label = { Text("Películas") },
            selected = false,
            onClick = { onCatalogClick("pelicula", "Películas") }
        )
        NavigationDrawerItem(
            icon = { Icon(painterResource(R.drawable.ic_tabler_calendar), contentDescription = null) },
            label = { Text("Temporadas") },
            selected = false,
            onClick = { onCatalogClick("temporada", "Temporadas") }
        )
        DrawerSection("Géneros")
        NavigationDrawerItem(
            icon = { Icon(painterResource(R.drawable.ic_tabler_flame), contentDescription = null) },
            label = { Text("En emisión") },
            selected = false,
            onClick = { onCatalogClick("genero/en-emision-1", "En emisión") }
        )
        NavigationDrawerItem(
            icon = { Icon(painterResource(R.drawable.ic_tabler_ban), contentDescription = null) },
            label = { Text("Sin censura") },
            selected = false,
            onClick = { onCatalogClick("genero/sin-censura", "Sin censura") }
        )
        NavigationDrawerItem(
            icon = { Icon(painterResource(R.drawable.ic_tabler_world), contentDescription = null) },
            label = { Text("Live Action") },
            selected = false,
            onClick = { onCatalogClick("genero/live-action", "Live Action") }
        )
        NavigationDrawerItem(
            icon = { Icon(painterResource(R.drawable.ic_tabler_clock), contentDescription = null) },
            label = { Text("Próximamente") },
            selected = false,
            onClick = { onCatalogClick("genero/proximamente", "Próximamente") }
        )
        NavigationDrawerItem(
            icon = { Icon(painterResource(R.drawable.ic_tabler_disc), contentDescription = null) },
            label = { Text("Blu-Ray / DVD") },
            selected = false,
            onClick = { onCatalogClick("genero/blu-ray-dvd-2", "Blu-Ray / DVD") }
        )
        NavigationDrawerItem(
            icon = { Icon(painterResource(R.drawable.ic_tabler_trophy), contentDescription = null) },
            label = { Text("Ganadores de premios") },
            selected = false,
            onClick = { onCatalogClick("genero/award-winning-anime", "Ganadores de premios") }
        )
        DrawerSection("Destacados")
        NavigationDrawerItem(
            icon = { Icon(painterResource(R.drawable.ic_tabler_trending_up), contentDescription = null) },
            label = { Text("Popular en la web") },
            selected = false,
            onClick = { onCatalogClick("tendencias", "Popular en la web") }
        )
        NavigationDrawerItem(
            icon = { Icon(painterResource(R.drawable.ic_tabler_star), contentDescription = null) },
            label = { Text("Mejores valorados") },
            selected = false,
            onClick = { onCatalogClick("ratings", "Mejores valorados") }
        )
    }
}

@Composable
private fun DrawerSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)
    )
}

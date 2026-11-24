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
    selectedPath: String? = null,
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
                    selectedPath = selectedPath,
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
                    selectedPath = selectedPath,
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
    selectedPath: String?,
    onCatalogClick: (String, String) -> Unit,
    onFavoritesClick: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.background
    ) {
        DrawerContent(
            logoVariant = logoVariant,
            selectedPath = selectedPath,
            onCatalogClick = onCatalogClick,
            onFavoritesClick = onFavoritesClick
        )
    }
}

private data class DrawerEntry(val label: String, val icon: Int, val path: String)

private val CatalogEntries = listOf(
    DrawerEntry("Últimos episodios", R.drawable.ic_tabler_player_play, "episodio"),
    DrawerEntry("Animes", R.drawable.ic_tabler_device_tv, "online"),
    DrawerEntry("Películas", R.drawable.ic_tabler_movie, "pelicula"),
    DrawerEntry("Temporadas", R.drawable.ic_tabler_calendar, "temporada")
)

private val GenreEntries = listOf(
    DrawerEntry("En emisión", R.drawable.ic_tabler_flame, "genero/en-emision-1"),
    DrawerEntry("Sin censura", R.drawable.ic_tabler_ban, "genero/sin-censura"),
    DrawerEntry("Live Action", R.drawable.ic_tabler_world, "genero/live-action"),
    DrawerEntry("Próximamente", R.drawable.ic_tabler_clock, "genero/proximamente"),
    DrawerEntry("Blu-Ray / DVD", R.drawable.ic_tabler_disc, "genero/blu-ray-dvd-2"),
    DrawerEntry("Ganadores de premios", R.drawable.ic_tabler_trophy, "genero/award-winning-anime")
)

private val FeaturedEntries = listOf(
    DrawerEntry("Popular en la web", R.drawable.ic_tabler_trending_up, "tendencias"),
    DrawerEntry("Mejores valorados", R.drawable.ic_tabler_star, "ratings")
)

@Composable
private fun DrawerContent(
    logoVariant: MutableIntState,
    selectedPath: String?,
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
            selected = selectedPath == FavoritesPath,
            onClick = onFavoritesClick
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))
        DrawerSection("Catálogos")
        CatalogEntries.forEach { entry ->
            DrawerItem(entry = entry, selectedPath = selectedPath, onCatalogClick = onCatalogClick)
        }
        DrawerSection("Géneros")
        GenreEntries.forEach { entry ->
            DrawerItem(entry = entry, selectedPath = selectedPath, onCatalogClick = onCatalogClick)
        }
        DrawerSection("Destacados")
        FeaturedEntries.forEach { entry ->
            DrawerItem(entry = entry, selectedPath = selectedPath, onCatalogClick = onCatalogClick)
        }
    }
}

private const val FavoritesPath = "favoritos"

@Composable
private fun DrawerItem(
    entry: DrawerEntry,
    selectedPath: String?,
    onCatalogClick: (String, String) -> Unit
) {
    NavigationDrawerItem(
        icon = { Icon(painterResource(entry.icon), contentDescription = null) },
        label = { Text(entry.label) },
        selected = selectedPath == entry.path,
        onClick = { onCatalogClick(entry.path, entry.label) }
    )
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

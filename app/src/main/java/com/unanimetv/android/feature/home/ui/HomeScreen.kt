package com.unanimetv.android.feature.home.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.ContextCompat
import coil3.compose.AsyncImage
import com.notanime.es.util.UnAnimeLog
import com.unanimetv.android.R
import com.unanimetv.android.core.designsystem.component.EpisodeCard
import com.unanimetv.android.core.designsystem.component.EmptyState
import com.unanimetv.android.core.designsystem.component.LoadingState
import com.unanimetv.android.core.designsystem.component.PosterCard
import com.unanimetv.android.core.designsystem.component.SectionHeader
import com.unanimetv.android.core.designsystem.tokens.Spacing
import com.unanimetv.android.ui.AppChrome
import com.unanimetv.android.ui.BrandMark
import com.unanimetv.android.ui.NotAnimeLogo
import com.unanimetv.android.ui.rememberNotAnimeLogoVariant


@Composable
fun HomeScreen(
    onEpisodeClick: (String) -> Unit,
    onTitleClick: (String) -> Unit = {},
    onSeeAll: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val viewModel: HomeViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        granted.forEach { (permission, ok) ->
            UnAnimeLog.d("permissions", "$permission granted=$ok")
        }
    }
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("permissions_requested", false)) return@LaunchedEffect
        prefs.edit().putBoolean("permissions_requested", true).apply()
        val toRequest = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) add(Manifest.permission.POST_NOTIFICATIONS)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED
            ) add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (toRequest.isNotEmpty()) permissionLauncher.launch(toRequest.toTypedArray())
    }

    when {
        uiState.isLoading -> LoadingState(modifier = modifier)
        uiState.error != null -> EmptyState(
            icon = painterResource(R.drawable.ic_tabler_alert_circle),
            message = uiState.error.orEmpty(),
            modifier = modifier
        )
        else -> HomeContent(
            uiState = uiState,
            onEpisodeClick = onEpisodeClick,
            onTitleClick = onTitleClick,
            onSeeAll = onSeeAll,
            modifier = modifier
        )
    }
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    onEpisodeClick: (String) -> Unit,
    onTitleClick: (String) -> Unit,
    onSeeAll: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val meta = uiState.sectionMeta
    val logoVariant = rememberNotAnimeLogoVariant()
    val gridState = rememberLazyGridState()
    val collapsed by remember {
        derivedStateOf {
            gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 40
        }
    }
    val collapseProgress by animateFloatAsState(
        targetValue = if (collapsed) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "collapse"
    )

    AppChrome(
        logoVariant = logoVariant,
        collapseProgress = { collapseProgress },
        onTitleClick = onTitleClick,
        modifier = modifier
    ) {
        HomeGrid(
            uiState = uiState,
            meta = meta,
            gridState = gridState,
            collapseProgress = collapseProgress,
            logoVariant = logoVariant,
            onEpisodeClick = onEpisodeClick,
            onTitleClick = onTitleClick,
            onSeeAll = onSeeAll
        )
    }
}

@Composable
private fun HomeGrid(
    uiState: HomeUiState,
    meta: Map<String, com.notanime.es.scraper.SectionMeta>,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    collapseProgress: Float,
    logoVariant: androidx.compose.runtime.MutableIntState,
    onEpisodeClick: (String) -> Unit,
    onTitleClick: (String) -> Unit,
    onSeeAll: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = gridState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = LocalConfiguration.current.screenWidthDp.dp * 0.012f,
            end = LocalConfiguration.current.screenWidthDp.dp * 0.012f,
            bottom = 120.dp
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        if (!isLandscape) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = Spacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    BrandMark(
                        size = 30.dp,
                        modifier = Modifier.graphicsLayer { alpha = 1f - collapseProgress }
                    )
                    NotAnimeLogo(
                        variantState = logoVariant,
                        modifier = Modifier.graphicsLayer { alpha = 1f - collapseProgress }
                    )
                }
            }
        }
        if (uiState.banner.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                BannerCarousel(
                    banner = uiState.banner,
                    onTitleClick = onTitleClick
                )
            }
        }
        if (uiState.featured.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(
                    title = "En emisión",
                    modifier = Modifier.padding(horizontal = Spacing.lg)
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                FeaturedRow(
                    featured = uiState.featured,
                    onTitleClick = onTitleClick
                )
            }
        }
        if (uiState.latestEpisodes.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(
                    title = "Últimos episodios",
                    count = meta["episodes"]?.count,
                    actionText = meta["episodes"]?.seeAll?.let { "Ver todo" },
                    onActionClick = { meta["episodes"]?.seeAll?.let { onSeeAll(it, "Últimos episodios") } },
                    modifier = Modifier.padding(horizontal = Spacing.lg)
                )
            }
            items(uiState.latestEpisodes, key = { "ep-${it.id}" }) { episode ->
                EpisodeCard(
                    title = episode.title,
                    badges = episode.badges,
                    imageUrl = episode.image,
                    onClick = { onEpisodeClick(episode.url) }
                )
            }
        }
        if (uiState.popular.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(
                    title = "Últimos animes",
                    count = meta["animes"]?.count,
                    actionText = meta["animes"]?.seeAll?.let { "Ver todo" },
                    onActionClick = { meta["animes"]?.seeAll?.let { onSeeAll(it, "Últimos animes") } },
                    modifier = Modifier.padding(horizontal = Spacing.lg)
                )
            }
            items(uiState.popular, key = { "an-${it.id}" }) { anime ->
                PosterCard(
                    title = anime.title,
                    imageUrl = anime.image,
                    onClick = { onTitleClick(anime.url) }
                )
            }
        }
        if (uiState.movies.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(
                    title = "Películas",
                    count = meta["movies"]?.count,
                    actionText = meta["movies"]?.seeAll?.let { "Ver todo" },
                    onActionClick = { meta["movies"]?.seeAll?.let { onSeeAll(it, "Películas") } },
                    modifier = Modifier.padding(horizontal = Spacing.lg)
                )
            }
            items(uiState.movies, key = { "mv-${it.id}" }) { anime ->
                PosterCard(
                    title = anime.title,
                    imageUrl = anime.image,
                    onClick = { onTitleClick(anime.url) }
                )
            }
        }
        if (uiState.seasons.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(
                    title = "Temporadas",
                    count = meta["seasons"]?.count,
                    actionText = meta["seasons"]?.seeAll?.let { "Ver todo" },
                    onActionClick = { meta["seasons"]?.seeAll?.let { onSeeAll(it, "Temporadas") } },
                    modifier = Modifier.padding(horizontal = Spacing.lg)
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                PosterRow(
                    items = uiState.seasons,
                    onTitleClick = onTitleClick
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BannerCarousel(
    banner: List<AnimeSummary>,
    onTitleClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state = rememberCarouselState { banner.size }
    HorizontalMultiBrowseCarousel(
        state = state,
        preferredItemWidth = 280.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = Spacing.md)
            .height(200.dp),
        itemSpacing = Spacing.sm,
        contentPadding = PaddingValues(horizontal = Spacing.lg)
    ) { index ->
            val item = banner[index]
            Box(
                modifier = Modifier
                    .height(200.dp)
                    .fillMaxWidth()
                    .clickable { onTitleClick(item.url) }
                    .maskClip(MaterialTheme.shapes.large)
            ) {
                if (item.image.isNotEmpty()) {
                    AsyncImage(
                        model = item.image,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(72.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.75f)
                                )
                            )
                        )
                )
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(Spacing.md)
                )
            }
        }
}

@Composable
private fun FeaturedRow(
    featured: List<FeaturedAnime>,
    onTitleClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        items(featured, key = { it.url }) { item ->
            Box(
                modifier = Modifier
                    .size(width = 130.dp, height = 195.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable { onTitleClick(item.url) }
            ) {
                if (item.image.isNotEmpty()) {
                    AsyncImage(
                        model = item.image,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(Spacing.sm)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildString {
                            item.rating?.let { append("★ $it") }
                            item.year?.let {
                                if (isNotEmpty()) append("  ")
                                append(it)
                            }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PosterRow(
    items: List<AnimeSummary>,
    onTitleClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        items(items, key = { it.url }) { item ->
            Box(
                modifier = Modifier
                    .size(width = 120.dp, height = 180.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable { onTitleClick(item.url) }
            ) {
                if (item.image.isNotEmpty()) {
                    AsyncImage(
                        model = item.image,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                            )
                        )
                )
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(Spacing.sm)
                )
            }
        }
    }
}

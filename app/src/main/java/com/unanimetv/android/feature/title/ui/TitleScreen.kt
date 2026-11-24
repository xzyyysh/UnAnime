package com.unanimetv.android.feature.title.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale
import coil3.compose.AsyncImage
import com.notanime.es.scraper.TitleData
import com.unanimetv.android.R
import com.unanimetv.android.core.favorites.FavoriteAnime
import com.unanimetv.android.core.favorites.FavoritesStore
import com.unanimetv.android.core.designsystem.component.EmptyState
import com.unanimetv.android.core.designsystem.component.LoadingState
import com.unanimetv.android.core.designsystem.component.StatusBadge
import com.unanimetv.android.core.designsystem.tokens.Spacing
import com.unanimetv.android.ui.LinkMenu

@Composable
fun TitleScreen(
    url: String,
    onEpisodeClick: (String) -> Unit,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val viewModel: TitleViewModel = viewModel(key = url) { TitleViewModel(url) }
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(Unit) { FavoritesStore.init(context) }

    when {
        uiState.isLoading -> LoadingState(modifier = modifier)
        uiState.error != null -> EmptyState(
            icon = painterResource(R.drawable.ic_tabler_alert_circle),
            message = uiState.error.orEmpty(),
            modifier = modifier
        )
        else -> TitleContent(
            data = uiState.data,
            selectedSeason = uiState.selectedSeason,
            onSeasonSelect = viewModel::selectSeason,
            onBack = onBack,
            onEpisodeClick = onEpisodeClick,
            modifier = modifier
        )
    }
}

@Composable
private fun TitleContent(
    data: TitleData?,
    selectedSeason: Int?,
    onSeasonSelect: (Int) -> Unit,
    onBack: () -> Unit,
    onEpisodeClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (data == null) return
    val context = LocalContext.current
    val favorites by FavoritesStore.favorites.collectAsState()
    val isFavorite = favorites.any { it.url == data.url }
    val wide = LocalConfiguration.current.screenWidthDp >= 600
    val season = data.seasons.firstOrNull { it.season == selectedSeason } ?: data.seasons.firstOrNull()
    val activeSeason = season?.season ?: 0
    var previewArtwork by remember { mutableStateOf(false) }
    val infoScroll = rememberScrollState()
    val episodesScroll = rememberScrollState()
    val pageScroll = rememberScrollState()

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = Spacing.lg, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_tabler_arrow_back),
                    contentDescription = "Volver",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            LinkMenu(
                url = data.url,
                isFavorite = isFavorite,
                onToggleFavorite = {
                    if (isFavorite) {
                        FavoritesStore.remove(data.url)
                    } else {
                        FavoritesStore.add(
                            FavoriteAnime(
                                url = data.url,
                                title = data.title,
                                image = data.poster,
                                rating = data.rating.value?.let { String.format(Locale.US, "%.1f", it) }
                            )
                        )
                    }
                    Toast.makeText(
                        context,
                        if (isFavorite) "Quitado de favoritos" else "Agregado a favoritos",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
        if (wide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = Spacing.lg, end = Spacing.lg)
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.42f)
                        .fillMaxHeight()
                        .verticalScroll(infoScroll)
                        .padding(end = Spacing.xl, bottom = BottomClearance)
                ) {
                    TitleInfo(
                        data = data,
                        posterWidth = 176.dp,
                        onArtworkClick = { previewArtwork = true }
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(0.58f)
                        .fillMaxHeight()
                        .verticalScroll(episodesScroll)
                        .padding(bottom = BottomClearance)
                ) {
                    EpisodesSection(
                        data = data,
                        selectedSeason = activeSeason,
                        onSeasonSelect = onSeasonSelect,
                        onEpisodeClick = onEpisodeClick,
                        modifier = Modifier.padding(top = Spacing.sm)
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(pageScroll)
                    .padding(horizontal = Spacing.lg)
                    .padding(bottom = BottomClearance)
            ) {
                TitleInfo(
                    data = data,
                    posterWidth = 120.dp,
                    onArtworkClick = { previewArtwork = true }
                )
                if (data.seasons.any { it.episodes.isNotEmpty() }) {
                    EpisodesSection(
                        data = data,
                        selectedSeason = activeSeason,
                        onSeasonSelect = onSeasonSelect,
                        onEpisodeClick = onEpisodeClick,
                        modifier = Modifier.padding(top = Spacing.sm)
                    )
                }
            }
        }
    }
    if (previewArtwork) {
        ArtworkPreview(
            url = data.poster,
            title = data.title,
            onDismiss = { previewArtwork = false }
        )
    }
}

@Composable
private fun TitleInfo(
    data: TitleData,
    posterWidth: Dp,
    onArtworkClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Header(data = data, posterWidth = posterWidth, onArtworkClick = onArtworkClick)
        if (data.genres.isNotEmpty()) {
            GenreRow(data = data, modifier = Modifier.padding(top = Spacing.md))
        }
        val synopsis = data.synopsis
        if (synopsis != null) {
            Text(
                text = synopsis,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.lg)
            )
        }
        if (data.cast.isNotEmpty()) {
            CastRow(data = data)
        }
    }
}

private val BottomClearance = 120.dp

@Composable
private fun Header(
    data: TitleData,
    posterWidth: Dp,
    onArtworkClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        AsyncImage(
            model = data.poster,
            contentDescription = data.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(posterWidth)
                .aspectRatio(2f / 3f)
                .clip(MaterialTheme.shapes.medium)
                .clickable(onClick = onArtworkClick)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = data.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            val originalTitle = data.originalTitle
            if (originalTitle != null) {
                Text(
                    text = originalTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.xs)
                )
            }
            data.rating.value?.let { value ->
                Text(
                    text = "★ ${value.toInt() / 10.0}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = Spacing.sm)
                )
            }
            val meta = buildList {
                data.network?.let { add(it) }
                data.runtime?.let { add(it) }
                data.seasonsCount?.let { add("$it temporadas") }
                data.episodesCount?.let { add("$it episodios") }
            }
            if (meta.isNotEmpty()) {
                Text(
                    text = meta.joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.sm)
                )
            }
        }
    }
}

@Composable
private fun ArtworkPreview(
    url: String,
    title: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        var scale by remember { mutableFloatStateOf(1f) }
        var offsetX by remember { mutableFloatStateOf(0f) }
        var offsetY by remember { mutableFloatStateOf(0f) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.94f)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = url,
                contentDescription = title,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Spacing.lg)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offsetX
                        translationY = offsetY
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            if (scale == 1f) {
                                offsetX = 0f
                                offsetY = 0f
                            } else {
                                val limitX = (scale - 1f) * size.width / 2f
                                val limitY = (scale - 1f) * size.height / 2f
                                offsetX = (offsetX + pan.x).coerceIn(-limitX, limitX)
                                offsetY = (offsetY + pan.y).coerceIn(-limitY, limitY)
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (scale > 1f) {
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                } else {
                                    scale = 2.5f
                                }
                            }
                        )
                    }
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(Spacing.lg)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_tabler_x),
                    contentDescription = "Cerrar",
                    tint = Color.White
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GenreRow(data: TitleData, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        data.genres.forEach { genre ->
            StatusBadge(text = genre.name)
        }
    }
}

@Composable
private fun CastRow(data: TitleData, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(top = Spacing.xl)) {
        Text(
            text = "Reparto",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
            modifier = Modifier.padding(top = Spacing.md)
        ) {
            items(data.cast) { member ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(72.dp)
                ) {
                    AsyncImage(
                        model = member.image,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    )
                    Text(
                        text = member.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = Spacing.xs)
                    )
                    Text(
                        text = member.role,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun EpisodesSection(
    data: TitleData,
    selectedSeason: Int,
    onSeasonSelect: (Int) -> Unit,
    onEpisodeClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(top = Spacing.xl)) {
        Text(
            text = "Temporadas y episodios",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (data.seasons.size > 1) {
            SecondaryScrollableTabRow(
                selectedTabIndex = data.seasons.indexOfFirst { it.season == selectedSeason }
                    .coerceAtLeast(0),
                edgePadding = 0.dp,
                modifier = Modifier.padding(top = Spacing.md)
            ) {
                data.seasons.forEach { season ->
                    Tab(
                        selected = season.season == selectedSeason,
                        onClick = { onSeasonSelect(season.season) },
                        text = { Text("Temporada ${season.season} (${season.episodes.size})") }
                    )
                }
            }
        }
        val season = data.seasons.firstOrNull { it.season == selectedSeason }
            ?: data.seasons.firstOrNull()
        season?.episodes?.forEachIndexed { index, episode ->
            EpisodeRow(
                episode = episode,
                onClick = { onEpisodeClick(episode.url) },
                modifier = if (index > 0) Modifier.padding(top = Spacing.sm) else Modifier.padding(top = Spacing.lg)
            )
        }
    }
}

@Composable
private fun EpisodeRow(
    episode: com.notanime.es.scraper.EpisodeEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        AsyncImage(
            model = episode.image,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(96.dp)
                .aspectRatio(16f / 9f)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${episode.season} - ${episode.episode}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = episode.title.ifEmpty { "Episodio ${episode.episode}" },
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val date = episode.date
            if (date != null) {
                Text(
                    text = date,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

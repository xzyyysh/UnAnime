package com.unanimetv.android.feature.title.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.notanime.es.scraper.TitleData
import com.unanimetv.android.core.designsystem.component.EmptyState
import com.unanimetv.android.core.designsystem.component.LoadingState
import com.unanimetv.android.core.designsystem.component.StatusBadge
import com.unanimetv.android.core.designsystem.tokens.Spacing

@Composable
fun TitleScreen(
    url: String,
    onEpisodeClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: TitleViewModel = viewModel(key = url) { TitleViewModel(url) }
    val uiState by viewModel.uiState.collectAsState()

    when {
        uiState.isLoading -> LoadingState(modifier = modifier)
        uiState.error != null -> EmptyState(
            icon = Icons.Rounded.ErrorOutline,
            message = uiState.error.orEmpty(),
            modifier = modifier
        )
        else -> TitleContent(
            data = uiState.data,
            onEpisodeClick = onEpisodeClick,
            modifier = modifier
        )
    }
}

@Composable
private fun TitleContent(
    data: TitleData?,
    onEpisodeClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (data == null) return
    var selectedSeason by remember(data.url) {
        mutableIntStateOf(data.seasons.firstOrNull()?.season ?: 0)
    }
    val season = data.seasons.firstOrNull { it.season == selectedSeason } ?: data.seasons.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg)
    ) {
        Header(data = data)
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
        if (season != null && season.episodes.isNotEmpty()) {
            EpisodesSection(
                data = data,
                selectedSeason = selectedSeason,
                onSeasonSelect = { selectedSeason = it },
                onEpisodeClick = onEpisodeClick
            )
        }
    }
}

@Composable
private fun Header(data: TitleData, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        AsyncImage(
            model = data.poster,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(120.dp)
                .aspectRatio(2f / 3f)
                .clip(MaterialTheme.shapes.medium)
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
                        text = { Text("Temporada ${season.season}") }
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

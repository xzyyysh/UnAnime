package com.unanimetv.android.feature.episode.ui

import android.app.Activity
import android.content.ComponentName
import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Hd
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import coil3.compose.AsyncImage
import com.notanime.es.extractor.StreamInfo
import com.notanime.es.scraper.EpisodeEntry
import com.notanime.es.scraper.SeasonEpisodes
import com.notanime.es.scraper.SourceServer
import com.notanime.es.util.UnAnimeLog
import com.unanimetv.android.core.designsystem.component.EmptyState
import com.unanimetv.android.core.designsystem.component.LoadingState
import com.unanimetv.android.core.designsystem.tokens.Spacing
import com.unanimetv.android.core.playback.CurrentStream
import com.unanimetv.android.core.playback.PlaybackStore
import com.unanimetv.android.playback.PlaybackService

private val ShellClearance = 120.dp

@Composable
fun EpisodeScreen(
    url: String,
    onTitleClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val viewModel: EpisodeViewModel = viewModel(key = url) { EpisodeViewModel(url) }
    val uiState by viewModel.uiState.collectAsState()

    when {
        uiState.isLoading && uiState.seasons.isEmpty() -> LoadingState(modifier = modifier)
        uiState.error != null && uiState.seasons.isEmpty() -> EmptyState(
            icon = Icons.Rounded.ErrorOutline,
            message = uiState.error.orEmpty(),
            modifier = modifier
        )
        else -> EpisodeContent(
            uiState = uiState,
            onSelectLanguage = viewModel::selectLanguage,
            onSelectServer = viewModel::selectServer,
            onOpenEpisode = viewModel::openEpisode,
            onTitleClick = onTitleClick,
            modifier = modifier
        )
    }
}

@Composable
private fun EpisodeContent(
    uiState: EpisodeUiState,
    onSelectLanguage: (String) -> Unit,
    onSelectServer: (SourceServer) -> Unit,
    onOpenEpisode: (String) -> Unit,
    onTitleClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var controller by remember { mutableStateOf<MediaController?>(null) }
    var started by remember { mutableStateOf(false) }
    var fullscreen by remember { mutableStateOf(false) }
    var qualityIndex by remember { mutableStateOf(0) }
    var loadedUrl by remember { mutableStateOf<String?>(null) }

    val urlState by rememberUpdatedState(uiState.url)

    DisposableEffect(Unit) {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            controller = try {
                future.get()
            } catch (e: Exception) {
                UnAnimeLog.e("player", "controller connect failed", e)
                null
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            controller?.let { c ->
                if (c.currentMediaItem != null) {
                    PlaybackStore.put(urlState, c.currentPosition)
                }
                c.stop()
                c.clearMediaItems()
                c.release()
                UnAnimeLog.d("player", "controller released")
            }
            MediaController.releaseFuture(future)
        }
    }

    DisposableEffect(controller) {
        val c = controller ?: return@DisposableEffect onDispose { }
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                val name = when (state) {
                    Player.STATE_IDLE -> "idle"
                    Player.STATE_BUFFERING -> "buffering"
                    Player.STATE_READY -> "ready"
                    Player.STATE_ENDED -> "ended"
                    else -> "unknown"
                }
                UnAnimeLog.d("player", "state=$name")
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                UnAnimeLog.d("player", "playing=$isPlaying")
                if (isPlaying) started = true
            }

            override fun onRenderedFirstFrame() {
                UnAnimeLog.d("player", "first frame rendered")
            }

            override fun onPlayerError(error: PlaybackException) {
                UnAnimeLog.e("player", "playback error ${error.errorCodeName}", error)
            }
        }
        c.addListener(listener)
        onDispose { c.removeListener(listener) }
    }

    val stream = uiState.selectedServer?.stream
    val qualities = stream?.qualities.orEmpty()
    val safeQualityIndex = qualityIndex.coerceIn(0, (qualities.size - 1).coerceAtLeast(0))
    val qualityUrl = qualities.getOrNull(safeQualityIndex)?.url ?: qualities.firstOrNull()?.url

    LaunchedEffect(controller, uiState.url, qualityUrl) {
        val c = controller ?: return@LaunchedEffect
        if (qualityUrl == null) return@LaunchedEffect
        val sameEpisode = loadedUrl == uiState.url
        if (!sameEpisode && loadedUrl != null && c.currentMediaItem != null) {
            PlaybackStore.put(loadedUrl.orEmpty(), c.currentPosition)
        }
        val position = if (sameEpisode && c.currentMediaItem != null) {
            c.currentPosition
        } else {
            PlaybackStore.get(uiState.url)
        }
        val wasPlaying = c.isPlaying
        CurrentStream.referer = stream?.referer
        CurrentStream.currentUrl = qualityUrl
        val builder = MediaItem.Builder().setUri(qualityUrl)
        if (qualityUrl.contains(".m3u8")) builder.setMimeType(MimeTypes.APPLICATION_M3U8)
        UnAnimeLog.d("player", "prepare url=$qualityUrl resume=${position > 0}")
        c.setMediaItem(builder.build(), if (position > 0) position else C.TIME_UNSET)
        c.prepare()
        c.playWhenReady = sameEpisode && wasPlaying
        loadedUrl = uiState.url
    }

    val activity = context as? Activity
    DisposableEffect(fullscreen) {
        if (fullscreen && activity != null) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            androidx.core.view.WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
                systemBarsBehavior =
                    androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            if (fullscreen && activity != null) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                androidx.core.view.WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                    .show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    if (fullscreen && controller != null && stream != null) {
        Dialog(
            onDismissRequest = { fullscreen = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                PlayerVideo(
                    player = controller!!,
                    onToggleFullscreen = { fullscreen = false },
                    modifier = Modifier.fillMaxSize()
                )
                IconButton(
                    onClick = { fullscreen = false },
                    modifier = Modifier.align(Alignment.TopStart).padding(Spacing.md)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = "Salir de pantalla completa",
                        tint = Color.White
                    )
                }
            }
        }
    }

    LaunchedEffect(uiState.url) {
        scrollState.animateScrollTo(0)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.lg, bottom = ShellClearance)
    ) {
        Text(
            text = uiState.title ?: "Episodio",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clickable(enabled = uiState.titleUrl != null) {
                uiState.titleUrl?.let(onTitleClick)
            }
        )
        PlayerSurface(
            selectedServer = uiState.selectedServer,
            languages = uiState.languages,
            selectedLanguage = uiState.selectedLanguage,
            isLoading = uiState.isLoading,
            resolvingUrl = uiState.resolvingUrl,
            backdropUrl = uiState.image,
            started = started,
            fullscreenActive = fullscreen,
            stream = stream,
            qualityIndex = safeQualityIndex,
            player = controller,
            onSelectLanguage = onSelectLanguage,
            onSelectServer = onSelectServer,
            onSelectQuality = { qualityIndex = it },
            onPlay = {
                controller?.playWhenReady = true
                started = true
            },
            onToggleFullscreen = { fullscreen = !fullscreen },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.md)
        )
        EpisodeList(
            seasons = uiState.seasons,
            currentUrl = uiState.url,
            onOpenEpisode = onOpenEpisode,
            modifier = Modifier.padding(top = Spacing.lg)
        )
        EpisodeNavBar(
            prevUrl = uiState.prevUrl,
            nextUrl = uiState.nextUrl,
            titleUrl = uiState.titleUrl,
            onOpenEpisode = onOpenEpisode,
            onTitleClick = onTitleClick,
            modifier = Modifier.padding(top = Spacing.lg)
        )
    }
}

@Composable
private fun PlayerSurface(
    selectedServer: SourceServer?,
    languages: List<LanguageGroup>,
    selectedLanguage: String?,
    isLoading: Boolean,
    resolvingUrl: String?,
    backdropUrl: String?,
    started: Boolean,
    fullscreenActive: Boolean,
    stream: StreamInfo?,
    qualityIndex: Int,
    player: Player?,
    onSelectLanguage: (String) -> Unit,
    onSelectServer: (SourceServer) -> Unit,
    onSelectQuality: (Int) -> Unit,
    onPlay: () -> Unit,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(MaterialTheme.shapes.large)
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> com.unanimetv.android.ui.NotAnimeLoader(
                compact = true,
                modifier = Modifier.fillMaxSize()
            )
            !started -> BackdropBanner(
                backdropUrl = backdropUrl,
                canPlay = stream != null,
                onPlay = onPlay
            )
            player != null && stream != null && !fullscreenActive -> PlayerVideo(
                player = player,
                onToggleFullscreen = onToggleFullscreen,
                modifier = Modifier.fillMaxSize()
            )
            fullscreenActive -> BackdropBanner(
                backdropUrl = backdropUrl,
                canPlay = false,
                onPlay = {}
            )
            player == null -> com.unanimetv.android.ui.NotAnimeLoader(
                compact = true,
                modifier = Modifier.fillMaxSize()
            )
            selectedServer == null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Rounded.PlayCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "Selecciona un servidor",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    modifier = Modifier.padding(top = Spacing.sm)
                )
            }
            stream == null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Rounded.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = selectedServer?.error ?: selectedServer?.skipReason ?: "No disponible",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = Spacing.sm)
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .padding(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (languages.size > 1) {
                LanguageChip(
                    languages = languages,
                    selectedLanguage = selectedLanguage,
                    onSelectLanguage = onSelectLanguage
                )
                Spacer(Modifier.width(Spacing.sm))
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                languages.firstOrNull { it.lang == selectedLanguage }?.servers?.forEach { server ->
                    PlayerChip(
                        server = server,
                        selected = server == selectedServer,
                        resolving = resolvingUrl == server.url,
                        onClick = { onSelectServer(server) }
                    )
                }
            }
            if ((stream?.qualities?.size ?: 0) > 1) {
                Spacer(Modifier.width(Spacing.sm))
                QualityChip(
                    qualities = stream?.qualities.orEmpty(),
                    selectedIndex = qualityIndex,
                    onSelect = onSelectQuality
                )
            }
        }
    }
}

@Composable
private fun BackdropBanner(
    backdropUrl: String?,
    canPlay: Boolean,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (backdropUrl != null) {
            AsyncImage(
                model = backdropUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(18.dp)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
        )
        if (canPlay) {
            IconButton(
                onClick = onPlay,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(72.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayCircle,
                    contentDescription = "Reproducir",
                    tint = Color.White,
                    modifier = Modifier.size(72.dp)
                )
            }
        }
    }
}

@Composable
private fun OverlayIconButton(
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(36.dp)
            .background(Color.Black.copy(alpha = 0.45f), androidx.compose.foundation.shape.CircleShape)
    ) {
        content()
    }
}

@Composable
private fun QualityChip(
    qualities: List<com.notanime.es.extractor.StreamQuality>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OverlayIconButton(enabled = true, onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Rounded.Hd,
                contentDescription = "Calidad",
                tint = Color.White
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            qualities.forEachIndexed { index, quality ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = quality.label + if (index == selectedIndex) " ✓" else "",
                            color = if (index == selectedIndex) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    },
                    onClick = {
                        onSelect(index)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun LanguageChip(
    languages: List<LanguageGroup>,
    selectedLanguage: String?,
    onSelectLanguage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = Color.Black.copy(alpha = 0.45f),
            onClick = { expanded = true }
        ) {
            Icon(
                imageVector = Icons.Rounded.Language,
                contentDescription = "Idioma",
                tint = Color.White,
                modifier = Modifier.padding(Spacing.sm).size(20.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            languages.forEach { group ->
                DropdownMenuItem(
                    text = { Text(languageLabel(group.lang)) },
                    onClick = {
                        onSelectLanguage(group.lang)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun PlayerChip(
    server: SourceServer,
    selected: Boolean,
    resolving: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.Black.copy(alpha = 0.45f)
    }
    val content = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        Color.White
    }
    Surface(
        shape = MaterialTheme.shapes.small,
        color = container,
        contentColor = content,
        onClick = onClick,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs)
        ) {
            when {
                resolving -> CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp
                )
                server.icon.isNotEmpty() -> AsyncImage(
                    model = server.icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                else -> Icon(
                    imageVector = Icons.Rounded.PlayCircle,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = server.name,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun EpisodeList(
    seasons: List<SeasonEpisodes>,
    currentUrl: String,
    onOpenEpisode: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        seasons.forEach { season ->
            Text(
                text = "Temporada ${season.season}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = Spacing.lg, bottom = Spacing.sm)
            )
            season.episodes.forEach { episode ->
                EpisodeRow(
                    episode = episode,
                    current = episode.url == currentUrl,
                    onClick = { onOpenEpisode(episode.url) },
                    modifier = Modifier.padding(top = Spacing.sm)
                )
            }
        }
    }
}

@Composable
private fun EpisodeRow(
    episode: EpisodeEntry,
    current: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = if (current) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(container)
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
                color = if (current) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
            Text(
                text = episode.title.ifEmpty { "Episodio ${episode.episode}" },
                style = MaterialTheme.typography.titleSmall,
                color = if (current) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (current) {
            Icon(
                imageVector = Icons.Rounded.PlayCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun EpisodeNavBar(
    prevUrl: String?,
    nextUrl: String?,
    titleUrl: String?,
    onOpenEpisode: (String) -> Unit,
    onTitleClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Button(
            onClick = { prevUrl?.let(onOpenEpisode) },
            enabled = prevUrl != null,
            colors = ButtonDefaults.outlinedButtonColors(),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                contentDescription = null
            )
            Text("Anterior")
        }
        IconButton(
            onClick = { titleUrl?.let(onTitleClick) },
            enabled = titleUrl != null,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f),
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        ) {
            Icon(
                imageVector = Icons.Rounded.Home,
                contentDescription = "Ir al anime"
            )
        }
        Button(
            onClick = { nextUrl?.let(onOpenEpisode) },
            enabled = nextUrl != null,
            colors = ButtonDefaults.outlinedButtonColors(),
            modifier = Modifier.weight(1f)
        ) {
            Text("Siguiente")
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null
            )
        }
    }
}

private fun languageLabel(lang: String): String = when (lang.uppercase()) {
    "SUB" -> "Sub"
    "LAT" -> "Latino"
    "ES" -> "Castellano"
    else -> lang
}

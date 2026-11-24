package com.unanimetv.android.feature.episode.ui

import android.app.Activity
import android.Manifest
import android.content.ComponentName
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import coil3.compose.AsyncImage
import com.notanime.es.extractor.StreamInfo
import com.notanime.es.scraper.EpisodeEntry
import com.notanime.es.scraper.EpisodeScreenshot
import com.notanime.es.scraper.SeasonEpisodes
import com.notanime.es.scraper.SourceServer
import com.notanime.es.util.UnAnimeLog
import com.unanimetv.android.R
import com.unanimetv.android.core.designsystem.component.EmptyState
import com.unanimetv.android.core.designsystem.component.LoadingState
import com.unanimetv.android.core.designsystem.tokens.Spacing
import com.unanimetv.android.core.media.ImageDownloader
import com.unanimetv.android.core.playback.CurrentStream
import com.unanimetv.android.core.playback.PlayerFullscreen
import com.unanimetv.android.core.playback.PlaybackStore
import com.unanimetv.android.ui.LinkMenu
import com.unanimetv.android.playback.PlaybackService

private val ShellClearance = 120.dp

@Composable
fun EpisodeScreen(
    url: String,
    onTitleClick: (String) -> Unit = {},
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val viewModel: EpisodeViewModel = viewModel(key = url) { EpisodeViewModel(url) }
    val uiState by viewModel.uiState.collectAsState()

    when {
        uiState.isLoading && uiState.seasons.isEmpty() -> LoadingState(modifier = modifier)
        uiState.error != null && uiState.seasons.isEmpty() -> EmptyState(
            icon = painterResource(R.drawable.ic_tabler_alert_circle),
            message = uiState.error.orEmpty(),
            modifier = modifier
        )
        else -> EpisodeContent(
            uiState = uiState,
            onBack = onBack,
            onSelectLanguage = viewModel::selectLanguage,
            onSelectServer = viewModel::selectServer,
            onOpenEpisode = viewModel::openEpisode,
            onReload = viewModel::reload,
            onTitleClick = onTitleClick,
            modifier = modifier
        )
    }
}

@Composable
private fun EpisodeContent(
    uiState: EpisodeUiState,
    onBack: () -> Unit,
    onSelectLanguage: (String) -> Unit,
    onSelectServer: (SourceServer) -> Unit,
    onOpenEpisode: (String) -> Unit,
    onReload: () -> Unit,
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
    var previewIndex by remember { mutableStateOf<Int?>(null) }

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
        builder.setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(uiState.title ?: "Episodio")
                .setArtist("UnAnime")
                .setArtworkUri(uiState.image?.let(android.net.Uri::parse))
                .build()
        )
        UnAnimeLog.d("player", "prepare url=$qualityUrl resume=${position > 0}")
        c.setMediaItem(builder.build(), if (position > 0) position else C.TIME_UNSET)
        c.prepare()
        c.playWhenReady = sameEpisode && wasPlaying
        loadedUrl = uiState.url
    }

    val activity = context as? Activity
    DisposableEffect(fullscreen) {
        PlayerFullscreen.set(fullscreen)
        if (fullscreen && activity != null) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            androidx.core.view.WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
                systemBarsBehavior =
                    androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            PlayerFullscreen.set(false)
            if (activity != null) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                androidx.core.view.WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                    .show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    androidx.activity.compose.BackHandler(enabled = fullscreen) {
        fullscreen = false
    }

    LaunchedEffect(uiState.url) {
        val c = controller
        val previous = loadedUrl
        if (c != null && previous != null && previous != uiState.url && c.currentMediaItem != null) {
            PlaybackStore.put(previous, c.currentPosition)
        }
        started = false
        loadedUrl = null
        qualityIndex = 0
        controller?.stop()
        controller?.clearMediaItems()
        scrollState.animateScrollTo(0)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.lg, bottom = ShellClearance)
        ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_tabler_arrow_back),
                    contentDescription = "Volver",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = uiState.title ?: "Episodio",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = uiState.titleUrl != null) {
                        uiState.titleUrl?.let(onTitleClick)
                    }
            )
            LinkMenu(
                url = uiState.url,
                onReload = {
                    // streams die or get blocked mid-session, this forces a cold re-scrape and server re-pick
                    started = false
                    loadedUrl = null
                    qualityIndex = 0
                    controller?.stop()
                    controller?.clearMediaItems()
                    onReload()
                }
            )
        }
        PlayerSurface(
            selectedServer = uiState.selectedServer,
            languages = uiState.languages,
            selectedLanguage = uiState.selectedLanguage,
            isLoading = uiState.isLoading,
            resolvingUrl = uiState.resolvingUrl,
            backdropUrl = uiState.image,
            started = started,
            fullscreenActive = fullscreen,
            title = uiState.title,
            stream = stream,
            qualityIndex = safeQualityIndex,
            player = controller,
            onSelectLanguage = onSelectLanguage,
            onSelectServer = onSelectServer,
            onPlay = {
                controller?.playWhenReady = true
                started = true
            },
            onToggleFullscreen = { fullscreen = !fullscreen },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.md)
        )
        EpisodeNavBar(
            prevUrl = uiState.prevUrl,
            nextUrl = uiState.nextUrl,
            titleUrl = uiState.titleUrl,
            onOpenEpisode = onOpenEpisode,
            onTitleClick = onTitleClick,
            modifier = Modifier.padding(top = Spacing.md)
        )
        if (uiState.gallery.isNotEmpty()) {
            ScreenshotGallery(
                screenshots = uiState.gallery,
                onSelect = { previewIndex = it },
                modifier = Modifier.padding(top = Spacing.lg)
            )
        }
        EpisodeList(
            seasons = uiState.seasons,
            currentUrl = uiState.url,
            onOpenEpisode = onOpenEpisode,
            modifier = Modifier.padding(top = Spacing.lg)
        )
        previewIndex?.let { index ->
            ScreenshotPreview(
                screenshots = uiState.gallery,
                initialIndex = index,
                fileNamePrefix = uiState.title ?: "episodio",
                onDismiss = { previewIndex = null }
            )
        }
        }
        if (fullscreen && controller != null && stream != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black)
            ) {
                PlayerVideo(
                    player = controller!!,
                    title = uiState.title,
                    fullscreen = true,
                    onToggleFullscreen = { fullscreen = false },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun ScreenshotGallery(
    screenshots: List<EpisodeScreenshot>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val state = rememberLazyListState()
    val current by remember { derivedStateOf { state.firstVisibleItemIndex } }
    Column(modifier = modifier) {
        Text(
            text = "Galería",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        LazyRow(
            state = state,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = state, snapPosition = SnapPosition.Center),
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            modifier = Modifier.padding(top = Spacing.md)
        ) {
            items(count = screenshots.size) { index ->
                val screenshot = screenshots[index]
                AsyncImage(
                    model = screenshot.thumb,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillParentMaxWidth(0.78f)
                        .aspectRatio(16f / 9f)
                        .clip(MaterialTheme.shapes.medium)
                        .clickable(onClick = { onSelect(index) })
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = Spacing.md)
        ) {
            repeat(screenshots.size) { index ->
                Box(
                    modifier = Modifier
                        .size(width = if (index == current) 20.dp else 6.dp, height = 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == current) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        )
                )
            }
        }
    }
}

@Composable
private fun ScreenshotPreview(
    screenshots: List<EpisodeScreenshot>,
    initialIndex: Int,
    fileNamePrefix: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var downloading by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<EpisodeScreenshot?>(null) }
    val rotation = remember { Animatable(0f) }
    val pagerState = rememberPagerState(initialPage = initialIndex) { screenshots.size }
    LaunchedEffect(downloading) {
        if (downloading) {
            while (downloading) {
                rotation.animateTo(
                    rotation.value + 360f,
                    animationSpec = tween(900, easing = LinearEasing)
                )
            }
        } else {
            rotation.snapTo(0f)
        }
    }
    val download: (EpisodeScreenshot) -> Unit = { screenshot ->
        if (!downloading) {
            downloading = true
            scope.launch {
                val index = screenshots.indexOf(screenshot).coerceAtLeast(0)
                runCatching {
                    ImageDownloader.download(
                        context,
                        screenshot.full,
                        ImageDownloader.safeFileName(fileNamePrefix, index)
                    )
                }
                    .onSuccess {
                        UnAnimeLog.d("download", "ok: $it")
                        Toast.makeText(context, "Imagen descargada en Descargas", Toast.LENGTH_SHORT).show()
                    }
                    .onFailure {
                        UnAnimeLog.e("download", "failed", it)
                        Toast.makeText(context, "No se pudo descargar", Toast.LENGTH_SHORT).show()
                    }
                downloading = false
            }
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val screenshot = pending
        pending = null
        if (granted && screenshot != null) download(screenshot)
    }
    val requestDownload: (EpisodeScreenshot) -> Unit = { screenshot ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            download(screenshot)
        } else {
            pending = screenshot
            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
        ) {
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = Spacing.xxl),
                pageSpacing = Spacing.lg,
                key = { screenshots[it].full },
                modifier = Modifier.fillMaxSize()
            ) { page ->
                AsyncImage(
                    model = screenshots[page].full,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Text(
                text = "${pagerState.currentPage + 1} / ${screenshots.size}",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = Spacing.lg)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                    .padding(horizontal = Spacing.md, vertical = Spacing.xs)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(Spacing.lg)
            ) {
                IconButton(
                    onClick = {
                        screenshots.getOrNull(pagerState.currentPage)?.let(requestDownload)
                    },
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.45f), CircleShape)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_tabler_download),
                        contentDescription = "Descargar",
                        tint = Color.White,
                        modifier = Modifier.graphicsLayer { rotationZ = rotation.value }
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.45f), CircleShape)
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
    title: String?,
    stream: StreamInfo?,
    qualityIndex: Int,
    player: Player?,
    onSelectLanguage: (String) -> Unit,
    onSelectServer: (SourceServer) -> Unit,
    onPlay: () -> Unit,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val picking = !started && (selectedServer == null || selectedServer.error != null)
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
                resolving = resolvingUrl != null,
                onPlay = onPlay
            )
            player != null && stream != null && !fullscreenActive -> PlayerVideo(
                player = player,
                title = title,
                fullscreen = false,
                onToggleFullscreen = onToggleFullscreen,
                modifier = Modifier.fillMaxSize()
            )
            fullscreenActive -> Box(modifier = Modifier.fillMaxSize().background(Color.Black))
            player == null -> com.unanimetv.android.ui.NotAnimeLoader(
                compact = true,
                modifier = Modifier.fillMaxSize()
            )
            selectedServer == null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painter = painterResource(R.drawable.ic_tabler_player_play),
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
                    painter = painterResource(R.drawable.ic_tabler_alert_circle),
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
        if (picking) {
            ServerPicker(
                languages = languages,
                selectedLanguage = selectedLanguage,
                servers = languages.firstOrNull { it.lang == selectedLanguage }?.servers.orEmpty(),
                selectedServer = selectedServer,
                resolvingUrl = resolvingUrl,
                onSelectLanguage = onSelectLanguage,
                onSelectServer = onSelectServer,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun ServerPicker(
    languages: List<LanguageGroup>,
    selectedLanguage: String?,
    servers: List<SourceServer>,
    selectedServer: SourceServer?,
    resolvingUrl: String?,
    onSelectLanguage: (String) -> Unit,
    onSelectServer: (SourceServer) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (languages.isNotEmpty()) {
            LanguageSelector(
                languages = languages,
                selectedLanguage = selectedLanguage,
                onSelectLanguage = onSelectLanguage
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            servers.forEach { server ->
                ServerOptionCard(
                    server = server,
                    selected = server == selectedServer,
                    resolving = resolvingUrl == server.url,
                    onClick = { onSelectServer(server) }
                )
            }
        }
    }
}

@Composable
private fun ServerOptionCard(
    server: SourceServer,
    selected: Boolean,
    resolving: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.White.copy(alpha = 0.08f)
    }
    Row(
        modifier = modifier
            .fillMaxWidth(0.72f)
            .clip(MaterialTheme.shapes.small)
            .background(container)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        if (server.icon.isNotEmpty()) {
            AsyncImage(
                model = server.icon,
                contentDescription = null,
                modifier = Modifier
                    .size(22.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_tabler_player_play),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = server.name,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    Color.White
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val detail = server.description.ifEmpty {
                server.error ?: server.skipReason.orEmpty()
            }
            if (detail.isNotEmpty()) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        Color.White.copy(alpha = 0.7f)
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (resolving) {
            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
        }
    }
}

@Composable
private fun BackdropBanner(
    backdropUrl: String?,
    canPlay: Boolean,
    resolving: Boolean,
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
        when {
            resolving -> CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(72.dp)
            )
            canPlay -> IconButton(
                onClick = onPlay,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(72.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_tabler_player_play),
                    contentDescription = "Reproducir",
                    tint = Color.White,
                    modifier = Modifier.size(72.dp)
                )
            }
        }
    }
}

@Composable
private fun LanguageSelector(
    languages: List<LanguageGroup>,
    selectedLanguage: String?,
    onSelectLanguage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        languages.forEach { group ->
            val selected = group.lang == selectedLanguage
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = Color.Black.copy(alpha = 0.45f),
                onClick = { onSelectLanguage(group.lang) },
                modifier = Modifier.graphicsLayer { alpha = if (selected) 1f else 0.4f }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(34.dp)
                ) {
                    val flag = languageFlag(group.lang)
                    if (flag != null) {
                        Text(text = flag, style = MaterialTheme.typography.headlineSmall)
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.ic_tabler_world),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
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
    val onContainer = if (current) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val onContainerVariant = if (current) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(container)
            .clickable(onClick = onClick)
            .padding(Spacing.sm),
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
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Text(
                text = episode.title.ifEmpty { "Episodio ${episode.episode}" },
                style = MaterialTheme.typography.titleSmall,
                color = onContainer,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = episodeDescription(episode),
                style = MaterialTheme.typography.bodySmall,
                color = onContainerVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (current) {
            Icon(
                painter = painterResource(R.drawable.ic_tabler_player_play),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

private fun episodeDescription(episode: EpisodeEntry): String = buildList {
    add("Temporada ${episode.season} · Episodio ${episode.episode}")
    episode.date?.let { add(it) }
}.joinToString(" · ")

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
        horizontalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = { prevUrl?.let(onOpenEpisode) },
            enabled = prevUrl != null,
            colors = ButtonDefaults.outlinedButtonColors()
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_tabler_chevron_left),
                contentDescription = null
            )
            Text("Anterior")
        }
        Spacer(Modifier.width(Spacing.lg))
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
                painter = painterResource(R.drawable.ic_tabler_home),
                contentDescription = "Ir al anime"
            )
        }
        Spacer(Modifier.width(Spacing.lg))
        Button(
            onClick = { nextUrl?.let(onOpenEpisode) },
            enabled = nextUrl != null,
            colors = ButtonDefaults.outlinedButtonColors()
        ) {
            Text("Siguiente")
            Icon(
                painter = painterResource(R.drawable.ic_tabler_chevron_right),
                contentDescription = null
            )
        }
    }
}

private fun languageFlag(lang: String): String? = when (lang.uppercase()) {
    "SUB" -> "🇯🇵"
    "LAT" -> "🇲🇽"
    "ES" -> "🇪🇸"
    else -> null
}

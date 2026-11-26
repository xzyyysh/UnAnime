package com.unanimetv.android.feature.title.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.session.MediaController
import com.notanime.es.extractor.StreamInfo
import com.notanime.es.scraper.SourceServer
import com.notanime.es.util.UnAnimeLog
import com.unanimetv.android.core.playback.CurrentStream
import com.unanimetv.android.core.playback.PlaybackStore
import com.unanimetv.android.core.playback.ui.FullscreenPlaybackEffect
import com.unanimetv.android.core.playback.ui.PlayerSurface
import com.unanimetv.android.core.playback.ui.rememberMediaController
import com.unanimetv.android.feature.episode.ui.EpisodeUiState
import com.unanimetv.android.feature.episode.ui.EpisodeViewModel

class MoviePlayback(
    val url: String,
    val onSelectLanguage: (String) -> Unit,
    val onSelectServer: (SourceServer) -> Unit,
    val onReload: () -> Unit,
    val onWebviewStream: (String, String, String?) -> Unit,
    val onDismissWebview: () -> Unit,
    val onWebviewTimeout: () -> Unit
) {
    var uiState by mutableStateOf(EpisodeUiState(url = url))
    var controller by mutableStateOf<MediaController?>(null)
    var started by mutableStateOf(false)
    var fullscreen by mutableStateOf(false)
    val stream: StreamInfo? get() = uiState.selectedServer?.stream

    fun reload() {
        started = false
        onReload()
    }
}

@Composable
fun rememberMoviePlayback(
    url: String,
    title: String?,
    posterUrl: String?
): MoviePlayback {
    val viewModel: EpisodeViewModel = viewModel(key = "movie-$url") { EpisodeViewModel(url) }
    val playback = remember {
        MoviePlayback(
            url,
            viewModel::selectLanguage,
            viewModel::selectServer,
            viewModel::reload,
            viewModel::onWebviewStream,
            viewModel::dismissWebview,
            viewModel::onWebviewTimeout
        )
    }
    val uiState by viewModel.uiState.collectAsState()
    playback.uiState = uiState
    playback.controller = rememberMediaController(url) { if (it) playback.started = true }

    val stream = playback.stream
    val qualityUrl = stream?.qualities?.firstOrNull()?.url
    var loadedUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(playback.controller, qualityUrl) {
        val c = playback.controller ?: return@LaunchedEffect
        if (qualityUrl == null) return@LaunchedEffect
        val sameMovie = loadedUrl == url
        if (!sameMovie && loadedUrl != null && c.currentMediaItem != null) {
            PlaybackStore.put(url, c.currentPosition)
        }
        val position = if (sameMovie && c.currentMediaItem != null) {
            c.currentPosition
        } else {
            PlaybackStore.get(url)
        }
        val wasPlaying = c.isPlaying
        CurrentStream.referer = stream?.referer
        CurrentStream.currentUrl = qualityUrl
        val builder = MediaItem.Builder().setUri(qualityUrl)
        if (qualityUrl.contains(".m3u8")) builder.setMimeType(MimeTypes.APPLICATION_M3U8)
        builder.setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title ?: "Película")
                .setArtist("UnAnime")
                .setArtworkUri(posterUrl?.let(Uri::parse))
                .build()
        )
        UnAnimeLog.d("player", "prepare movie url=$qualityUrl resume=${position > 0}")
        c.setMediaItem(builder.build(), if (position > 0) position else C.TIME_UNSET)
        c.prepare()
        c.playWhenReady = sameMovie && wasPlaying
        loadedUrl = url
    }

    FullscreenPlaybackEffect(fullscreen = playback.fullscreen) { playback.fullscreen = false }

    return playback
}

@Composable
fun MoviePlayerSurface(
    playback: MoviePlayback,
    modifier: Modifier = Modifier
) {
    PlayerSurface(
        selectedServer = playback.uiState.selectedServer,
        languages = playback.uiState.languages,
        selectedLanguage = playback.uiState.selectedLanguage,
        isLoading = playback.uiState.isLoading,
        resolvingUrl = playback.uiState.resolvingUrl,
        backdropUrl = playback.uiState.image,
        started = playback.started,
        fullscreenActive = playback.fullscreen,
        title = playback.uiState.title,
        stream = playback.stream,
        qualityIndex = 0,
        player = playback.controller,
        onSelectLanguage = playback.onSelectLanguage,
        onSelectServer = playback.onSelectServer,
        onPlay = {
            playback.controller?.playWhenReady = true
            playback.started = true
        },
        onToggleFullscreen = { playback.fullscreen = !playback.fullscreen },
        unavailableHint = "Reload the movie to choose another source maybe.",
        modifier = modifier
    )
}

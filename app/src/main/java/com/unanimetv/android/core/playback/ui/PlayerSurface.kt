package com.unanimetv.android.core.playback.ui

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.notanime.es.extractor.StreamInfo
import com.notanime.es.scraper.SourceServer
import com.unanimetv.android.R
import com.unanimetv.android.core.designsystem.tokens.Spacing
import com.unanimetv.android.ui.NotAnimeLoader

data class LanguageGroup(
    val lang: String,
    val servers: List<SourceServer>
)

@Composable
fun PlayerSurface(
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
    player: androidx.media3.common.Player?,
    onSelectLanguage: (String) -> Unit,
    onSelectServer: (SourceServer) -> Unit,
    onPlay: () -> Unit,
    onToggleFullscreen: () -> Unit,
    unavailableHint: String? = null,
    modifier: Modifier = Modifier
) {
    val unavailable = selectedServer != null && stream == null &&
        (selectedServer.error != null || selectedServer.skipReason != null)
    val picking = !started && !unavailable && (selectedServer == null || selectedServer.error != null)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(MaterialTheme.shapes.large)
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> NotAnimeLoader(
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
            unavailable -> UnavailableServer(
                message = selectedServer?.error ?: selectedServer?.skipReason.orEmpty(),
                hint = unavailableHint
            )
            player == null -> NotAnimeLoader(
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
private fun UnavailableServer(
    message: String,
    hint: String?,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(Spacing.lg)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_tabler_alert_circle),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = message,
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Spacing.sm)
        )
        if (hint != null) {
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Spacing.xs)
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
            canPlay -> androidx.compose.material3.IconButton(
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

private fun languageFlag(lang: String): String? = when (lang.uppercase()) {
    "SUB" -> "🇯🇵"
    "LAT" -> "🇲🇽"
    "ES" -> "🇪🇸"
    else -> null
}

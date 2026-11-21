package com.unanimetv.android.feature.episode.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.unanimetv.android.R
import com.notanime.es.util.UnAnimeLog
import com.unanimetv.android.core.designsystem.tokens.Spacing
import com.unanimetv.android.ui.NotAnimeLoader
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.roundToLong

private val SKIP_MS = 15_000L

private val SPEEDS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

@Composable
fun PlayerVideo(
    player: Player,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    var playing by remember { mutableStateOf(false) }
    var ended by remember { mutableStateOf(false) }
    var buffering by remember { mutableStateOf(false) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var bufferedMs by remember { mutableLongStateOf(0L) }
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    var controlsVisible by remember { mutableStateOf(true) }
    var settingsOpen by remember { mutableStateOf(false) }
    var speed by remember { mutableStateOf(1f) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                val name = when (state) {
                    Player.STATE_IDLE -> "idle"
                    Player.STATE_BUFFERING -> "buffering"
                    Player.STATE_READY -> "ready"
                    Player.STATE_ENDED -> "ended"
                    else -> "unknown"
                }
                UnAnimeLog.d("player", "ui state=$name")
                buffering = state == Player.STATE_BUFFERING
                ended = state == Player.STATE_ENDED
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                UnAnimeLog.d("player", "ui playing=$isPlaying")
                playing = isPlaying
                if (isPlaying) controlsVisible = true
            }
        }
        player.addListener(listener)
        playing = player.isPlaying
        buffering = player.playbackState == Player.STATE_BUFFERING
        ended = player.playbackState == Player.STATE_ENDED
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(player) {
        while (true) {
            positionMs = player.currentPosition.coerceAtLeast(0L)
            bufferedMs = player.bufferedPosition.coerceAtLeast(0L)
            durationMs = player.duration.takeIf { it != Long.MIN_VALUE }?.coerceAtLeast(0L) ?: 0L
            delay(if (playing) 500L else 1000L)
        }
    }

    LaunchedEffect(controlsVisible, playing, dragging, settingsOpen) {
        if (controlsVisible && playing && !dragging && !settingsOpen) {
            delay(4000)
            controlsVisible = false
            settingsOpen = false
        }
    }

    val activeFraction = if (dragging) {
        dragValue
    } else if (durationMs > 0) {
        positionMs.toFloat() / durationMs.toFloat()
    } else {
        0f
    }
    val bufferedFraction = if (durationMs > 0) {
        (bufferedMs.toFloat() / durationMs.toFloat()).coerceIn(activeFraction, 1f)
    } else {
        0f
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                android.view.LayoutInflater.from(ctx)
                    .inflate(R.layout.view_player_video, null, false) as PlayerView
            },
            update = { view ->
                if (view.player != player) view.player = player
            },
            onRelease = { view ->
                view.player = null
            },
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    controlsVisible = !controlsVisible
                    if (!controlsVisible) settingsOpen = false
                }
        )
        if (buffering && !ended) {
            NotAnimeLoader(compact = true, modifier = Modifier.fillMaxSize())
        }
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SkipButton(seconds = -15) {
                    player.seekTo((player.currentPosition - SKIP_MS).coerceAtLeast(0L))
                }
                Spacer(modifier = Modifier.size(Spacing.md))
                IconButton(
                    onClick = {
                        when {
                            ended -> {
                                player.seekTo(0L)
                                player.play()
                            }
                            player.isPlaying -> player.pause()
                            else -> player.play()
                        }
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                ) {
                    Icon(
                        imageVector = when {
                            ended -> Icons.Rounded.Replay
                            playing -> Icons.Rounded.Pause
                            else -> Icons.Rounded.PlayArrow
                        },
                        contentDescription = when {
                            ended -> "Reiniciar"
                            playing -> "Pausar"
                            else -> "Reproducir"
                        },
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.size(Spacing.md))
                SkipButton(seconds = 15) {
                    player.seekTo(
                        (player.currentPosition + SKIP_MS).coerceAtMost(
                            player.duration.coerceAtLeast(0L)
                        )
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(tween(200)) + slideInVertically(tween(250)) { it / 4 },
            exit = fadeOut(tween(200)) + slideOutVertically(tween(250)) { it / 4 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md, vertical = Spacing.xs)
            ) {
                CompactSeekbar(
                    activeFraction = activeFraction,
                    bufferedFraction = bufferedFraction,
                    enabled = durationMs > 0,
                    onSeekStart = {
                        dragging = true
                        dragValue = activeFraction
                    },
                    onSeek = { dragValue = it.coerceIn(0f, 1f) },
                    onSeekEnd = {
                        player.seekTo((dragValue * durationMs).roundToLong())
                        dragging = false
                    }
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatTime(if (dragging) (dragValue * durationMs).roundToLong() else positionMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                    Text(
                        text = " / ${formatTime(durationMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = { settingsOpen = !settingsOpen },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Configuración",
                            tint = if (settingsOpen) MaterialTheme.colorScheme.primary else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onToggleFullscreen,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Fullscreen,
                            contentDescription = "Pantalla completa",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = controlsVisible && settingsOpen,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = Spacing.md, bottom = 76.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(8.dp))
                    .padding(vertical = Spacing.xs)
            ) {
                SPEEDS.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable {
                                speed = option
                                player.setPlaybackSpeed(option)
                                settingsOpen = false
                            }
                            .padding(horizontal = Spacing.lg, vertical = Spacing.xs)
                    ) {
                        Text(
                            text = "${option}x",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (option == speed) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.White
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactSeekbar(
    activeFraction: Float,
    bufferedFraction: Float,
    enabled: Boolean,
    onSeekStart: () -> Unit,
    onSeek: (Float) -> Unit,
    onSeekEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(22.dp)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    onSeek(offset.x / size.width.toFloat())
                    onSeekEnd()
                }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { onSeekStart() },
                    onDragEnd = { onSeekEnd() },
                    onDragCancel = { onSeekEnd() }
                ) { change, _ ->
                    onSeek(change.position.x / size.width.toFloat())
                    change.consume()
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        val trackHeight = 3.dp
        val thumbSize = 11.dp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .background(Color.White.copy(alpha = 0.25f))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(bufferedFraction)
                .height(trackHeight)
                .background(Color.White.copy(alpha = 0.4f))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(activeFraction)
                .height(trackHeight)
                .background(MaterialTheme.colorScheme.primary)
        )
        Box(
            modifier = Modifier
                .offset(x = (activeFraction * maxWidth.value - (thumbSize.value / 2)).dp)
                .size(thumbSize)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        )
    }
}

@Composable
private fun SkipButton(
    seconds: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(44.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(38.dp)
                .background(Color.Black.copy(alpha = 0.45f), CircleShape)
        ) {
            Text(
                text = (if (seconds > 0) "+" else "") + seconds.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

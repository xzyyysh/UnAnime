package com.unanimetv.android.core.playback.ui

import android.app.Activity
import android.content.ComponentName
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.notanime.es.util.UnAnimeLog
import com.unanimetv.android.core.playback.PlayerFullscreen
import com.unanimetv.android.core.playback.PlaybackStore
import com.unanimetv.android.playback.PlaybackService

@Composable
fun rememberMediaController(
    url: String,
    onPlaying: ((Boolean) -> Unit)? = null
): MediaController? {
    val context = LocalContext.current
    var controller by remember { mutableStateOf<MediaController?>(null) }
    val urlState by rememberUpdatedState(url)

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
                if (state == Player.STATE_READY) {
                    val duration = c.duration
                    if (duration > 0) PlaybackStore.putDuration(urlState, duration)
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                UnAnimeLog.d("player", "playing=$isPlaying")
                if (isPlaying) onPlaying?.invoke(isPlaying)
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
    return controller
}

@Composable
fun FullscreenPlaybackEffect(
    fullscreen: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
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
    BackHandler(enabled = fullscreen) { onDismiss() }
}

package com.unanimetv.android.core.playback.ui

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.unanimetv.android.core.designsystem.tokens.Spacing
import kotlinx.coroutines.delay

private const val WARMUP_MS = 2_500L
private const val TIMEOUT_MS = 60_000L

private fun familyOf(host: String): String? = when {
    host.contains("filemoo", ignoreCase = true) -> "filemoon"
    host.contains("dood", ignoreCase = true) || host.contains("playmogo", ignoreCase = true) -> "dood"
    else -> null
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewStreamDialog(
    url: String,
    onStream: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var captured by remember { mutableStateOf(false) }
    val startHost = remember(url) { Uri.parse(url).host.orEmpty() }

    LaunchedEffect(url) {
        delay(TIMEOUT_MS)
        onDismiss()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true)
    ) {
        Column {
            Text(
                text = "Resolviendo stream — esperá la verificación",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = Spacing.sm)
            )
            androidx.compose.ui.viewinterop.AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        var loadStartedAt = 0L
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                request: WebResourceRequest
                            ): Boolean {
                                val host = request.url.host.orEmpty()
                                val allowed = host == startHost ||
                                    familyOf(host) != null ||
                                    host.endsWith("challenges.cloudflare.com")
                                return !allowed
                            }

                            override fun onPageFinished(view: WebView, finishedUrl: String) {
                                view.evaluateJavascript(
                                    "(function(){['.vjs-big-play-button','.captcha_l','.jw-icon-display','[class*=play-button]'].forEach(function(s){var e=document.querySelector(s);if(e)e.click();});})()",
                                    null
                                )
                            }

                            override fun shouldInterceptRequest(
                                view: WebView,
                                request: WebResourceRequest
                            ): WebResourceResponse? {
                                val target = request.url.toString()
                                val isMedia = target.contains(".m3u8") || target.contains(".mp4")
                                val warmedUp = loadStartedAt == 0L ||
                                    System.currentTimeMillis() - loadStartedAt >= WARMUP_MS
                                if (!captured && isMedia && warmedUp) {
                                    captured = true
                                    onStream(target)
                                }
                                return null
                            }
                        }
                        loadStartedAt = System.currentTimeMillis()
                        loadUrl(url)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(MaterialTheme.shapes.medium)
            )
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .padding(top = Spacing.xs)
                    .align(androidx.compose.ui.Alignment.End)
            ) {
                Text(text = "Cancelar")
            }
        }
    }
}

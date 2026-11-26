package com.unanimetv.android.core.playback.ui

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.unanimetv.android.core.designsystem.tokens.Spacing
import com.unanimetv.android.ui.NotAnimeLoader
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
    var webviewRef by remember { mutableStateOf<WebView?>(null) }
    val startHost = remember(url) { Uri.parse(url).host.orEmpty() }
    val seamless = remember(url) { familyOf(Uri.parse(url).host.orEmpty()) == "dood" }

    LaunchedEffect(url) {
        delay(TIMEOUT_MS)
        onDismiss()
    }

    LaunchedEffect(url, webviewRef) {
        val webview = webviewRef ?: return@LaunchedEffect
        while (!captured) {
            delay(1_000)
            webview.evaluateJavascript(
                "(function(){var v=document.querySelector('video');if(!v)return '';return v.src||v.currentSrc||'';})()"
            ) { value ->
                val src = value?.trim()?.removeSurrounding("\"")
                if (!captured && !src.isNullOrEmpty() && src.startsWith("http") && !src.contains("blob:")) {
                    captured = true
                    onStream(src)
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true)
    ) {
        Column {
            if (!seamless) {
                Text(
                    text = "Resolviendo stream — esperá la verificación",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = Spacing.sm)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(MaterialTheme.shapes.medium)
            ) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            webviewRef = this
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
                    modifier = Modifier.fillMaxSize(),
                    onRelease = { webviewRef = null }
                )
                if (seamless) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.94f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            NotAnimeLoader()
                            Text(
                                text = "Resolviendo stream…",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = Spacing.md)
                            )
                        }
                    }
                }
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .padding(top = Spacing.xs)
                    .align(Alignment.End)
            ) {
                Text(text = "Cancelar")
            }
        }
    }
}

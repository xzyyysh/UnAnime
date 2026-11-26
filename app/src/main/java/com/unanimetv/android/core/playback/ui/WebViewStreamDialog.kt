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

private fun tapAt(webview: WebView, x: Float, y: Float) {
    val now = android.os.SystemClock.uptimeMillis()
    val down = android.view.MotionEvent.obtain(now, now, android.view.MotionEvent.ACTION_DOWN, x, y, 0)
    webview.dispatchTouchEvent(down)
    down.recycle()
    val up = android.view.MotionEvent.obtain(now, now + 80, android.view.MotionEvent.ACTION_UP, x, y, 0)
    webview.dispatchTouchEvent(up)
    up.recycle()
}

private const val GATE_CLICK_SCRIPT =
    "<script>(function(){var n=0;var t=setInterval(function(){var b=document.querySelector('.captcha-gate__play');if(b){b.click();}if(++n>40)clearInterval(t);},400);})();</script>"

private fun injectedGateResponse(url: String): android.webkit.WebResourceResponse? {
    return try {
        val body = kotlinx.coroutines.runBlocking { com.notanime.es.net.CronetHttp.get(url) }.body
        val injected = body.replace("</body>", "$GATE_CLICK_SCRIPT</body>")
        android.webkit.WebResourceResponse(
            "text/html",
            "utf-8",
            java.io.ByteArrayInputStream(injected.toByteArray(Charsets.UTF_8))
        )
    } catch (e: Exception) {
        com.notanime.es.util.UnAnimeLog.w("webview-stream", "gate injection failed", e)
        null
    }
}

private fun familyOf(host: String): String? = when {
    host.contains("filemoo", ignoreCase = true) || host.contains("n1mwq", ignoreCase = true) -> "filemoon"
    host.contains("dood", ignoreCase = true) || host.contains("playmogo", ignoreCase = true) -> "dood"
    else -> null
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewStreamDialog(
    url: String,
    onStream: (String, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var captured by remember { mutableStateOf(false) }
    var webviewRef by remember { mutableStateOf<WebView?>(null) }
    val startedAt = remember(url) { System.currentTimeMillis() }
    val startHost = remember(url) { Uri.parse(url).host.orEmpty() }
    val seamless = remember(url) { familyOf(Uri.parse(url).host.orEmpty()) == "dood" }

    LaunchedEffect(url) {
        delay(TIMEOUT_MS)
        onDismiss()
    }

    LaunchedEffect(url, webviewRef) {
        val webview = webviewRef ?: return@LaunchedEffect
        while (!captured) {
            delay(2_000)
            if (System.currentTimeMillis() - startedAt > 3_000 && webview.width > 0) {
                webview.evaluateJavascript(
                    "(function(){var f=document.querySelector('iframe');if(!f)return '';var r=f.getBoundingClientRect();return [r.left,r.top,r.width,r.height].join(',');})()"
                ) { value ->
                    val parts = value?.trim()?.removeSurrounding("\"")
                        ?.split(",")?.mapNotNull { it.toFloatOrNull() }
                    if (!captured && parts != null && parts.size == 4) {
                        val density = webview.resources.displayMetrics.density
                        val x = (parts[0] + parts[2] / 2f) * density
                        val y = (parts[1] + parts[3] / 2f) * density
                        tapAt(webview, x, y)
                    }
                }
            }
            webview.evaluateJavascript(
                "(function(){var v=document.querySelector('video');if(!v)return '';return v.src||v.currentSrc||'';})()"
            ) { value ->
                val src = value?.trim()?.removeSurrounding("\"")
                if (!captured && !src.isNullOrEmpty() && src.startsWith("http") && !src.contains("blob:")) {
                    captured = true
                    onStream(src, null)
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
                                        "(function(){['.captcha-gate__play','.vjs-big-play-button','.captcha_l','.jw-icon-display','[class*=play-button]'].forEach(function(s){var e=document.querySelector(s);if(e)e.click();});})()",
                                        null
                                    )
                                }

                                override fun shouldInterceptRequest(
                                    view: WebView,
                                    request: WebResourceRequest
                                ): WebResourceResponse? {
                                    val target = request.url.toString()
                                    if (!captured &&
                                        request.url.host.orEmpty().contains("n1mwq") &&
                                        !request.isForMainFrame &&
                                        request.requestHeaders["Accept"]?.contains("text/html") == true
                                    ) {
                                        return injectedGateResponse(target)
                                    }
                                    val isMedia = target.contains(".m3u8") || target.contains(".mp4")
                                    val warmedUp = loadStartedAt == 0L ||
                                        System.currentTimeMillis() - loadStartedAt >= WARMUP_MS
                                    if (!captured && isMedia && warmedUp) {
                                        captured = true
                                        val referer = request.requestHeaders.entries
                                            .firstOrNull { it.key.equals("referer", ignoreCase = true) }
                                            ?.value
                                        onStream(target, referer)
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

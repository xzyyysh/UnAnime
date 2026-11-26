package com.notanime.es.extractor

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.notanime.es.util.UnAnimeLog
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import java.io.IOException
import java.net.URI
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object WebViewStream {

    private const val TIMEOUT_MS = 30_000L
    private const val WARMUP_MS = 2_500L

    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    suspend fun resolve(url: String): StreamInfo = withTimeout(TIMEOUT_MS) {
        val captured = awaitStreamUrl(url)
        val referer = runCatching {
            val uri = URI(url)
            "${uri.scheme}://${uri.host}/"
        }.getOrDefault("")
        UnAnimeLog.d("webview-stream", "captured $captured")
        if (captured.contains(".m3u8")) {
            StreamInfo(
                host = labelOf(url),
                videoId = null,
                qualities = Hls.masterWithVariants(captured, referer),
                referer = referer
            )
        } else {
            StreamInfo(
                host = labelOf(url),
                videoId = null,
                qualities = listOf(StreamQuality("original", captured)),
                referer = referer
            )
        }
    }

    private fun labelOf(url: String): String = when {
        url.contains("filemoo", ignoreCase = true) -> "filemoon"
        url.contains("dood", ignoreCase = true) -> "dood"
        else -> runCatching { URI(url).host.orEmpty() }.getOrDefault("webview")
    }

    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun awaitStreamUrl(url: String): String = suspendCancellableCoroutine { cont ->
        val context = appContext
        if (context == null) {
            cont.resumeWithException(IOException("webview resolver not initialized"))
            return@suspendCancellableCoroutine
        }
        val done = AtomicBoolean(false)
        val startedAt = System.currentTimeMillis()
        fun cleanup(webview: WebView) {
            mainHandler.post {
                webview.stopLoading()
                webview.destroy()
            }
        }
        mainHandler.post {
            val webview = WebView(context)
            webview.settings.javaScriptEnabled = true
            webview.settings.domStorageEnabled = true
            webview.settings.mediaPlaybackRequiresUserGesture = false
            webview.setBackgroundColor(0x00000000)
            webview.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
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
                    val warmedUp = System.currentTimeMillis() - startedAt >= WARMUP_MS
                    if (!done.get() && isMedia && warmedUp) {
                        if (done.compareAndSet(false, true)) {
                            UnAnimeLog.d("webview-stream", "media request $target")
                            cleanup(view)
                            cont.resume(target)
                        }
                    }
                    return null
                }
            }
            webview.loadUrl(url)
            cont.invokeOnCancellation {
                if (done.compareAndSet(false, true)) {
                    cleanup(webview)
                }
            }
        }
    }
}

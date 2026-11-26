package com.notanime.es.extractor

import java.io.IOException
import java.net.URI

class WebViewRequiredException(val url: String) : IOException("webview interaction required")

object WebViewStream {

    @Volatile private var interactiveResolver: (suspend (String) -> StreamInfo)? = null

    fun setResolver(resolver: (suspend (String) -> StreamInfo)?) {
        interactiveResolver = resolver
    }

    suspend fun resolve(url: String): StreamInfo {
        if (url.contains("filemoo", ignoreCase = true)) {
            Filemoon.precheckError(url)?.let { throw IOException(it) }
        }
        val resolver = interactiveResolver ?: throw WebViewRequiredException(url)
        return resolver(url)
    }

    suspend fun buildInfo(url: String, mediaUrl: String, referer: String? = null): StreamInfo {
        val effectiveReferer = referer ?: runCatching {
            val uri = URI(url)
            "${uri.scheme}://${uri.host}/"
        }.getOrDefault("")
        return if (mediaUrl.contains(".m3u8")) {
            StreamInfo(
                host = labelOf(url),
                videoId = null,
                qualities = Hls.masterWithVariants(mediaUrl, effectiveReferer),
                referer = effectiveReferer
            )
        } else {
            StreamInfo(
                host = labelOf(url),
                videoId = null,
                qualities = listOf(StreamQuality("original", mediaUrl)),
                referer = effectiveReferer
            )
        }
    }

    private fun labelOf(url: String): String = when {
        url.contains("filemoo", ignoreCase = true) -> "filemoon"
        else -> "dood"
    }
}

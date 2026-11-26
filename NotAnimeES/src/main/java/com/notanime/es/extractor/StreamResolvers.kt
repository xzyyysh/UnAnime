package com.notanime.es.extractor

import java.io.IOException
import java.net.URI

object StreamResolvers {

    private val resolvers: Map<String, suspend (String) -> StreamInfo> = mapOf(
        "gupload.site" to { url -> Gupload.resolve(url) },
        "streamtape.com" to { url -> Streamtape.resolve(url) },
        "streamtape.to" to { url -> Streamtape.resolve(url) },
        "uqload.vc" to { url -> Uqload.resolve(url) },
        "mp4upload.com" to { url -> Mp4upload.resolve(url) },
        "www.mp4upload.com" to { url -> Mp4upload.resolve(url) },
        "savefiles.top" to { url -> Savefiles.resolve(url) },
        "vidara.to" to { url -> Vidara.resolve(url) },
        "hexload.com" to { url -> Hexload.resolve(url) }
    )

    private val resolverPatterns: List<Pair<Regex, suspend (String) -> StreamInfo>> = listOf(
        Regex("uqload", RegexOption.IGNORE_CASE) to { url -> Uqload.resolve(url) },
        Regex("filemoo", RegexOption.IGNORE_CASE) to { url -> WebViewStream.resolve(url) },
        Regex("dood", RegexOption.IGNORE_CASE) to { url -> WebViewStream.resolve(url) }
    )

    val skippedHosts: Map<String, String> = mapOf(
        "mxdrop.sx" to "captcha-gate",
        "vixeo.io" to "host-down-522",
        "playmate.to" to "host-down-525",
        "anonstream.co" to "untested"
    )

    private val skippedHostPatterns: List<Pair<Regex, String>> = listOf(
        Regex("mxdrop", RegexOption.IGNORE_CASE) to "captcha-gate"
    )

    fun hostOf(url: String): String? = try {
        URI(url).host?.lowercase()
    } catch (_: Exception) {
        null
    }

    fun resolverFor(url: String): (suspend (String) -> StreamInfo)? {
        val host = hostOf(url) ?: return null
        resolvers[host]?.let { return it }
        return resolverPatterns.firstOrNull { it.first.containsMatchIn(host) }?.second
    }

    suspend fun resolveStream(url: String): StreamInfo {
        val resolver = resolverFor(url)
            ?: throw IOException("no resolver for host ${hostOf(url) ?: "unknown"}")
        return resolver(url)
    }

    fun skipReasonFor(url: String): String? {
        val host = hostOf(url) ?: return null
        skippedHosts[host]?.let { return it }
        return skippedHostPatterns.firstOrNull { it.first.containsMatchIn(host) }?.second
    }
}

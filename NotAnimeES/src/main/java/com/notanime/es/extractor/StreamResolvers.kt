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

    val skippedHosts: Map<String, String> = mapOf(
        "mxdrop.sx" to "captcha-gate",
        "vixeo.io" to "host-down-522",
        "playmate.to" to "host-down-525",
        "filemoon0.top" to "antibot-pow",
        "anonstream.co" to "untested"
    )

    fun hostOf(url: String): String? = try {
        URI(url).host?.lowercase()
    } catch (_: Exception) {
        null
    }

    fun resolverFor(url: String): (suspend (String) -> StreamInfo)? =
        hostOf(url)?.let { resolvers[it] }

    suspend fun resolveStream(url: String): StreamInfo {
        val resolver = resolverFor(url)
            ?: throw IOException("no resolver for host ${hostOf(url) ?: "unknown"}")
        return resolver(url)
    }

    fun skipReasonFor(url: String): String? = hostOf(url)?.let { skippedHosts[it] }
}

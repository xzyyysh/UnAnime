package com.notanime.es.extractor

import com.notanime.es.extractor.FormDl.fetchEmbedAndPostDl
import com.notanime.es.extractor.FormDl.retry3
import com.notanime.es.util.UnAnimeLog
import java.io.IOException

object Savefiles {

    private const val DL_URL = "https://savefiles.top/dl"

    private val masterRegex = Regex("sources:\\s*\\[\\s*\\{\\s*file:\\s*\"([^\"]+\\.m3u8[^\"]*)\"")
    private val fileCodeRegex = Regex("/e/([^/?#]+)")

    private suspend fun resolveOnce(embedUrl: String, code: String): StreamInfo {
        UnAnimeLog.d("savefiles", "resolving $code")
        val result = fetchEmbedAndPostDl(embedUrl, DL_URL, code)
        val master = masterRegex.find(result.html)?.groupValues?.get(1)
            ?: throw IOException("savefiles: stream not found")
        return StreamInfo(
            host = "savefiles",
            videoId = code,
            qualities = Hls.masterWithVariants(master, "https://savefiles.top/")
        ).also { UnAnimeLog.d("savefiles", "ok: ${it.qualities.size} qualities") }
    }

    suspend fun resolve(url: String): StreamInfo {
        val code = fileCodeRegex.find(url)?.groupValues?.get(1)
            ?: throw IOException("savefiles: no file code")
        return retry3("savefiles") { resolveOnce(url, code) }
    }
}

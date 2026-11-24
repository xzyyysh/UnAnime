package com.notanime.es.extractor

import com.notanime.es.net.CronetHttp
import com.notanime.es.util.UnAnimeLog
import java.io.IOException

object Mp4upload {

    private const val REFERER = "https://ww4.animeonline.ninja/"

    private val srcRegex = Regex("src:\\s*\"([^\"]+\\.mp4)\"")
    private val fallbackRegex = Regex("https?:[^\"'\\s]+\\.mp4")
    private val videoIdRegex = Regex("embed-([a-z0-9]+)")

    suspend fun resolve(url: String): StreamInfo {
        UnAnimeLog.d("mp4upload", "resolving $url")
        val response = CronetHttp.get(url, mapOf("referer" to REFERER))
        if (!response.isSuccessful) throw IOException("mp4upload ${response.code}")
        val html = response.body
        val src = srcRegex.find(html)?.groupValues?.get(1)
            ?: fallbackRegex.find(html)?.value
            ?: throw IOException("mp4upload: stream not found")
        return StreamInfo(
            host = "mp4upload",
            videoId = videoIdRegex.find(url)?.groupValues?.get(1),
            qualities = listOf(StreamQuality("original", src)),
            referer = "https://www.mp4upload.com/"
        ).also { UnAnimeLog.d("mp4upload", "ok: original") }
    }
}

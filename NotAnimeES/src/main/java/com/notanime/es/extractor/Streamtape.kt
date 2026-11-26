package com.notanime.es.extractor

import com.notanime.es.net.CronetHttp
import kotlinx.coroutines.delay
import java.io.IOException
import com.notanime.es.util.UnAnimeLog

object Streamtape {

    private data class LinkPart(
        val prefix: String,
        val junk: String,
        val ops: List<Int>
    )

    private val assignmentRegex = Regex(
        "getElementById\\('[^']*'\\)\\.innerHTML\\s*=\\s*(['\"])([^'\"]*)\\1\\s*\\+\\s*(?:''\\s*\\+\\s*)?\\(?\\s*(['\"])([^'\"]*)\\3\\s*\\)?((?:\\.substring\\(\\d+\\))*)"
    )
    private val substringRegex = Regex("\\.substring\\((\\d+)\\)")
    private val urlPrefixRegex = Regex("^/{1,2}streamtape\\.com/get_video\\?")
    private val tokenRegex = Regex("token=([A-Za-z0-9]+)")
    private val videoIdRegex = Regex("/(?:e|v)/([^/?#]+)")

    private fun parseAssignments(html: String): List<LinkPart> {
        return assignmentRegex.findAll(html).map { match ->
            LinkPart(
                prefix = match.groupValues[2],
                junk = match.groupValues[4],
                ops = substringRegex.findAll(match.groupValues[5]).map { it.groupValues[1].toInt() }.toList()
            )
        }.toList()
    }

    private fun evaluate(part: LinkPart, videoId: String?): String? {
        var value = part.junk
        for (op in part.ops) {
            if (op >= value.length) return null
            value = value.substring(op)
        }
        val url = part.prefix + value
        if (!urlPrefixRegex.containsMatchIn(url)) return null
        if (videoId != null && !url.contains("id=$videoId&")) return null
        val token = tokenRegex.find(url)?.groupValues?.get(1) ?: return null
        return "https:$url"
    }

    private suspend fun resolveOnce(url: String): StreamInfo {
        val response = CronetHttp.get(url)
        if (!response.isSuccessful) throw IOException("streamtape ${response.code}")
        val html = response.body
        val videoId = videoIdRegex.find(url)?.groupValues?.get(1)
        val links = LinkedHashSet<String>()
        for (part in parseAssignments(html)) {
            evaluate(part, videoId)?.let { links.add(it) }
        }
        if (links.isEmpty()) throw IOException("streamtape: no link found")
        return StreamInfo(
            host = "streamtape",
            videoId = videoId,
            qualities = listOf(StreamQuality("original", links.first()))
        )
    }

    suspend fun resolve(url: String): StreamInfo {
        UnAnimeLog.d("streamtape", "resolving $url")
        var lastError: Exception = IOException("streamtape: no link found")
        repeat(3) { attempt ->
            try {
                return resolveOnce(url).also { UnAnimeLog.d("streamtape", "ok: link resolved") }
            } catch (e: Exception) {
                lastError = e
                if (attempt < 2) {
                    UnAnimeLog.w("streamtape", "attempt ${attempt + 1} failed", e)
                    delay(1500)
                }
            }
        }
        throw lastError
    }
}

package com.notanime.es.extractor

import com.notanime.es.net.WebViewHttp
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.json.JSONObject
import java.io.IOException
import java.util.Base64
import com.notanime.es.util.UnAnimeLog

object Gupload {

    private val QUALITIES = listOf("1080p", "720p", "480p", "360p")

    private val keyGroupRegex = Regex("_p=\\[([^\\]]+)\\]")
    private val keyPartRegex = Regex("'([^']*)'")
    private val encodedRegex = Regex("'([a-f0-9]{8}~[A-Za-z0-9+/=\\\\]{40,})'")
    private val m3u8SuffixRegex = Regex("[^/]+\\.m3u8$")

    private fun decode(encoded: String, key: String): String? {
        val parts = encoded.split("~")
        if (parts.size < 2) return null
        val bytes = try {
            Base64.getDecoder().decode(parts[1].replace("\\", ""))
        } catch (_: Exception) {
            return null
        }
        return buildString {
            for (i in bytes.indices) {
                append(((bytes[i].toInt() and 0xFF) xor key[i % key.length].code).toChar())
            }
        }
    }

    private fun extractKey(html: String): String? {
        val group = keyGroupRegex.find(html)?.groupValues?.get(1) ?: return null
        val parts = keyPartRegex.findAll(group).map { it.groupValues[1] }.toList()
        return parts.joinToString("").takeIf { it.isNotEmpty() }
    }

    private suspend fun checkQuality(base: String, quality: String): StreamQuality? {
        return try {
            val response = WebViewHttp.get("$base$quality.m3u8")
            if (response.isSuccessful && response.body.trimStart().startsWith("#EXTM3U")) {
                StreamQuality(quality, "$base$quality.m3u8")
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun resolve(url: String): StreamInfo {
        UnAnimeLog.d("gupload", "resolving $url")
        val response = WebViewHttp.get(url)
        if (!response.isSuccessful) throw IOException("gupload ${response.code} $url")
        val html = response.body
        val key = extractKey(html) ?: throw IOException("gupload: key not found")
        var videoUrl: String? = null
        var videoId: String? = null
        for (match in encodedRegex.findAll(html)) {
            val decoded = decode(match.groupValues[1], key) ?: continue
            if (!decoded.contains("videoUrl")) continue
            val cfg = try {
                JSONObject(decoded)
            } catch (_: Exception) {
                continue
            }
            val candidate = cfg.optString("videoUrl")
            if (candidate.isNotEmpty()) {
                videoUrl = candidate
                videoId = if (cfg.has("videoId") && !cfg.isNull("videoId")) {
                    cfg.get("videoId").toString()
                } else {
                    null
                }
            }
        }
        val finalUrl = videoUrl ?: throw IOException("gupload: videoUrl not found")
        val base = finalUrl.replace(m3u8SuffixRegex, "")
        val qualities = coroutineScope {
            QUALITIES.map { quality -> async { checkQuality(base, quality) } }.awaitAll().filterNotNull()
        }.sortedByDescending { QUALITIES.indexOf(it.label) }
        UnAnimeLog.d("gupload", "ok: ${qualities.size} qualities")
        return StreamInfo(host = "gupload", videoId = videoId, qualities = qualities)
    }
}

package com.notanime.es.extractor

import com.notanime.es.extractor.FormDl.retry3
import com.notanime.es.net.WebViewHttp
import com.notanime.es.util.UnAnimeLog
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

object Vidara {

    private const val STREAM_URL = "https://vidara.to/api/stream"

    private val fileCodeRegex = Regex("/e/([^/?#]+)")

    private suspend fun resolveOnce(embedUrl: String, code: String): StreamInfo {
        UnAnimeLog.d("vidara", "resolving $code")
        val response = WebViewHttp.post(
            url = STREAM_URL,
            body = JSONObject().put("filecode", code).put("device", "web").put("codecs", JSONArray()).toString(),
            contentType = "application/json",
            headers = mapOf("referer" to embedUrl)
        )
        if (!response.isSuccessful) throw IOException("vidara ${response.code}")
        val master = JSONObject(response.body).optString("streaming_url")
        if (master.isEmpty()) throw IOException("vidara: stream not found")
        return StreamInfo(
            host = "vidara",
            videoId = code,
            qualities = Hls.masterWithVariants(master, "https://vidara.to/")
        ).also { UnAnimeLog.d("vidara", "ok: ${it.qualities.size} qualities") }
    }

    suspend fun resolve(url: String): StreamInfo {
        val code = fileCodeRegex.find(url)?.groupValues?.get(1)
            ?: throw IOException("vidara: no file code")
        return retry3("vidara") { resolveOnce(url, code) }
    }
}

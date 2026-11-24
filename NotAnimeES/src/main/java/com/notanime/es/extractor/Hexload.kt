package com.notanime.es.extractor

import com.notanime.es.extractor.FormDl.retry3
import com.notanime.es.net.WebViewHttp
import com.notanime.es.util.UnAnimeLog
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder

object Hexload {

    private const val DOWNLOAD_URL = "https://hexload.com/download"

    private val fileIdRegex = Regex("embed-([a-z0-9]+)")

    private suspend fun resolveOnce(embedUrl: String, id: String): StreamInfo {
        UnAnimeLog.d("hexload", "resolving $id")
        val body = mapOf(
            "op" to "download3",
            "id" to id,
            "ajax" to "1",
            "method_free" to "1",
            "dataType" to "json"
        ).entries.joinToString("&") { (key, value) ->
            "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
        }
        val response = WebViewHttp.post(
            url = DOWNLOAD_URL,
            body = body,
            contentType = "application/x-www-form-urlencoded",
            headers = mapOf("referer" to embedUrl)
        )
        if (!response.isSuccessful) throw IOException("hexload ${response.code}")
        val data = JSONObject(response.body)
        val streamUrl = data.optJSONObject("result")?.optString("url")
        if (data.optString("msg") != "OK" || streamUrl.isNullOrEmpty()) {
            throw IOException("hexload: stream not found")
        }
        return StreamInfo(
            host = "hexload",
            videoId = id,
            qualities = listOf(StreamQuality("original", streamUrl))
        ).also { UnAnimeLog.d("hexload", "ok: original") }
    }

    suspend fun resolve(url: String): StreamInfo {
        val id = fileIdRegex.find(url)?.groupValues?.get(1)
            ?: throw IOException("hexload: no file id")
        return retry3("hexload") { resolveOnce(url, id) }
    }
}

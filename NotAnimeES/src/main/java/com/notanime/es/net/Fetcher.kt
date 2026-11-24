package com.notanime.es.net

import com.notanime.es.util.UnAnimeLog
import org.json.JSONObject
import java.io.IOException

interface Fetcher {
    suspend fun html(url: String): String
    suspend fun json(url: String): JSONObject
}

class DirectFetcher : Fetcher {

    override suspend fun html(url: String): String = fetch(url)

    override suspend fun json(url: String): JSONObject = JSONObject(fetch(url))

    private suspend fun fetch(url: String): String {
        val response = WebViewHttp.get(url)
        if (!response.isSuccessful) {
            UnAnimeLog.e("net", "fetch failed ${response.code} $url [${challengeMarker(response.body)}] ${response.body.take(250).replace(Regex("\\s+"), " ")}")
            throw IOException("direct ${response.code} $url")
        }
        return response.body
    }

    private fun challengeMarker(body: String): String = when {
        body.contains("cdn-cgi/challenge-platform", ignoreCase = true) -> "challenge-js"
        body.contains("cf-challenge", ignoreCase = true) -> "challenge-managed"
        body.contains("Attention Required", ignoreCase = true) -> "access-denied"
        body.contains("error code: 1020", ignoreCase = true) -> "ip-blocked-1020"
        body.contains("Just a moment", ignoreCase = true) -> "challenge"
        else -> "unknown"
    }
}

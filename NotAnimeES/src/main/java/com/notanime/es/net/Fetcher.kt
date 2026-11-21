package com.notanime.es.net

import com.notanime.es.NotAnimeConfig
import com.notanime.es.util.UnAnimeLog
import org.json.JSONObject
import java.io.IOException

interface Fetcher {
    suspend fun html(url: String): String
    suspend fun json(url: String): JSONObject
}

class DirectFetcher(
    private val userAgent: String? = null,
    private val cookieProvider: (() -> String?)? = null
) : Fetcher {

    override suspend fun html(url: String): String =
        fetch(url, accept = "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8", dest = "document", site = "none")

    override suspend fun json(url: String): JSONObject =
        JSONObject(fetch(url, accept = "application/json", dest = "empty", site = "same-origin"))

    private suspend fun fetch(url: String, accept: String, dest: String, site: String): String {
        val ua = userAgent ?: CronetHttp.USER_AGENT
        val chromeVersion = CHROME_VERSION.find(ua)?.groupValues?.get(1) ?: "157"
        val headers = buildMap {
            put("accept", accept)
            put("accept-language", "es-ES,es;q=0.9,en;q=0.8")
            put("sec-ch-ua", "\"Chromium\";v=\"$chromeVersion\", \"Google Chrome\";v=\"$chromeVersion\", \"Not?A_Brand\";v=\"99\"")
            put("sec-ch-ua-mobile", "?1")
            put("sec-ch-ua-platform", "\"Android\"")
            put("sec-fetch-dest", dest)
            put("sec-fetch-mode", if (dest == "document") "navigate" else "cors")
            put("sec-fetch-site", site)
            if (cookieProvider != null && url.startsWith(NotAnimeConfig.ORIGIN)) {
                cookieProvider.invoke()?.let { put("cookie", it) }
            }
        }
        val response = CronetHttp.get(url, headers, userAgent)
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

    private companion object {
        val CHROME_VERSION = Regex("""Chrome/(\d+)""")
    }
}

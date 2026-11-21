package com.notanime.es.scraper

import com.notanime.es.NotAnimeConfig
import com.notanime.es.net.Fetcher
import org.json.JSONObject
import java.net.URLEncoder
import com.notanime.es.util.UnAnimeLog

data class SearchResult(
    val title: String,
    val url: String,
    val image: String,
    val rating: String?
)

object SearchScraper {

    private const val NONCE_TTL_MS = 30 * 60 * 1000L

    private var nonceCache: Pair<String, Long>? = null

    private suspend fun getNonce(fetcher: Fetcher): String {
        nonceCache?.let { (value, at) ->
            if (System.currentTimeMillis() - at < NONCE_TTL_MS) {
                UnAnimeLog.d("search", "nonce cache hit")
                return value
            }
        }
        val html = fetcher.html("${NotAnimeConfig.ORIGIN}/0/")
        val nonce = HomeScraper.extractNonce(html) ?: throw IllegalStateException("nonce not found")
        UnAnimeLog.d("search", "nonce refreshed")
        nonceCache = nonce to System.currentTimeMillis()
        return nonce
    }

    suspend fun search(fetcher: Fetcher, keyword: String): List<SearchResult> {
        val q = keyword.trim()
        if (q.length < 3) return emptyList()
        val nonce = getNonce(fetcher)
        val encoded = URLEncoder.encode(q, "UTF-8").replace("+", "%20")
        val data = fetcher.json(
            "${NotAnimeConfig.ORIGIN}/wp-json/dooplay/search?keyword=$encoded&nonce=$nonce"
        )
        if (data.has("error")) {
            UnAnimeLog.w("search", "site returned error for \"$q\"")
            return emptyList()
        }
        val results = mutableListOf<SearchResult>()
        val keys = data.keys()
        while (keys.hasNext()) {
            val entry = data.optJSONObject(keys.next()) ?: continue
            val url = entry.optString("url")
            if (url.isEmpty()) continue
            val imdb = entry.optJSONObject("extra")?.opt("imdb") as? String
            results.add(
                SearchResult(
                    title = entry.optString("title"),
                    url = url,
                    image = entry.optString("img"),
                    rating = imdb
                )
            )
        }
        return results.also {
            UnAnimeLog.d("search", "\"$q\" -> ${it.size} results")
        }
    }
}

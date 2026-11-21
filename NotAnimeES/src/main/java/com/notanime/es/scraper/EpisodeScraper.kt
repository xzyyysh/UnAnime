package com.notanime.es.scraper

import com.notanime.es.NotAnimeConfig
import com.notanime.es.net.Fetcher
import com.notanime.es.util.UnAnimeLog
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

data class ServerOption(
    val nume: Int,
    val label: String
)

data class EmbedServer(
    val name: String,
    val lang: String,
    val url: String,
    val icon: String = "",
    val description: String = ""
)

data class EpisodeNav(
    val prev: String?,
    val next: String?,
    val titleUrl: String?
)

data class EpisodeData(
    val postId: Int,
    val type: String,
    val servers: List<ServerOption>,
    val nav: EpisodeNav,
    val seasonEpisodes: List<SeasonEpisodes>,
    val embedUrl: String?,
    val embedServers: List<EmbedServer>,
    val image: String? = null,
    val title: String? = null
)

object EpisodeScraper {

    private val playerUrlRegex = Regex("go_to_player\\('([^']+)'\\)")
    private val odClassRegex = Regex("\\s+")

    fun parseEpisodePage(doc: Document): Triple<Int, String, Pair<List<ServerOption>, EpisodeNav>> {
        val first = doc.selectFirst("li.dooplay_player_option")
        val postId = first?.attr("data-post")?.toIntOrNull() ?: 0
        val type = first?.attr("data-type") ?: "tv"
        val servers = mutableListOf<ServerOption>()
        for (li in doc.select("li.dooplay_player_option")) {
            val nume = li.attr("data-nume").toIntOrNull() ?: 0
            if (nume != 0) {
                servers.add(ServerOption(nume, li.selectFirst(".title")?.text()?.trim().orEmpty()))
            }
        }
        var prev: String? = null
        var next: String? = null
        var titleUrl: String? = null
        for (el in doc.select(".pag_episodes a")) {
            val href = el.attr("href")
            val text = el.text().lowercase()
            when {
                text.contains("anterior") -> prev = href.takeIf { it != "#" }
                text.contains("siguiente") -> next = href.takeIf { it != "#" }
                href.contains("/online/") -> titleUrl = href
            }
        }
        return Triple(postId, type, servers to EpisodeNav(prev, next, titleUrl))
    }

    fun parseEmbed(html: String, embedUrl: String): List<EmbedServer> {
        val doc = Jsoup.parse(html)
        val servers = mutableListOf<EmbedServer>()
        var lang = ""
        for (el in doc.select("[onclick*=go_to_player]")) {
            val group = el.closest(".OD")
            if (group != null) {
                lang = group.className()
                    .split(odClassRegex)
                    .firstOrNull { it.startsWith("OD_") && it != "ODDIV" }
                    ?.substring(3)
                    ?.replace('_', ' ')
                    ?: lang
            }
            val url = playerUrlRegex.find(el.attr("onclick"))?.groupValues?.get(1)
            val name = el.selectFirst("span")?.text()?.trim().orEmpty()
            val description = el.selectFirst("p")?.text()?.trim().orEmpty()
            val icon = el.selectFirst("img")?.attr("src").orEmpty()
            val iconUrl = if (icon.startsWith("http") || icon.isEmpty()) {
                icon
            } else {
                runCatching { java.net.URI(embedUrl).resolve(icon).toString() }.getOrDefault(icon)
            }
            if (url != null) servers.add(EmbedServer(name, lang, url, iconUrl, description))
        }
        return servers
    }

    private val ogTitleSiteSuffixRegex = Regex("\\|\\s*[^|]+$")
    private val ogTitleTagRegex = Regex("【[^】]*】")

    fun cleanOgTitle(raw: String): String = raw
        .replace(ogTitleTagRegex, " ")
        .replace(ogTitleSiteSuffixRegex, "")
        .replace("▷", " ")
        .replace(odClassRegex, " ")
        .trim()

    suspend fun getEpisode(fetcher: Fetcher, episodeUrl: String): EpisodeData {
        val html = fetcher.html(episodeUrl)
        val doc = Jsoup.parse(html)
        val (postId, type, serversAndNav) = parseEpisodePage(doc)
        if (postId == 0) throw IllegalStateException("postId not found for $episodeUrl")
        val (servers, nav) = serversAndNav
        val seasonEpisodes = TitleScraper.parseSeasonBoxes(doc)
        val image = doc.selectFirst("meta[property=og:image]")?.attr("content")?.trim()?.takeIf { it.isNotEmpty() }
        val title = doc.selectFirst("meta[property=og:title]")?.attr("content")
            ?.let(::cleanOgTitle)?.takeIf { it.isNotEmpty() }
        UnAnimeLog.d("episode", "postId=$postId type=$type servers=${servers.size}")
        var embedUrl: String? = null
        try {
            val data = fetcher.json(
                "${NotAnimeConfig.ORIGIN}/wp-json/dooplayer/v1/post/$postId?type=$type&source=1"
            )
            embedUrl = data.optString("embed_url").takeIf { it.isNotEmpty() }
            UnAnimeLog.d("episode", "embed=$embedUrl")
        } catch (e: Exception) {
            UnAnimeLog.w("episode", "dooplayer failed for postId=$postId", e)
            embedUrl = null
        }
        val embedServers = if (embedUrl != null) {
            try {
                parseEmbed(fetcher.html(embedUrl), embedUrl).also {
                    UnAnimeLog.d("episode", "embed servers=${it.size}")
                }
            } catch (e: Exception) {
                UnAnimeLog.w("episode", "embed parse failed", e)
                emptyList()
            }
        } else {
            emptyList()
        }
        return EpisodeData(postId, type, servers, nav, seasonEpisodes, embedUrl, embedServers, image, title)
    }
}

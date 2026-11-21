package com.notanime.es.scraper

import com.notanime.es.NotAnimeConfig
import com.notanime.es.util.DebugCapture
import com.notanime.es.util.UnAnimeLog
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

data class TitleItem(
    val id: Int,
    val url: String,
    val title: String,
    val year: String?,
    val image: String,
    val type: String?,
    val rating: String? = null
)

data class EpisodeItem(
    val id: Int,
    val url: String,
    val anime: String,
    val episode: Int?,
    val image: String,
    val quality: String?
)

data class SectionMeta(
    val count: String?,
    val seeAll: String?
)

data class HomeData(
    val banner: List<TitleItem>,
    val latestEpisodes: List<EpisodeItem>,
    val onAir: List<TitleItem>,
    val latestAnimes: List<TitleItem>,
    val latestMovies: List<TitleItem>,
    val seasons: List<TitleItem>,
    val sectionMeta: Map<String, SectionMeta> = emptyMap()
)

object HomeScraper {

    private val SECTION_LATEST_EPISODES = "ÚLTIMOS EPISODIOS"
    private val SECTION_ON_AIR = "EN EMISIÓN"
    private val SECTION_LATEST_ANIMES = "ÚLTIMOS ANIMES AGREGADOS"
    private val SECTION_LATEST_MOVIES = "ÚLTIMAS PELICULAS AGREGADAS"
    private val SECTION_SEASONS = "TEMPORADAS"

    private val postIdRegex = Regex("post-(\\d+)")
    private val episodeNumberRegex = Regex("(\\d+)")
    private val wpSizeRegex = Regex("-(\\d+)x(\\d+)\\.(jpg|jpeg|png|webp)$")
    private val nonceRegex = Regex("""dtGonza = \{"api":[^}]*?"nonce":"([a-f0-9]+)"""")

    private fun absolute(url: String): String {
        if (url.isEmpty() || url.startsWith("data:")) return ""
        return when {
            url.startsWith("http") -> url
            url.startsWith("//") -> "https:$url"
            else -> "${NotAnimeConfig.ORIGIN}$url"
        }
    }

    private fun imageOf(article: Element, poster: Boolean = false): String {
        val img = article.selectFirst("img") ?: return ""
        val url = sequenceOf(img.attr("data-src"), img.attr("src"))
            .firstOrNull { it.isNotEmpty() && !it.startsWith("data:") }
            ?: return ""
        val resolved = absolute(url)
        return if (poster) upscalePoster(resolved) else resolved
    }

    private fun upscalePoster(url: String): String {
        val match = wpSizeRegex.find(url) ?: return url
        val width = match.groupValues[1].toInt()
        val height = match.groupValues[2].toInt()
        if (height <= width) return url
        return url.replaceRange(match.range, ".${match.groupValues[3]}")
    }

    fun parseTitle(article: Element): TitleItem? {
        val link = article.selectFirst(".data h3 a[href], a[href]") ?: return null
        val href = link.attr("href")
        val id = postIdRegex.find(article.attr("id"))?.groupValues?.get(1)?.toInt() ?: 0
        val cls = article.className()
        val kind = when {
            cls.contains("movies") -> "movies"
            cls.contains("seasons") -> "seasons"
            cls.contains("tvshows") -> "tvshows"
            else -> null
        }
        val type = article.selectFirst(".item_type")?.text()?.trim()
        return TitleItem(
            id = id,
            url = absolute(href),
            title = article.selectFirst("h3.title, h3")?.text()?.trim().orEmpty(),
            year = article.selectFirst(".data span")?.text()?.trim()?.takeIf { it.isNotEmpty() && it.all { ch -> ch.isDigit() } },
            image = imageOf(article, poster = true),
            type = type?.takeIf { it.isNotEmpty() } ?: kind,
            rating = article.selectFirst(".rating")?.text()?.trim()?.takeIf { it.isNotEmpty() }
        )
    }

    fun parseEpisode(article: Element): EpisodeItem? {
        val href = article.selectFirst("a[href*=\"/episodio/\"]")?.attr("href") ?: return null
        val id = postIdRegex.find(article.attr("id"))?.groupValues?.get(1)?.toInt() ?: 0
        val epText = article.selectFirst(".epiposter h4, h4")?.text()?.trim().orEmpty()
        return EpisodeItem(
            id = id,
            url = absolute(href),
            anime = article.selectFirst(".data h3, h3")?.text()?.trim().orEmpty(),
            episode = episodeNumberRegex.find(epText)?.groupValues?.get(1)?.toInt(),
            image = imageOf(article),
            quality = article.selectFirst(".quality")?.text()?.trim()?.takeIf { it.isNotEmpty() }
        )
    }

    private fun sectionArticles(doc: Document, key: String): List<Element> {
        for (h2 in doc.select("h2")) {
            if (!h2.text().trim().contains(key)) continue
            var node = h2.parent()?.nextElementSibling()
            while (node != null) {
                val articles = node.select("article")
                if (articles.isNotEmpty()) return articles.toList()
                node = node.nextElementSibling()
            }
            return emptyList()
        }
        return emptyList()
    }

    private fun titles(doc: Document, key: String): List<TitleItem> {
        val seen = mutableSetOf<String>()
        val out = mutableListOf<TitleItem>()
        for (article in sectionArticles(doc, key)) {
            if (article.hasClass("episodes")) continue
            val item = parseTitle(article)
            if (item != null && item.title.isNotEmpty() && seen.add(item.url)) {
                out.add(item)
            }
        }
        return out
    }

    private fun sectionMeta(doc: Document): Map<String, SectionMeta> {
        val keys = listOf(
            "EPISODIOS" to "episodes",
            "EMISIÓN" to "onAir",
            "ANIMES" to "animes",
            "PELICULAS" to "movies",
            "TEMPORADAS" to "seasons"
        )
        val out = mutableMapOf<String, SectionMeta>()
        for (h2 in doc.select("h2")) {
            val text = h2.text().trim().uppercase()
            val key = keys.firstOrNull { text.contains(it.first) }?.second ?: continue
            val header = h2.parent() ?: continue
            val span = header.selectFirst("span") ?: continue
            val count = span.ownText().trim().takeIf { it.any { ch -> ch.isDigit() } }
            val seeAll = header.selectFirst("a.see-all[href]")?.attr("abs:href")?.takeIf { it.isNotEmpty() }
            out[key] = SectionMeta(count, seeAll)
        }
        return out
    }

    fun parseHome(html: String): HomeData {
        val doc = org.jsoup.Jsoup.parse(html)
        UnAnimeLog.d("scrape", "home html: ${html.length} chars, ${doc.select("h2").size} h2, ${doc.select("article").size} articles")

        val banner = doc.select("div.animation-1.slider article.item")
            .mapNotNull { parseTitle(it) }
            .distinctBy { it.url }

        val seenEpisodes = mutableSetOf<Int>()
        val episodes = mutableListOf<EpisodeItem>()
        val episodeArticles = sectionArticles(doc, SECTION_LATEST_EPISODES)
        for (article in episodeArticles) {
            val item = parseEpisode(article)
            if (item != null && seenEpisodes.add(item.id)) {
                episodes.add(item)
            }
        }

        val onAir = titles(doc, SECTION_ON_AIR)
        val latestAnimes = titles(doc, SECTION_LATEST_ANIMES)
        val latestMovies = titles(doc, SECTION_LATEST_MOVIES)
        val seasons = titles(doc, SECTION_SEASONS)

        if (episodes.isEmpty() || onAir.isEmpty()) {
            val h2Texts = doc.select("h2").take(8).map { it.text().trim() }
            UnAnimeLog.w(
                "scrape",
                "home sections missing: episodes=${episodeArticles.size} articles -> ${episodes.size} parsed, onAir=${onAir.size}, h2s=$h2Texts"
            )
            DebugCapture.capture("home_variant.html", html)
        }

        return HomeData(
            banner = banner,
            latestEpisodes = episodes,
            onAir = onAir,
            latestAnimes = latestAnimes,
            latestMovies = latestMovies,
            seasons = seasons,
            sectionMeta = sectionMeta(doc)
        ).also {
            UnAnimeLog.d(
                "scrape",
                "home: banner=${it.banner.size}, ${it.latestEpisodes.size} episodes, ${it.onAir.size} on air, ${it.latestAnimes.size} animes, ${it.latestMovies.size} movies, ${it.seasons.size} seasons, meta=${it.sectionMeta.entries.joinToString { (k, v) -> "$k=${v.count}/${v.seeAll?.substringAfterLast('/')}" }}"
            )
        }
    }

    fun extractNonce(html: String): String? = nonceRegex.find(html)?.groupValues?.get(1)
}

package com.notanime.es.scraper

import com.notanime.es.NotAnimeConfig
import com.notanime.es.net.Fetcher
import com.notanime.es.util.UnAnimeLog
import org.jsoup.Jsoup

data class ArchivePage(
    val items: List<TitleItem>,
    val page: Int,
    val totalPages: Int
)

data class Genre(
    val slug: String,
    val name: String,
    val url: String
)

object CatalogScraper {

    private val paginationRegex = Regex("Página (\\d+) de (\\d+)")
    private val genreSlugRegex = Regex("/genero/([^/]+)/?$")
    private val trailingNumberRegex = Regex("-\\d+$")

    fun parseArchive(html: String): ArchivePage {
        val doc = Jsoup.parse(html)
        val seen = mutableSetOf<String>()
        val items = mutableListOf<TitleItem>()
        for (article in doc.select("article")) {
            val item = HomeScraper.parseTitle(article)
            if (item != null && item.title.isNotEmpty() && seen.add(item.url)) {
                items.add(item)
            }
        }
        val pagText = doc.selectFirst(".pagination span")?.text().orEmpty()
        val match = paginationRegex.find(pagText)
        return ArchivePage(
            items = items,
            page = match?.groupValues?.get(1)?.toInt() ?: 1,
            totalPages = match?.groupValues?.get(2)?.toInt() ?: 1
        ).also {
            UnAnimeLog.d("catalog", "archive: ${it.items.size} items, page ${it.page}/${it.totalPages}")
        }
    }

    suspend fun getArchive(fetcher: Fetcher, path: String, page: Int = 1): ArchivePage {
        val clean = path.trim('/')
        val url = if (page > 1) {
            "${NotAnimeConfig.ORIGIN}/$clean/page/$page/"
        } else {
            "${NotAnimeConfig.ORIGIN}/$clean/"
        }
        return parseArchive(fetcher.html(url))
    }

    suspend fun getGenres(fetcher: Fetcher): List<Genre> {
        val html = fetcher.html("${NotAnimeConfig.ORIGIN}/genero/accion/")
        val doc = Jsoup.parse(html)
        val genres = LinkedHashMap<String, Genre>()
        for (el in doc.select("a[href*=/genero/]")) {
            val href = el.attr("href")
            if (href.contains("/page/")) continue
            val slug = genreSlugRegex.find(href)?.groupValues?.get(1) ?: continue
            if (genres.containsKey(slug)) continue
            val name = el.text().trim().takeIf { it.isNotEmpty() }
                ?: slug.replace(trailingNumberRegex, "").replace('-', ' ')
            genres[slug] = Genre(slug, name, "${NotAnimeConfig.ORIGIN}/genero/$slug/")
        }
        return genres.values.sortedBy { it.name.lowercase() }.also {
            UnAnimeLog.d("catalog", "genres: ${it.size}")
        }
    }
}

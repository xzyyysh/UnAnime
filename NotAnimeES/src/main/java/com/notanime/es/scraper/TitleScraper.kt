package com.notanime.es.scraper

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import com.notanime.es.util.UnAnimeLog

data class GenreRef(
    val slug: String,
    val name: String
)

data class EpisodeEntry(
    val season: Int,
    val episode: Int,
    val url: String,
    val title: String,
    val date: String?,
    val image: String
)

data class SeasonEpisodes(
    val season: Int,
    val date: String?,
    val rating: String?,
    val episodes: List<EpisodeEntry>
)

data class CastMember(
    val name: String,
    val role: String,
    val image: String
)

data class TitleData(
    val id: Int,
    val url: String,
    val title: String,
    val originalTitle: String?,
    val poster: String,
    val synopsis: String?,
    val network: String?,
    val genres: List<GenreRef>,
    val rating: Rating,
    val tmdbRating: Rating,
    val seasonsCount: Int?,
    val episodesCount: Int?,
    val runtime: String?,
    val cast: List<CastMember>,
    val seasons: List<SeasonEpisodes>,
    val type: String = "tv"
)

data class Rating(
    val value: Double?,
    val votes: Int?
)

object TitleScraper {

    private val postIdRegex = Regex("postid-(\\d+)")
    private val seasonFromListRegex = Regex("(\\d+)\\s*-\\s*\\d+")
    private val numerandoRegex = Regex("(\\d+)\\s*-\\s*(\\d+)")
    private val whitespaceRegex = Regex("\\s+")
    private val dateRegex = Regex("([A-Z][a-z]{2}\\.\\s*\\d{1,2},\\s*\\d{4})")
    private val genreSlugRegex = Regex("/genero/([^/]+)/?$")
    private val tmdbValueRegex = Regex("([\\d.]+)")
    private val tmdbVotesRegex = Regex("(\\d+)\\s*votos")

    private fun absolute(url: String): String {
        if (url.isEmpty() || url.startsWith("data:")) return ""
        return if (url.startsWith("http")) url else "https://ww4.animeonline.ninja$url"
    }

    private fun img(el: Element): String {
        val image = el.selectFirst("img") ?: return ""
        val src = if (image.hasAttr("data-src")) image.attr("data-src") else image.attr("src")
        return absolute(src)
    }

    private fun customFields(doc: Document): Map<String, String> {
        val fields = mutableMapOf<String, String>()
        for (el in doc.select(".custom_fields")) {
            val key = el.selectFirst(".variante")?.text()?.trim()?.lowercase() ?: continue
            if (key.isNotEmpty()) {
                fields[key] = el.selectFirst(".valor")?.text()?.trim().orEmpty()
            }
        }
        return fields
    }

    fun parseSeasonBoxes(doc: Document): List<SeasonEpisodes> {
        val seasons = mutableListOf<SeasonEpisodes>()
        for (box in doc.select("#seasons .se-c")) {
            val firstNumerando = box.selectFirst(".numerando")?.text()?.trim().orEmpty()
            val seasonFromList = seasonFromListRegex.find(firstNumerando)?.groupValues?.get(1)?.toInt()
            val season = box.selectFirst(".se-t")?.text()?.trim()?.toIntOrNull() ?: seasonFromList ?: 0
            val titleText = box.selectFirst(".title")?.text()?.replace(whitespaceRegex, " ")?.trim().orEmpty()
            seasons.add(
                SeasonEpisodes(
                    season = season,
                    date = dateRegex.find(titleText)?.value,
                    rating = box.selectFirst(".se_rating")?.text()?.trim()?.takeIf { it.isNotEmpty() },
                    episodes = parseEpisodeList(box)
                )
            )
        }
        return seasons
    }

    private fun parseEpisodeList(box: Element): List<EpisodeEntry> {
        val episodes = mutableListOf<EpisodeEntry>()
        for (item in box.select("ul.episodios li")) {
            val numerando = item.selectFirst(".numerando")?.text()?.trim().orEmpty()
            val parts = numerandoRegex.find(numerando)
            val link = item.selectFirst(".episodiotitle a")
            episodes.add(
                EpisodeEntry(
                    season = parts?.groupValues?.get(1)?.toInt() ?: 0,
                    episode = parts?.groupValues?.get(2)?.toInt() ?: 0,
                    url = absolute(link?.attr("href").orEmpty()),
                    title = link?.text()?.trim().orEmpty(),
                    date = item.selectFirst(".date")?.text()?.trim()?.takeIf { it.isNotEmpty() },
                    image = img(item)
                )
            )
        }
        return episodes
    }

    fun parseTitlePage(html: String, url: String): TitleData {
        val doc = Jsoup.parse(html)
        val header = doc.selectFirst(".sheader")
        val id = postIdRegex.find(doc.body().attr("class"))?.groupValues?.get(1)?.toInt() ?: 0
        val fields = customFields(doc)
        val ratingValue = doc.selectFirst("[itemprop=ratingValue]")?.text()?.trim()?.toDoubleOrNull()
        val ratingVotes = doc.selectFirst("[itemprop=ratingCount]")?.text()?.trim()?.toIntOrNull()
        val tmdbRaw = fields["tmdb rating"].orEmpty()
        val tmdbMatch = tmdbValueRegex.find(tmdbRaw)
        val tmdbVotes = tmdbVotesRegex.find(tmdbRaw)

        val genres = mutableListOf<GenreRef>()
        for (el in doc.select(".sgeneros a")) {
            val slug = genreSlugRegex.find(el.attr("href"))?.groupValues?.get(1)
            if (slug != null) genres.add(GenreRef(slug, el.text().trim()))
        }

        val type = doc.selectFirst("li.dooplay_player_option")?.attr("data-type")?.takeIf { it.isNotEmpty() }
            ?: if (doc.body().attr("class").contains("single-movies")) "movie" else "tv"

        val cast = mutableListOf<CastMember>()
        for (person in doc.select(".person")) {
            cast.add(
                CastMember(
                    name = person.selectFirst(".name")?.text()?.trim().orEmpty(),
                    role = person.selectFirst(".caracter")?.text()?.trim().orEmpty(),
                    image = img(person)
                )
            )
        }

        return TitleData(
            id = id,
            url = url,
            title = header?.selectFirst("h1")?.text()?.trim().orEmpty(),
            originalTitle = fields["título original"],
            poster = img(header ?: doc.body()),
            synopsis = doc.selectFirst(".wp-content p")?.text()?.trim()?.takeIf { it.isNotEmpty() },
            network = header?.selectFirst(".extra a")?.text()?.trim()?.takeIf { it.isNotEmpty() },
            genres = genres,
            rating = Rating(ratingValue, ratingVotes),
            tmdbRating = Rating(
                value = tmdbMatch?.groupValues?.get(1)?.toDoubleOrNull(),
                votes = tmdbVotes?.groupValues?.get(1)?.toIntOrNull()
            ),
            seasonsCount = fields["temporadas"]?.toIntOrNull(),
            episodesCount = fields["episodios"]?.toIntOrNull(),
            runtime = fields["duración media"],
            cast = cast,
            seasons = parseSeasonBoxes(doc),
            type = type
        ).also {
            UnAnimeLog.d(
                "scrape",
                "title \"${it.title}\" id=${it.id} type=${it.type} seasons=${it.seasons.size} cast=${it.cast.size}"
            )
        }
    }
}

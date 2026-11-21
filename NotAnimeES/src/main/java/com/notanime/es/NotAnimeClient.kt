package com.notanime.es

import com.notanime.es.extractor.StreamInfo
import com.notanime.es.extractor.StreamResolvers
import com.notanime.es.net.DirectFetcher
import com.notanime.es.net.Fetcher
import com.notanime.es.scraper.ArchivePage
import com.notanime.es.scraper.CatalogScraper
import com.notanime.es.scraper.EmbedServer
import com.notanime.es.scraper.EpisodeData
import com.notanime.es.scraper.EpisodeScraper
import com.notanime.es.scraper.Genre
import com.notanime.es.scraper.HomeData
import com.notanime.es.scraper.HomeScraper
import com.notanime.es.scraper.SearchResult
import com.notanime.es.scraper.SearchScraper
import com.notanime.es.scraper.SourceGroup
import com.notanime.es.scraper.SourceServer
import com.notanime.es.scraper.SourcesResolver
import com.notanime.es.scraper.TitleData
import com.notanime.es.scraper.TitleScraper

class NotAnimeClient(private val fetcher: Fetcher = DirectFetcher()) {

    suspend fun home(): HomeData =
        HomeScraper.parseHome(fetcher.html("${NotAnimeConfig.ORIGIN}/inicio/"))

    suspend fun search(keyword: String): List<SearchResult> =
        SearchScraper.search(fetcher, keyword)

    suspend fun genres(): List<Genre> =
        CatalogScraper.getGenres(fetcher)

    suspend fun genre(slug: String, page: Int = 1): ArchivePage =
        CatalogScraper.getArchive(fetcher, "genero/$slug", page)

    suspend fun catalog(key: String, page: Int = 1): ArchivePage {
        val path = CATALOG_PATHS[key] ?: throw IllegalArgumentException(
            "unknown catalog $key, available: ${CATALOG_PATHS.keys}"
        )
        return CatalogScraper.getArchive(fetcher, path, page)
    }

    suspend fun archive(path: String, page: Int = 1): ArchivePage {
        val clean = when {
            path.startsWith("http") -> java.net.URI(path).path?.trim('/').orEmpty()
            else -> path.trim('/')
        }
        require(clean in CATALOG_PATHS.values || clean.startsWith("genero/")) {
            "unknown archive path $clean"
        }
        return CatalogScraper.getArchive(fetcher, clean, page)
    }

    suspend fun title(path: String): TitleData {
        val url = when {
            path.startsWith("http") -> path
            path.contains("/") -> "${NotAnimeConfig.ORIGIN}/$path"
            else -> "${NotAnimeConfig.ORIGIN}/online/$path"
        }
        return TitleScraper.parseTitlePage(fetcher.html(url), url)
    }

    suspend fun episode(path: String): EpisodeData {
        val url = if (path.startsWith("http")) path else "${NotAnimeConfig.ORIGIN}/episodio/$path"
        return EpisodeScraper.getEpisode(fetcher, url)
    }

    suspend fun episodeSources(path: String): List<SourceGroup> {
        val url = if (path.startsWith("http")) path else "${NotAnimeConfig.ORIGIN}/episodio/$path"
        return SourcesResolver.getSources(fetcher, url)
    }

    suspend fun episodeServerGroups(path: String): List<SourceGroup> {
        val url = if (path.startsWith("http")) path else "${NotAnimeConfig.ORIGIN}/episodio/$path"
        return SourcesResolver.listServers(fetcher, url)
    }

    suspend fun resolveServer(server: EmbedServer): SourceServer =
        SourcesResolver.resolveOne(server)

    suspend fun resolve(url: String): StreamInfo =
        StreamResolvers.resolveStream(url)

    companion object {
        val CATALOG_PATHS = mapOf(
            "animes" to "online",
            "movies" to "pelicula",
            "seasons" to "temporada",
            "episodes" to "episodio",
            "trending" to "tendencias",
            "ratings" to "ratings"
        )
    }
}

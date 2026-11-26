package com.notanime.es.scraper

import com.notanime.es.extractor.StreamErrors
import com.notanime.es.extractor.StreamInfo
import com.notanime.es.extractor.StreamResolvers
import com.notanime.es.net.Fetcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import com.notanime.es.util.UnAnimeLog
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

data class SourceServer(
    val name: String,
    val lang: String,
    val url: String,
    val host: String?,
    val resolvable: Boolean,
    val skipReason: String?,
    val stream: StreamInfo?,
    val error: String?,
    val icon: String = ""
)

data class SourceGroup(
    val lang: String,
    val servers: List<SourceServer>
)

object SourcesResolver {

    private const val CACHE_TTL_MS = 10 * 60 * 1000L
    private const val CONCURRENCY = 4
    private const val RETRY_DELAY_MS = 2000L

    private val cache = ConcurrentHashMap<String, Pair<Long, List<SourceGroup>>>()

    suspend fun getSources(fetcher: Fetcher, episodeUrl: String): List<SourceGroup> {
        cache[episodeUrl]?.let { (at, groups) ->
            if (System.currentTimeMillis() - at < CACHE_TTL_MS) {
                UnAnimeLog.d("sources", "cache hit $episodeUrl")
                return groups
            }
        }
        val groups = resolveSources(fetcher, episodeUrl)
        cache[episodeUrl] = System.currentTimeMillis() to groups
        UnAnimeLog.d("sources", "done: ${groups.size} language groups")
        return groups
    }

    suspend fun listServers(fetcher: Fetcher, episodeUrl: String): List<SourceGroup> {
        val episode = EpisodeScraper.getEpisode(fetcher, episodeUrl)
        UnAnimeLog.d("sources", "listing ${episode.embedServers.size} servers for $episodeUrl")
        return groupServers(episode.embedServers)
    }

    suspend fun resolveOne(server: EmbedServer): SourceServer =
        resolveServer(server, ConcurrentHashMap())

    private fun groupServers(servers: List<EmbedServer>): List<SourceGroup> {
        val groups = LinkedHashMap<String, MutableList<SourceServer>>()
        for (server in servers) {
            val host = StreamResolvers.hostOf(server.url)
            val resolvable = StreamResolvers.resolverFor(server.url) != null
            val skipReason = StreamResolvers.skipReasonFor(server.url)
            groups.getOrPut(server.lang) { mutableListOf() }.add(
                SourceServer(
                    name = server.name,
                    lang = server.lang,
                    url = server.url,
                    host = host,
                    resolvable = resolvable,
                    skipReason = skipReason,
                    stream = null,
                    error = null,
                    icon = server.icon
                )
            )
        }
        return groups.map { (lang, list) -> SourceGroup(lang, list) }
    }

    private suspend fun resolveSources(fetcher: Fetcher, episodeUrl: String): List<SourceGroup> {
        val episode = EpisodeScraper.getEpisode(fetcher, episodeUrl)
        UnAnimeLog.d("sources", "resolving ${episode.embedServers.size} servers for $episodeUrl")
        val hostErrors = ConcurrentHashMap<String, String>()
        val semaphore = Semaphore(CONCURRENCY)
        val resolved = coroutineScope {
            episode.embedServers.map { server ->
                async {
                    semaphore.withPermit { resolveServer(server, hostErrors) }
                }
            }.awaitAll()
        }
        val groups = LinkedHashMap<String, MutableList<SourceServer>>()
        for (server in resolved) {
            groups.getOrPut(server.lang) { mutableListOf() }.add(server)
        }
        return groups.map { (lang, servers) -> SourceGroup(lang, servers) }
    }

    private suspend fun resolveServer(
        server: EmbedServer,
        hostErrors: ConcurrentHashMap<String, String>
    ): SourceServer {
        val host = StreamResolvers.hostOf(server.url)
        val resolvable = StreamResolvers.resolverFor(server.url) != null
        val skipReason = StreamResolvers.skipReasonFor(server.url)
        val base = SourceServer(
            name = server.name,
            lang = server.lang,
            url = server.url,
            host = host,
            resolvable = resolvable,
            skipReason = skipReason,
            stream = null,
            error = null,
            icon = server.icon
        )
        if (!resolvable) {
            val probed = StreamErrors.probe(server.url)
            val reason = probed ?: skipReason
            UnAnimeLog.d("sources", "${server.name} skipped ($host): ${reason ?: "no resolver"}")
            return base.copy(skipReason = reason)
        }
        val priorError = host?.let { hostErrors[it] }
        if (priorError != null) {
            UnAnimeLog.d("sources", "${server.name} reused host error: $priorError")
            return base.copy(error = priorError)
        }
        return try {
            base.copy(stream = StreamResolvers.resolveStream(server.url)).also {
                UnAnimeLog.d("sources", "${server.name} ($host) ok: ${it.stream?.qualities?.size ?: 0} qualities")
            }
        } catch (first: Exception) {
            UnAnimeLog.w("sources", "${server.name} ($host) failed, retrying", first)
            try {
                delay(RETRY_DELAY_MS)
                base.copy(stream = StreamResolvers.resolveStream(server.url)).also {
                    UnAnimeLog.d("sources", "${server.name} ($host) ok on retry: ${it.stream?.qualities?.size ?: 0} qualities")
                }
            } catch (e: Exception) {
                val probed = StreamErrors.probe(server.url)
                val message = probed ?: e.message ?: e.toString()
                host?.let { hostErrors[it] = message }
                UnAnimeLog.e("sources", "${server.name} ($host) failed: $message", e)
                base.copy(error = message)
            }
        }
    }
}

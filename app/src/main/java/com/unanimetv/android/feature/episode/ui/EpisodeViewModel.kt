package com.unanimetv.android.feature.episode.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notanime.es.NotAnimeConfig
import com.notanime.es.scraper.EmbedServer
import com.notanime.es.scraper.SeasonEpisodes
import com.notanime.es.scraper.SourceServer
import com.unanimetv.android.core.network.ApiClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EpisodeViewModel(
    initialUrl: String
) : ViewModel() {

    private var currentUrl: String = initialUrl

    private val _uiState = MutableStateFlow(EpisodeUiState(url = initialUrl))
    val uiState: StateFlow<EpisodeUiState> = _uiState.asStateFlow()

    private var resolveJob: Job? = null

    init {
        load()
    }

    fun selectLanguage(lang: String) {
        _uiState.update { it.copy(selectedLanguage = lang, selectedServer = null) }
    }

    fun selectServer(server: SourceServer) {
        if (server.stream != null || server.error != null || !server.resolvable) {
            _uiState.update { it.copy(selectedServer = server, resolvingUrl = null) }
            return
        }
        _uiState.update { it.copy(selectedServer = server, resolvingUrl = server.url) }
        resolveJob?.cancel()
        resolveJob = viewModelScope.launch {
            try {
                val resolved = ApiClient.resolveServer(
                    EmbedServer(name = server.name, lang = server.lang, url = server.url, description = server.description)
                )
                _uiState.update { state ->
                    state.copy(
                        languages = state.languages.mapServers(server.url) { resolved },
                        selectedServer = resolved,
                        resolvingUrl = null
                    )
                }
            } catch (e: Exception) {
                val failed = server.copy(error = e.message ?: "Error al resolver")
                _uiState.update { state ->
                    state.copy(
                        languages = state.languages.mapServers(server.url) { failed },
                        selectedServer = failed,
                        resolvingUrl = null
                    )
                }
            }
        }
    }

    fun openEpisode(url: String) {
        if (url == currentUrl) return
        resolveJob?.cancel()
        currentUrl = url
        _uiState.value = EpisodeUiState(url = url)
        load()
    }

    fun reload() {
        resolveJob?.cancel()
        _uiState.value = EpisodeUiState(url = currentUrl)
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val episode = ApiClient.episode(currentUrl)
                val groups = ApiClient.episodeServerGroups(currentUrl)
                val entry = episode.seasonEpisodes
                    .flatMap { it.episodes }
                    .firstOrNull { it.url == currentUrl }
                _uiState.value = EpisodeUiState(
                    isLoading = false,
                    url = currentUrl,
                    title = entry?.title?.takeIf { it.isNotEmpty() } ?: episode.title,
                    titleUrl = episode.nav.titleUrl?.absoluteUrl(),
                    image = episode.image ?: entry?.image?.takeIf { it.isNotEmpty() },
                    prevUrl = episode.nav.prev?.absoluteUrl(),
                    nextUrl = episode.nav.next?.absoluteUrl(),
                    languages = groups.map { LanguageGroup(it.lang, it.servers) },
                    selectedLanguage = groups.firstOrNull()?.lang,
                    seasons = episode.seasonEpisodes,
                    gallery = episode.gallery
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Error inesperado")
                }
            }
        }
    }

    private fun String.absoluteUrl(): String =
        if (startsWith("http")) this else "${NotAnimeConfig.ORIGIN}$this"

    private fun List<LanguageGroup>.mapServers(
        url: String,
        transform: (SourceServer) -> SourceServer
    ): List<LanguageGroup> = map { group ->
        group.copy(servers = group.servers.map { if (it.url == url) transform(it) else it })
    }
}

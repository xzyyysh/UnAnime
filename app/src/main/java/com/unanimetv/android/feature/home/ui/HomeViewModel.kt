package com.unanimetv.android.feature.home.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notanime.es.util.UnAnimeLog
import com.unanimetv.android.core.network.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val home = ApiClient.home()
                _uiState.value = HomeUiState(
                    isLoading = false,
                    banner = home.banner.take(10).map {
                        AnimeSummary(id = it.id, title = it.title, image = it.image, url = it.url)
                    },
                    featured = home.onAir.take(20).map {
                        FeaturedAnime(
                            title = it.title,
                            year = it.year,
                            image = it.image,
                            rating = it.rating,
                            url = it.url
                        )
                    },
                    latestEpisodes = home.latestEpisodes.map { episode ->
                        EpisodeUi(
                            id = episode.id,
                            url = episode.url,
                            title = episode.anime,
                            image = episode.image,
                            badges = buildList {
                                episode.episode?.let { add("Episodio $it") }
                                episode.quality?.let { add(it) }
                            }
                        )
                    },
                    popular = home.latestAnimes.take(18).map {
                        AnimeSummary(id = it.id, title = it.title, image = it.image, url = it.url)
                    },
                    movies = home.latestMovies.take(18).map {
                        AnimeSummary(id = it.id, title = it.title, image = it.image, url = it.url)
                    },
                    seasons = home.seasons.take(18).map {
                        AnimeSummary(id = it.id, title = it.title, image = it.image, url = it.url)
                    },
                    sectionMeta = home.sectionMeta
                )
                UnAnimeLog.d(
                    "home",
                    "ui: banner=${_uiState.value.banner.size}, featured=${_uiState.value.featured.size}, " +
                        "episodes=${_uiState.value.latestEpisodes.size}, animes=${_uiState.value.popular.size}, " +
                        "movies=${_uiState.value.movies.size}, seasons=${_uiState.value.seasons.size}"
                )
            } catch (e: Exception) {
                UnAnimeLog.e("home", "load failed", e)
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Error inesperado")
                }
            }
        }
    }
}

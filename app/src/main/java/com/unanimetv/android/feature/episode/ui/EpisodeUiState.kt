package com.unanimetv.android.feature.episode.ui

import com.notanime.es.scraper.EpisodeScreenshot
import com.notanime.es.scraper.SeasonEpisodes
import com.notanime.es.scraper.SourceServer

data class EpisodeUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val url: String = "",
    val title: String? = null,
    val titleUrl: String? = null,
    val image: String? = null,
    val prevUrl: String? = null,
    val nextUrl: String? = null,
    val languages: List<LanguageGroup> = emptyList(),
    val selectedLanguage: String? = null,
    val selectedServer: SourceServer? = null,
    val resolvingUrl: String? = null,
    val seasons: List<SeasonEpisodes> = emptyList(),
    val gallery: List<EpisodeScreenshot> = emptyList()
)

data class LanguageGroup(
    val lang: String,
    val servers: List<SourceServer>
)

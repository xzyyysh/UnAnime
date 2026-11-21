package com.unanimetv.android.feature.home.ui

import com.notanime.es.scraper.SectionMeta

 data class HomeUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val banner: List<AnimeSummary> = emptyList(),
    val featured: List<FeaturedAnime> = emptyList(),
    val latestEpisodes: List<EpisodeUi> = emptyList(),
    val popular: List<AnimeSummary> = emptyList(),
    val movies: List<AnimeSummary> = emptyList(),
    val seasons: List<AnimeSummary> = emptyList(),
    val sectionMeta: Map<String, SectionMeta> = emptyMap()
)

data class FeaturedAnime(
    val title: String,
    val year: String?,
    val image: String,
    val rating: String?,
    val url: String
)

data class EpisodeUi(
    val id: Int,
    val url: String,
    val title: String,
    val image: String,
    val badges: List<String>
)

data class AnimeSummary(
    val id: Int,
    val title: String,
    val image: String,
    val url: String,
    val rating: String? = null
)

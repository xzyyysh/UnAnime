package com.unanimetv.android.feature.title.ui

import com.notanime.es.scraper.TitleData

data class TitleUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val data: TitleData? = null
)

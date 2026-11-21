package com.unanimetv.android.feature.catalog.ui

import com.unanimetv.android.feature.home.ui.AnimeSummary

data class CatalogUiState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val items: List<AnimeSummary> = emptyList(),
    val page: Int = 1,
    val totalPages: Int = 1
)

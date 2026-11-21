package com.unanimetv.android.feature.catalog.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notanime.es.util.UnAnimeLog
import com.unanimetv.android.core.network.ApiClient
import com.unanimetv.android.feature.home.ui.AnimeSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CatalogViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CatalogUiState())
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    private var archivePath: String? = null

    fun load(path: String) {
        if (archivePath == path && _uiState.value.items.isNotEmpty()) return
        archivePath = path
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, items = emptyList()) }
            try {
                val page = ApiClient.archive(path)
                _uiState.value = CatalogUiState(
                    isLoading = false,
                    items = page.items.map {
                        AnimeSummary(id = it.id, title = it.title, image = it.image, url = it.url, rating = it.rating)
                    },
                    page = page.page,
                    totalPages = page.totalPages
                )
                UnAnimeLog.d("catalog", "ui: ${_uiState.value.items.size} items, page ${page.page}/${page.totalPages}")
            } catch (e: Exception) {
                UnAnimeLog.e("catalog", "load failed for $path", e)
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error inesperado") }
            }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        val path = archivePath ?: return
        if (state.isLoading || state.isLoadingMore || state.page >= state.totalPages) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            try {
                val next = state.page + 1
                val page = ApiClient.archive(path, next)
                _uiState.update {
                    it.copy(
                        isLoadingMore = false,
                        items = it.items + page.items.map { item ->
                            AnimeSummary(id = item.id, title = item.title, image = item.image, url = item.url, rating = item.rating)
                        },
                        page = page.page,
                        totalPages = page.totalPages
                    )
                }
                UnAnimeLog.d("catalog", "loadMore: page $next -> ${_uiState.value.items.size} items")
            } catch (e: Exception) {
                UnAnimeLog.e("catalog", "loadMore failed", e)
                _uiState.update { it.copy(isLoadingMore = false) }
            }
        }
    }
}

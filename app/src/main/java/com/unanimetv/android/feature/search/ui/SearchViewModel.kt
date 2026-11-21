package com.unanimetv.android.feature.search.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unanimetv.android.core.network.ApiClient
import com.notanime.es.scraper.SearchResult
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val isSearching: Boolean = false,
    val error: String? = null,
    val results: List<SearchResult> = emptyList()
)

@OptIn(FlowPreview::class)
class SearchViewModel : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _query
                .debounce(400)
                .collectLatest { value ->
                    val q = value.trim()
                    if (q.length < 3) {
                        _uiState.value = SearchUiState()
                        return@collectLatest
                    }
                    _uiState.update { it.copy(isSearching = true, error = null) }
                    try {
                        val results = ApiClient.search(q)
                        _uiState.update { it.copy(isSearching = false, results = results) }
                    } catch (e: Exception) {
                        _uiState.update {
                            it.copy(isSearching = false, error = e.message ?: "Error inesperado")
                        }
                    }
                }
        }
    }

    fun onQueryChange(query: String) {
        _query.value = query
    }

    fun clear() {
        _query.value = ""
    }
}

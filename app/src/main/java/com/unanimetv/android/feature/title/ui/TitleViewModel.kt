package com.unanimetv.android.feature.title.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unanimetv.android.core.network.ApiClient
import com.unanimetv.android.core.history.HistoryEntry
import com.unanimetv.android.core.history.HistoryStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TitleViewModel(
    private val url: String,
    
) : ViewModel() {

    private val _uiState = MutableStateFlow(TitleUiState())
    val uiState: StateFlow<TitleUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun selectSeason(season: Int) {
        _uiState.update { it.copy(selectedSeason = season) }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val data = ApiClient.title(url)
                _uiState.update { it.copy(isLoading = false, data = data) }
                HistoryStore.record(
                    HistoryEntry(
                        url = url,
                        type = "title",
                        title = data.title,
                        image = data.poster,
                        watchedAt = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Error inesperado")
                }
            }
        }
    }
}

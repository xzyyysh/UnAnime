package com.unanimetv.android.feature.title.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unanimetv.android.core.network.ApiClient
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

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                _uiState.update { it.copy(isLoading = false, data = ApiClient.title(url)) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Error inesperado")
                }
            }
        }
    }
}

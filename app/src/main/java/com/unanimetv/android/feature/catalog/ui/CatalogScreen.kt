package com.unanimetv.android.feature.catalog.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import com.unanimetv.android.R
import com.unanimetv.android.core.designsystem.component.EmptyState
import com.unanimetv.android.core.designsystem.component.LoadingState
import com.unanimetv.android.core.designsystem.component.PosterCard
import com.unanimetv.android.core.designsystem.tokens.Spacing
import com.unanimetv.android.ui.AppChrome
import com.unanimetv.android.ui.rememberNotAnimeLogoVariant

@Composable
fun CatalogScreen(
    path: String,
    title: String,
    onTitleClick: (String) -> Unit,
    onCatalogClick: (String, String) -> Unit,
    onFavoritesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: CatalogViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val logoVariant = rememberNotAnimeLogoVariant()

    LaunchedEffect(path) {
        viewModel.load(path)
    }

    val gridState = rememberLazyGridState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val info = gridState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= info.totalItemsCount - 6
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMore()
    }

    AppChrome(
        logoVariant = logoVariant,
        collapseProgress = { 1f },
        alwaysSolid = true,
        onTitleClick = onTitleClick,
        onCatalogClick = onCatalogClick,
        onFavoritesClick = onFavoritesClick,
        modifier = modifier
    ) {
        when {
            uiState.isLoading -> LoadingState(modifier = Modifier.fillMaxSize())
            uiState.error != null -> EmptyState(
                icon = painterResource(R.drawable.ic_tabler_alert_circle),
                message = uiState.error.orEmpty(),
                modifier = Modifier.fillMaxSize()
            )
            else -> LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Spacing.lg,
                end = Spacing.lg,
                top = 110.dp,
                bottom = Spacing.lg
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            item(span = { GridItemSpan(2) }) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Spacing.sm)
                )
            }
            items(uiState.items.size) { index ->
                val item = uiState.items[index]
                PosterCard(
                    title = item.title,
                    imageUrl = item.image,
                    onClick = { onTitleClick(item.url) },
                    rating = item.rating
                )
            }
            if (uiState.isLoadingMore) {
                item(span = { GridItemSpan(2) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.lg),
                        contentAlignment = Alignment.Center
                    ) {
                        com.unanimetv.android.ui.NotAnimeLoader(compact = true, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            if (uiState.page >= uiState.totalPages && uiState.items.isNotEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Text(
                        text = "Fin de los resultados",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.lg)
                    )
                }
            }
        }
    }
    }
}

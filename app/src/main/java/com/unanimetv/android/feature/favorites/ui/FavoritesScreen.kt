package com.unanimetv.android.feature.favorites.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.unanimetv.android.R
import com.unanimetv.android.core.designsystem.component.EmptyState
import com.unanimetv.android.core.designsystem.tokens.Spacing
import com.unanimetv.android.core.favorites.FavoriteAnime
import com.unanimetv.android.core.favorites.FavoritesStore

@Composable
fun FavoritesScreen(
    onTitleClick: (String) -> Unit,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val favorites by FavoritesStore.favorites.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.LaunchedEffect(Unit) { FavoritesStore.init(context) }
    var selecting by remember { mutableStateOf(false) }
    val selected = remember { mutableListOf<String>().toMutableStateList() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_tabler_arrow_back),
                    contentDescription = "Volver",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = if (selecting) "Seleccionados ${selected.size}" else "Favoritos",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
            if (selecting) {
                TextButton(
                    onClick = {
                        FavoritesStore.removeUrls(selected)
                        selected.clear()
                        selecting = false
                    },
                    enabled = selected.isNotEmpty()
                ) {
                    Text(text = "Delete ${selected.size}")
                }
                TextButton(onClick = {
                    selecting = false
                    selected.clear()
                }) {
                    Text(text = "Cancelar")
                }
            } else if (favorites.isNotEmpty()) {
                TextButton(onClick = { selecting = true }) {
                    Text(text = "Seleccionar")
                }
            }
        }
        if (favorites.isEmpty()) {
            EmptyState(
                message = "Sin favoritos aún. Agrega animes desde su página con el menú ⋮"
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = Spacing.lg,
                    end = Spacing.lg,
                    bottom = 120.dp
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                modifier = Modifier.fillMaxSize()
            ) {
                items(favorites, key = { it.url }) { favorite ->
                    FavoriteCard(
                        favorite = favorite,
                        selecting = selecting,
                        selected = favorite.url in selected,
                        onClick = {
                            if (selecting) {
                                if (favorite.url in selected) selected.remove(favorite.url)
                                else selected.add(favorite.url)
                            } else {
                                onTitleClick(favorite.url)
                            }
                        },
                        onRemove = { FavoritesStore.remove(favorite.url) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoriteCard(
    favorite: FavoriteAnime,
    selecting: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuOpen by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .then(
                if (selected) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.medium
                    )
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = favorite.image,
            contentDescription = favorite.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                    )
                )
                .padding(Spacing.sm)
        ) {
            Column {
                Text(
                    text = favorite.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                favorite.rating?.let { rating ->
                    Text(
                        text = "★ $rating",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
        if (selecting && selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(Spacing.sm)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .padding(4.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_tabler_check),
                    contentDescription = "Seleccionado",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(2.dp)
                )
            }
        }
        Box(modifier = Modifier.align(Alignment.TopStart)) {
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    painter = painterResource(R.drawable.ic_tabler_dots_vertical),
                    contentDescription = "Más opciones",
                    tint = Color.White
                )
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                shape = MaterialTheme.shapes.medium
            ) {
                DropdownMenuItem(
                    text = { Text("Quitar de favoritos") },
                    onClick = {
                        menuOpen = false
                        onRemove()
                    }
                )
            }
        }
    }
}

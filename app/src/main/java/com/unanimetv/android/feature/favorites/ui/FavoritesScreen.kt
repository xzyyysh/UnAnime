package com.unanimetv.android.feature.favorites.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.unanimetv.android.R
import com.unanimetv.android.core.designsystem.component.EmptyState
import com.unanimetv.android.core.designsystem.tokens.Spacing
import com.unanimetv.android.core.favorites.FavoriteAnime
import com.unanimetv.android.core.favorites.FavoriteEpisode
import com.unanimetv.android.core.favorites.FavoritesStore

@Composable
fun FavoritesScreen(
    onTitleClick: (String) -> Unit,
    onEpisodeClick: (String) -> Unit,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val favorites by FavoritesStore.favorites.collectAsState()
    val episodes by FavoritesStore.episodes.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(Unit) { FavoritesStore.init(context) }
    var tab by remember { mutableStateOf(0) }
    var selecting by remember { mutableStateOf(false) }
    val selected = remember { mutableListOf<String>().toMutableStateList() }
    var confirmDelete by remember { mutableStateOf(false) }
    var pendingRemove by remember { mutableStateOf<String?>(null) }
    val showEpisodes = tab == 1
    val activeCount = if (showEpisodes) episodes.size else favorites.size

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
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
                Spacer(modifier = Modifier.weight(1f))
                if (selecting) {
                    TextButton(onClick = {
                        selecting = false
                        selected.clear()
                    }) {
                        Text(text = "Cancelar")
                    }
                } else if (activeCount > 0) {
                    TextButton(onClick = { selecting = true }) {
                        Text(text = "Seleccionar")
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier.padding(horizontal = Spacing.lg)
            ) {
                FavoriteTab(
                    label = "Animes",
                    count = favorites.size,
                    selected = !showEpisodes,
                    onClick = {
                        tab = 0
                        selecting = false
                        selected.clear()
                    }
                )
                FavoriteTab(
                    label = "Episodios",
                    count = episodes.size,
                    selected = showEpisodes,
                    onClick = {
                        tab = 1
                        selecting = false
                        selected.clear()
                    }
                )
            }
            if (showEpisodes && episodes.isEmpty()) {
                EmptyState(
                    message = "Sin episodios aún. Agrégalos desde el menú ⋮ del episodio."
                )
            } else if (showEpisodes) {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = Spacing.lg,
                        end = Spacing.lg,
                        top = Spacing.md,
                        bottom = 120.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    modifier = Modifier.fillMaxSize()
                ) {
                    lazyItems(episodes, key = { it.url }) { episode ->
                        EpisodeFavoriteRow(
                            episode = episode,
                            selecting = selecting,
                            selected = episode.url in selected,
                            onClick = {
                                if (selecting) {
                                    if (episode.url in selected) selected.remove(episode.url)
                                    else selected.add(episode.url)
                                } else {
                                    onEpisodeClick(episode.url)
                                }
                            },
                            onRemove = { pendingRemove = episode.url }
                        )
                    }
                }
            } else if (favorites.isEmpty()) {
                EmptyState(
                    message = "Sin favoritos aún. Agrega animes desde su página con el menú ⋮"
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(
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
                            onRemove = { pendingRemove = favorite.url }
                        )
                    }
                }
            }
        }
        if (selecting) {
            Button(
                onClick = { confirmDelete = true },
                enabled = selected.isNotEmpty(),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = Spacing.lg, bottom = Spacing.lg)
            ) {
                Text(text = "Delete ${selected.size}")
            }
        }
    }
    if (confirmDelete) {
        DeleteConfirmDialog(
            count = selected.size,
            onConfirm = {
                if (showEpisodes) FavoritesStore.removeEpisodeUrls(selected)
                else FavoritesStore.removeUrls(selected)
                selected.clear()
                selecting = false
                confirmDelete = false
            },
            onDismiss = { confirmDelete = false }
        )
    }
    pendingRemove?.let { url ->
        DeleteConfirmDialog(
            count = 1,
            onConfirm = {
                if (showEpisodes) FavoritesStore.removeEpisode(url)
                else FavoritesStore.remove(url)
                pendingRemove = null
            },
            onDismiss = { pendingRemove = null }
        )
    }
}

@Composable
private fun DeleteConfirmDialog(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.padding(horizontal = 48.dp)
        ) {
            Column(modifier = Modifier.padding(Spacing.lg)) {
                Text(
                    text = "¿Eliminar favoritos?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (count == 1) "Se eliminará 1 favorito." else "Se eliminarán $count favoritos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.xs)
                )
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.md)
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = "Cancelar")
                    }
                    Button(onClick = onConfirm) {
                        Text(text = "Eliminar")
                    }
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

@Composable
private fun FavoriteTab(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val background = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val content = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        modifier = Modifier
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = content
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = content.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun EpisodeFavoriteRow(
    episode: FavoriteEpisode,
    selecting: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
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
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .width(112.dp)
                .aspectRatio(16f / 9f)
                .clip(MaterialTheme.shapes.small)
        ) {
            AsyncImage(
                model = episode.image.takeIf { it.isNotEmpty() },
                contentDescription = episode.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            )
            if (selecting && selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(Spacing.xs)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .padding(3.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_tabler_check),
                        contentDescription = "Seleccionado",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = Spacing.md, vertical = Spacing.sm)
        ) {
            episode.animeTitle?.let { anime ->
                Text(
                    text = anime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = episode.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    painter = painterResource(R.drawable.ic_tabler_dots_vertical),
                    contentDescription = "Más opciones",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
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

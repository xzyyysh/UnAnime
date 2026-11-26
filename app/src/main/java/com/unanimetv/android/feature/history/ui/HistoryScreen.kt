package com.unanimetv.android.feature.history.ui

import android.content.Context
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.unanimetv.android.R
import com.unanimetv.android.core.designsystem.component.CompactProgress
import com.unanimetv.android.core.designsystem.component.EmptyState
import com.unanimetv.android.core.designsystem.tokens.Spacing
import com.unanimetv.android.core.history.HistoryEntry
import com.unanimetv.android.core.history.HistoryStore
import com.unanimetv.android.core.playback.PlaybackStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    onTitleClick: (String) -> Unit,
    onEpisodeClick: (String) -> Unit,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val entries by HistoryStore.entries.collectAsState()
    val progress by PlaybackStore.progress.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    var selecting by remember { mutableStateOf(false) }
    val selected = remember { mutableListOf<String>().toMutableStateList() }
    var confirmDelete by remember { mutableStateOf(false) }
    val wide = LocalConfiguration.current.screenWidthDp >= 600
    val watched = entries
        .filter { (progress[it.url]?.durationMs ?: 0L) > 0L }
        .sortedByDescending { progress[it.url]?.updatedAt ?: 0L }
    val activeItems = if (tab == 0) entries else watched

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
                    text = if (selecting) "Seleccionados ${selected.size}" else "Historial",
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
                } else if (activeItems.isNotEmpty()) {
                    TextButton(onClick = { selecting = true }) {
                        Text(text = "Seleccionar")
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier.padding(horizontal = Spacing.lg)
            ) {
                HistoryTab(label = "Vistos", count = entries.size, selected = tab == 0, onClick = {
                    tab = 0
                    selecting = false
                    selected.clear()
                })
                HistoryTab(
                    label = "Progreso",
                    count = progress.values.count { it.durationMs > 0 },
                    selected = tab == 1,
                    onClick = {
                        tab = 1
                        selecting = false
                        selected.clear()
                    }
                )
            }
            if (tab == 0) {
                if (entries.isEmpty()) {
                    EmptyState(message = "Sin historial aún. Lo que veas aparece acá.")
                } else {
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
                        items(entries, key = { it.url }) { entry ->
                            HistoryRow(
                                entry = entry,
                                progress = progress[entry.url]?.fraction ?: 0f,
                                showProgress = entry.type == "episode",
                                wide = wide,
                                selecting = selecting,
                                selected = entry.url in selected,
                                onClick = {
                                    if (selecting) {
                                        if (entry.url in selected) selected.remove(entry.url)
                                        else selected.add(entry.url)
                                    } else if (entry.type == "title") onTitleClick(entry.url)
                                    else onEpisodeClick(entry.url)
                                }
                            )
                        }
                    }
                }
            } else {
                if (watched.isEmpty()) {
                    EmptyState(message = "Sin progreso aún. Reproducí un episodio para verlo acá.")
                } else {
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
                        items(watched, key = { it.url }) { entry ->
                            val p = progress[entry.url] ?: return@items
                            HistoryRow(
                                entry = entry,
                                progress = p.fraction,
                                showProgress = true,
                                wide = wide,
                                selecting = selecting,
                                selected = entry.url in selected,
                                onClick = {
                                    if (selecting) {
                                        if (entry.url in selected) selected.remove(entry.url)
                                        else selected.add(entry.url)
                                    } else if (entry.type == "title") onTitleClick(entry.url)
                                    else onEpisodeClick(entry.url)
                                }
                            )
                        }
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
                Text(text = "Eliminar ${selected.size}")
            }
        }
    }
    if (confirmDelete) {
        DeleteConfirmDialog(
            count = selected.size,
            onConfirm = {
                HistoryStore.removeUrls(selected)
                selected.clear()
                selecting = false
                confirmDelete = false
            },
            onDismiss = { confirmDelete = false }
        )
    }
}

@Composable
private fun DeleteConfirmDialog(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(Spacing.lg)) {
                Text(
                    text = "¿Eliminar del historial?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Se eliminarán $count elementos.",
                    style = MaterialTheme.typography.bodyMedium,
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
                    TextButton(onClick = onConfirm) {
                        Text(
                            text = "Eliminar",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

private fun formatWatchedAt(context: Context, timestamp: Long): String {
    if (timestamp <= 0) return ""
    val format = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
    return format.format(Date(timestamp))
}

@Composable
private fun HistoryTab(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val container = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val content = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(container)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.xs)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = content
        )
        if (count >= 0) {
            Spacer(modifier = Modifier.width(Spacing.xs))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = content.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun HistoryRow(
    entry: HistoryEntry,
    progress: Float,
    showProgress: Boolean,
    wide: Boolean,
    selecting: Boolean,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
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
            .padding(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        AsyncImage(
            model = entry.image,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(if (wide) 120.dp else 96.dp)
                .aspectRatio(16f / 9f)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        )
        Column(modifier = Modifier.weight(1f)) {
            if (entry.type == "episode" && entry.label != null) {
                Text(
                    text = entry.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = entry.title.ifEmpty { "Sin título" },
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val subtitle = buildList {
                if (entry.type == "episode") entry.animeTitle?.let { add(it) }
                else add("Título")
                entry.label?.takeIf { entry.type != "episode" }?.let { add(it) }
            }
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (showProgress && progress > 0f) {
                CompactProgress(
                    fraction = progress,
                    modifier = Modifier.padding(top = Spacing.xs)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatWatchedAt(LocalContext.current, entry.watchedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (showProgress && progress > 0f) {
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        Icon(
            painter = painterResource(
                if (entry.type == "title") R.drawable.ic_tabler_device_tv else R.drawable.ic_tabler_player_play
            ),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                .padding(Spacing.xs)
        )
    }
}

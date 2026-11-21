package com.unanimetv.android.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.unanimetv.android.R
import com.unanimetv.android.core.designsystem.tokens.Spacing
import com.unanimetv.android.core.update.UpdateChecker
import java.util.Locale

@Composable
fun UpdateDialog(
    currentVersion: String,
    update: UpdateChecker.Update,
    download: UpdateChecker.Download,
    onRememberLater: () -> Unit,
    onDownload: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(Spacing.xl)) {
                Text(
                    text = "Actualización disponible",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.md)
                ) {
                    Text(
                        text = currentVersion,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.size(Spacing.md))
                    Icon(
                        painter = painterResource(R.drawable.ic_tabler_arrow_big_up),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.size(Spacing.md))
                    Text(
                        text = update.version,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                when (download) {
                    is UpdateChecker.Download.Downloading -> DownloadProgress(download)
                    is UpdateChecker.Download.Installing -> InstallingProgress()
                    is UpdateChecker.Download.Idle, UpdateChecker.Download.Failed -> {
                        if (download is UpdateChecker.Download.Failed) {
                            Text(
                                text = "Error al descargar, intenta de nuevo",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = Spacing.md)
                            )
                        }
                        Text(
                            text = update.changelog.ifBlank { "Sin notas de cambios" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(top = Spacing.md)
                                .heightIn(max = 320.dp)
                                .verticalScroll(rememberScrollState())
                        )
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = Spacing.md)
                        ) {
                            TextButton(onClick = onRememberLater) {
                                Text("Recuérdalo más tarde")
                            }
                            Button(onClick = onDownload) {
                                Text(if (download is UpdateChecker.Download.Failed) "Reintentar" else "Descargar")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadProgress(download: UpdateChecker.Download.Downloading) {
    val animated by animateFloatAsState(
        targetValue = download.progress,
        animationSpec = tween(durationMillis = 450),
        label = "update-progress"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.lg)
    ) {
        LinearProgressIndicator(
            progress = { animated.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.sm)
        ) {
            Text(
                text = "${(animated * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = String.format(Locale.US, "%.1f MB/s", download.speedMbps),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InstallingProgress() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.lg)
    ) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Text(
            text = "Instalando...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.sm)
        )
    }
}

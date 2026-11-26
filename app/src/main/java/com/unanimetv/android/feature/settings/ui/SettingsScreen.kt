package com.unanimetv.android.feature.settings.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil3.imageLoader
import com.unanimetv.android.R
import com.unanimetv.android.core.BuildInfo
import com.unanimetv.android.core.designsystem.theme.ThemeController
import com.unanimetv.android.core.playback.CurrentStream
import com.unanimetv.android.core.update.UpdateChecker
import com.unanimetv.android.core.update.UpdateNotifier
import com.unanimetv.android.core.designsystem.theme.ThemeMode
import com.unanimetv.android.core.designsystem.theme.ThemeStyle
import com.unanimetv.android.core.designsystem.theme.label
import com.unanimetv.android.core.designsystem.tokens.Spacing
import com.unanimetv.android.ui.NotAnimeLogo
import java.io.File

private const val GITHUB_URL = "https://github.com/xzyyysh/UnAnime"
private const val SOURCE_URL = "https://ww4.animeonline.ninja"

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val themeSettings by ThemeController.settings.collectAsState()
    var cacheCleared by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = Spacing.lg)
    ) {
        Text(
            text = "Ajustes",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = Spacing.lg)
        )
        AppearanceCard(
            settings = themeSettings,
            onModeChange = ThemeController::setMode,
            onStyleChange = ThemeController::setStyle,
            onMaterialYouChange = ThemeController::setMaterialYou,
            onAmoledChange = ThemeController::setAmoled
        )
        Spacer(modifier = Modifier.height(Spacing.lg))
        CacheCard(
            cleared = cacheCleared,
            onClear = {
                context.imageLoader.memoryCache?.clear()
                context.imageLoader.diskCache?.clear()
                cacheTargets(context).forEach { runCatching { it.deleteRecursively() } }
                CurrentStream.clearCache(context)
                cacheCleared = true
            }
        )
        Spacer(modifier = Modifier.height(Spacing.lg))
        UpdateCard()
        Spacer(modifier = Modifier.height(Spacing.lg))
        AboutCard()
        Spacer(modifier = Modifier.height(Spacing.md))
        Text(
            text = "All rights reserved",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(120.dp))
    }
}

@Composable
private fun AboutCard() {
    val context = LocalContext.current
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Icon(
                    painterResource(R.drawable.ic_tabler_info_circle),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Acerca de",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(Spacing.md))
            NotAnimeLogo()
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = "Versión ${BuildInfo.version}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = "Reproductor de anime open source para Android. " +
                    "Explora, busca y mira tus animes favoritos con una interfaz rápida y moderna.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            Text(
                text = "Credits: xzyyysh (alias: f3l1x)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL)))
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        painterResource(R.drawable.ic_tabler_brand_github),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(text = "GitHub")
                }
                Button(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SOURCE_URL)))
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        painterResource(R.drawable.ic_tabler_world),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(text = "AnimeOnlineNinja")
                }
            }
            Spacer(modifier = Modifier.height(Spacing.sm))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppearanceCard(
    settings: com.unanimetv.android.core.designsystem.theme.ThemeSettings,
    onModeChange: (ThemeMode) -> Unit,
    onStyleChange: (ThemeStyle) -> Unit,
    onMaterialYouChange: (Boolean) -> Unit,
    onAmoledChange: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Icon(
                    painterResource(R.drawable.ic_tabler_palette),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Apariencia",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(Spacing.md))
            ThemeDropdown(
                label = "Modo",
                selected = when (settings.mode) {
                    ThemeMode.SYSTEM -> "Sistema"
                    ThemeMode.LIGHT -> "Claro"
                    ThemeMode.DARK -> "Oscuro"
                },
                options = listOf("Sistema", "Claro", "Oscuro"),
                onSelect = { option ->
                    onModeChange(
                        when (option) {
                            "Claro" -> ThemeMode.LIGHT
                            "Oscuro" -> ThemeMode.DARK
                            else -> ThemeMode.SYSTEM
                        }
                    )
                }
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            ThemeDropdown(
                label = "Estilo de tema",
                selected = settings.style.label,
                options = ThemeStyle.entries.map { it.label },
                onSelect = { option ->
                    onStyleChange(ThemeStyle.entries.first { it.label == option })
                }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.md))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Material You",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Usa los colores del sistema (Android 12+)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.materialYou,
                    onCheckedChange = onMaterialYouChange
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Amoled",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Negro puro en modo oscuro",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.amoled,
                    onCheckedChange = onAmoledChange
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeDropdown(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .menuAnchor(androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selected,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            }
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun UpdateCard() {
    val context = LocalContext.current
    val checking by UpdateChecker.checking.collectAsState()
    var notifierEnabled by remember { mutableStateOf(UpdateNotifier.isEnabled(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            UpdateNotifier.setEnabled(context, true)
            UpdateNotifier.schedule(context)
            notifierEnabled = true
        }
    }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Icon(
                    painterResource(R.drawable.ic_tabler_refresh),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Actualización",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(Spacing.md))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Versión actual",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = BuildInfo.version,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.clickable(enabled = !checking) {
                        UpdateChecker.checkNow(context)
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm)
                    ) {
                        if (checking) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Text(
                                text = "Buscar",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.md))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Notificaciones",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Notifica cuando haya una nueva versión disponible",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = notifierEnabled,
                    onCheckedChange = { turnOn ->
                        when {
                            turnOn -> {
                                if (Build.VERSION.SDK_INT >= 33 &&
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                                ) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    UpdateNotifier.setEnabled(context, true)
                                    UpdateNotifier.schedule(context)
                                    notifierEnabled = true
                                }
                            }
                            else -> {
                                UpdateNotifier.setEnabled(context, false)
                                UpdateNotifier.schedule(context)
                                notifierEnabled = false
                            }
                        }
                    }
                )
            }
        }
    }
}

private fun cacheTargets(context: Context): List<File> {
    val dataDir = context.applicationInfo.dataDir
    val webView = File(dataDir, "app_webview")
    return listOf(
        File(context.cacheDir, "downloads"),
        File(context.cacheDir, "update"),
        File(context.cacheDir, "captures"),
        File(context.cacheDir, "history.json"),
        File(context.cacheDir, "playback_progress.json"),
        File(webView, "Default/Code Cache"),
        File(webView, "Default/GPUCache"),
        File(webView, "GPUCache"),
        File(webView, "Default/HTTP Cache")
    )
}

private fun dirSize(file: File): Long =
    if (file.exists()) file.walkTopDown().filter { it.isFile }.sumOf { it.length() } else 0L

@Composable
private fun CacheCard(
    cleared: Boolean,
    onClear: () -> Unit
) {
    val context = LocalContext.current
    val sizeBytes = remember(cleared) {
        val dirs = cacheTargets(context).sumOf { dirSize(it) } + dirSize(File(context.cacheDir, "stream_cache"))
        (context.imageLoader.diskCache?.size ?: 0L) + dirs
    }
    val sizeText = remember(sizeBytes) {
        val mb = sizeBytes / (1024.0 * 1024.0)
        if (mb >= 1.0) String.format("%.1f MB", mb) else "${sizeBytes / 1024} KB"
    }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Icon(
                    painterResource(R.drawable.ic_tabler_shield),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Datos",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(Spacing.md))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Caché",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (cleared) "Caché borrada" else "$sizeText en disco",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.clickable(onClick = onClear)
                ) {
                    Text(
                        text = "Borrar",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm)
                    )
                }
            }
        }
    }
}

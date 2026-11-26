package com.unanimetv.android.core.media.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import coil3.compose.AsyncImage
import com.notanime.es.scraper.EpisodeScreenshot
import com.notanime.es.util.UnAnimeLog
import com.unanimetv.android.R
import com.unanimetv.android.core.designsystem.tokens.Spacing
import com.unanimetv.android.core.media.ImageDownloader
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ScreenshotGallery(
    screenshots: List<EpisodeScreenshot>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val state = rememberLazyListState()
    val current by remember { derivedStateOf { state.firstVisibleItemIndex } }
    Column(modifier = modifier) {
        Text(
            text = "Galería",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        LazyRow(
            state = state,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = state, snapPosition = SnapPosition.Center),
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            modifier = Modifier.padding(top = Spacing.md)
        ) {
            items(count = screenshots.size) { index ->
                val screenshot = screenshots[index]
                AsyncImage(
                    model = screenshot.thumb,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillParentMaxWidth(0.78f)
                        .aspectRatio(16f / 9f)
                        .clip(MaterialTheme.shapes.medium)
                        .clickable(onClick = { onSelect(index) })
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = Spacing.md)
        ) {
            repeat(screenshots.size) { index ->
                Box(
                    modifier = Modifier
                        .size(width = if (index == current) 20.dp else 6.dp, height = 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == current) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        )
                )
            }
        }
    }
}

@Composable
fun ScreenshotPreview(
    screenshots: List<EpisodeScreenshot>,
    initialIndex: Int,
    fileNamePrefix: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var downloading by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<EpisodeScreenshot?>(null) }
    val rotation = remember { Animatable(0f) }
    val pagerState = rememberPagerState(initialPage = initialIndex) { screenshots.size }
    LaunchedEffect(downloading) {
        if (downloading) {
            while (downloading) {
                rotation.animateTo(
                    rotation.value + 360f,
                    animationSpec = tween(900, easing = LinearEasing)
                )
            }
        } else {
            rotation.snapTo(0f)
        }
    }
    val download: (EpisodeScreenshot) -> Unit = { screenshot ->
        if (!downloading) {
            downloading = true
            scope.launch {
                val index = screenshots.indexOf(screenshot).coerceAtLeast(0)
                runCatching {
                    ImageDownloader.download(
                        context,
                        screenshot.full,
                        ImageDownloader.safeFileName(fileNamePrefix, index)
                    )
                }
                    .onSuccess {
                        UnAnimeLog.d("download", "ok: $it")
                        Toast.makeText(context, "Imagen descargada en Descargas", Toast.LENGTH_SHORT).show()
                    }
                    .onFailure {
                        UnAnimeLog.e("download", "failed", it)
                        Toast.makeText(context, "No se pudo descargar", Toast.LENGTH_SHORT).show()
                    }
                downloading = false
            }
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val screenshot = pending
        pending = null
        if (granted && screenshot != null) download(screenshot)
    }
    val requestDownload: (EpisodeScreenshot) -> Unit = { screenshot ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            download(screenshot)
        } else {
            pending = screenshot
            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        var neighbors by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            delay(400)
            neighbors = true
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
        ) {
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = Spacing.xxl),
                pageSpacing = Spacing.lg,
                key = { screenshots[it].full },
                modifier = Modifier.fillMaxSize()
            ) { page ->
                if (page == initialIndex || neighbors) {
                    AsyncImage(
                        model = screenshots[page].full,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        onSuccess = { if (page == initialIndex) neighbors = true },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Text(
                text = "${pagerState.currentPage + 1} / ${screenshots.size}",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = Spacing.lg)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                    .padding(horizontal = Spacing.md, vertical = Spacing.xs)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(Spacing.lg)
            ) {
                IconButton(
                    onClick = {
                        screenshots.getOrNull(pagerState.currentPage)?.let(requestDownload)
                    },
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.45f), CircleShape)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_tabler_download),
                        contentDescription = "Descargar",
                        tint = Color.White,
                        modifier = Modifier.graphicsLayer { rotationZ = rotation.value }
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.45f), CircleShape)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_tabler_x),
                        contentDescription = "Cerrar",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

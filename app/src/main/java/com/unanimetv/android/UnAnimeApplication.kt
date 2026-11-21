package com.unanimetv.android

import android.app.Application
import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebSettings
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.notanime.es.NotAnimeConfig
import com.unanimetv.android.core.BuildInfo
import com.unanimetv.android.core.designsystem.theme.ThemeController
import com.unanimetv.android.core.network.ApiClient
import com.notanime.es.util.UnAnimeLog
import okhttp3.OkHttpClient
import okio.Path.Companion.toPath
import java.net.URI

class UnAnimeApplication : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()
        BuildInfo.init(this)
        com.unanimetv.android.core.playback.PlaybackStore.init(this)
        ThemeController.init(this)
    }

    override fun newImageLoader(context: Context): ImageLoader {
        val originHost = URI(NotAnimeConfig.ORIGIN).host
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                val builder = request.newBuilder()
                    .header("user-agent", WebSettings.getDefaultUserAgent(this))
                if (URI(request.url.toString()).host == originHost) {
                    CookieManager.getInstance().getCookie(NotAnimeConfig.ORIGIN)?.let {
                        builder.header("cookie", it)
                    }
                }
                chain.proceed(builder.build())
            }
            .build()
        UnAnimeLog.d("images", "image loader ready")
        return ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { client }))
            }
            .crossfade(true)
            .memoryCache {
                MemoryCache.Builder().maxSizeBytes(128L * 1024 * 1024).build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache").absolutePath.toPath())
                    .maxSizeBytes(250L * 1024 * 1024)
                    .build()
            }
            .build()
    }
}

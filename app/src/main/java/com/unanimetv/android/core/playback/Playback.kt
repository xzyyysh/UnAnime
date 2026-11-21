package com.unanimetv.android.core.playback

import android.content.Context
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheKeyFactory
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.notanime.es.util.UnAnimeLog
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

object CurrentStream {

    private const val PLAYER_UA =
        "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Mobile Safari/537.36"

    @Volatile
    var referer: String? = null

    @Volatile
    var currentUrl: String? = null

    private const val CACHE_MAX_BYTES = 2L * 1024 * 1024 * 1024

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val cacheLock = Any()

    private var cache: SimpleCache? = null

    private fun streamCache(context: Context): SimpleCache {
        synchronized(cacheLock) {
            cache?.let { return it }
            val dir = File(context.cacheDir, "stream_cache")
            val instance = SimpleCache(
                dir,
                LeastRecentlyUsedCacheEvictor(CACHE_MAX_BYTES),
                StandaloneDatabaseProvider(context)
            )
            UnAnimeLog.d("player", "stream cache ready dir=${dir.path} max=2GB")
            cache = instance
            return instance
        }
    }

    private val cacheKeyFactory = CacheKeyFactory { dataSpec ->
        dataSpec.uri.buildUpon().clearQuery().build().toString()
    }

    fun dataSourceFactory(context: Context): DataSource.Factory = DataSource.Factory {
        val headers = buildMap {
            referer?.let { put("Referer", it) }
        }
        UnAnimeLog.d("player", "data source open url=${currentUrl ?: "-"} referer=${referer ?: "-"}")
        val upstreamFactory = OkHttpDataSource.Factory(client)
            .setUserAgent(PLAYER_UA)
            .setDefaultRequestProperties(headers)
        CacheDataSource(
            streamCache(context),
            upstreamFactory.createDataSource(),
            androidx.media3.datasource.FileDataSource(),
            androidx.media3.datasource.cache.CacheDataSink.Factory()
                .setCache(streamCache(context))
                .createDataSink(),
            CacheDataSource.FLAG_BLOCK_ON_CACHE or CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR,
            null,
            cacheKeyFactory
        )
    }
}

object PlaybackStore {

    private const val PREFS = "playback_positions"
    private const val MIN_RESUME_MS = 5_000L

    private lateinit var prefs: android.content.SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    fun get(url: String): Long =
        if (this::prefs.isInitialized) prefs.getLong(url, 0L) else 0L

    fun put(url: String, positionMs: Long) {
        if (!this::prefs.isInitialized) return
        val clamped = if (positionMs < MIN_RESUME_MS) 0L else positionMs
        prefs.edit().putLong(url, clamped).apply()
        UnAnimeLog.d("store", "saved position ${clamped}ms for ${url.substringAfterLast('/')}")
    }
}

package com.unanimetv.android.core.favorites

import android.content.Context
import androidx.core.content.edit
import com.notanime.es.util.UnAnimeLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class FavoriteAnime(
    val url: String,
    val title: String,
    val image: String,
    val rating: String? = null
)

data class FavoriteEpisode(
    val url: String,
    val title: String,
    val image: String,
    val animeTitle: String? = null,
    val animeUrl: String? = null,
    val addedAt: Long = 0L
)

object FavoritesStore {

    private const val PREFS = "favorites"
    private const val KEY = "items"
    private const val EPISODE_KEY = "episodes"

    private val _favorites = MutableStateFlow<List<FavoriteAnime>>(emptyList())
    val favorites: StateFlow<List<FavoriteAnime>> = _favorites.asStateFlow()

    private val _episodes = MutableStateFlow<List<FavoriteEpisode>>(emptyList())
    val episodes: StateFlow<List<FavoriteEpisode>> = _episodes.asStateFlow()

    private var appContext: Context? = null

    fun init(context: Context) {
        if (appContext != null) return
        synchronized(this) {
            if (appContext != null) return
            appContext = context.applicationContext
            _favorites.value = read()
            _episodes.value = readEpisodes()
            UnAnimeLog.d(
                "favorites",
                "loaded ${_favorites.value.size} favorites, ${_episodes.value.size} episodes"
            )
        }
    }

    fun isFavorite(url: String): Boolean =
        _favorites.value.any { it.url == url }

    fun isEpisodeFavorite(url: String): Boolean =
        _episodes.value.any { it.url == url }

    fun addEpisode(item: FavoriteEpisode) {
        synchronized(this) {
            if (_episodes.value.any { it.url == item.url }) return
            _episodes.value = listOf(item) + _episodes.value
            persistEpisodes()
        }
    }

    fun removeEpisode(url: String) {
        synchronized(this) {
            _episodes.value = _episodes.value.filterNot { it.url == url }
            persistEpisodes()
        }
    }

    fun removeEpisodeUrls(urls: Collection<String>) {
        if (urls.isEmpty()) return
        synchronized(this) {
            _episodes.value = _episodes.value.filterNot { it.url in urls }
            persistEpisodes()
        }
    }

    fun add(item: FavoriteAnime) {
        synchronized(this) {
            if (_favorites.value.any { it.url == item.url }) return
            _favorites.value = listOf(item) + _favorites.value
            persist()
        }
    }

    fun remove(url: String) {
        synchronized(this) {
            _favorites.value = _favorites.value.filterNot { it.url == url }
            persist()
        }
    }

    fun removeUrls(urls: Collection<String>) {
        if (urls.isEmpty()) return
        synchronized(this) {
            _favorites.value = _favorites.value.filterNot { it.url in urls }
            persist()
        }
    }

    private fun read(): List<FavoriteAnime> {
        val ctx = appContext ?: return emptyList()
        return runCatching {
            val json = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY, null) ?: return emptyList()
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                FavoriteAnime(
                    url = o.getString("url"),
                    title = o.getString("title"),
                    image = o.optString("image"),
                    rating = o.optString("rating").takeIf { it.isNotEmpty() }
                )
            }
        }.getOrElse {
            UnAnimeLog.e("favorites", "load failed", it)
            emptyList()
        }
    }

    private fun readEpisodes(): List<FavoriteEpisode> {
        val ctx = appContext ?: return emptyList()
        return runCatching {
            val json = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(EPISODE_KEY, null) ?: return emptyList()
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                FavoriteEpisode(
                    url = o.getString("url"),
                    title = o.getString("title"),
                    image = o.optString("image"),
                    animeTitle = o.optString("animeTitle").takeIf { it.isNotEmpty() },
                    animeUrl = o.optString("animeUrl").takeIf { it.isNotEmpty() },
                    addedAt = o.optLong("addedAt")
                )
            }
        }.getOrElse {
            UnAnimeLog.e("favorites", "load episodes failed", it)
            emptyList()
        }
    }

    private fun persistEpisodes() {
        val ctx = appContext ?: return
        val arr = JSONArray()
        _episodes.value.forEach { e ->
            arr.put(JSONObject().apply {
                put("url", e.url)
                put("title", e.title)
                put("image", e.image)
                e.animeTitle?.let { put("animeTitle", it) }
                e.animeUrl?.let { put("animeUrl", it) }
                put("addedAt", e.addedAt)
            })
        }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { putString(EPISODE_KEY, arr.toString()) }
    }

    private fun persist() {
        val ctx = appContext ?: return
        val arr = JSONArray()
        _favorites.value.forEach { f ->
            arr.put(JSONObject().apply {
                put("url", f.url)
                put("title", f.title)
                put("image", f.image)
                f.rating?.let { put("rating", it) }
            })
        }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { putString(KEY, arr.toString()) }
    }
}

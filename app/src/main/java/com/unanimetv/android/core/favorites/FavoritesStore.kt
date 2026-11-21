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

object FavoritesStore {

    private const val PREFS = "favorites"
    private const val KEY = "items"

    private val _favorites = MutableStateFlow<List<FavoriteAnime>>(emptyList())
    val favorites: StateFlow<List<FavoriteAnime>> = _favorites.asStateFlow()

    private var appContext: Context? = null

    fun init(context: Context) {
        if (appContext != null) return
        synchronized(this) {
            if (appContext != null) return
            appContext = context.applicationContext
            _favorites.value = read()
            UnAnimeLog.d("favorites", "loaded ${_favorites.value.size} favorites")
        }
    }

    fun isFavorite(url: String): Boolean =
        _favorites.value.any { it.url == url }

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

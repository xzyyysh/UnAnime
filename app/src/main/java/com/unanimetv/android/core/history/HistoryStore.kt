package com.unanimetv.android.core.history

import android.content.Context
import com.notanime.es.util.UnAnimeLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class HistoryEntry(
    val url: String,
    val type: String,
    val title: String,
    val image: String,
    val animeTitle: String? = null,
    val animeUrl: String? = null,
    val label: String? = null,
    val watchedAt: Long
)

object HistoryStore {

    private const val MAX_ENTRIES = 300

    private var file: File? = null

    private val _entries = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val entries: StateFlow<List<HistoryEntry>> = _entries.asStateFlow()

    fun init(context: Context) {
        if (file != null) return
        synchronized(this) {
            if (file != null) return
            file = File(context.cacheDir, "history.json")
            _entries.value = read()
            UnAnimeLog.d("history", "loaded ${_entries.value.size} entries")
        }
    }

    fun record(entry: HistoryEntry) {
        if (file == null) return
        synchronized(this) {
            _entries.value = (listOf(entry) + _entries.value.filterNot { it.url == entry.url })
                .sortedByDescending { it.watchedAt }
                .take(MAX_ENTRIES)
            persist()
        }
    }

    fun clear() {
        synchronized(this) {
            _entries.value = emptyList()
            persist()
        }
    }

    fun episode(url: String): HistoryEntry? = _entries.value.firstOrNull { it.url == url }

    private fun read(): List<HistoryEntry> {
        val target = file ?: return emptyList()
        return runCatching {
            if (!target.exists()) return emptyList()
            val arr = JSONArray(target.readText())
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                HistoryEntry(
                    url = o.getString("url"),
                    type = o.optString("type", "episode"),
                    title = o.optString("title"),
                    image = o.optString("image"),
                    animeTitle = o.optString("animeTitle").takeIf { it.isNotEmpty() },
                    animeUrl = o.optString("animeUrl").takeIf { it.isNotEmpty() },
                    label = o.optString("label").takeIf { it.isNotEmpty() },
                    watchedAt = o.optLong("watchedAt")
                )
            }.sortedByDescending { it.watchedAt }
        }.getOrElse {
            UnAnimeLog.e("history", "load failed", it)
            emptyList()
        }
    }

    private fun persist() {
        val target = file ?: return
        runCatching {
            val arr = JSONArray()
            _entries.value.forEach { e ->
                arr.put(JSONObject().apply {
                    put("url", e.url)
                    put("type", e.type)
                    put("title", e.title)
                    put("image", e.image)
                    e.animeTitle?.let { put("animeTitle", it) }
                    e.animeUrl?.let { put("animeUrl", it) }
                    e.label?.let { put("label", it) }
                    put("watchedAt", e.watchedAt)
                })
            }
            target.writeText(arr.toString())
        }.onFailure { UnAnimeLog.e("history", "persist failed", it) }
    }
}

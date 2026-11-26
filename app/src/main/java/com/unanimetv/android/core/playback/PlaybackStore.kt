package com.unanimetv.android.core.playback

import android.content.Context
import androidx.core.content.edit
import com.notanime.es.util.UnAnimeLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.File

data class PlaybackProgress(
    val url: String,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long
) {
    val fraction: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}

object PlaybackStore {

    private const val MIN_RESUME_MS = 5_000L
    private const val MAX_ENTRIES = 500

    private var file: File? = null

    private val _progress = MutableStateFlow<Map<String, PlaybackProgress>>(emptyMap())
    val progress: StateFlow<Map<String, PlaybackProgress>> = _progress.asStateFlow()

    fun init(context: Context) {
        if (file != null) return
        synchronized(this) {
            if (file != null) return
            file = File(context.cacheDir, "playback_progress.json")
            _progress.value = read()
            UnAnimeLog.d("store", "loaded ${_progress.value.size} playback entries")
        }
    }

    fun get(url: String): Long = _progress.value[url]?.positionMs ?: 0L

    fun put(url: String, positionMs: Long) {
        if (file == null) return
        synchronized(this) {
            val current = _progress.value[url]
            val clamped = if (positionMs < MIN_RESUME_MS) 0L else positionMs
            val updated = current?.copy(positionMs = clamped, updatedAt = System.currentTimeMillis())
                ?: PlaybackProgress(url, clamped, 0L, System.currentTimeMillis())
            _progress.value = (_progress.value + (url to updated)).trimToMax()
            persist()
        }
    }

    fun putDuration(url: String, durationMs: Long) {
        if (file == null || durationMs <= 0) return
        synchronized(this) {
            val current = _progress.value[url]
            if (current?.durationMs == durationMs) return
            val updated = current?.copy(durationMs = durationMs)
                ?: PlaybackProgress(url, 0L, durationMs, System.currentTimeMillis())
            _progress.value = (_progress.value + (url to updated)).trimToMax()
            persist()
        }
    }

    fun entry(url: String): PlaybackProgress? = _progress.value[url]

    private fun Map<String, PlaybackProgress>.trimToMax(): Map<String, PlaybackProgress> =
        entries.sortedByDescending { it.value.updatedAt }.take(MAX_ENTRIES).associate { it.key to it.value }

    private fun read(): Map<String, PlaybackProgress> {
        val target = file ?: return emptyMap()
        return runCatching {
            if (!target.exists()) return emptyMap()
            val obj = JSONObject(target.readText())
            obj.keys().asSequence().associateWith { key ->
                val o = obj.getJSONObject(key)
                PlaybackProgress(
                    url = key,
                    positionMs = o.optLong("position"),
                    durationMs = o.optLong("duration"),
                    updatedAt = o.optLong("updatedAt")
                )
            }
        }.getOrElse {
            UnAnimeLog.e("store", "load failed", it)
            emptyMap()
        }
    }

    private fun persist() {
        val target = file ?: return
        runCatching {
            val obj = JSONObject()
            _progress.value.forEach { (url, p) ->
                obj.put(url, JSONObject().apply {
                    put("position", p.positionMs)
                    put("duration", p.durationMs)
                    put("updatedAt", p.updatedAt)
                })
            }
            target.writeText(obj.toString())
        }.onFailure { UnAnimeLog.e("store", "persist failed", it) }
    }
}

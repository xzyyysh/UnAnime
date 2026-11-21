package com.unanimetv.android.core.update

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.content.FileProvider
import com.notanime.es.util.UnAnimeLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {

    data class Update(
        val version: String,
        val changelog: String,
        val apkUrl: String,
        val apkSize: Long
    )

    sealed class Download {
        data object Idle : Download()
        data class Downloading(val progress: Float, val speedMbps: Float) : Download()
        data object Installing : Download()
        data object Failed : Download()
    }

    private const val RELEASES_URL = "https://api.github.com/repos/xzyyysh/UnAnime/releases/latest"
    private const val CHECK_INTERVAL_MS = 30 * 60_000L
    private const val SNOOZE_MS = 24 * 60 * 60_000L
    private const val PREFS = "update_checker"
    private const val KEY_SNOOZED_AT = "snoozed_at"
    private const val KEY_SNOOZED_VERSION = "snoozed_version"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _update = MutableStateFlow<Update?>(null)
    val update: StateFlow<Update?> = _update.asStateFlow()

    private val _download = MutableStateFlow<Download>(Download.Idle)
    val download: StateFlow<Download> = _download.asStateFlow()

    private var started = false

    fun start(context: Context) {
        if (started) return
        started = true
        val appContext = context.applicationContext
        scope.launch {
            while (true) {
                check(appContext)
                delay(CHECK_INTERVAL_MS)
            }
        }
    }

    fun rememberLater(context: Context) {
        val current = _update.value ?: return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_SNOOZED_AT, System.currentTimeMillis())
            .putString(KEY_SNOOZED_VERSION, current.version)
            .apply()
        _update.value = null
    }

    fun download(context: Context) {
        val current = _update.value ?: return
        if (_download.value is Download.Downloading || _download.value is Download.Installing) return
        _download.value = Download.Downloading(0f, 0f)
        val appContext = context.applicationContext
        scope.launch {
            val dir = File(appContext.cacheDir, "update").apply { mkdirs() }
            val apk = File(dir, "UnAnime_${current.version}.apk")
            try {
                val connection = URL(current.apkUrl).openConnection() as HttpURLConnection
                connection.connect()
                val total = current.apkSize.takeIf { it > 0 } ?: connection.contentLengthLong
                connection.inputStream.use { input ->
                    apk.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var received = 0L
                        var sampleAt = SystemClock.elapsedRealtime()
                        var sampleBytes = 0L
                        var speed = 0f
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            received += read
                            val now = SystemClock.elapsedRealtime()
                            if (now - sampleAt >= 500) {
                                speed = (received - sampleBytes) * 1000f / (now - sampleAt) / 1_000_000f
                                sampleAt = now
                                sampleBytes = received
                                _download.value = Download.Downloading(
                                    if (total > 0) received.toFloat() / total else 0f,
                                    speed
                                )
                            }
                        }
                        _download.value = Download.Downloading(1f, speed)
                    }
                }
                UnAnimeLog.i("update", "apk downloaded (${apk.length()} bytes)")
                _download.value = Download.Installing
                install(appContext, apk)
            } catch (e: Exception) {
                UnAnimeLog.e("update", "download failed", e)
                apk.delete()
                _download.value = Download.Failed
            }
        }
    }

    fun clearDownloadState() {
        _download.value = Download.Idle
    }

    private fun install(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private suspend fun check(context: Context) {
        val latest = runCatching { fetchLatest() }
            .onFailure { UnAnimeLog.w("update", "check failed: ${it.message}") }
            .getOrNull() ?: return
        if (!isNewer(latest.version, com.unanimetv.android.core.BuildInfo.version)) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val snoozedVersion = prefs.getString(KEY_SNOOZED_VERSION, null)
        val snoozedAt = prefs.getLong(KEY_SNOOZED_AT, 0L)
        if (snoozedVersion == latest.version && System.currentTimeMillis() - snoozedAt < SNOOZE_MS) return
        UnAnimeLog.i("update", "update available: ${latest.version}")
        _update.value = latest
    }

    private fun fetchLatest(): Update? {
        val connection = URL(RELEASES_URL).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.setRequestProperty("accept", "application/vnd.github+json")
        connection.setRequestProperty("user-agent", "UnAnime")
        if (connection.responseCode != 200) return null
        val json = JSONObject(connection.inputStream.bufferedReader().readText())
        val version = json.optString("tag_name").removePrefix("v")
        val changelog = json.optString("body")
        var apkUrl = ""
        var apkSize = 0L
        val assets = json.optJSONArray("assets")
        assets?.let {
            for (i in 0 until it.length()) {
                val asset = it.getJSONObject(i)
                if (asset.optString("name").endsWith(".apk")) {
                    apkUrl = asset.optString("browser_download_url")
                    apkSize = asset.optLong("size")
                    break
                }
            }
        }
        if (version.isEmpty() || apkUrl.isEmpty()) return null
        return Update(version, changelog, apkUrl, apkSize)
    }

    private fun isNewer(latest: String, current: String): Boolean {
        val latestParts = latest.split('.').map { it.trim().toIntOrNull() ?: 0 }
        val currentParts = current.split('.').map { it.trim().toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(latestParts.size, currentParts.size)) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l != c) return l > c
        }
        return false
    }
}

package com.unanimetv.android.core.network

import android.content.Context
import android.content.pm.ApplicationInfo
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import com.notanime.es.NotAnimeClient
import com.notanime.es.NotAnimeConfig
import com.notanime.es.net.DirectFetcher
import com.notanime.es.util.DebugCapture
import com.notanime.es.util.UnAnimeLog
import java.io.File

object ApiClient {

    private const val CLEARANCE_URL = "${NotAnimeConfig.ORIGIN}/inicio/"

    @Volatile private var appContext: Context? = null

    @Volatile private var syncedUserAgent: String? = null

    fun init(context: Context) {
        val app = context.applicationContext
        appContext = app
        com.notanime.es.net.CronetHttp.init(app)
        syncedUserAgent = WebSettings.getDefaultUserAgent(app)
        com.notanime.es.net.CronetHttp.configure(defaultUserAgent = syncedUserAgent)
        DebugCapture.dir = File(app.cacheDir, "captures").apply { mkdirs() }
        if (app.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
    }

    private fun httpClient(): NotAnimeClient = NotAnimeClient(
        DirectFetcher(
            userAgent = syncedUserAgent,
            cookieProvider = {
                CookieManager.getInstance().getCookie(NotAnimeConfig.ORIGIN)
            }
        )
    )

    private fun webViewClient(context: Context): NotAnimeClient =
        NotAnimeClient(WebViewFetcher(context))

    private fun isChallenge(e: Exception): Boolean {
        val message = e.message ?: return false
        return message.contains("403") || message.contains("503")
    }

    private suspend fun <T> call(block: suspend (NotAnimeClient) -> T): T {
        val context = appContext ?: throw IllegalStateException("ApiClient not initialized")
        var failure: Exception? = null
        try {
            return block(httpClient())
        } catch (e: Exception) {
            if (!isChallenge(e)) throw e
            failure = e
        }
        UnAnimeLog.w("cloudflare", "challenge detected, resyncing user agent")
        syncUserAgent(context)
        try {
            return block(httpClient())
        } catch (e: Exception) {
            if (!isChallenge(e)) throw e
            failure = e
        }
        UnAnimeLog.w("cloudflare", "clearance rejected, opening gate")
        CloudflareGate.clear()
        if (!CloudflareGate.solve(CLEARANCE_URL)) {
            throw failure ?: IllegalStateException("challenge not solved")
        }
        syncUserAgent(context)
        try {
            return block(httpClient())
        } catch (e: Exception) {
            if (!isChallenge(e)) throw e
            UnAnimeLog.w("cloudflare", "still blocked, fetching via webview")
            return block(webViewClient(context))
        }
    }

    private fun syncUserAgent(context: Context) {
        val agent = WebSettings.getDefaultUserAgent(context)
        if (agent == syncedUserAgent) return
        syncedUserAgent = agent
        com.notanime.es.net.CronetHttp.configure(defaultUserAgent = agent)
    }

    suspend fun home() = call { it.home() }

    suspend fun search(query: String) = call { it.search(query) }

    suspend fun genres() = call { it.genres() }

    suspend fun genre(name: String, page: Int) = call { it.genre(name, page) }

    suspend fun catalog(key: String, page: Int) = call { it.catalog(key, page) }

    suspend fun archive(path: String, page: Int = 1) = call { it.archive(path, page) }

    suspend fun title(url: String) = call { it.title(url) }

    suspend fun episode(url: String) = call { it.episode(url) }

    suspend fun episodeSources(url: String) = call { it.episodeSources(url) }

    suspend fun episodeServerGroups(url: String) = call { it.episodeServerGroups(url) }

    suspend fun resolveServer(server: com.notanime.es.scraper.EmbedServer) =
        call { it.resolveServer(server) }

    suspend fun resolve(url: String) = call { it.resolve(url) }
}

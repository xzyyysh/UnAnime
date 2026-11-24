package com.notanime.es.net

import android.annotation.SuppressLint
import android.content.Context
import android.net.http.SslError
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.notanime.es.NotAnimeConfig
import com.notanime.es.util.UnAnimeLog
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.IOException
import java.net.URI

data class HttpResponse(
    val code: Int,
    val body: String,
    val setCookies: List<String>
) {
    val isSuccessful: Boolean get() = code in 200..299
}

object WebViewHttp {

    private const val TIMEOUT_MS = 30_000L
    private const val CHALLENGE_WAIT_MS = 20_000L
    private const val POLL_MS = 500L

    private const val BODY_EXPR =
        "NotAnimeBridge.onBody(document.contentType === 'text/html' ? document.documentElement.outerHTML : (document.body ? document.body.innerText : ''))"

    private var appContext: Context? = null
    private var view: WebView? = null
    private val mutex = Mutex()
    private val bridge = Bridge()

    private class Bridge {
        @Volatile var body: CompletableDeferred<String>? = null
        @Volatile var json: CompletableDeferred<JSONObject>? = null

        @JavascriptInterface
        fun onBody(value: String) {
            body?.complete(value)
        }

        @JavascriptInterface
        fun onJson(value: String) {
            json?.complete(runCatching { JSONObject(value) }.getOrElse { JSONObject() })
        }
    }

    fun init(context: Context) {
        if (appContext == null) appContext = context.applicationContext
    }

    suspend fun get(
        url: String,
        headers: Map<String, String> = emptyMap(),
        userAgent: String? = null
    ): HttpResponse = mutex.withLock { withContext(Dispatchers.Main) { navigate(url, headers) } }

    suspend fun post(
        url: String,
        body: String,
        contentType: String,
        headers: Map<String, String> = emptyMap(),
        userAgent: String? = null
    ): HttpResponse = mutex.withLock {
        withContext(Dispatchers.Main) {
            val referer = header(headers, "referer")
            if (!sameHost(view?.url, url)) {
                navigate(referer?.takeIf { sameHost(it, url) } ?: origin(url), emptyMap())
            }
            fetchPost(url, body, contentType, referer)
        }
    }

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    private fun obtain(): WebView {
        view?.let { return it }
        val context = appContext ?: throw IllegalStateException("WebViewHttp.init not called")
        return WebView(context).also {
            it.settings.javaScriptEnabled = true
            it.settings.domStorageEnabled = true
            it.addJavascriptInterface(bridge, "NotAnimeBridge")
            view = it
        }
    }

    private suspend fun navigate(url: String, headers: Map<String, String>): HttpResponse {
        val target = obtain()
        val extra = buildMap {
            header(headers, "referer")?.let { put("Referer", it) }
            header(headers, "cookie")?.let { put("Cookie", it) }
        }
        var pending = CompletableDeferred<String>()
        var failure: IOException? = null
        target.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val scheme = request.url.scheme?.lowercase()
                return scheme != "http" && scheme != "https"
            }

            override fun onPageFinished(view: WebView, url: String) {
                view.evaluateJavascript(BODY_EXPR, null)
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame && error.errorCode != -1) {
                    failure = IOException("webview ${error.errorCode} ${error.description} $url")
                }
            }

            @SuppressLint("WebViewClientOnReceivedSslError")
            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                if (NotAnimeConfig.isOriginHost(error.url)) handler.proceed() else handler.cancel()
            }
        }
        bridge.body = pending
        UnAnimeLog.d("webview", "get $url")
        target.loadUrl(url, extra)
        val start = System.currentTimeMillis()
        var html = ""
        while (true) {
            val chunk = withTimeoutOrNull(POLL_MS) { pending.await() }
            if (chunk == null) {
                failure?.let { throw it }
                if (System.currentTimeMillis() - start > TIMEOUT_MS) break
                continue
            }
            html = chunk
            if (!isChallenge(html) || System.currentTimeMillis() - start >= CHALLENGE_WAIT_MS) break
            pending = CompletableDeferred()
            bridge.body = pending
            target.evaluateJavascript(BODY_EXPR, null)
        }
        if (html.isEmpty()) throw IOException("webview timeout $url")
        val code = if (isChallenge(html)) 403 else 200
        return HttpResponse(code, html, cookiesFor(url))
    }

    private suspend fun fetchPost(
        url: String,
        body: String,
        contentType: String,
        referer: String?
    ): HttpResponse {
        val target = view ?: obtain()
        val pending = CompletableDeferred<JSONObject>()
        bridge.json = pending
        val script = buildString {
            append("NotAnimeBridge.onJson(JSON.stringify((async()=>{try{const r=await fetch(")
            append(jsString(url))
            append(",{method:'POST',credentials:'include',headers:{'content-type':")
            append(jsString(contentType))
            if (referer != null && sameHost(referer, url)) {
                append(",referrer:")
                append(jsString(referer))
            }
            append(",body:")
            append(jsString(body))
            append("});return{status:r.status,text:await r.text()}}catch(e){return{status:0,text:'fetch failed: '+e}}})()))")
        }
        UnAnimeLog.d("webview", "post $url")
        target.evaluateJavascript(script, null)
        val result = withTimeoutOrNull(TIMEOUT_MS) { pending.await() }
            ?: throw IOException("webview post timeout $url")
        return HttpResponse(result.optInt("status", 0), result.optString("text"), cookiesFor(url))
    }

    private fun header(headers: Map<String, String>, name: String): String? =
        headers.entries.firstOrNull { it.key.equals(name, true) }?.value

    private fun sameHost(a: String?, b: String): Boolean {
        val hostA = a?.let { runCatching { URI(it).host }.getOrNull() } ?: return false
        val hostB = runCatching { URI(b).host }.getOrNull() ?: return false
        return hostA.equals(hostB, true)
    }

    private fun origin(url: String): String = runCatching {
        val uri = URI(url)
        "${uri.scheme}://${uri.host}/"
    }.getOrDefault(url)

    private fun cookiesFor(url: String): List<String> =
        CookieManager.getInstance().getCookie(url)?.split("; ")?.filter { it.contains("=") } ?: emptyList()

    private fun jsString(value: String): String = buildString {
        append('"')
        for (ch in value) {
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                else -> append(ch)
            }
        }
        append('"')
    }

    private fun isChallenge(body: String): Boolean =
        body.contains("Just a moment", ignoreCase = true) ||
            body.contains("cf-challenge", ignoreCase = true) ||
            body.contains("Attention Required", ignoreCase = true) ||
            body.contains("error code: 1020", ignoreCase = true)
}

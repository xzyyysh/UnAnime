package com.unanimetv.android.core.network

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import com.notanime.es.net.Fetcher
import com.notanime.es.util.UnAnimeLog
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

class WebViewFetcher(private val context: Context) : Fetcher {

    private inner class HtmlBridge(private val deferred: CompletableDeferred<String>) {
        @JavascriptInterface
        fun onHtml(html: String) {
            deferred.complete(html)
        }

        @JavascriptInterface
        fun onText(text: String) {
            deferred.complete(text)
        }
    }

    override suspend fun html(url: String): String = load(url, HTML_EXPR)

    override suspend fun json(url: String): JSONObject = JSONObject(load(url, TEXT_EXPR))

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    private suspend fun load(url: String, expression: String): String =
        withContext(Dispatchers.Main) {
            UnAnimeLog.d("webview", "fetch $url")
            val deferred = CompletableDeferred<String>()
            val webView = WebView(context)
            try {
                webView.settings.javaScriptEnabled = true
                webView.addJavascriptInterface(HtmlBridge(deferred), "UnAnimeBridge")
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, viewUrl: String) {
                        val title = view.title ?: ""
                        if (title.contains("just a moment", ignoreCase = true)) return
                        view.evaluateJavascript(expression, null)
                    }
                }
                webView.loadUrl(url)
                withTimeout(TIMEOUT_MS) { deferred.await() }.also {
                    UnAnimeLog.d("webview", "fetch ok (${it.length} chars)")
                }
            } finally {
                webView.stopLoading()
                webView.destroy()
            }
        }

    private companion object {
        const val TIMEOUT_MS = 30_000L
        const val HTML_EXPR = "UnAnimeBridge.onHtml(document.documentElement.outerHTML)"
        const val TEXT_EXPR =
            "UnAnimeBridge.onText(document.body ? document.body.innerText : document.documentElement.textContent)"
    }
}

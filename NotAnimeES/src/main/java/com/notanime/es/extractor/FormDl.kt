package com.notanime.es.extractor

import com.notanime.es.net.CronetHttp
import kotlinx.coroutines.delay
import java.io.IOException
import java.net.URLEncoder

object FormDl {

    private const val REFERER = "https://ww4.animeonline.ninja/"

    data class DlFlowResult(
        val html: String,
        val cookies: String
    )

    suspend fun fetchEmbedAndPostDl(embedUrl: String, dlUrl: String, fileCode: String): DlFlowResult {
        val page = CronetHttp.get(embedUrl, mapOf("referer" to REFERER))
        if (!page.isSuccessful) throw IOException("embed ${page.code}")
        val cookies = page.setCookies.joinToString("; ") { it.substringBefore(';') }
        val body = mapOf(
            "op" to "embed",
            "file_code" to fileCode,
            "auto" to "1",
            "referer" to REFERER
        ).entries.joinToString("&") { (key, value) ->
            "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
        }
        val post = CronetHttp.post(
            url = dlUrl,
            body = body,
            contentType = "application/x-www-form-urlencoded",
            headers = mapOf("referer" to embedUrl, "cookie" to cookies)
        )
        if (!post.isSuccessful) throw IOException("dl ${post.code}")
        return DlFlowResult(post.body, cookies)
    }

    suspend fun <T> retry3(label: String, block: suspend () -> T): T {
        var lastError: Exception = IllegalStateException("$label: failed")
        repeat(3) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                lastError = e
                if (attempt >= 2) throw lastError
                delay(1500)
            }
        }
        throw lastError
    }
}

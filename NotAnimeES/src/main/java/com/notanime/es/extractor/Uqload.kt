package com.notanime.es.extractor

import com.notanime.es.net.CronetHttp
import kotlinx.coroutines.delay
import java.io.IOException
import java.net.URLEncoder
import com.notanime.es.util.UnAnimeLog

object Uqload {

    private const val REFERER = "https://ww4.animeonline.ninja/"
    private const val DL_URL = "https://uqload.vc/dl"

    private val packedRegex = Regex(
        "eval\\(function\\(p,a,c,k,e,d\\)\\{[^}]*\\}\\('((?:\\\\.|[^'\\\\])*)',(\\d+),(\\d+),'([^']*)'\\.split\\('\\|'\\)"
    )
    private val escapeRegex = Regex("\\\\(.)")
    private val masterRegex = Regex("https?:[^\"']+?\\.m3u8[^\"']*")
    private val fileCodeRegex = Regex("(?:/e/|embed-)([A-Za-z0-9]+)")

    private fun unpackPacked(html: String): String? {
        val match = packedRegex.find(html) ?: return null
        var payload = match.groupValues[1].replace(escapeRegex, "$1")
        val radix = match.groupValues[2].toInt()
        val count = match.groupValues[3].toInt()
        val words = match.groupValues[4].split("|")
        for (i in count - 1 downTo 0) {
            val word = words.getOrNull(i) ?: continue
            if (word.isNotEmpty()) {
                payload = Regex("\\b${i.toString(radix)}\\b").replace(payload) { word }
            }
        }
        return payload
    }

    private fun formBody(fileCode: String): String = mapOf(
        "op" to "embed",
        "file_code" to fileCode,
        "auto" to "1",
        "referer" to REFERER
    ).entries.joinToString("&") { (key, value) ->
        "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
    }

    private suspend fun resolveOnce(embedUrl: String, code: String): StreamInfo {
        UnAnimeLog.d("uqload", "resolving $code")
        val page = CronetHttp.get(embedUrl, mapOf("referer" to REFERER))
        if (!page.isSuccessful) throw IOException("uqload ${page.code}")
        val cookies = page.setCookies.joinToString("; ") { it.substringBefore(';') }
        val post = CronetHttp.post(
            url = DL_URL,
            body = formBody(code),
            contentType = "application/x-www-form-urlencoded",
            headers = mapOf("referer" to embedUrl, "cookie" to cookies)
        )
        if (!post.isSuccessful) throw IOException("uqload dl ${post.code}")
        val js = unpackPacked(post.body)
        val master = js?.let { masterRegex.find(it)?.value } ?: throw IOException("uqload: stream not found")
        return StreamInfo(
            host = "uqload",
            videoId = code,
            qualities = Hls.masterWithVariants(master, "https://uqload.vc/")
        ).also { UnAnimeLog.d("uqload", "ok: ${it.qualities.size} qualities") }
    }

    suspend fun resolve(url: String): StreamInfo {
        val code = fileCodeRegex.find(url)?.groupValues?.get(1)
            ?: throw IOException("uqload: no file code")
        var lastError: Exception = IOException("uqload: failed")
        repeat(3) { attempt ->
            try {
                return resolveOnce(url, code)
            } catch (e: Exception) {
                lastError = e
                if (attempt < 2) {
                    UnAnimeLog.w("uqload", "attempt ${attempt + 1} failed", e)
                    delay(1500)
                }
            }
        }
        throw lastError
    }
}

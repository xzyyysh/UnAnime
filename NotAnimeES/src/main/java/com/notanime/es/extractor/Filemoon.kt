package com.notanime.es.extractor

import com.notanime.es.net.CronetHttp
import java.net.URI

object Filemoon {

    fun videoIdOf(url: String): String? = runCatching {
        val segments = URI(url).path.orEmpty().split('/').filter { it.isNotEmpty() }
        val index = segments.indexOfFirst { it.equals("e", ignoreCase = true) }
        if (index >= 0 && index + 1 < segments.size) segments[index + 1] else null
    }.getOrNull()

    suspend fun precheckError(url: String): String? {
        val id = videoIdOf(url) ?: return null
        val host = runCatching { URI(url).host }.getOrNull() ?: return null
        return try {
            val response = CronetHttp.get(
                "https://$host/api/videos/$id/embed/details",
                mapOf("referer" to url)
            )
            if (response.code == 404 || response.code == 410) {
                StreamErrors.reasonFrom(response.body) ?: StreamErrors.reasonFromCode(response.code)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}

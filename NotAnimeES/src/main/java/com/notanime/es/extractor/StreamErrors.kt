package com.notanime.es.extractor

import com.notanime.es.net.CronetHttp
import com.notanime.es.util.UnAnimeLog

object StreamErrors {

    private val markers: List<Pair<Regex, String>> = listOf(
        Regex("can'?t find the video", RegexOption.IGNORE_CASE) to "Video no encontrado",
        Regex("(video|file) (was|has been) (deleted|removed)", RegexOption.IGNORE_CASE) to "Video eliminado",
        Regex("(video|file) not found", RegexOption.IGNORE_CASE) to "Video no encontrado",
        Regex("no longer available", RegexOption.IGNORE_CASE) to "Video no disponible",
        Regex("video (has been )?disabled", RegexOption.IGNORE_CASE) to "Video deshabilitado",
        Regex("(geo-?blocked|geo-?restricted|not available in your country)", RegexOption.IGNORE_CASE) to "Bloqueado en tu región",
        Regex("(under maintenance|temporarily unavailable|try again later)", RegexOption.IGNORE_CASE) to "Servidor con problemas",
        Regex("copyright (claim|infringement|takedown)", RegexOption.IGNORE_CASE) to "Video retirado",
        Regex("(just a moment|checking your browser|challenge-platform|cf-chl|ddos-guard|enable javascript and cookies)", RegexOption.IGNORE_CASE) to "Protección anti-bot"
    )

    fun reasonFrom(html: String): String? =
        markers.firstOrNull { it.first.containsMatchIn(html) }?.second

    fun reasonFromCode(code: Int): String? = when (code) {
        404, 410 -> "Video eliminado"
        401, 403 -> "Acceso denegado"
        502, 503, 504 -> "Servidor caído"
        else -> null
    }

    suspend fun probe(url: String): String? {
        return try {
            val response = CronetHttp.get(url)
            if (!response.isSuccessful) {
                reasonFromCode(response.code) ?: "Error HTTP ${response.code}"
            } else {
                reasonFrom(response.body)
            }
        } catch (e: Exception) {
            UnAnimeLog.w("stream-errors", "probe failed $url", e)
            "Sin conexión al servidor"
        }
    }
}

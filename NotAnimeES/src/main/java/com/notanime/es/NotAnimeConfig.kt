package com.notanime.es

import java.net.URI

object NotAnimeConfig {
    const val ORIGIN = "https://ww4.animeonline.ninja"
    private const val HOST = "ww4.animeonline.ninja"

    fun isOriginHost(url: String): Boolean =
        runCatching { URI(url).host }.getOrNull()?.equals(HOST, ignoreCase = true) == true
}

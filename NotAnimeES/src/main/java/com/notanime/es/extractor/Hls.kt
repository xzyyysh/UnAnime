package com.notanime.es.extractor

import com.notanime.es.net.CronetHttp
import java.net.URI

data class HlsVariant(
    val label: String,
    val url: String,
    val bandwidth: Int?
)

object Hls {

    private val resolutionRegex = Regex("RESOLUTION=(\\d+)x(\\d+)")
    private val bandwidthRegex = Regex("BANDWIDTH=(\\d+)")

    fun parseMasterPlaylist(text: String, baseUrl: String? = null): List<HlsVariant> {
        val variants = mutableListOf<HlsVariant>()
        val lines = text.split("\n")
        for (i in lines.indices) {
            val line = lines[i].trim()
            if (!line.startsWith("#EXT-X-STREAM-INF")) continue
            val attrs = line.substringAfter(':')
            val resolution = resolutionRegex.find(attrs)
            val bandwidth = bandwidthRegex.find(attrs)
            val uri = lines.getOrNull(i + 1)?.trim().orEmpty()
            if (uri.isEmpty() || uri.startsWith("#")) continue
            val url = if (baseUrl != null && !uri.startsWith("http")) {
                runCatching { URI(baseUrl).resolve(uri).toString() }.getOrDefault(uri)
            } else {
                uri
            }
            variants.add(
                HlsVariant(
                    label = resolution?.let { "${it.groupValues[2]}p" } ?: "variant",
                    url = url,
                    bandwidth = bandwidth?.groupValues?.get(1)?.toInt()
                )
            )
        }
        return variants
    }

    suspend fun masterWithVariants(master: String, referer: String): List<StreamQuality> {
        val qualities = mutableListOf(StreamQuality("master", master))
        try {
            val response = CronetHttp.get(master, mapOf("referer" to referer))
            if (response.isSuccessful) {
                parseMasterPlaylist(response.body, master).forEach {
                    qualities.add(StreamQuality(it.label, it.url))
                }
            }
        } catch (_: Exception) {
        }
        qualities.sortWith { a, b ->
            when {
                a.label == "master" -> -1
                b.label == "master" -> 1
                else -> b.label.compareTo(a.label)
            }
        }
        return qualities
    }
}

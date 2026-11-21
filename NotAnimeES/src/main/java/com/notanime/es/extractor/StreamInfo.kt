package com.notanime.es.extractor

data class StreamQuality(
    val label: String,
    val url: String
)

data class StreamInfo(
    val host: String,
    val videoId: String?,
    val qualities: List<StreamQuality>,
    val referer: String? = null
)

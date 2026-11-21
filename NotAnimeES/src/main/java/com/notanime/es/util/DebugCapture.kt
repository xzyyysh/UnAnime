package com.notanime.es.util

import java.io.File

object DebugCapture {

    @Volatile var dir: File? = null

    fun capture(name: String, content: String) {
        val target = dir ?: return
        runCatching {
            File(target, name).writeText(content)
            UnAnimeLog.d("debug", "captured $name (${content.length} chars)")
        }
    }
}

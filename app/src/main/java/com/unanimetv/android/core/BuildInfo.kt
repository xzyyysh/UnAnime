package com.unanimetv.android.core

import android.content.Context

object BuildInfo {

    @Volatile private var versionName: String? = null

    fun init(context: Context) {
        versionName = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull()
    }

    val version: String get() = versionName ?: "1.0"
}

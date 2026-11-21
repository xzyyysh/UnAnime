package com.notanime.es.util

import android.util.Log

object UnAnimeLog {

    private const val TAG = "UnAnime"

    fun d(subTag: String, message: String) = log(Log.DEBUG, subTag, message, null)

    fun i(subTag: String, message: String) = log(Log.INFO, subTag, message, null)

    fun w(subTag: String, message: String, throwable: Throwable? = null) =
        log(Log.WARN, subTag, message, throwable)

    fun e(subTag: String, message: String, throwable: Throwable? = null) =
        log(Log.ERROR, subTag, message, throwable)

    private fun log(priority: Int, subTag: String, message: String, throwable: Throwable?) {
        val text = "[$subTag] $message"
        if (throwable != null) {
            Log.println(priority, TAG, "$text\n${Log.getStackTraceString(throwable)}")
        } else {
            Log.println(priority, TAG, text)
        }
    }
}

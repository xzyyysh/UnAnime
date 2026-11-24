package com.unanimetv.android.core.network

import android.webkit.CookieManager
import com.notanime.es.NotAnimeConfig
import com.notanime.es.util.UnAnimeLog
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object CloudflareGate {

    private const val TIMEOUT_MS = 120_000L
    private const val CLEARANCE_DOMAIN = ".animeonline.ninja"

    private val mutex = Mutex()

    private val _challengeUrl = MutableStateFlow<String?>(null)
    val challengeUrl: StateFlow<String?> = _challengeUrl

    suspend fun solve(url: String): Boolean = mutex.withLock {
        if (hasClearance()) return@withLock true
        UnAnimeLog.d("cloudflare", "gate open, waiting for user to pass challenge")
        _challengeUrl.value = url
        var solved = false
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < TIMEOUT_MS && _challengeUrl.value != null) {
            if (hasClearance()) {
                solved = true
                break
            }
            delay(500)
        }
        _challengeUrl.value = null
        if (solved) {
            CookieManager.getInstance().flush()
            UnAnimeLog.d("cloudflare", "cf_clearance acquired via gate (persisted)")
        } else {
            UnAnimeLog.w("cloudflare", "gate timeout or dismissed without clearance")
        }
        solved
    }

    fun clear() {
        val manager = CookieManager.getInstance()
        val expired = "Expires=Thu, 01 Jan 1970 00:00:00 GMT; Max-Age=0; Path=/"
        manager.setCookie(NotAnimeConfig.ORIGIN, "cf_clearance=; $expired")
        manager.setCookie(NotAnimeConfig.ORIGIN, "cf_clearance=; Domain=$CLEARANCE_DOMAIN; $expired")
        val stillThere = manager.getCookie(NotAnimeConfig.ORIGIN)?.contains("cf_clearance") == true
        UnAnimeLog.d("cloudflare", "clearance cleared (still present: $stillThere)")
        if (stillThere) {
            manager.removeAllCookies(null)
            UnAnimeLog.w("cloudflare", "targeted clear failed, removed all cookies")
        }
        manager.flush()
    }

    fun dismiss() {
        _challengeUrl.value = null
    }

    private fun hasClearance(): Boolean =
        CookieManager.getInstance()
            .getCookie(NotAnimeConfig.ORIGIN)
            ?.contains("cf_clearance") == true
}

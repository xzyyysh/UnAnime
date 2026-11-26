package com.notanime.es.net

import android.content.Context
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import org.chromium.net.CronetEngine
import org.chromium.net.CronetException
import org.chromium.net.UploadDataProviders
import org.chromium.net.UrlRequest
import org.chromium.net.UrlResponseInfo
import com.notanime.es.util.UnAnimeLog
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.CancellationException
import java.util.concurrent.Executors
import java.util.zip.GZIPInputStream
import java.util.zip.InflaterInputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class HttpResponse(
    val code: Int,
    val body: String,
    val setCookies: List<String>
) {
    val isSuccessful: Boolean get() = code in 200..299
}

object CronetHttp {
    const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/157.0.0.0 Mobile Safari/537.36"

    private const val BUFFER_SIZE = 128 * 1024
    private const val REQUEST_TIMEOUT_MS = 15_000L

    @Volatile private var defaultUserAgent: String? = null

    @Volatile private var engine: CronetEngine? = null

    private val executor = Executors.newCachedThreadPool { runnable ->
        Thread(runnable, "cronet-callback").apply { isDaemon = true }
    }

    fun init(context: Context) {
        if (engine != null) return
        engine = CronetEngine.Builder(context.applicationContext)
            .enableHttp2(true)
            .enableQuic(true)
            .build()
        UnAnimeLog.d("net", "cronet engine ready")
    }

    fun configure(defaultUserAgent: String? = null) {
        this.defaultUserAgent = defaultUserAgent
        UnAnimeLog.d("net", "user agent updated (${defaultUserAgent?.take(40)})")
    }

    suspend fun get(
        url: String,
        headers: Map<String, String> = emptyMap(),
        userAgent: String? = null
    ): HttpResponse = request(url, null, null, headers, userAgent)

    suspend fun post(
        url: String,
        body: String,
        contentType: String,
        headers: Map<String, String> = emptyMap(),
        userAgent: String? = null
    ): HttpResponse = request(url, body, contentType, headers, userAgent)

    private suspend fun request(
        url: String,
        body: String?,
        contentType: String?,
        headers: Map<String, String>,
        userAgent: String?
    ): HttpResponse {
        val engine = engine ?: throw IllegalStateException("CronetHttp.init not called")
        val method = if (body != null) "POST" else "GET"
        UnAnimeLog.d("net", "$method $url")
        return withTimeout(REQUEST_TIMEOUT_MS) {
            suspendCancellableCoroutine<HttpResponse> { cont ->
            val output = ByteArrayOutputStream()
            var readCount = 0
            var readBytes = 0L
            val buffer = ByteBuffer.allocateDirect(BUFFER_SIZE)
            val callback = object : UrlRequest.Callback() {
                override fun onRedirectReceived(request: UrlRequest, info: UrlResponseInfo, newLocationUrl: String) {
                    request.followRedirect()
                }

                override fun onResponseStarted(request: UrlRequest, info: UrlResponseInfo) {
                    UnAnimeLog.d("net", "$url body stream started")
                    request.read(buffer)
                }

                override fun onReadCompleted(request: UrlRequest, info: UrlResponseInfo, byteBuffer: ByteBuffer) {
                    try {
                        byteBuffer.flip()
                        val n = byteBuffer.remaining()
                        val chunk = ByteArray(n)
                        byteBuffer.get(chunk)
                        output.write(chunk)
                        readCount++
                        readBytes += n
                        byteBuffer.clear()
                        request.read(byteBuffer)
                    } catch (e: Exception) {
                        UnAnimeLog.e("net", "$method $url read error", e)
                        cont.resumeWithException(e)
                    }
                }

                override fun onSucceeded(request: UrlRequest, info: UrlResponseInfo) {
                    try {
                        val setCookies = info.allHeadersAsList
                            .filter { it.key.equals("set-cookie", ignoreCase = true) }
                            .map { it.value }
                        UnAnimeLog.d("net", "$method $url -> ${info.httpStatusCode} ($readCount reads, $readBytes bytes)")
                        val body = decode(output)
                        if (body.contains('\u0000')) {
                            UnAnimeLog.w("net", "$url body contains ${body.count { it == '\u0000' }} null chars ($readCount reads, $readBytes bytes)")
                        }
                        cont.resume(
                            HttpResponse(
                                code = info.httpStatusCode,
                                body = body,
                                setCookies = setCookies
                            )
                        )
                    } catch (e: Exception) {
                        UnAnimeLog.e("net", "$method $url callback error", e)
                        cont.resumeWithException(e)
                    }
                }

                override fun onFailed(request: UrlRequest, info: UrlResponseInfo, error: CronetException) {
                    UnAnimeLog.e("net", "$method $url failed", error)
                    cont.resumeWithException(error)
                }

                override fun onCanceled(request: UrlRequest, info: UrlResponseInfo?) {
                    cont.resumeWithException(CancellationException("request cancelled"))
                }
            }
            val builder = engine.newUrlRequestBuilder(url, callback, executor)
            builder.setHttpMethod(method)
            builder.addHeader("user-agent", userAgent ?: defaultUserAgent ?: USER_AGENT)
            builder.addHeader("accept-encoding", "identity")
            headers.forEach { (name, value) -> builder.addHeader(name, value) }
            if (body != null && contentType != null) {
                builder.addHeader("content-type", contentType)
                builder.setUploadDataProvider(UploadDataProviders.create(body.toByteArray()), executor)
            }
            val request = builder.build()
            cont.invokeOnCancellation { request.cancel() }
            request.start()
            }
        }
    }

    private fun decode(output: ByteArrayOutputStream): String {
        val bytes = output.toByteArray()
        val stream = when {
            bytes.size > 2 && bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte() ->
                GZIPInputStream(ByteArrayInputStream(bytes))
            bytes.size > 1 && (bytes[0].toInt() and 0x0f) == 8 ->
                InflaterInputStream(ByteArrayInputStream(bytes))
            else -> return String(bytes, Charsets.UTF_8)
        }
        return stream.use { it.readBytes().toString(Charsets.UTF_8) }
    }
}

package com.unanimetv.android.core.media

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.notanime.es.util.UnAnimeLog
import com.unanimetv.android.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object ImageDownloader {

    private const val CHANNEL_ID = "downloads"

    suspend fun download(context: Context, url: String, fileName: String) {
        val bytes = withContext(Dispatchers.IO) { fetch(url) }
        val savedName = withContext(Dispatchers.IO) { save(context, fileName, bytes) }
        notifyDownloaded(context, savedName, bytes)
    }

    fun safeFileName(base: String, index: Int): String {
        val clean = base.replace(Regex("[^A-Za-z0-9 _-]"), "").trim()
            .replace(Regex("\\s+"), "_")
            .take(60)
            .ifEmpty { "screenshot" }
        return "UnAnime_${clean}_${index + 1}.jpg"
    }

    private fun fetch(url: String): ByteArray {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000
        connection.instanceFollowRedirects = true
        try {
            if (connection.responseCode !in 200..299) error("HTTP ${connection.responseCode}")
            return connection.inputStream.use { it.readBytes() }
        } finally {
            connection.disconnect()
        }
    }

    private fun save(context: Context, fileName: String, bytes: ByteArray): String {
        File(context.cacheDir, "downloads").apply { mkdirs() }
            .resolve(fileName)
            .writeBytes(bytes)
        val resolver = context.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("MediaStore insert failed")
            resolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("output stream null")
            resolver.update(uri, ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }, null, null)
            UnAnimeLog.d("download", "saved via MediaStore: $fileName")
            return fileName
        }
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!dir.exists()) dir.mkdirs()
        var file = File(dir, fileName)
        var n = 1
        while (file.exists()) {
            file = File(dir, fileName.replace(".jpg", "_${n++}.jpg"))
        }
        file.writeBytes(bytes)
        MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("image/jpeg"), null)
        UnAnimeLog.d("download", "saved legacy: ${file.name}")
        return file.name
    }

    private fun notifyDownloaded(context: Context, fileName: String, bytes: ByteArray) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Descargas", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            UnAnimeLog.d("download", "post notifications not granted, skipping notification")
            return
        }
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_brand_mark)
            .setContentTitle("Imagen descargada")
            .setContentText(fileName)
            .setAutoCancel(true)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let { bitmap ->
            builder.setLargeIcon(bitmap)
            builder.setStyle(NotificationCompat.BigPictureStyle().bigPicture(bitmap).bigLargeIcon(null as android.graphics.Bitmap?))
        }
        manager.notify(fileName.hashCode(), builder.build())
        UnAnimeLog.d("download", "notification sent: $fileName")
    }
}

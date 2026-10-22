package com.kanae.soreada.reader

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipFile

/** Decodes pages straight from the EPUB zip, with a byte-sized bitmap cache. */
class PageImageLoader(file: File) {

    private val zip = ZipFile(file)
    private val mutex = Mutex()
    private val cacheLimit = minOf(
        Runtime.getRuntime().maxMemory() / 5,
        96L * 1024L * 1024L,
    ).toInt()

    private val cache = object : LruCache<String, Bitmap>(cacheLimit) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    @Volatile
    private var closed = false

    suspend fun load(entry: String, requestWidth: Int, requestHeight: Int): Bitmap? =
        withContext(Dispatchers.IO) {
            if (closed || requestWidth <= 0 || requestHeight <= 0) return@withContext null
            val key = "$entry@${requestWidth}x$requestHeight"
            cache.get(key)?.let { return@withContext it }
            mutex.withLock {
                if (closed) return@withLock null
                cache.get(key)?.let { return@withLock it }
                val bitmap = runCatching { decode(entry, requestWidth, requestHeight) }.getOrNull()
                if (bitmap != null) cache.put(key, bitmap)
                bitmap
            }
        }

    private fun decode(entry: String, requestWidth: Int, requestHeight: Int): Bitmap? {
        val zipEntry = zip.getEntry(entry) ?: return null
        val bytes = zip.getInputStream(zipEntry).use { it.readBytes() }
        if (bytes.isEmpty()) return null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (bounds.outWidth / (sample * 2) >= requestWidth &&
            bounds.outHeight / (sample * 2) >= requestHeight
        ) {
            sample *= 2
        }

        val decoded = BitmapFactory.decodeByteArray(
            bytes,
            0,
            bytes.size,
            BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            },
        ) ?: return null

        if (decoded.width <= requestWidth && decoded.height <= requestHeight) return decoded

        val scale = minOf(
            requestWidth.toFloat() / decoded.width,
            requestHeight.toFloat() / decoded.height,
        ).coerceAtMost(1f)
        if (scale >= 0.999f) return decoded
        val scaled = Bitmap.createScaledBitmap(
            decoded,
            (decoded.width * scale).toInt().coerceAtLeast(1),
            (decoded.height * scale).toInt().coerceAtLeast(1),
            true,
        )
        decoded.recycle()
        return scaled
    }

    fun close() {
        if (closed) return
        closed = true
        cache.evictAll()
        runCatching { zip.close() }
    }
}

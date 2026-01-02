package com.devsapiens.phonemagic.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.content.res.Resources
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URLEncoder
import com.devsapiens.phonemagic.R

/**
 * Simple disk LRU cache implemented by storing files in a directory and evicting oldest files
 * when total size exceeds [maxSizeBytes]. Thread-safe.
 */
class DiskLruImageCache(private val cacheDir: File, private val maxSizeBytes: Long = 20L * 1024L * 1024L) {

    init {
        if (!cacheDir.exists()) cacheDir.mkdirs()
    }

    private fun safeKey(key: String): String {
        // encode the key so it's safe as a filename
        return URLEncoder.encode(key, "utf-8")
    }

    @Synchronized
    fun getBitmap(key: String): Bitmap? {
        val file = File(cacheDir, safeKey(key))
        if (!file.exists()) return null
        return try {
            // update last modified to mark recently used
            file.setLastModified(System.currentTimeMillis())
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (_: Throwable) {
            null
        }
    }

    @Synchronized
    fun putBitmap(key: String, bitmap: Bitmap) {
        val file = File(cacheDir, safeKey(key))
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            }
            trimToSize()
        } catch (_: IOException) {
            // best-effort: ignore
        }
    }

    @Synchronized
    private fun trimToSize() {
        try {
            val files = cacheDir.listFiles() ?: return
            var total = files.fold(0L) { acc, f -> acc + f.length() }
            if (total <= maxSizeBytes) return
            // sort by lastModified ascending (oldest first)
            val sorted = files.sortedBy { it.lastModified() }
            for (f in sorted) {
                if (total <= maxSizeBytes) break
                val len = f.length()
                if (f.delete()) {
                    total -= len
                }
            }
        } catch (_: Throwable) {
            // ignore trimming errors
        }
    }

    @Suppress("unused")
    @Synchronized
    fun clear() {
        try {
            val files = cacheDir.listFiles() ?: return
            for (f in files) f.delete()
        } catch (_: Throwable) {
            // ignore
        }
    }
}

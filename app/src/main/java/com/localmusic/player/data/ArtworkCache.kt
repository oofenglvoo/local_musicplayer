package com.localmusic.player.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

object ArtworkCache {

    private const val DIR_NAME = "artwork"
    private const val MAX_DIMENSION = 600
    private const val MAX_CACHE_BYTES = 200L * 1024 * 1024

    fun artworkDir(context: Context): File =
        File(context.filesDir, DIR_NAME).apply { if (!exists()) mkdirs() }

    fun cacheKeyFor(path: String): String {
        val digest = MessageDigest.getInstance("MD5")
        val bytes = digest.digest(path.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun artworkFile(context: Context, cacheKey: String): File =
        File(artworkDir(context), "$cacheKey.jpg")

    private fun mtimeFile(context: Context, cacheKey: String): File =
        File(artworkDir(context), "$cacheKey.mtime")

    private fun sourceMtime(path: String): Long = runCatching { File(path).lastModified() }.getOrDefault(0L)

    fun existingArtwork(context: Context, cacheKey: String): File? {
        val f = artworkFile(context, cacheKey)
        return if (f.exists() && f.length() > 0) f else null
    }

    /** Saves already-extracted embedded picture bytes, downscaled, to the cache. */
    suspend fun savePicture(context: Context, audioPath: String, raw: ByteArray): String? =
        withContext(Dispatchers.IO) {
            val cacheKey = cacheKeyFor(audioPath)
            val target = artworkFile(context, cacheKey)
            val scaled = decodeScaled(raw) ?: return@withContext null
            val result = runCatching {
                target.outputStream().use { out ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, 88, out)
                }
                target.absolutePath
            }.getOrNull()
            scaled.recycle()
            if (result != null) {
                runCatching { mtimeFile(context, cacheKey).writeText(sourceMtime(audioPath).toString()) }
                runCatching { trim(context) }
            }
            result
        }

    suspend fun extractAndSave(context: Context, audioPath: String, cacheKey: String): String? =
        withContext(Dispatchers.IO) {
            val target = artworkFile(context, cacheKey)
            val sourceStamp = sourceMtime(audioPath)
            if (target.exists() && target.length() > 0) {
                val cachedStamp = runCatching { mtimeFile(context, cacheKey).readText().toLong() }.getOrNull()
                if (cachedStamp != null && cachedStamp == sourceStamp) return@withContext target.absolutePath
            }

            val raw = readEmbeddedPicture(audioPath) ?: return@withContext null
            val scaled = decodeScaled(raw) ?: return@withContext null
            val result = runCatching {
                target.outputStream().use { out ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, 88, out)
                }
                target.absolutePath
            }.getOrNull()
            scaled.recycle()
            if (result != null) {
                runCatching { mtimeFile(context, cacheKey).writeText(sourceStamp.toString()) }
                runCatching { trim(context) }
            }
            result
        }

    /** Evicts least-recently-used artwork files when the cache exceeds [MAX_CACHE_BYTES]. */
    private fun trim(context: Context) {
        val dir = artworkDir(context)
        val files = dir.listFiles()?.filter { it.name.endsWith(".jpg") } ?: return
        var total = files.sumOf { it.length() }
        if (total <= MAX_CACHE_BYTES) return
        files.sortedBy { it.lastModified() }.forEach { f ->
            if (total <= MAX_CACHE_BYTES) return
            val len = f.length()
            if (f.delete()) {
                total -= len
                mtimeFile(context, f.nameWithoutExtension).delete()
            }
        }
    }

    private fun readEmbeddedPicture(audioPath: String): ByteArray? {
        val file = File(audioPath)
        if (!file.exists()) return null
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(audioPath)
            retriever.embeddedPicture
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun decodeScaled(raw: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(raw, 0, raw.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var scale = 1
        while (bounds.outWidth / (scale * 2) >= MAX_DIMENSION ||
            bounds.outHeight / (scale * 2) >= MAX_DIMENSION
        ) {
            scale *= 2
        }

        val options = BitmapFactory.Options().apply { inSampleSize = scale }
        return runCatching {
            BitmapFactory.decodeByteArray(raw, 0, raw.size, options)
        }.getOrNull()
    }

    suspend fun clearAll(context: Context) = withContext(Dispatchers.IO) {
        artworkDir(context).listFiles()?.forEach { it.delete() }
    }
}

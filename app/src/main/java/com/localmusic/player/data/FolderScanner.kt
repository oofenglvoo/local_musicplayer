package com.localmusic.player.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.Build
import com.localmusic.player.data.db.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs

object FolderScanner {

    val AUDIO_EXTENSIONS = setOf(
        "mp3", "flac", "wav", "aac", "m4a", "ogg", "opus", "wma", "ape", "aiff", "mid", "amr",
    )

    fun isAudioFile(file: File): Boolean =
        file.isFile && file.extension.lowercase() in AUDIO_EXTENSIONS

    /**
     * Deterministic synthetic id for a file that is not present in MediaStore.
     * Derived from the absolute path so it is stable across rescans.
     */
    fun syntheticIdFor(path: String): Long =
        -(abs(path.hashCode().toLong()) + 1_000_000L)

    suspend fun scanDirectory(
        context: Context,
        root: File,
        excludedFolders: Set<String> = emptySet(),
        minDurationSec: Int = 0,
    ): List<SongEntity> = withContext(Dispatchers.IO) {
        val result = mutableListOf<SongEntity>()
        val stack = ArrayDeque<File>()
        if (root.isDirectory) stack.add(root)

        val excluded = excludedFolders.map { it.trimEnd('/') }.toSet()

        while (stack.isNotEmpty()) {
            currentCoroutineContext().ensureActive()
            val dir = stack.removeLast()
            val children = dir.listFiles() ?: continue
            for (child in children) {
                currentCoroutineContext().ensureActive()
                when {
                    child.isDirectory -> {
                        if (!child.name.startsWith(".") && !isExcluded(child, excluded)) {
                            stack.add(child)
                        }
                    }
                    isAudioFile(child) -> {
                        val song = readMetadata(context, child)
                        if (song.duration >= minDurationSec * 1000L) {
                            result += song
                        }
                    }
                }
            }
        }
        result
    }

    private fun isExcluded(dir: File, excluded: Set<String>): Boolean {
        val path = dir.absolutePath
        return excluded.any { path == it || path.startsWith("$it/") }
    }

    private suspend fun readMetadata(
        context: Context,
        file: File,
    ): SongEntity {
        var title = file.nameWithoutExtension
        var artist = "未知艺术家"
        var album = "未知专辑"
        var duration = 0L
        var track = 0
        var year = 0
        var disc = 0
        var artworkPath: String? = null

        val path = file.absolutePath
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(path)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.takeIf { it.isNotBlank() }?.let { title = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.takeIf { it.isNotBlank() && it != "<unknown>" }?.let { artist = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?.takeIf { it.isNotBlank() }?.let { album = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()?.let { duration = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
                ?.substringBefore('/')?.trim()?.toIntOrNull()?.let { track = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
                ?.substringBefore('-')?.trim()?.toIntOrNull()?.let { year = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER)
                ?.substringBefore('/')?.trim()?.toIntOrNull()?.let { disc = it }

            val embedded = runCatching { retriever.embeddedPicture }.getOrNull()
            if (embedded != null && embedded.isNotEmpty()) {
                artworkPath = runCatching {
                    ArtworkCache.savePicture(context, path, embedded)
                }.getOrNull()
            }
        } catch (_: Exception) {
        } finally {
            runCatching { retriever.release() }
        }

        return SongEntity(
            id = syntheticIdFor(path),
            title = title,
            artist = artist,
            album = album,
            albumId = 0L,
            artistId = 0L,
            duration = duration,
            path = path,
            trackNumber = track,
            size = file.length(),
            dateAdded = file.lastModified() / 1000L,
            displayName = file.name,
            mimeType = mimeForExtension(file.extension.lowercase()),
            artworkPath = artworkPath,
            year = year,
            discNumber = disc,
        )
    }

    private fun mimeForExtension(ext: String): String = when (ext) {
        "mp3" -> "audio/mpeg"
        "flac" -> "audio/flac"
        "wav" -> "audio/wav"
        "aac", "m4a" -> "audio/mp4"
        "ogg", "opus" -> "audio/ogg"
        "wma" -> "audio/x-ms-wma"
        else -> "audio/*"
    }

    fun listSubDirectories(root: File): List<File> =
        (root.listFiles() ?: emptyArray())
            .filter { it.isDirectory && !it.name.startsWith(".") }
            .sortedBy { it.name.lowercase() }

    fun roots(): List<File> {
        val candidates = mutableListOf<File>()
        candidates += File("/storage/emulated/0")
        candidates += File("/sdcard")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching {
                candidates += File("/storage").listFiles()?.filter { it.isDirectory } ?: emptyList()
            }
        }
        return candidates.map { canonicalish(it) }.distinctBy { it.absolutePath }
            .filter { it.exists() }
    }

    private fun canonicalish(f: File): File = runCatching { f.canonicalFile }.getOrDefault(f)

    fun commonMusicFolders(): List<File> {
        val base = File("/storage/emulated/0")
        return listOf("Music", "Download", "Downloads", "Documents", "Podcasts", "Recordings")
            .map { File(base, it) }
            .filter { it.exists() && it.isDirectory }
    }
}

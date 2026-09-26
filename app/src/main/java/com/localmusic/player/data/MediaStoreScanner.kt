package com.localmusic.player.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.Build
import android.provider.MediaStore
import com.localmusic.player.R
import com.localmusic.player.data.db.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File

object MediaStoreScanner {

    suspend fun scan(
        context: Context,
        excludedFolders: Set<String> = emptySet(),
        minDurationSec: Int = 0,
    ): List<SongEntity> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<SongEntity>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.YEAR,
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 0"

        val excluded = excludedFolders.map { it.trimEnd('/') }.toSet()

        context.contentResolver.query(
            collection,
            projection,
            selection,
            null,
            "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC",
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)

            while (cursor.moveToNext()) {
                currentCoroutineContext().ensureActive()
                val path = cursor.getString(dataCol) ?: ""
                if (excluded.isNotEmpty() && excluded.any { path == it || path.startsWith("$it/") }) {
                    continue
                }
                val duration = cursor.getLong(durationCol)
                if (duration < minDurationSec * 1000L) continue

                val artist = cursor.getString(artistCol)
                val rawTrack = cursor.getInt(trackCol)
                songs += SongEntity(
                    id = cursor.getLong(idCol),
                    title = cursor.getString(titleCol)
                        ?: context.getString(R.string.meta_unknown_song),
                    artist = if (artist.isNullOrBlank() || artist == "<unknown>") {
                        context.getString(R.string.meta_unknown_artist)
                    } else {
                        artist
                    },
                    album = cursor.getString(albumCol)
                        ?: context.getString(R.string.meta_unknown_album),
                    albumId = cursor.getLong(albumIdCol),
                    artistId = 0L,
                    duration = duration,
                    path = path,
                    trackNumber = if (rawTrack > 1000) rawTrack % 1000 else rawTrack,
                    size = cursor.getLong(sizeCol),
                    dateAdded = cursor.getLong(dateCol),
                    displayName = cursor.getString(nameCol) ?: "",
                    mimeType = cursor.getString(mimeCol) ?: "",
                    year = cursor.getInt(yearCol),
                    discNumber = if (rawTrack > 1000) rawTrack / 1000 else 0,
                )
            }
        }
        songs
    }

    suspend fun hasPermission(context: Context): Boolean = withContext(Dispatchers.IO) {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_AUDIO
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
        context.checkSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    suspend fun readEmbeddedPicture(path: String): ByteArray? = withContext(Dispatchers.IO) {
        val file = File(path)
        if (!file.exists()) return@withContext null
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(path)
            retriever.embeddedPicture
        } catch (e: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }
}

package com.localmusic.player.data

import android.content.ContentUris
import android.provider.MediaStore
import com.localmusic.player.data.db.SongEntity
import java.io.File

data class AlbumArtSource(
    val artworkPath: String?,
    val albumId: Long,
)

fun SongEntity.albumArtSource(): AlbumArtSource =
    AlbumArtSource(artworkPath = artworkPath, albumId = albumId)

fun AlbumArtSource.toModel(): Any? = when {
    !artworkPath.isNullOrBlank() && File(artworkPath).exists() -> File(artworkPath)
    albumId > 0L -> ContentUris.withAppendedId(
        MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
        albumId,
    )
    else -> null
}

fun albumArtUri(albumId: Long): android.net.Uri? =
    if (albumId > 0L) {
        ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, albumId)
    } else {
        null
    }

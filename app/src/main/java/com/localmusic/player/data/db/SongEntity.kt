package com.localmusic.player.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val artistId: Long,
    val duration: Long,
    val path: String,
    val trackNumber: Int,
    val size: Long,
    val dateAdded: Long,
    val displayName: String,
    val mimeType: String,
    val artworkPath: String? = null,
    val year: Int = 0,
    val discNumber: Int = 0,
    val playCount: Int = 0,
    val lastPlayedAt: Long = 0L,
)

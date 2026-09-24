package com.localmusic.player.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "queue_state")
data class QueueStateEntity(
    @PrimaryKey val id: Int = 0,
    val songId: Long,
    val positionMs: Long,
    val shuffleMode: Int,
    val repeatMode: Int,
    val updatedAt: Long,
)

@Entity(
    tableName = "queue_items",
    indices = [Index("songId")],
)
data class QueueItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: Long,
    val orderIndex: Int,
)

@Entity(
    tableName = "play_history",
    indices = [Index("songId"), Index("playedAt")],
)
data class PlayHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: Long,
    val playedAt: Long,
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: Long,
    val positionMs: Long,
    val label: String,
    val createdAt: Long,
)

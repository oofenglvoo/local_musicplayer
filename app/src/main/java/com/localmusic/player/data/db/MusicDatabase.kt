package com.localmusic.player.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        SongEntity::class,
        SongFtsEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class,
        FavoriteEntity::class,
        QueueStateEntity::class,
        QueueItemEntity::class,
        PlayHistoryEntity::class,
        BookmarkEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun queueDao(): QueueDao
    abstract fun playHistoryDao(): PlayHistoryDao
    abstract fun bookmarkDao(): BookmarkDao
}

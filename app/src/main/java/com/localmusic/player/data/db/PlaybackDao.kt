package com.localmusic.player.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface QueueDao {
    @Query("SELECT * FROM queue_state WHERE id = 0")
    suspend fun getState(): QueueStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveState(state: QueueStateEntity)

    @Query("SELECT * FROM queue_items ORDER BY orderIndex ASC")
    suspend fun getItems(): List<QueueItemEntity>

    @Query("SELECT * FROM queue_items ORDER BY orderIndex ASC")
    fun observeItems(): Flow<List<QueueItemEntity>>

    @Query("DELETE FROM queue_items")
    suspend fun clearItems()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<QueueItemEntity>)

    @Transaction
    suspend fun replaceQueue(items: List<QueueItemEntity>, state: QueueStateEntity) {
        clearItems()
        insertItems(items)
        saveState(state)
    }
}

@Dao
interface PlayHistoryDao {
    @Query("SELECT * FROM play_history ORDER BY playedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<PlayHistoryEntity>>

    @Insert
    suspend fun insert(history: PlayHistoryEntity)

    @Query("DELETE FROM play_history WHERE id NOT IN (SELECT id FROM play_history ORDER BY playedAt DESC LIMIT 500)")
    suspend fun trim()

    @Query("DELETE FROM play_history")
    suspend fun clear()
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE songId = :songId ORDER BY positionMs ASC")
    fun observeForSong(songId: Long): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE songId = :songId ORDER BY positionMs ASC")
    suspend fun getForSong(songId: Long): List<BookmarkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: BookmarkEntity): Long

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun delete(id: Long)
}

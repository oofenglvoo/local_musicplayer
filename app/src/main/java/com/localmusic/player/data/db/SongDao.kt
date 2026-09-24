package com.localmusic.player.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getById(id: Long): SongEntity?

    @Query("SELECT * FROM songs WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<SongEntity>

    @Query("SELECT * FROM songs ORDER BY dateAdded DESC LIMIT :limit")
    fun observeRecentlyAdded(limit: Int): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY playCount DESC LIMIT :limit")
    fun observeMostPlayed(limit: Int): Flow<List<SongEntity>>

    @Query("DELETE FROM songs")
    suspend fun clear()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(songs: List<SongEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(song: SongEntity)

    @Query("UPDATE songs SET playCount = playCount + 1, lastPlayedAt = :time WHERE id = :songId")
    suspend fun incrementPlayCount(songId: Long, time: Long)
}

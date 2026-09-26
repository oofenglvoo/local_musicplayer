package com.localmusic.player.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

data class PlayStat(
    val path: String,
    val playCount: Int,
    val lastPlayedAt: Long,
)

private const val PAGE_SIZE = 200L

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<SongEntity>>

    @Query(
        "SELECT * FROM songs WHERE rtrim(path, replace(path, '/', '')) = :folder ORDER BY title COLLATE NOCASE ASC",
    )
    fun observeByFolder(folder: String): Flow<List<SongEntity>>

    @Query(
        """
        SELECT songs.* FROM songs
        JOIN songs_fts ON songs_fts.rowid = songs.id
        WHERE songs_fts MATCH :matchQuery
        ORDER BY title COLLATE NOCASE ASC
        """,
    )
    suspend fun search(matchQuery: String): List<SongEntity>

    @Query(
        "SELECT * FROM songs WHERE title LIKE :likeQuery ORDER BY title COLLATE NOCASE ASC LIMIT :limit OFFSET :offset",
    )
    suspend fun searchByTitle(likeQuery: String, limit: Int, offset: Int): List<SongEntity>

    @Query(
        """
        SELECT DISTINCT rtrim(path, replace(path, '/', '')) AS folder
        FROM songs
        ORDER BY folder
        LIMIT :pageSize OFFSET :offset
        """,
    )
    suspend fun pageFolders(pageSize: Int, offset: Int): List<String>

    @Query(
        """
        SELECT DISTINCT rtrim(path, replace(path, '/', '')) AS folder
        FROM songs
        ORDER BY folder
        """,
    )
    fun observeFolders(): Flow<List<String>>

    suspend fun pagedFolders(): List<String> {
        var offset = 0
        val out = mutableListOf<String>()
        while (true) {
            val page = pageFolders(PAGE_SIZE.toInt(), offset)
            if (page.isEmpty()) break
            out += page
            offset += PAGE_SIZE.toInt()
        }
        return out
    }

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getById(id: Long): SongEntity?

    @Query("SELECT * FROM songs WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<SongEntity>

    @Query("SELECT path, playCount, lastPlayedAt FROM songs")
    suspend fun playStats(): List<PlayStat>

    @Query("SELECT * FROM songs ORDER BY dateAdded DESC LIMIT :limit")
    fun observeRecentlyAdded(limit: Int): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY playCount DESC LIMIT :limit")
    fun observeMostPlayed(limit: Int): Flow<List<SongEntity>>

    @Query("DELETE FROM songs")
    suspend fun clear()

    @Query("DELETE FROM songs_fts")
    suspend fun clearFts()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(songs: List<SongEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(song: SongEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFts(rows: List<SongFtsEntity>)

    @Transaction
    suspend fun replaceAll(songs: List<SongEntity>) {
        clear()
        clearFts()
        insertAll(songs)
        insertFts(
            songs.map {
                SongFtsEntity(
                    rowid = it.id,
                    title = it.title,
                    artist = it.artist,
                    album = it.album,
                    displayName = it.displayName,
                )
            }
        )
    }

    @Query("UPDATE songs SET playCount = playCount + 1, lastPlayedAt = :time WHERE id = :songId")
    suspend fun incrementPlayCount(songId: Long, time: Long)
}

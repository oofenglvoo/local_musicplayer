package com.localmusic.player.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt ASC")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylist(id: Long): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :name WHERE id = :id")
    suspend fun renamePlaylist(id: Long, name: String)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)

    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position ASC")
    fun observePlaylistSongs(playlistId: Long): Flow<List<PlaylistSongCrossRef>>

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun nextPosition(playlistId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRef(ref: PlaylistSongCrossRef)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSong(playlistId: Long, songId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId)")
    suspend fun containsSong(playlistId: Long, songId: Long): Boolean

    @Query("UPDATE playlist_songs SET position = :position WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun updatePosition(playlistId: Long, songId: Long, position: Int)

    @Transaction
    suspend fun reorder(playlistId: Long, orderedSongIds: List<Long>) {
        orderedSongIds.forEachIndexed { index, songId ->
            updatePosition(playlistId, songId, index)
        }
    }

    @Transaction
    suspend fun addSong(playlistId: Long, songId: Long) {
        insertCrossRef(PlaylistSongCrossRef(playlistId, songId, nextPosition(playlistId)))
    }

    @Transaction
    suspend fun addSongs(playlistId: Long, songIds: List<Long>) {
        var pos = nextPosition(playlistId)
        songIds.forEach { songId ->
            insertCrossRef(PlaylistSongCrossRef(playlistId, songId, pos))
            pos += 1
        }
    }
}

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun observeFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE songId = :songId)")
    fun observeIsFavorite(songId: Long): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE songId = :songId")
    suspend fun remove(songId: Long)

    @Query("SELECT songId FROM favorites")
    suspend fun favoriteIds(): List<Long>
}

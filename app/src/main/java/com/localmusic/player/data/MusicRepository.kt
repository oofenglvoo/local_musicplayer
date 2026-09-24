package com.localmusic.player.data

import android.content.Context
import com.localmusic.player.data.db.FavoriteDao
import com.localmusic.player.data.db.FavoriteEntity
import com.localmusic.player.data.db.PlaylistDao
import com.localmusic.player.data.db.PlaylistEntity
import com.localmusic.player.data.db.PlaylistSongCrossRef
import com.localmusic.player.data.db.SongDao
import com.localmusic.player.data.db.SongEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao,
    private val favoriteDao: FavoriteDao,
    private val settingsStore: SettingsStore,
) {
    val songs: Flow<List<SongEntity>> = songDao.observeAll()

    val playlists: Flow<List<PlaylistEntity>> = playlistDao.observePlaylists()

    val scannedFolders: Flow<Set<String>> = settingsStore.scannedFolders

    val favoriteIds: Flow<Set<Long>> = favoriteDao.observeFavorites()
        .map { list -> list.map { it.songId }.toSet() }

    fun songsInPlaylist(playlistId: Long): Flow<List<SongEntity>> =
        playlistDao.observePlaylistSongs(playlistId).map { refs ->
            if (refs.isEmpty()) emptyList()
            else {
                val map = songDao.getByIds(refs.map { it.songId }).associateBy { it.id }
                refs.mapNotNull { map[it.songId] }
            }
        }

    fun playlistRefs(playlistId: Long): Flow<List<PlaylistSongCrossRef>> =
        playlistDao.observePlaylistSongs(playlistId)

    suspend fun getPlaylistName(playlistId: Long): String? =
        playlistDao.getPlaylist(playlistId)?.name

    fun favoriteSongs(): Flow<List<SongEntity>> =
        favoriteDao.observeFavorites().map { favorites ->
            if (favorites.isEmpty()) emptyList()
            else {
                val map = songDao.getByIds(favorites.map { it.songId }).associateBy { it.id }
                favorites.mapNotNull { map[it.songId] }
            }
        }

    suspend fun getSong(id: Long): SongEntity? = songDao.getById(id)

    suspend fun refreshLibrary(): Int {
        val fromMediaStore = MediaStoreScanner.scan(context)
        val fromFolders = scanAllFolders()
        val merged = mergeSongs(fromMediaStore, fromFolders)
        songDao.clear()
        songDao.insertAll(merged)
        return merged.size
    }

    suspend fun rebuildWithFolder(path: String): Int {
        settingsStore.addScannedFolder(path)
        return refreshLibrary()
    }

    suspend fun removeFolder(path: String) {
        settingsStore.removeScannedFolder(path)
        refreshLibrary()
    }

    private suspend fun scanAllFolders(): List<SongEntity> {
        val folders = settingsStore.scannedFolders.first()
        val out = mutableListOf<SongEntity>()
        for (path in folders) {
            val dir = java.io.File(path)
            if (dir.exists() && dir.isDirectory) {
                out += FolderScanner.scanDirectory(context, dir)
            }
        }
        return out
    }

    private fun mergeSongs(
        mediaStore: List<SongEntity>,
        folders: List<SongEntity>,
    ): List<SongEntity> {
        val seenPaths = mediaStore.mapTo(mutableSetOf()) { it.path }
        val merged = mediaStore.toMutableList()
        for (song in folders) {
            if (song.path !in seenPaths) {
                seenPaths += song.path
                merged += song
            }
        }
        return merged.sortedWith(
            compareBy({ it.title.lowercase() }, { it.artist.lowercase() })
        )
    }

    suspend fun createPlaylist(name: String): Long =
        playlistDao.insertPlaylist(PlaylistEntity(name = name, createdAt = System.currentTimeMillis()))

    suspend fun renamePlaylist(id: Long, name: String) = playlistDao.renamePlaylist(id, name)

    suspend fun deletePlaylist(id: Long) = playlistDao.deletePlaylist(id)

    suspend fun addToPlaylist(playlistId: Long, songId: Long) = playlistDao.addSong(playlistId, songId)

    suspend fun removeFromPlaylist(playlistId: Long, songId: Long) =
        playlistDao.removeSong(playlistId, songId)

    suspend fun toggleFavorite(songId: Long) {
        if (favoriteDao.favoriteIds().contains(songId)) {
            favoriteDao.remove(songId)
        } else {
            favoriteDao.add(FavoriteEntity(songId, System.currentTimeMillis()))
        }
    }

    suspend fun readEmbeddedPicture(path: String): ByteArray? =
        MediaStoreScanner.readEmbeddedPicture(path)
}

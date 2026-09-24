package com.localmusic.player.data

import android.content.Context
import com.localmusic.player.data.db.BookmarkDao
import com.localmusic.player.data.db.BookmarkEntity
import com.localmusic.player.data.db.FavoriteDao
import com.localmusic.player.data.db.FavoriteEntity
import com.localmusic.player.data.db.PlayHistoryDao
import com.localmusic.player.data.db.PlayHistoryEntity
import com.localmusic.player.data.db.PlaylistDao
import com.localmusic.player.data.db.PlaylistEntity
import com.localmusic.player.data.db.PlaylistSongCrossRef
import com.localmusic.player.data.db.QueueDao
import com.localmusic.player.data.db.QueueItemEntity
import com.localmusic.player.data.db.QueueStateEntity
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
    private val queueDao: QueueDao,
    private val playHistoryDao: PlayHistoryDao,
    private val bookmarkDao: BookmarkDao,
    private val settingsStore: SettingsStore,
) {
    val songs: Flow<List<SongEntity>> = songDao.observeAll()

    val playlists: Flow<List<PlaylistEntity>> = playlistDao.observePlaylists()

    val scannedFolders: Flow<Set<String>> = settingsStore.scannedFolders

    val excludedFolders: Flow<Set<String>> = settingsStore.excludedFolders

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

    fun recentlyAdded(limit: Int = 100): Flow<List<SongEntity>> =
        songDao.observeRecentlyAdded(limit)

    fun mostPlayed(limit: Int = 100): Flow<List<SongEntity>> =
        songDao.observeMostPlayed(limit)

    fun recentlyPlayed(limit: Int = 100): Flow<List<SongEntity>> =
        playHistoryDao.observeRecent(limit).map { history ->
            val ids = history.map { it.songId }.distinct()
            if (ids.isEmpty()) emptyList()
            else {
                val map = songDao.getByIds(ids).associateBy { it.id }
                ids.mapNotNull { map[it] }
            }
        }

    suspend fun getSong(id: Long): SongEntity? = songDao.getById(id)

    suspend fun refreshLibrary(): Int {
        val excluded = settingsStore.excludedFolders.first()
        val minDuration = settingsStore.minDurationSec.first()
        val fromMediaStore = MediaStoreScanner.scan(context, excluded, minDuration)
        val fromFolders = scanAllFolders(excluded, minDuration)
        val merged = mergeSongs(fromMediaStore, fromFolders)
        val overrides = settingsStore.artworkOverrides.first()
        val withArtwork = if (overrides.isEmpty()) merged else merged.map { song ->
            val override = overrides[song.id]
            if (override.isNullOrBlank()) song else song.copy(artworkPath = override)
        }
        songDao.clear()
        songDao.insertAll(withArtwork)
        return withArtwork.size
    }

    suspend fun rebuildWithFolder(path: String): Int {
        settingsStore.addScannedFolder(path)
        return refreshLibrary()
    }

    suspend fun removeFolder(path: String) {
        settingsStore.removeScannedFolder(path)
    }

    suspend fun addExcludedFolder(path: String) {
        settingsStore.addExcludedFolder(path)
    }

    suspend fun removeExcludedFolder(path: String) {
        settingsStore.removeExcludedFolder(path)
    }

    private suspend fun scanAllFolders(
        excluded: Set<String>,
        minDurationSec: Int,
    ): List<SongEntity> {
        val folders = settingsStore.scannedFolders.first()
        val out = mutableListOf<SongEntity>()
        for (path in folders) {
            val dir = java.io.File(path)
            if (dir.exists() && dir.isDirectory) {
                out += FolderScanner.scanDirectory(context, dir, excluded, minDurationSec)
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

    suspend fun addSongsToPlaylist(playlistId: Long, songIds: List<Long>) =
        playlistDao.addSongs(playlistId, songIds)

    suspend fun removeFromPlaylist(playlistId: Long, songId: Long) =
        playlistDao.removeSong(playlistId, songId)

    suspend fun reorderPlaylist(playlistId: Long, orderedSongIds: List<Long>) =
        playlistDao.reorder(playlistId, orderedSongIds)

    suspend fun toggleFavorite(songId: Long) {
        if (favoriteDao.favoriteIds().contains(songId)) {
            favoriteDao.remove(songId)
        } else {
            favoriteDao.add(FavoriteEntity(songId, System.currentTimeMillis()))
        }
    }

    suspend fun recordPlay(songId: Long) {
        val now = System.currentTimeMillis()
        songDao.incrementPlayCount(songId, now)
        playHistoryDao.insert(PlayHistoryEntity(songId = songId, playedAt = now))
        playHistoryDao.trim()
    }

    suspend fun saveQueueState(
        songIds: List<Long>,
        currentSongId: Long,
        positionMs: Long,
        shuffleMode: Int,
        repeatMode: Int,
    ) {
        queueDao.replaceQueue(
            items = songIds.mapIndexed { index, id ->
                QueueItemEntity(songId = id, orderIndex = index)
            },
            state = QueueStateEntity(
                songId = currentSongId,
                positionMs = positionMs,
                shuffleMode = shuffleMode,
                repeatMode = repeatMode,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun loadQueueState(): Pair<List<Long>, QueueStateEntity?> {
        val items = queueDao.getItems().map { it.songId }
        val state = queueDao.getState()
        return items to state
    }

    suspend fun currentQueueSongIds(): List<Long> = queueDao.getItems().map { it.songId }

    fun bookmarksFor(songId: Long): Flow<List<BookmarkEntity>> = bookmarkDao.observeForSong(songId)

    suspend fun addBookmark(songId: Long, positionMs: Long, label: String) =
        bookmarkDao.insert(
            BookmarkEntity(
                songId = songId,
                positionMs = positionMs,
                label = label,
                createdAt = System.currentTimeMillis(),
            )
        )

    suspend fun deleteBookmark(id: Long) = bookmarkDao.delete(id)

    suspend fun readEmbeddedPicture(path: String): ByteArray? =
        MediaStoreScanner.readEmbeddedPicture(path)

    val artworkOverrides: Flow<Map<Long, String>> = settingsStore.artworkOverrides

    suspend fun setArtworkOverride(songId: Long, cachedPath: String) =
        settingsStore.setArtworkOverride(songId, cachedPath)

    suspend fun clearArtworkOverride(songId: Long) =
        settingsStore.clearArtworkOverride(songId)
}

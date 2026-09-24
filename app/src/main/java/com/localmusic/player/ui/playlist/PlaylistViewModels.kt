package com.localmusic.player.ui.playlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localmusic.player.data.MusicRepository
import com.localmusic.player.data.db.SongEntity
import com.localmusic.player.playback.PlayerConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    private val repository: MusicRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val playlistId: Long = savedStateHandle.get<Long>(ARG_PLAYLIST_ID) ?: -1L

    private val _playlistName = MutableStateFlow("")
    val playlistName: StateFlow<String> = _playlistName.asStateFlow()

    val songs: StateFlow<List<SongEntity>> = repository.songsInPlaylist(playlistId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteIds: StateFlow<Set<Long>> = repository.favoriteIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    init {
        viewModelScope.launch {
            repository.getPlaylistName(playlistId)?.let { _playlistName.value = it }
        }
    }

    fun playAll() {
        val list = songs.value
        if (list.isNotEmpty()) PlayerConnection.playSongs(list, 0)
    }

    fun shufflePlay() {
        val list = songs.value
        if (list.isNotEmpty()) PlayerConnection.playSongsShuffled(list)
    }

    fun removeFromPlaylist(songId: Long) {
        viewModelScope.launch { repository.removeFromPlaylist(playlistId, songId) }
    }

    fun move(from: Int, to: Int) {
        val list = songs.value.toMutableList()
        if (from !in list.indices || to !in list.indices) return
        val item = list.removeAt(from)
        list.add(to, item)
        viewModelScope.launch { repository.reorderPlaylist(playlistId, list.map { it.id }) }
    }

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch { repository.toggleFavorite(songId) }
    }

    companion object {
        const val ARG_PLAYLIST_ID = "playlistId"
    }
}

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repository: MusicRepository,
) : ViewModel() {

    val favoriteIds: StateFlow<Set<Long>> = repository.favoriteIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val songs: StateFlow<List<SongEntity>> = repository.favoriteSongs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch { repository.toggleFavorite(songId) }
    }
}

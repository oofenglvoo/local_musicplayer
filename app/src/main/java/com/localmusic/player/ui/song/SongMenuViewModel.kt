package com.localmusic.player.ui.song

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localmusic.player.data.MusicRepository
import com.localmusic.player.data.db.PlaylistEntity
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
class SongMenuViewModel @Inject constructor(
    private val repository: MusicRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val appContext: android.content.Context,
) : ViewModel() {

    val playlists: StateFlow<List<PlaylistEntity>> = repository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun consumeMessage() {
        _message.value = null
    }

    fun playNext(songs: List<SongEntity>) {
        PlayerConnection.playNext(songs)
        _message.value = appContext.getString(com.localmusic.player.R.string.song_menu_added_next)
    }

    fun addToQueue(songs: List<SongEntity>) {
        PlayerConnection.addToQueue(songs)
        _message.value = appContext.getString(com.localmusic.player.R.string.song_menu_added_queue)
    }

    fun addToPlaylist(playlistId: Long, songs: List<SongEntity>) {
        viewModelScope.launch {
            repository.addSongsToPlaylist(playlistId, songs.map { it.id })
            val name = repository.getPlaylistName(playlistId).orEmpty()
            _message.value = appContext.getString(com.localmusic.player.R.string.song_menu_added_playlist, songs.size, name)
        }
    }

    fun createPlaylistWith(name: String, songs: List<SongEntity>) {
        viewModelScope.launch {
            val id = repository.createPlaylist(name)
            repository.addSongsToPlaylist(id, songs.map { it.id })
            _message.value = appContext.getString(com.localmusic.player.R.string.song_menu_created_playlist, name, songs.size)
        }
    }

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch { repository.toggleFavorite(songId) }
    }

    fun setArtworkOverride(songId: Long, path: String) {
        viewModelScope.launch {
            repository.setArtworkOverride(songId, path)
            repository.refreshLibrary()
            _message.value = appContext.getString(com.localmusic.player.R.string.song_menu_artwork_updated)
        }
    }

    fun deleteSongs(songs: List<SongEntity>, context: android.content.Context, onDeleted: () -> Unit) {
        viewModelScope.launch {
            var ok = 0
            songs.forEach { song ->
                val songFile = java.io.File(song.path)
                val deleted = if (songFile.exists()) songFile.delete() else true
                if (deleted) {
                    context.contentResolver.delete(
                        android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        "${android.provider.MediaStore.Audio.Media.DATA} = ?",
                        arrayOf(song.path),
                    )
                    ok++
                }
            }
            repository.refreshLibrary()
            _message.value = appContext.getString(com.localmusic.player.R.string.song_menu_deleted, ok)
            onDeleted()
        }
    }
}

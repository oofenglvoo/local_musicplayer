package com.localmusic.player.lyrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localmusic.player.data.MusicRepository
import com.localmusic.player.playback.PlayerConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LyricsUiState(
    val loading: Boolean = false,
    val lyrics: Lyrics? = null,
    val songId: Long = -1L,
)

@HiltViewModel
class LyricsViewModel @Inject constructor(
    private val repository: MusicRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LyricsUiState())
    val state: StateFlow<LyricsUiState> = _state.asStateFlow()

    private var lastLoadedSongId = -1L

    fun loadFor(songId: Long, path: String) {
        if (songId == lastLoadedSongId) return
        lastLoadedSongId = songId
        _state.value = LyricsUiState(loading = true, songId = songId)
        viewModelScope.launch {
            val lyrics = if (path.isBlank()) null else LyricsLoader.load(path)
            _state.value = LyricsUiState(loading = false, lyrics = lyrics, songId = songId)
        }
    }
}

@HiltViewModel
class LyricsInlineViewModel @Inject constructor(
    private val repository: MusicRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LyricsUiState())
    val state: StateFlow<LyricsUiState> = _state.asStateFlow()

    private var lastLoadedSongId = -1L

    init {
        viewModelScope.launch {
            PlayerConnection.nowPlaying.collect { nowPlaying ->
                if (nowPlaying.isEmpty) {
                    lastLoadedSongId = -1L
                    _state.value = LyricsUiState()
                    return@collect
                }
                if (nowPlaying.songId == lastLoadedSongId) return@collect
                lastLoadedSongId = nowPlaying.songId
                _state.value = LyricsUiState(loading = true, songId = nowPlaying.songId)
                val song = repository.getSong(nowPlaying.songId)
                val lyrics = song?.path?.let { LyricsLoader.load(it) }
                _state.value = LyricsUiState(
                    loading = false,
                    lyrics = lyrics,
                    songId = nowPlaying.songId,
                )
            }
        }
    }
}

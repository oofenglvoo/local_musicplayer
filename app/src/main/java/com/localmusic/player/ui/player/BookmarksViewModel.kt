package com.localmusic.player.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localmusic.player.data.MusicRepository
import com.localmusic.player.data.db.BookmarkEntity
import com.localmusic.player.playback.PlayerConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val repository: MusicRepository,
) : ViewModel() {

    private val songId = MutableStateFlow(-1L)

    val bookmarks: StateFlow<List<BookmarkEntity>> = songId
        .flatMapLatest { id ->
            if (id <= 0) flowOf(emptyList()) else repository.bookmarksFor(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun setSong(id: Long) {
        songId.value = id
    }

    fun addBookmarkAtCurrent(label: String) {
        val id = songId.value
        if (id <= 0) return
        val position = PlayerConnection.controller()?.currentPosition ?: 0L
        viewModelScope.launch {
            repository.addBookmark(id, position, label.ifBlank { "书签" })
            _message.value = "已添加书签"
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.deleteBookmark(id) }
    }

    fun seekTo(positionMs: Long) {
        PlayerConnection.seekTo(positionMs)
    }

    fun consumeMessage() {
        _message.value = null
    }
}

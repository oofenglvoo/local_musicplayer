package com.localmusic.player.lyrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localmusic.player.data.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class LyricsEditState(
    val songPath: String = "",
    val lrcPath: String = "",
    val content: String = "",
    val offsetMs: Long = 0L,
    val loading: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class LyricsEditViewModel @Inject constructor(
    private val repository: MusicRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val appContext: android.content.Context,
) : ViewModel() {

    private val _state = MutableStateFlow(LyricsEditState())
    val state: StateFlow<LyricsEditState> = _state.asStateFlow()

    private val offsets = mutableMapOf<String, Long>()

    fun load(songId: Long) {
        viewModelScope.launch {
            _state.value = LyricsEditState(loading = true)
            val song = repository.getSong(songId) ?: run {
                _state.value = LyricsEditState(
                    message = appContext.getString(com.localmusic.player.R.string.lyrics_song_missing)
                )
                return@launch
            }
            val lrcPath = withContext(Dispatchers.IO) { findLrcPath(song.path) }
            val content = withContext(Dispatchers.IO) {
                if (lrcPath != null) runCatching { File(lrcPath).readText() }.getOrDefault("")
                else ""
            }
            _state.value = LyricsEditState(
                songPath = song.path,
                lrcPath = lrcPath.orEmpty(),
                content = content,
                offsetMs = offsets[song.path] ?: 0L,
                loading = false,
            )
        }
    }

    fun updateContent(content: String) {
        _state.value = _state.value.copy(content = content)
    }

    fun adjustOffset(deltaMs: Long) {
        val newOffset = (_state.value.offsetMs + deltaMs).coerceIn(-30_000L, 30_000L)
        offsets[_state.value.songPath] = newOffset
        _state.value = _state.value.copy(
            offsetMs = newOffset,
            message = appContext.getString(com.localmusic.player.R.string.lyrics_offset_message, newOffset),
        )
    }

    fun currentOffset(path: String): Long = offsets[path] ?: 0L

    fun save(onSaved: () -> Unit) {
        val state = _state.value
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val target = state.lrcPath.ifBlank { deriveLrcPath(state.songPath) }
                    val withOffset = if (state.offsetMs != 0L) {
                        "[offset:${state.offsetMs}]\n" + state.content
                    } else {
                        state.content
                    }
                    File(target).writeText(withOffset)
                    target
                }.getOrNull()
            }
            _state.value = _state.value.copy(
                lrcPath = result ?: state.lrcPath,
                message = if (result != null) {
                    appContext.getString(com.localmusic.player.R.string.lyrics_saved)
                } else {
                    appContext.getString(com.localmusic.player.R.string.lyrics_save_failed)
                },
            )
            if (result != null) onSaved()
        }
    }

    fun consumeMessage() {
        _state.value = _state.value.copy(message = null)
    }

    private fun findLrcPath(audioPath: String): String? {
        val audio = File(audioPath)
        val parent = audio.parentFile ?: return null
        val base = audio.nameWithoutExtension
        listOf("$base.lrc", "$base.LRC", "$base.Lrc").forEach { name ->
            val f = File(parent, name)
            if (f.exists()) return f.absolutePath
        }
        return null
    }

    private fun deriveLrcPath(audioPath: String): String {
        val audio = File(audioPath)
        return File(audio.parentFile, "${audio.nameWithoutExtension}.lrc").absolutePath
    }
}

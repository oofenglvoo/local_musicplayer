package com.localmusic.player.ui.folder

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localmusic.player.data.FolderScanner
import com.localmusic.player.data.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class BrowserState(
    val currentDir: File? = null,
    val subDirs: List<File> = emptyList(),
    val audioCount: Int = 0,
    val canGoUp: Boolean = false,
)

data class ScanProgressState(
    val scanned: Int,
    val total: Int,
    val currentName: String,
)

@HiltViewModel
class FolderBrowserViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val settingsStore: com.localmusic.player.data.SettingsStore,
    savedStateHandle: SavedStateHandle,
    @dagger.hilt.android.qualifiers.ApplicationContext private val appContext: android.content.Context,
) : ViewModel() {

    private val _state = MutableStateFlow(BrowserState())
    val state: StateFlow<BrowserState> = _state.asStateFlow()

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    private val _progress = MutableStateFlow<ScanProgressState?>(null)
    val progress: StateFlow<ScanProgressState?> = _progress.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val targetPlaylistId: Long = savedStateHandle.get<Long>(ARG_PLAYLIST_ID) ?: -1L
    val isPlaylistMode: Boolean = targetPlaylistId > 0L

    val scannedFolders: StateFlow<Set<String>> = repository.scannedFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val roots: List<File> = FolderScanner.roots()
    val shortcuts: List<File> = FolderScanner.commonMusicFolders()

    fun open(dir: File) {
        viewModelScope.launch {
            _state.value = withContext(Dispatchers.IO) {
                val subDirs = FolderScanner.listSubDirectories(dir)
                val audio = (dir.listFiles() ?: emptyArray()).count { FolderScanner.isAudioFile(it) }
                BrowserState(
                    currentDir = dir,
                    subDirs = subDirs,
                    audioCount = audio,
                    canGoUp = dir.parentFile?.let { it.canRead() && it.absolutePath != dir.absolutePath } == true,
                )
            }
            runCatching { settingsStore.setLastBrowserDir(dir.absolutePath) }
        }
    }

    suspend fun rememberAndResolveStart(): File? {
        val saved = runCatching { settingsStore.lastBrowserDir.first() }.getOrNull()
        val savedDir = saved?.let { File(it) }?.takeIf { it.exists() && it.isDirectory }
        return savedDir ?: shortcuts.firstOrNull() ?: roots.firstOrNull()
    }

    fun goUp() {
        val parent = _state.value.currentDir?.parentFile ?: return
        open(parent)
    }

    fun scanCurrentFolder() {
        val dir = _state.value.currentDir ?: return
        _scanning.value = true
        _progress.value = ScanProgressState(0, 0, "")
        viewModelScope.launch {
            val count = runCatching {
                if (isPlaylistMode) {
                    repository.scanFolderIntoPlaylist(targetPlaylistId, dir.absolutePath) { p ->
                        _progress.value = ScanProgressState(p.scanned, p.total, p.currentName)
                    }
                } else {
                    repository.rebuildWithFolder(dir.absolutePath)
                }
            }.getOrDefault(0)
            _scanning.value = false
            _progress.value = null
            _message.value = appContext.getString(
                com.localmusic.player.R.string.folder_scanned_message,
                dir.name,
                count,
            )
        }
    }

    fun excludeCurrentFolder() {
        val dir = _state.value.currentDir ?: return
        _scanning.value = true
        viewModelScope.launch {
            val count = runCatching {
                repository.addExcludedFolder(dir.absolutePath)
                repository.refreshLibrary()
            }.getOrDefault(0)
            _scanning.value = false
            _message.value = appContext.getString(
                com.localmusic.player.R.string.folder_excluded_message,
                dir.name,
                count,
            )
        }
    }

    fun removeFolder(path: String) {
        _scanning.value = true
        viewModelScope.launch {
            runCatching {
                repository.removeFolder(path)
                repository.refreshLibrary()
            }
            _scanning.value = false
            _message.value = appContext.getString(com.localmusic.player.R.string.folder_removed_message)
        }
    }

    fun rescanAll() {
        _scanning.value = true
        viewModelScope.launch {
            val count = runCatching { repository.refreshLibrary() }.getOrDefault(0)
            _scanning.value = false
            _message.value = appContext.getString(com.localmusic.player.R.string.folder_rescan_message, count)
        }
    }

    fun consumeMessage() {
        _message.value = null
    }

    companion object {
        const val ARG_PLAYLIST_ID = "playlistId"
    }
}

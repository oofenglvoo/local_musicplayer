package com.localmusic.player.ui.folder

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

@HiltViewModel
class FolderBrowserViewModel @Inject constructor(
    private val repository: MusicRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BrowserState())
    val state: StateFlow<BrowserState> = _state.asStateFlow()

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

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
        }
    }

    fun goUp() {
        val parent = _state.value.currentDir?.parentFile ?: return
        open(parent)
    }

    fun scanCurrentFolder() {
        val dir = _state.value.currentDir ?: return
        _scanning.value = true
        viewModelScope.launch {
            val count = runCatching { repository.rebuildWithFolder(dir.absolutePath) }
                .getOrDefault(0)
            _scanning.value = false
            _message.value = "已扫描 ${dir.name}，音乐库共 $count 首"
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
            _message.value = "已排除 ${dir.name}，音乐库共 $count 首"
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
            _message.value = "已移除目录并更新音乐库"
        }
    }

    fun rescanAll() {
        _scanning.value = true
        viewModelScope.launch {
            val count = runCatching { repository.refreshLibrary() }.getOrDefault(0)
            _scanning.value = false
            _message.value = "重新扫描完成，共 $count 首"
        }
    }

    fun consumeMessage() {
        _message.value = null
    }
}

package com.localmusic.player.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localmusic.player.data.AlbumGroup
import com.localmusic.player.data.ArtistGroup
import com.localmusic.player.data.LibraryGrouper
import com.localmusic.player.data.MusicRepository
import com.localmusic.player.data.SettingsStore
import com.localmusic.player.data.SongSort
import com.localmusic.player.data.db.PlaylistEntity
import com.localmusic.player.data.db.SongEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LibraryTab(val label: String) {
    SONGS("歌曲"),
    ALBUMS("专辑"),
    ARTISTS("艺术家"),
    FOLDERS("文件夹"),
    PLAYLISTS("播放列表"),
}

data class SongsUiState(
    val songs: List<SongEntity> = emptyList(),
    val sort: SongSort = SongSort.TITLE,
    val ascending: Boolean = true,
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    val songs: StateFlow<List<SongEntity>> = repository.songs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<PlaylistEntity>> = repository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteIds: StateFlow<Set<Long>> = repository.favoriteIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val recentlyAdded: StateFlow<List<SongEntity>> = repository.recentlyAdded()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mostPlayed: StateFlow<List<SongEntity>> = repository.mostPlayed()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyPlayed: StateFlow<List<SongEntity>> = repository.recentlyPlayed()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sortField: StateFlow<SongSort> = settingsStore.sortField
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SongSort.TITLE)

    val sortAscending: StateFlow<Boolean> = settingsStore.sortAscending
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val gridAlbums: StateFlow<Boolean> = settingsStore.gridAlbums
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val selectedTab: StateFlow<LibraryTab> = settingsStore.libraryTab
        .map { name -> runCatching { LibraryTab.valueOf(name) }.getOrDefault(LibraryTab.SONGS) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryTab.SONGS)

    val sortedSongs: StateFlow<List<SongEntity>> =
        combine(repository.songs, settingsStore.sortField, settingsStore.sortAscending) { list, field, asc ->
            LibraryGrouper.sortSongs(list, field, asc)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val albums = combine(repository.songs, settingsStore.sortField, settingsStore.sortAscending) { list, field, asc ->
        val sorted = LibraryGrouper.sortSongs(list, field, asc)
        LibraryGrouper.albums(sorted)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val artists = combine(repository.songs, settingsStore.sortField, settingsStore.sortAscending) { list, field, asc ->
        val sorted = LibraryGrouper.sortSongs(list, field, asc)
        LibraryGrouper.artists(sorted)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders = repository.songs
        .let { flow ->
            combine(flow, settingsStore.sortAscending) { list, asc ->
                LibraryGrouper.folders(list).let { if (asc) it else it.reversed() }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    fun albumByKey(key: String): Flow<AlbumGroup?> =
        repository.songs.map { list ->
            LibraryGrouper.albums(list).firstOrNull { it.key == key }
        }

    fun artistByName(name: String): Flow<ArtistGroup?> =
        repository.songs.map { list ->
            LibraryGrouper.artists(list).firstOrNull { it.artist.equals(name, ignoreCase = true) }
        }

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            val count = runCatching { repository.refreshLibrary() }.getOrDefault(0)
            _loading.value = false
            _message.value = "已更新音乐库，共 $count 首"
        }
    }

    fun consumeMessage() {
        _message.value = null
    }

    fun setSort(field: SongSort) {
        viewModelScope.launch { settingsStore.setSortField(field) }
    }

    fun toggleSortDirection() {
        viewModelScope.launch { settingsStore.setSortAscending(!sortAscending.value) }
    }

    fun setTab(tab: LibraryTab) {
        viewModelScope.launch { settingsStore.setLibraryTab(tab.name) }
    }

    fun createPlaylist(name: String, onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch { onCreated(repository.createPlaylist(name)) }
    }

    fun renamePlaylist(id: Long, name: String) {
        viewModelScope.launch { repository.renamePlaylist(id, name) }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch { repository.deletePlaylist(id) }
    }

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch { repository.toggleFavorite(songId) }
    }

    fun songsInPlaylist(id: Long): Flow<List<SongEntity>> = repository.songsInPlaylist(id)
}

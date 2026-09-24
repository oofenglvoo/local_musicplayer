package com.localmusic.player.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localmusic.player.data.AlbumGroup
import com.localmusic.player.data.ArtistGroup
import com.localmusic.player.data.LibraryGrouper
import com.localmusic.player.data.MusicRepository
import com.localmusic.player.data.SettingsStore
import com.localmusic.player.data.db.SongEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchResults(
    val query: String = "",
    val songs: List<SongEntity> = emptyList(),
    val albums: List<AlbumGroup> = emptyList(),
    val artists: List<ArtistGroup> = emptyList(),
) {
    val isEmpty: Boolean
        get() = query.isNotBlank() && songs.isEmpty() && albums.isEmpty() && artists.isEmpty()
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val history: StateFlow<List<String>> = settingsStore.searchHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<Set<Long>> = repository.favoriteIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val results: StateFlow<SearchResults> =
        combine(repository.songs, _query) { all, q ->
            if (q.isBlank()) {
                SearchResults(query = q)
            } else {
                val matched = all.filter { song ->
                    song.title.contains(q, ignoreCase = true) ||
                        song.artist.contains(q, ignoreCase = true) ||
                        song.album.contains(q, ignoreCase = true) ||
                        song.displayName.contains(q, ignoreCase = true)
                }
                SearchResults(
                    query = q,
                    songs = matched,
                    albums = LibraryGrouper.albums(matched),
                    artists = LibraryGrouper.artists(matched),
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchResults())

    fun setQuery(q: String) {
        _query.value = q
    }

    fun commitSearch(q: String) {
        if (q.isBlank()) return
        viewModelScope.launch { settingsStore.addSearchTerm(q.trim()) }
    }

    fun clearHistory() {
        viewModelScope.launch { settingsStore.clearSearchHistory() }
    }

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch { repository.toggleFavorite(songId) }
    }
}

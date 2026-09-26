package com.localmusic.player.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.localmusic.player.R
import com.localmusic.player.data.AlbumArtSource
import com.localmusic.player.playback.PlayerConnection
import com.localmusic.player.ui.Artwork
import com.localmusic.player.ui.library.SectionHeader
import com.localmusic.player.ui.SongRow

private enum class SearchTab(@androidx.annotation.StringRes val labelRes: Int) {
    SONGS(com.localmusic.player.R.string.search_tab_songs),
    ALBUMS(com.localmusic.player.R.string.search_tab_albums),
    ARTISTS(com.localmusic.player.R.string.search_tab_artists),
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onOpenAlbum: (String) -> Unit = {},
    onOpenArtist: (String) -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favorites.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { viewModel.setQuery(it) },
                        placeholder = { Text(stringResource(R.string.search_hint)) },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.common_clear_input))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                query.isBlank() -> {
                    SearchHistory(
                        history = history,
                        onSelect = { viewModel.setQuery(it) },
                        onClear = { viewModel.clearHistory() },
                    )
                }
                results.isEmpty -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(R.string.search_no_result, query),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                else -> {
                    PrimaryTabRow(selectedTabIndex = tab) {
                        SearchTab.entries.forEachIndexed { index, t ->
                            Tab(
                                selected = tab == index,
                                onClick = { tab = index },
                                text = {
                                    val count = when (t) {
                                        SearchTab.SONGS -> results.songs.size
                                        SearchTab.ALBUMS -> results.albums.size
                                        SearchTab.ARTISTS -> results.artists.size
                                    }
                                    Text(stringResource(R.string.search_tab_count, stringResource(t.labelRes), count))
                                },
                            )
                        }
                    }
                    when (SearchTab.entries[tab]) {
                        SearchTab.SONGS -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(results.songs, key = { it.id }) { song ->
                                SongRow(
                                    song = song,
                                    isFavorite = song.id in favoriteIds,
                                    onFavoriteClick = { viewModel.toggleFavorite(song.id) },
                                    onClick = {
                                        viewModel.commitSearch(query)
                                        PlayerConnection.playSongs(results.songs, results.songs.indexOf(song))
                                    },
                                )
                            }
                        }
                        SearchTab.ALBUMS -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(results.albums, key = { it.key }) { album ->
                                ListItem(
                                    modifier = Modifier.clickable {
                                        viewModel.commitSearch(query)
                                        onOpenAlbum(album.key)
                                    },
                                    headlineContent = { Text(album.album, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    supportingContent = { Text(stringResource(R.string.search_album_subtitle, album.artist, album.songs.size)) },
                                    leadingContent = {
                                        Artwork(
                                            source = AlbumArtSource(album.artworkPath, album.albumId),
                                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
                                        )
                                    },
                                )
                            }
                        }
                        SearchTab.ARTISTS -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(results.artists, key = { it.artist.lowercase() }) { artist ->
                                ListItem(
                                    modifier = Modifier.clickable {
                                        viewModel.commitSearch(query)
                                        onOpenArtist(artist.artist)
                                    },
                                    headlineContent = { Text(artist.artist, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    supportingContent = { Text(stringResource(R.string.search_artist_subtitle, artist.songs.size, artist.albumCount)) },
                                    leadingContent = {
                                        Artwork(
                                            source = AlbumArtSource(artist.artworkPath, artist.representative?.albumId ?: 0L),
                                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(24.dp)),
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun SearchHistory(
    history: List<String>,
    onSelect: (String) -> Unit,
    onClear: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.search_history), style = MaterialTheme.typography.titleMedium)
            if (history.isNotEmpty()) {
                TextButton(onClick = onClear) { Text(stringResource(R.string.common_clear)) }
            }
        }
        if (history.isEmpty()) {
            Text(
                stringResource(R.string.search_empty_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        } else {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                history.forEach { term ->
                    ListItem(
                        modifier = Modifier.clickable { onSelect(term) },
                        headlineContent = { Text(term) },
                        leadingContent = {
                            Icon(Icons.Default.History, contentDescription = null)
                        },
                    )
                }
            }
        }
    }
}

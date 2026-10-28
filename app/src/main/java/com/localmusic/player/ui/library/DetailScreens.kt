package com.localmusic.player.ui.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.localmusic.player.R
import com.localmusic.player.data.AlbumGroup
import com.localmusic.player.data.FolderGroup
import com.localmusic.player.playback.PlayerConnection
import com.localmusic.player.ui.ScrollableSongList
import com.localmusic.player.ui.SongRow
import com.localmusic.player.ui.SongSearchAction
import com.localmusic.player.ui.SongSearchField
import com.localmusic.player.ui.filterSongs
import com.localmusic.player.ui.rememberCurrentSongId
import com.localmusic.player.ui.rememberSongSearchState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoListScreen(
    kind: AutoList,
    viewModel: LibraryViewModel,
    onBack: () -> Unit,
) {
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val currentSongId = rememberCurrentSongId()
    val songs by when (kind) {
        AutoList.ALL_SONGS -> viewModel.sortedSongs
        AutoList.RECENTLY_ADDED -> viewModel.recentlyAdded
        AutoList.RECENTLY_PLAYED -> viewModel.recentlyPlayed
        AutoList.MOST_PLAYED -> viewModel.mostPlayed
    }.collectAsStateWithLifecycle()
    val search = rememberSongSearchState()
    val filtered = remember(songs, search.query) { filterSongs(songs, search.query) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = {
                    if (search.active) {
                        SongSearchField(state = search)
                    } else {
                        Text(
                            "${stringResource(kind.labelRes)} · ${songs.size}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { if (search.active) search.close() else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    if (!search.active && songs.isNotEmpty()) {
                        SongSearchAction(state = search)
                        IconButton(onClick = { PlayerConnection.playSongs(songs, 0) }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.common_play_all))
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            songs.isEmpty() -> {
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    EmptyHint(stringResource(R.string.library_empty_hint))
                }
            }
            filtered.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(R.string.search_no_result, search.query.trim()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            else -> {
                ScrollableSongList(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(bottom = 8.dp),
                ) {
                    itemsIndexed(filtered, key = { _, s -> s.id }) { _, song ->
                        SongRow(
                            song = song,
                            isFavorite = song.id in favoriteIds,
                            isCurrent = song.id == currentSongId,
                            onFavoriteClick = { viewModel.toggleFavorite(song.id) },
                            onClick = { PlayerConnection.playSongs(songs, songs.indexOf(song)) },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    albumKey: String,
    viewModel: LibraryViewModel,
    onBack: () -> Unit,
) {
    val album by viewModel.albumByKey(albumKey)
        .collectAsStateWithLifecycle(initialValue = null)
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val currentSongId = rememberCurrentSongId()
    val search = rememberSongSearchState()

    val current = album
    val currentSongs = current?.songs.orEmpty()
    val filtered = remember(currentSongs, search.query) { filterSongs(currentSongs, search.query) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = {
                    if (search.active) {
                        SongSearchField(state = search)
                    } else {
                        Text(
                            current?.album ?: stringResource(R.string.library_tab_albums),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { if (search.active) search.close() else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    if (!search.active && currentSongs.isNotEmpty()) {
                        SongSearchAction(state = search)
                        IconButton(onClick = { PlayerConnection.playSongsShuffled(currentSongs) }) {
                            Icon(Icons.Default.Shuffle, contentDescription = stringResource(R.string.common_shuffle_all))
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (current == null) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                EmptyHint(stringResource(R.string.library_empty_hint))
            }
        } else {
            ScrollableSongList(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                item {
                    PlayableHeader(
                        title = current.album,
                        subtitle = buildString {
                            append(current.artist)
                            if (current.year > 0) append(" · ${current.year}")
                            append(" · " + stringResource(R.string.playlist_song_count, current.songs.size))
                        },
                        artworkPath = current.artworkPath,
                        albumId = current.albumId,
                        onPlayAll = { PlayerConnection.playSongs(current.songs, 0) },
                        onShuffle = { PlayerConnection.playSongsShuffled(current.songs) },
                    )
                }
                if (filtered.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                stringResource(R.string.search_no_result, search.query.trim()),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    itemsIndexed(filtered, key = { _, s -> s.id }) { _, song ->
                        SongRow(
                            song = song,
                            isFavorite = song.id in favoriteIds,
                            isCurrent = song.id == currentSongId,
                            onFavoriteClick = { viewModel.toggleFavorite(song.id) },
                            onClick = { PlayerConnection.playSongs(current.songs, current.songs.indexOf(song)) },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(
    artistName: String,
    viewModel: LibraryViewModel,
    onOpenAlbum: (AlbumGroup) -> Unit,
    onBack: () -> Unit,
) {
    val artist by viewModel.artistByName(artistName)
        .collectAsStateWithLifecycle(initialValue = null)
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val currentSongId = rememberCurrentSongId()
    val search = rememberSongSearchState()

    val current = artist
    val artistSongs = current?.songs.orEmpty()
    val filtered = remember(artistSongs, search.query) { filterSongs(artistSongs, search.query) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = {
                    if (search.active) {
                        SongSearchField(state = search)
                    } else {
                        Text(
                            current?.artist ?: stringResource(R.string.library_tab_artists),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { if (search.active) search.close() else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    if (!search.active && artistSongs.isNotEmpty()) {
                        SongSearchAction(state = search)
                        IconButton(onClick = { PlayerConnection.playSongsShuffled(artistSongs) }) {
                            Icon(Icons.Default.Shuffle, contentDescription = stringResource(R.string.common_shuffle_all))
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (current == null) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                EmptyHint(stringResource(R.string.library_empty_hint))
            }
        } else {
            ScrollableSongList(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                item {
                    PlayableHeader(
                        title = current.artist,
                        subtitle = stringResource(R.string.search_artist_subtitle, current.songs.size, current.albumCount),
                        artworkPath = current.artworkPath,
                        albumId = current.representative?.albumId ?: 0L,
                        onPlayAll = { PlayerConnection.playSongs(current.songs, 0) },
                        onShuffle = { PlayerConnection.playSongsShuffled(current.songs) },
                    )
                }
                if (filtered.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                stringResource(R.string.search_no_result, search.query.trim()),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    itemsIndexed(filtered, key = { _, s -> s.id }) { _, song ->
                        SongRow(
                            song = song,
                            isFavorite = song.id in favoriteIds,
                            isCurrent = song.id == currentSongId,
                            onFavoriteClick = { viewModel.toggleFavorite(song.id) },
                            onClick = { PlayerConnection.playSongs(current.songs, current.songs.indexOf(song)) },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailScreen(
    folderPath: String,
    viewModel: LibraryViewModel,
    onBack: () -> Unit,
) {
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val currentSongId = rememberCurrentSongId()
    val folderSongs by viewModel.songsInFolder(folderPath)
        .collectAsStateWithLifecycle(emptyList())
    val folder: FolderGroup? = folders.firstOrNull { it.path == folderPath }
    val search = rememberSongSearchState()
    val filtered = remember(folderSongs, search.query) { filterSongs(folderSongs, search.query) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = {
                    if (search.active) {
                        SongSearchField(state = search)
                    } else {
                        Text(
                            folder?.name
                                ?: folderPath.substringAfterLast('/').ifBlank { stringResource(R.string.library_tab_folders) },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { if (search.active) search.close() else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    if (!search.active && folderSongs.isNotEmpty()) {
                        SongSearchAction(state = search)
                    }
                },
            )
        },
    ) { padding ->
        if (folder == null) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                EmptyHint(stringResource(R.string.library_empty_hint))
            }
        } else {
            ScrollableSongList(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                if (filtered.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                stringResource(R.string.search_no_result, search.query.trim()),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    itemsIndexed(filtered, key = { _, s -> s.id }) { _, song ->
                        SongRow(
                            song = song,
                            isFavorite = song.id in favoriteIds,
                            isCurrent = song.id == currentSongId,
                            onFavoriteClick = { viewModel.toggleFavorite(song.id) },
                            onClick = { PlayerConnection.playSongs(folderSongs, folderSongs.indexOf(song)) },
                        )
                    }
                }
            }
        }
    }
}

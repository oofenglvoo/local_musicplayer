package com.localmusic.player.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.localmusic.player.data.AlbumGroup
import com.localmusic.player.data.FolderGroup
import com.localmusic.player.playback.PlayerConnection
import com.localmusic.player.ui.SongRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoListScreen(
    kind: AutoList,
    viewModel: LibraryViewModel,
    onBack: () -> Unit,
) {
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val songs by when (kind) {
        AutoList.RECENTLY_ADDED -> viewModel.recentlyAdded
        AutoList.RECENTLY_PLAYED -> viewModel.recentlyPlayed
        AutoList.MOST_PLAYED -> viewModel.mostPlayed
    }.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${kind.label} · ${songs.size}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (songs.isNotEmpty()) {
                        IconButton(onClick = { PlayerConnection.playSongs(songs, 0) }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "播放全部")
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (songs.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                EmptyHint("暂无歌曲")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                itemsIndexed(songs, key = { _, s -> s.id }) { index, song ->
                    SongRow(
                        song = song,
                        isFavorite = song.id in favoriteIds,
                        onFavoriteClick = { viewModel.toggleFavorite(song.id) },
                        onClick = { PlayerConnection.playSongs(songs, index) },
                    )
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

    val current = album
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(current?.album ?: "专辑") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    val list = current?.songs
                    if (!list.isNullOrEmpty()) {
                        IconButton(onClick = { PlayerConnection.playSongsShuffled(list) }) {
                            Icon(Icons.Default.Shuffle, contentDescription = "随机播放")
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (current == null) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                EmptyHint("未找到该专辑")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                item {
                    PlayableHeader(
                        title = current.album,
                        subtitle = buildString {
                            append(current.artist)
                            if (current.year > 0) append(" · ${current.year}")
                            append(" · ${current.songs.size} 首")
                        },
                        artworkPath = current.artworkPath,
                        albumId = current.albumId,
                        onPlayAll = { PlayerConnection.playSongs(current.songs, 0) },
                        onShuffle = { PlayerConnection.playSongsShuffled(current.songs) },
                    )
                }
                itemsIndexed(current.songs, key = { _, s -> s.id }) { index, song ->
                    SongRow(
                        song = song,
                        isFavorite = song.id in favoriteIds,
                        onFavoriteClick = { viewModel.toggleFavorite(song.id) },
                        onClick = { PlayerConnection.playSongs(current.songs, index) },
                    )
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

    val current = artist
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(current?.artist ?: "艺术家") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    val list = current?.songs
                    if (!list.isNullOrEmpty()) {
                        IconButton(onClick = { PlayerConnection.playSongsShuffled(list) }) {
                            Icon(Icons.Default.Shuffle, contentDescription = "随机播放")
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (current == null) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                EmptyHint("未找到该艺术家")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                item {
                    PlayableHeader(
                        title = current.artist,
                        subtitle = "${current.songs.size} 首 · ${current.albumCount} 张专辑",
                        artworkPath = current.artworkPath,
                        albumId = current.representative?.albumId ?: 0L,
                        onPlayAll = { PlayerConnection.playSongs(current.songs, 0) },
                        onShuffle = { PlayerConnection.playSongsShuffled(current.songs) },
                    )
                }
                itemsIndexed(current.songs, key = { _, s -> s.id }) { index, song ->
                    SongRow(
                        song = song,
                        isFavorite = song.id in favoriteIds,
                        onFavoriteClick = { viewModel.toggleFavorite(song.id) },
                        onClick = { PlayerConnection.playSongs(current.songs, index) },
                    )
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
    val folder: FolderGroup? = folders.firstOrNull { it.path == folderPath }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(folder?.name ?: "文件夹") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        if (folder == null) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                EmptyHint("未找到该文件夹")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                itemsIndexed(folder.songs, key = { _, s -> s.id }) { index, song ->
                    SongRow(
                        song = song,
                        isFavorite = song.id in favoriteIds,
                        onFavoriteClick = { viewModel.toggleFavorite(song.id) },
                        onClick = { PlayerConnection.playSongs(folder.songs, index) },
                    )
                }
            }
        }
    }
}

package com.localmusic.player.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.localmusic.player.data.db.SongEntity
import com.localmusic.player.playback.PlayerConnection
import com.localmusic.player.ui.library.LibraryViewModel
import com.localmusic.player.util.toDurationString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onOpenFolderPicker: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var showOnlyFavorites by remember { mutableStateOf(false) }

    val displayed = remember(songs, query, favoriteIds, showOnlyFavorites) {
        songs.filter { song ->
            (query.isBlank() ||
                song.title.contains(query, ignoreCase = true) ||
                song.artist.contains(query, ignoreCase = true)) &&
                (!showOnlyFavorites || song.id in favoriteIds)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("本地音乐 · ${songs.size}") },
                actions = {
                    IconButton(onClick = { showOnlyFavorites = !showOnlyFavorites }) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = "收藏",
                            tint = if (showOnlyFavorites) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onOpenFolderPicker) {
                        Icon(Icons.Default.Folder, contentDescription = "选择目录")
                    }
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "重新扫描")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SearchBar(query = query, onQueryChange = { query = it })

            if (displayed.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = when {
                            songs.isEmpty() -> "未发现本地音乐，点击右上角扫描目录或重新扫描"
                            showOnlyFavorites -> "还没有收藏歌曲"
                            else -> "没有匹配的歌曲"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(displayed, key = { _, s -> s.id }) { index, song ->
                        SongRow(
                            song = song,
                            isFavorite = song.id in favoriteIds,
                            onFavoriteClick = { viewModel.toggleFavorite(song.id) },
                            onClick = { PlayerConnection.playSongs(displayed, index) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    androidx.compose.material3.OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text("搜索歌曲 / 艺术家") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
    )
}

@Composable
fun SongRow(
    song: SongEntity,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = {
            Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(
                "${song.artist} · ${song.album}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingContent = {
            SongArtwork(
                song = song,
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    song.duration.toDurationString(),
                    style = MaterialTheme.typography.labelSmall,
                )
                IconButton(onClick = onFavoriteClick) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline,
                    )
                }
            }
        },
    )
}

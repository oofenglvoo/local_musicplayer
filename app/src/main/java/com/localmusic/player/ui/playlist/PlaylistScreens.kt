package com.localmusic.player.ui.playlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.localmusic.player.data.db.SongEntity
import com.localmusic.player.ui.Artwork
import com.localmusic.player.ui.SongRow
import com.localmusic.player.data.AlbumArtSource
import com.localmusic.player.playback.PlayerConnection
import com.localmusic.player.ui.library.LibraryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistListScreen(
    onOpenPlaylist: (Long) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<Pair<Long, String>?>(null) }
    var deleteTarget by remember { mutableStateOf<Pair<Long, String>?>(null) }
    var menuOpenId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("播放列表") },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "新建播放列表")
                    }
                },
            )
        },
    ) { padding ->
        if (playlists.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text("还没有播放列表，点击右上角新建", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(playlists, key = { it.id }) { playlist ->
                    ListItem(
                        modifier = Modifier.clickable { onOpenPlaylist(playlist.id) },
                        headlineContent = { Text(playlist.name) },
                        supportingContent = {
                            Text("创建于 ${formatDate(playlist.createdAt)}")
                        },
                        leadingContent = {
                            Icon(Icons.Default.PlaylistPlay, contentDescription = null)
                        },
                        trailingContent = {
                            Box {
                                IconButton(onClick = { menuOpenId = playlist.id }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "更多")
                                }
                                DropdownMenu(
                                    expanded = menuOpenId == playlist.id,
                                    onDismissRequest = { menuOpenId = null },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("重命名") },
                                        leadingIcon = {
                                            Icon(Icons.Default.Edit, contentDescription = null)
                                        },
                                        onClick = {
                                            renameTarget = playlist.id to playlist.name
                                            menuOpenId = null
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text("删除", color = MaterialTheme.colorScheme.error)
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                            )
                                        },
                                        onClick = {
                                            deleteTarget = playlist.id to playlist.name
                                            menuOpenId = null
                                        },
                                    )
                                }
                            }
                        },
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        NameDialog(
            title = "新建播放列表",
            initial = "",
            confirmLabel = "创建",
            onDismiss = { showCreateDialog = false },
            onConfirm = {
                if (it.isNotBlank()) viewModel.createPlaylist(it)
                showCreateDialog = false
            },
        )
    }

    renameTarget?.let { (id, current) ->
        NameDialog(
            title = "重命名播放列表",
            initial = current,
            confirmLabel = "保存",
            onDismiss = { renameTarget = null },
            onConfirm = {
                if (it.isNotBlank()) viewModel.renamePlaylist(id, it)
                renameTarget = null
            },
        )
    }

    deleteTarget?.let { (id, name) ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除播放列表") },
            text = { Text("确定要删除「$name」吗？歌曲文件不会被删除。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePlaylist(id)
                    deleteTarget = null
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun NameDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("播放列表名称") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.trim()) }) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    onBack: () -> Unit,
    viewModel: PlaylistDetailViewModel = hiltViewModel(),
) {
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val allSongs by viewModel.allSongs.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val playlistName by viewModel.playlistName.collectAsStateWithLifecycle()
    var reorderMode by remember { mutableStateOf(false) }
    var removeTarget by remember { mutableStateOf<SongEntity?>(null) }
    var showAddSongs by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(playlistName.ifBlank { "播放列表" }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.playAll() }) {
                        Icon(Icons.Default.PlaylistPlay, contentDescription = "播放全部")
                    }
                    IconButton(onClick = { viewModel.shufflePlay() }) {
                        Icon(Icons.Default.Shuffle, contentDescription = "随机播放")
                    }
                    IconButton(onClick = { selectedIds = emptySet(); showAddSongs = true }) {
                        Icon(Icons.Default.Add, contentDescription = "添加歌曲")
                    }
                    IconButton(onClick = { reorderMode = !reorderMode }) {
                        Icon(
                            Icons.Default.DragHandle,
                            contentDescription = "调整顺序",
                            tint = if (reorderMode) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
    ) { padding ->
        if (songs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text("播放列表为空", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            if (reorderMode) {
                ReorderableSongList(
                    songs = songs,
                    modifier = Modifier.fillMaxSize().padding(padding),
                    onMove = { from, to -> viewModel.move(from, to) },
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                    items(songs, key = { it.id }) { song ->
                        SongRow(
                            song = song,
                            isFavorite = song.id in favoriteIds,
                            onFavoriteClick = { viewModel.toggleFavorite(song.id) },
                            onClick = { PlayerConnection.playSongs(songs, songs.indexOf(song)) },
                            onLongClick = { removeTarget = song },
                        )
                    }
                }
            }
        }
    }

    removeTarget?.let { song ->
        AlertDialog(
            onDismissRequest = { removeTarget = null },
            title = { Text("从播放列表移除") },
            text = { Text("将「${song.title}」从该播放列表移除？（不会删除文件）") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeFromPlaylist(song.id)
                    removeTarget = null
                }) { Text("移除") }
            },
            dismissButton = {
                TextButton(onClick = { removeTarget = null }) { Text("取消") }
            },
        )
    }

    if (showAddSongs) {
        AlertDialog(
            onDismissRequest = { showAddSongs = false },
            title = { Text("添加歌曲") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    items(allSongs, key = { it.id }) { song ->
                        val checked = song.id in selectedIds
                        ListItem(
                            modifier = Modifier.clickable {
                                selectedIds = if (checked) selectedIds - song.id else selectedIds + song.id
                            },
                            headlineContent = { Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            supportingContent = { Text(song.artist, maxLines = 1) },
                            leadingContent = {
                                androidx.compose.material3.Checkbox(
                                    checked = checked,
                                    onCheckedChange = null,
                                )
                            },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addSongs(selectedIds.toList())
                    showAddSongs = false
                }) { Text("添加（${selectedIds.size}）") }
            },
            dismissButton = { TextButton(onClick = { showAddSongs = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun ReorderableSongList(
    songs: List<SongEntity>,
    modifier: Modifier = Modifier,
    onMove: (Int, Int) -> Unit,
) {
    val listState = rememberLazyListState()
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }

    LazyColumn(state = listState, modifier = modifier) {
        items(songs, key = { it.id }) { song ->
            val index = songs.indexOf(song)
            val isDragging = draggingIndex == index
            ListItem(
                modifier = Modifier.pointerInput(song.id) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { draggingIndex = index },
                        onDragEnd = { draggingIndex = null; dragOffset = 0f },
                        onDragCancel = { draggingIndex = null; dragOffset = 0f },
                    ) { change, dragAmount ->
                        change.consume()
                        dragOffset += dragAmount.y
                        val itemHeight = 72f
                        if (dragOffset > itemHeight / 2 && index < songs.lastIndex) {
                            onMove(index, index + 1)
                            draggingIndex = index + 1
                            dragOffset = 0f
                        } else if (dragOffset < -itemHeight / 2 && index > 0) {
                            onMove(index, index - 1)
                            draggingIndex = index - 1
                            dragOffset = 0f
                        }
                    }
                },
                headlineContent = { Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                supportingContent = { Text(song.artist, maxLines = 1) },
                leadingContent = {
                    Icon(
                        Icons.Default.DragHandle,
                        contentDescription = null,
                        tint = if (isDragging) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onBack: () -> Unit = {},
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的喜欢 · ${songs.size}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (songs.isNotEmpty()) {
                        IconButton(onClick = { PlayerConnection.playSongsShuffled(songs) }) {
                            Icon(Icons.Default.Shuffle, contentDescription = "随机播放")
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (songs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text("还没有收藏歌曲", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(songs, key = { it.id }) { song ->
                    SongRow(
                        song = song,
                        isFavorite = song.id in favoriteIds,
                        onFavoriteClick = { viewModel.toggleFavorite(song.id) },
                        onClick = { PlayerConnection.playSongs(songs, songs.indexOf(song)) },
                    )
                }
            }
        }
    }
}

private fun formatDate(ms: Long): String {
    if (ms <= 0) return "未知"
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(ms))
}

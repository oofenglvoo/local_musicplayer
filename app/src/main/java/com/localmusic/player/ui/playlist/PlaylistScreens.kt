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
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.localmusic.player.R
import com.localmusic.player.data.db.SongEntity
import com.localmusic.player.playback.PlayerConnection
import com.localmusic.player.ui.SongRow

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
                title = { Text(playlistName.ifBlank { stringResource(R.string.library_tab_playlists) }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.playAll() }) {
                        Icon(Icons.Default.PlaylistPlay, contentDescription = stringResource(R.string.common_play_all))
                    }
                    IconButton(onClick = { viewModel.shufflePlay() }) {
                        Icon(Icons.Default.Shuffle, contentDescription = stringResource(R.string.common_shuffle_all))
                    }
                    IconButton(onClick = { selectedIds = emptySet(); showAddSongs = true }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.playlist_add_songs))
                    }
                    IconButton(onClick = { reorderMode = !reorderMode }) {
                        Icon(
                            Icons.Default.DragHandle,
                            contentDescription = null,
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
                Text(stringResource(R.string.playlist_empty), style = MaterialTheme.typography.bodyMedium)
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
            title = { Text(stringResource(R.string.playlist_remove_from)) },
            text = { Text(stringResource(R.string.detail_remove_confirm, song.title)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeFromPlaylist(song.id)
                    removeTarget = null
                }) { Text(stringResource(R.string.common_remove)) }
            },
            dismissButton = {
                TextButton(onClick = { removeTarget = null }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }

    if (showAddSongs) {
        AlertDialog(
            onDismissRequest = { showAddSongs = false },
            title = { Text(stringResource(R.string.playlist_add_songs)) },
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
                }) { Text(stringResource(R.string.detail_add_count, selectedIds.size)) }
            },
            dismissButton = { TextButton(onClick = { showAddSongs = false }) { Text(stringResource(R.string.common_cancel)) } },
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
                title = { Text("${stringResource(R.string.playlist_favorites)} · ${songs.size}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    if (songs.isNotEmpty()) {
                        IconButton(onClick = { PlayerConnection.playSongsShuffled(songs) }) {
                            Icon(Icons.Default.Shuffle, contentDescription = stringResource(R.string.common_shuffle_all))
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
                Text(stringResource(R.string.playlist_empty), style = MaterialTheme.typography.bodyMedium)
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

package com.localmusic.player.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.localmusic.player.R
import com.localmusic.player.data.AlbumArtSource
import com.localmusic.player.data.db.SongEntity
import com.localmusic.player.ui.Artwork
import com.localmusic.player.ui.FastScrollbar
import com.localmusic.player.ui.library.LibraryViewModel
import com.localmusic.player.ui.playlist.FavoritesViewModel
import com.localmusic.player.ui.theme.AppAccent

@Composable
fun HomeScreen(
    onOpenSearch: () -> Unit,
    onOpenAllSongs: () -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    onOpenFavorites: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val favoriteViewModel: FavoritesViewModel = hiltViewModel()
    val favoriteSongs by favoriteViewModel.songs.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<Pair<Long, String>?>(null) }
    var deleteTarget by remember { mutableStateOf<Pair<Long, String>?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(com.localmusic.player.ui.theme.AppPageTop, MaterialTheme.colorScheme.background),
                ),
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SearchEntry(onClick = onOpenSearch, modifier = Modifier.weight(1f))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.tab_home),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { showCreate = true },
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = AppAccent,
                    modifier = Modifier.size(20.dp),
                )
                Text(stringResource(R.string.playlist_new_short), style = MaterialTheme.typography.bodyMedium, color = AppAccent)
            }
        }

        val gridState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            LazyColumn(
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                item {
                    PlaylistRowItem(
                        title = stringResource(R.string.common_all_songs),
                        subtitle = stringResource(R.string.home_all_songs_subtitle),
                        song = null,
                        badge = Icons.Default.MusicNote,
                        onClick = onOpenAllSongs,
                    )
                }
                item {
                    PlaylistRowItem(
                        title = stringResource(R.string.playlist_favorites),
                        subtitle = stringResource(R.string.playlist_favorites_subtitle),
                        song = favoriteSongs.firstOrNull(),
                        badge = Icons.Default.Favorite,
                        onClick = onOpenFavorites,
                    )
                }
                items(playlists, key = { it.id }) { playlist ->
                    val playlistSongs by viewModel.songsInPlaylist(playlist.id)
                        .collectAsStateWithLifecycle(emptyList())
                    PlaylistRowItem(
                        title = playlist.name,
                        subtitle = stringResource(R.string.playlist_mine),
                        song = playlistSongs.firstOrNull(),
                        badge = null,
                        onClick = { onOpenPlaylist(playlist.id) },
                        onMore = { renameTarget = playlist.id to playlist.name },
                    )
                }
            }
            FastScrollbar(
                listState = gridState,
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            )
        }
    }

    if (showCreate) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text(stringResource(R.string.playlist_new_short)) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text(stringResource(R.string.playlist_name_short)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                DialogActionButton(onClick = {
                    if (name.isNotBlank()) viewModel.createPlaylist(name.trim())
                    showCreate = false
                }) { Text(stringResource(R.string.common_create)) }
            },
            dismissButton = {
                DialogActionButton(onClick = { showCreate = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }

    renameTarget?.let { (id, current) ->
        var name by remember(renameTarget) { mutableStateOf(current) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text(stringResource(R.string.playlist_actions)) },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true) },
            confirmButton = {
                DialogActionButton(onClick = {
                    if (name.isNotBlank()) viewModel.renamePlaylist(id, name.trim())
                    renameTarget = null
                }) { Text(stringResource(R.string.playlist_rename)) }
            },
            dismissButton = {
                DialogActionButton(onClick = {
                    deleteTarget = id to current
                    renameTarget = null
                }) { Text(stringResource(R.string.common_delete)) }
            },
        )
    }
    deleteTarget?.let { (id, name) ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.playlist_delete_short)) },
            text = { Text(stringResource(R.string.playlist_delete_confirm_short, name)) },
            confirmButton = { DialogActionButton(onClick = { viewModel.deletePlaylist(id); deleteTarget = null }) { Text(stringResource(R.string.common_delete)) } },
            dismissButton = { DialogActionButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
}

@Composable
private fun SearchEntry(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            stringResource(R.string.home_search_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DialogActionButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .heightIn(min = 52.dp)
            .padding(horizontal = 12.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    ) { content() }
}

@Composable
private fun PlaylistRowItem(
    title: String,
    subtitle: String,
    song: SongEntity?,
    badge: androidx.compose.ui.graphics.vector.ImageVector?,
    onClick: () -> Unit,
    onMore: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.linearGradient(
                        listOf(AppAccent.copy(alpha = 0.85f), AppAccent.copy(alpha = 0.45f)),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (song != null) {
                Artwork(
                    source = AlbumArtSource(song.artworkPath, song.albumId),
                    modifier = Modifier.fillMaxSize(),
                )
            } else if (badge != null) {
                Icon(
                    badge,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = AppAccent,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            Icons.Default.PlayArrow,
            contentDescription = stringResource(R.string.common_play),
            tint = AppAccent,
            modifier = Modifier.size(26.dp),
        )
        if (onMore != null) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.playlist_actions),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(24.dp)
                    .clickable(onClick = onMore),
            )
        }
    }
}

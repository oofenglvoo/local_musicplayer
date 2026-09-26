package com.localmusic.player.ui.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.localmusic.player.data.AlbumArtSource
import com.localmusic.player.data.db.SongEntity
import com.localmusic.player.playback.PlayerConnection
import com.localmusic.player.ui.Artwork
import com.localmusic.player.ui.library.LibraryViewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.localmusic.player.ui.theme.AppAccent

private data class PlaylistCard(
    val title: String,
    val subtitle: String,
    val song: SongEntity?,
    val accent: Color,
    val onClick: () -> Unit,
)

@Composable
fun PlaylistHubScreen(
    onOpenPlaylist: (Long) -> Unit,
    onOpenFavorites: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val songs by viewModel.sortedSongs.collectAsStateWithLifecycle()
    val favoriteViewModel: FavoritesViewModel = hiltViewModel()
    val favoriteSongs by favoriteViewModel.songs.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<Pair<Long, String>?>(null) }
    var deleteTarget by remember { mutableStateOf<Pair<Long, String>?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "歌单",
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
                Text("新建歌单", style = MaterialTheme.typography.bodyMedium, color = AppAccent)
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                PlaylistCardItem(
                    PlaylistCard("我的喜欢", "收藏的歌曲都在这里", favoriteSongs.firstOrNull(), AppAccent, onOpenFavorites),
                    badge = Icons.Default.Favorite,
                )
            }
            items(playlists, key = { it.id }) { playlist ->
                val playlistSongs by viewModel.songsInPlaylist(playlist.id)
                    .collectAsStateWithLifecycle(emptyList())
                PlaylistCardItem(
                    PlaylistCard(
                        title = playlist.name,
                        subtitle = "我的歌单",
                        song = playlistSongs.firstOrNull(),
                        accent = AppAccent,
                    ) { onOpenPlaylist(playlist.id) },
                    onMore = {
                        renameTarget = playlist.id to playlist.name
                    },
                )
            }
        }
    }

    if (showCreate) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("新建歌单") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("歌单名称") },
                    singleLine = true,
                )
            },
            confirmButton = {
                DialogActionButton(onClick = {
                    if (name.isNotBlank()) viewModel.createPlaylist(name.trim())
                    showCreate = false
                }) { Text("创建") }
            },
            dismissButton = {
                DialogActionButton(onClick = { showCreate = false }) { Text("取消") }
            },
        )
    }

    renameTarget?.let { (id, current) ->
        var name by remember(renameTarget) { mutableStateOf(current) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("歌单操作") },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true) },
            confirmButton = {
                DialogActionButton(onClick = {
                    if (name.isNotBlank()) viewModel.renamePlaylist(id, name.trim())
                    renameTarget = null
                }) { Text("重命名") }
            },
            dismissButton = {
                DialogActionButton(onClick = {
                    deleteTarget = id to current
                    renameTarget = null
                }) { Text("删除") }
            },
        )
    }
    deleteTarget?.let { (id, name) ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除歌单") },
            text = { Text("确定删除「$name」吗？歌曲文件不会被删除。") },
            confirmButton = { DialogActionButton(onClick = { viewModel.deletePlaylist(id); deleteTarget = null }) { Text("删除") } },
            dismissButton = { DialogActionButton(onClick = { deleteTarget = null }) { Text("取消") } },
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
private fun PlaylistCardItem(
    card: PlaylistCard,
    badge: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onMore: (() -> Unit)? = null,
) {
    Column(modifier = Modifier.clickable(onClick = card.onClick)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.15f)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(
                        listOf(card.accent.copy(alpha = 0.85f), card.accent.copy(alpha = 0.45f)),
                    ),
                ),
        ) {
            if (card.song != null) {
                Artwork(
                    source = AlbumArtSource(card.song.artworkPath, card.song.albumId),
                    modifier = Modifier.fillMaxSize(),
                )
            } else if (badge != null) {
                Icon(
                    badge,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.align(Alignment.Center).size(48.dp),
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "播放",
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp),
                )
            }
            if (onMore != null) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "歌单操作",
                    tint = Color.White,
                    modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).clickable(onClick = onMore),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row {
            Text(
                card.title,
                style = MaterialTheme.typography.titleMedium,
                color = AppAccent,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.size(6.dp))
            Text(
                card.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

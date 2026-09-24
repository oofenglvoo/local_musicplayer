package com.localmusic.player.ui.song

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.localmusic.player.data.db.SongEntity
import com.localmusic.player.ui.SongArtwork
import com.localmusic.player.util.SongActions
import com.localmusic.player.util.toDurationString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongContextMenuSheet(
    song: SongEntity,
    isFavorite: Boolean,
    viewModel: SongMenuViewModel,
    onDismiss: () -> Unit,
    onShowDetail: (SongEntity) -> Unit,
    onEditLyrics: (SongEntity) -> Unit = {},
    onEditArtwork: (SongEntity) -> Unit = {},
    onDeleted: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showAddToPlaylist by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SongArtwork(
                    song = song,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        song.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${song.artist} · ${song.album}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            HorizontalDivider()

            MenuRow(Icons.Default.SkipNext, "下一首播放") {
                viewModel.playNext(listOf(song))
                onDismiss()
            }
            MenuRow(Icons.Default.QueueMusic, "加入播放队列") {
                viewModel.addToQueue(listOf(song))
                onDismiss()
            }
            MenuRow(Icons.AutoMirrored.Filled.PlaylistAdd, "添加到播放列表") {
                showAddToPlaylist = true
            }
            MenuRow(
                if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                if (isFavorite) "取消收藏" else "收藏",
            ) {
                viewModel.toggleFavorite(song.id)
                onDismiss()
            }
            MenuRow(Icons.Default.Share, "分享") {
                SongActions.shareSong(context, song)
                onDismiss()
            }
            MenuRow(Icons.Default.EditNote, "编辑歌词") {
                onEditLyrics(song)
                onDismiss()
            }
            MenuRow(Icons.Default.Image, "更换封面") {
                onEditArtwork(song)
                onDismiss()
            }
            MenuRow(Icons.Default.Notifications, "设为铃声") {
                SongActions.setAsRingtone(context, song)
                onDismiss()
            }
            MenuRow(Icons.Default.Info, "歌曲详情") {
                onShowDetail(song)
                onDismiss()
            }
            MenuRow(
                Icons.Default.Delete,
                "删除文件",
                tint = MaterialTheme.colorScheme.error,
            ) {
                showDeleteConfirm = true
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除文件") },
            text = { Text("确定要永久删除「${song.title}」吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.deleteSongs(listOf(song), context) {
                        onDeleted()
                        onDismiss()
                    }
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            },
        )
    }

    if (showAddToPlaylist) {
        AddToPlaylistDialog(
            viewModel = viewModel,
            onDismiss = { showAddToPlaylist = false },
            onDone = { onDismiss() },
        )
    }
}

@Composable
private fun MenuRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(label, color = tint) },
        leadingContent = { Icon(icon, contentDescription = null, tint = tint) },
    )
}

@Composable
fun AddToPlaylistDialog(
    viewModel: SongMenuViewModel,
    onDismiss: () -> Unit,
    onDone: () -> Unit,
    pendingSongs: List<SongEntity> = emptyList(),
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("新建播放列表") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = { Text("播放列表名称") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank() && pendingSongs.isNotEmpty()) {
                        viewModel.createPlaylistWith(newName.trim(), pendingSongs)
                    }
                    showCreate = false
                    onDone()
                }) { Text("创建") }
            },
            dismissButton = {
                TextButton(onClick = { showCreate = false }) { Text("取消") }
            },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加到播放列表") },
        text = {
            Column(modifier = Modifier.heightIn(max = 400.dp)) {
                if (playlists.isEmpty()) {
                    Text("暂无播放列表", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        playlists.forEach { playlist ->
                            ListItem(
                                modifier = Modifier.clickable {
                                    viewModel.addToPlaylist(playlist.id, pendingSongs)
                                    onDone()
                                },
                                headlineContent = { Text(playlist.name) },
                                leadingContent = {
                                    Icon(Icons.Default.PlaylistPlay, contentDescription = null)
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { showCreate = true }) { Text("新建") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongDetailSheet(
    song: SongEntity,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val info = remember(song.id) { SongActions.readFileInfo(song) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SongArtwork(
                song = song,
                modifier = Modifier
                    .size(160.dp)
                    .clip(RoundedCornerShape(16.dp)),
            )
            Text(
                song.title,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            HorizontalDivider()

            DetailRow("专辑", song.album)
            DetailRow("时长", song.duration.toDurationString())
            DetailRow("格式", song.mimeType.ifBlank { "未知" })
            DetailRow("比特率", if (info.bitrateKbps > 0) "${info.bitrateKbps} kbps" else "未知")
            DetailRow("采样率", if (info.sampleRateHz > 0) "${info.sampleRateHz} Hz" else "未知")
            DetailRow("文件大小", formatSize(info.sizeBytes))
            DetailRow("年份", if (song.year > 0) song.year.toString() else "未知")
            DetailRow("音轨", if (song.trackNumber > 0) song.trackNumber.toString() else "未知")
            DetailRow("唱片", if (song.discNumber > 0) song.discNumber.toString() else "未知")
            DetailRow("播放次数", "${song.playCount} 次")
            DetailRow("文件路径", song.path)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "未知"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return when {
        mb >= 1 -> "%.2f MB".format(mb)
        else -> "%.0f KB".format(kb)
    }
}

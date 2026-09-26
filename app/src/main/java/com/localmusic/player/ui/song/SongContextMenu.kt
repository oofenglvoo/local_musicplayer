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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.localmusic.player.R
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

            MenuRow(Icons.Default.SkipNext, stringResource(R.string.song_menu_play_next)) {
                viewModel.playNext(listOf(song))
                onDismiss()
            }
            MenuRow(Icons.Default.QueueMusic, stringResource(R.string.song_menu_add_queue)) {
                viewModel.addToQueue(listOf(song))
                onDismiss()
            }
            MenuRow(Icons.AutoMirrored.Filled.PlaylistAdd, stringResource(R.string.song_menu_add_playlist)) {
                showAddToPlaylist = true
            }
            MenuRow(
                if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                stringResource(if (isFavorite) R.string.song_menu_unfavorite else R.string.song_menu_favorite),
            ) {
                viewModel.toggleFavorite(song.id)
                onDismiss()
            }
            MenuRow(Icons.Default.Share, stringResource(R.string.song_menu_share)) {
                SongActions.shareSong(context, song)
                onDismiss()
            }
            MenuRow(Icons.Default.EditNote, stringResource(R.string.song_menu_edit_lyrics)) {
                onEditLyrics(song)
                onDismiss()
            }
            MenuRow(Icons.Default.Image, stringResource(R.string.song_menu_change_artwork)) {
                onEditArtwork(song)
                onDismiss()
            }
            MenuRow(Icons.Default.Notifications, stringResource(R.string.song_menu_set_ringtone)) {
                SongActions.setAsRingtone(context, song)
                onDismiss()
            }
            MenuRow(Icons.Default.Info, stringResource(R.string.song_detail_title)) {
                onShowDetail(song)
                onDismiss()
            }
            MenuRow(
                Icons.Default.Delete,
                stringResource(R.string.library_delete_files_title),
                tint = MaterialTheme.colorScheme.error,
            ) {
                showDeleteConfirm = true
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.library_delete_files_title)) },
            text = { Text(stringResource(R.string.song_menu_delete_confirm, song.title)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.deleteSongs(listOf(song), context) {
                        onDeleted()
                        onDismiss()
                    }
                }) {
                    Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.common_cancel)) }
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
            title = { Text(stringResource(R.string.library_new_playlist)) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = { Text(stringResource(R.string.library_playlist_name)) },
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
                }) { Text(stringResource(R.string.common_create)) }
            },
            dismissButton = {
                TextButton(onClick = { showCreate = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.song_menu_add_playlist)) },
        text = {
            Column(modifier = Modifier.heightIn(max = 400.dp)) {
                if (playlists.isEmpty()) {
                    Text(stringResource(R.string.library_no_playlists), style = MaterialTheme.typography.bodyMedium)
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
            TextButton(onClick = { showCreate = true }) { Text(stringResource(R.string.common_new)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
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

            DetailRow(stringResource(R.string.detail_field_album), song.album)
            DetailRow(stringResource(R.string.detail_field_duration), song.duration.toDurationString())
            DetailRow(stringResource(R.string.detail_field_format), song.mimeType.ifBlank { stringResource(R.string.common_unknown) })
            DetailRow(stringResource(R.string.detail_field_bitrate), if (info.bitrateKbps > 0) "${info.bitrateKbps} kbps" else stringResource(R.string.common_unknown))
            DetailRow(stringResource(R.string.detail_field_sample_rate), if (info.sampleRateHz > 0) "${info.sampleRateHz} Hz" else stringResource(R.string.common_unknown))
            DetailRow(stringResource(R.string.detail_field_size), formatSize(info.sizeBytes))
            DetailRow(stringResource(R.string.detail_field_year), if (song.year > 0) song.year.toString() else stringResource(R.string.common_unknown))
            DetailRow(stringResource(R.string.detail_field_track), if (song.trackNumber > 0) song.trackNumber.toString() else stringResource(R.string.common_unknown))
            DetailRow(stringResource(R.string.detail_field_disc), if (song.discNumber > 0) song.discNumber.toString() else stringResource(R.string.common_unknown))
            DetailRow(stringResource(R.string.detail_field_play_count), stringResource(R.string.detail_play_count_value, song.playCount))
            DetailRow(stringResource(R.string.detail_field_path), song.path)
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
    if (bytes <= 0) return "-"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return when {
        mb >= 1 -> "%.2f MB".format(mb)
        else -> "%.0f KB".format(kb)
    }
}

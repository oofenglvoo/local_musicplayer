package com.localmusic.player.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.localmusic.player.R
import com.localmusic.player.data.AlbumGroup
import com.localmusic.player.data.ArtistGroup
import com.localmusic.player.data.FolderGroup
import com.localmusic.player.data.SongSort
import com.localmusic.player.data.db.SongEntity
import com.localmusic.player.playback.PlayerConnection
import com.localmusic.player.ui.Artwork
import com.localmusic.player.ui.SongRow
import com.localmusic.player.data.AlbumArtSource
import com.localmusic.player.util.SelectionState

@Composable
fun SongsTab(
    viewModel: LibraryViewModel,
    songs: List<SongEntity>,
    favoriteIds: Set<Long>,
    onSongClick: (List<SongEntity>, Int) -> Unit,
    onSongLongClick: (SongEntity) -> Unit = {},
    onOpenAutoList: (AutoList) -> Unit = {},
    selection: SelectionState? = null,
) {
    var query by remember { mutableStateOf("") }

    val displayed = remember(songs, query) {
        if (query.isBlank()) songs
        else songs.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true) ||
                it.album.contains(query, ignoreCase = true) ||
                it.displayName.contains(query, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AutoList.entries.forEach { list ->
                androidx.compose.material3.AssistChip(
                    onClick = { onOpenAutoList(list) },
                    label = { Text(stringResource(list.labelRes)) },
                )
            }
        }
        Text(
            stringResource(R.string.common_quick_entry),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(AutoList.entries) { list ->
                androidx.compose.material3.ElevatedCard(
                    modifier = Modifier.size(width = 142.dp, height = 82.dp).clickable { onOpenAutoList(list) },
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(list.labelRes), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(R.string.common_browse_play), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Text(
            stringResource(R.string.common_all_songs),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
        if (displayed.isEmpty()) {
            EmptyHint(
                if (songs.isEmpty()) stringResource(R.string.common_no_local_music)
                else stringResource(R.string.common_no_match_song)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                itemsIndexed(displayed, key = { _, s -> s.id }) { index, song ->
                    val selectionMode = selection?.enabled == true
                    SongRow(
                        song = song,
                        isFavorite = song.id in favoriteIds,
                        onClick = {
                            if (selectionMode) selection?.toggle(song.id)
                            else onSongClick(displayed, index)
                        },
                        onLongClick = {
                            if (selectionMode) selection?.toggle(song.id)
                            else {
                                selection?.start(song.id)
                                onSongLongClick(song)
                            }
                        },
                        onFavoriteClick = { viewModel.toggleFavorite(song.id) },
                        selected = selection?.selected?.contains(song.id) == true,
                        selectionMode = selectionMode,
                    )
                }
            }
        }
    }
}

enum class AutoList(@androidx.annotation.StringRes val labelRes: Int) {
    RECENTLY_ADDED(com.localmusic.player.R.string.autolist_recently_added),
    RECENTLY_PLAYED(com.localmusic.player.R.string.autolist_recently_played),
    MOST_PLAYED(com.localmusic.player.R.string.autolist_most_played),
}

@Composable
fun AlbumsTab(
    albums: List<AlbumGroup>,
    onOpenAlbum: (AlbumGroup) -> Unit,
    grid: Boolean = true,
) {
    if (albums.isEmpty()) {
        EmptyHint(stringResource(R.string.common_no_albums))
        return
    }
    if (grid) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(albums, key = { it.key }) { album ->
                AlbumGridItem(album = album, onClick = { onOpenAlbum(album) })
            }
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(albums, key = { it.key }) { album ->
                AlbumListRow(album = album, onClick = { onOpenAlbum(album) })
            }
        }
    }
}

@Composable
private fun AlbumGridItem(album: AlbumGroup, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Artwork(
            source = AlbumArtSource(album.artworkPath, album.albumId),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp)),
        )
        Text(
            album.album,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            album.artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AlbumListRow(album: AlbumGroup, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(album.album, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(stringResource(R.string.search_album_subtitle, album.artist, album.songs.size), maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        leadingContent = {
            Artwork(
                source = AlbumArtSource(album.artworkPath, album.albumId),
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
            )
        },
    )
}

@Composable
fun ArtistsTab(
    artists: List<ArtistGroup>,
    onOpenArtist: (ArtistGroup) -> Unit,
) {
    if (artists.isEmpty()) {
        EmptyHint(stringResource(R.string.common_no_artists))
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(artists, key = { it.artist.lowercase() }) { artist ->
            ListItem(
                modifier = Modifier.clickable { onOpenArtist(artist) },
                headlineContent = { Text(artist.artist, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                supportingContent = { Text(stringResource(R.string.search_artist_subtitle, artist.songs.size, artist.albumCount)) },
                leadingContent = {
                    Artwork(
                        source = AlbumArtSource(
                            artist.artworkPath,
                            artist.representative?.albumId ?: 0L,
                        ),
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(24.dp)),
                    )
                },
            )
        }
    }
}

@Composable
fun FoldersTab(
    folders: List<FolderGroup>,
    onOpenFolder: (FolderGroup) -> Unit,
) {
    if (folders.isEmpty()) {
        EmptyHint(stringResource(R.string.common_no_folders))
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(folders, key = { it.path }) { folder ->
            ListItem(
                modifier = Modifier.clickable { onOpenFolder(folder) },
                headlineContent = { Text(folder.name ?: stringResource(R.string.library_root_folder), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                supportingContent = {
                    Text(
                        stringResource(R.string.playlist_song_count, folder.songs.size) + " · " + folder.path,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        }
    }
}

@Composable
fun SortBar(
    sort: SongSort,
    ascending: Boolean,
    onSortChange: (SongSort) -> Unit,
    onToggleDirection: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            Row(
                modifier = Modifier
                    .clickable { expanded = true }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.common_sort_prefix) + stringResource(sort.labelRes), style = MaterialTheme.typography.labelLarge)
                Text(
                    if (ascending) " ↑" else " ↓",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .clickable { onToggleDirection() }
                        .padding(start = 4.dp),
                )
            }
            androidx.compose.material3.DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                SongSort.entries.forEach { field ->
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(stringResource(field.labelRes)) },
                        onClick = {
                            onSortChange(field)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyHint(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
    )
    HorizontalDivider()
}

@Composable
fun PlayableHeader(
    title: String,
    subtitle: String? = null,
    artworkPath: String?,
    albumId: Long,
    onPlayAll: () -> Unit,
    onShuffle: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Artwork(
            source = AlbumArtSource(artworkPath, albumId),
            modifier = Modifier.size(200.dp).clip(RoundedCornerShape(16.dp)),
        )
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 12.dp),
        )
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Row(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            androidx.compose.material3.FilledTonalButton(onClick = onPlayAll) {
                Text(stringResource(R.string.common_play_all))
            }
            if (onShuffle != null) {
                androidx.compose.material3.OutlinedButton(onClick = onShuffle) {
                    Text(stringResource(R.string.common_shuffle_all))
                }
            }
        }
    }
}

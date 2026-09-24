package com.localmusic.player.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.localmusic.player.data.AlbumGroup
import com.localmusic.player.data.ArtistGroup
import com.localmusic.player.data.FolderGroup
import com.localmusic.player.ui.library.AlbumsTab
import com.localmusic.player.ui.library.ArtistsTab
import com.localmusic.player.ui.library.FoldersTab
import com.localmusic.player.ui.library.LibraryTab
import com.localmusic.player.ui.library.LibraryViewModel
import com.localmusic.player.ui.library.SortBar
import com.localmusic.player.ui.library.SongsTab
import com.localmusic.player.util.rememberSelectionState
import com.localmusic.player.util.toDurationString
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    onOpenFolderPicker: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onPlaySongs: (List<com.localmusic.player.data.db.SongEntity>, Int) -> Unit,
    onOpenAlbum: (AlbumGroup) -> Unit = {},
    onOpenArtist: (ArtistGroup) -> Unit = {},
    onOpenFolder: (FolderGroup) -> Unit = {},
    onOpenPlaylist: (Long) -> Unit = {},
    onOpenAutoList: (com.localmusic.player.ui.library.AutoList) -> Unit = {},
    onSongLongClick: (com.localmusic.player.data.db.SongEntity) -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel(),
    songMenuViewModel: com.localmusic.player.ui.song.SongMenuViewModel = hiltViewModel(),
) {
    val songs by viewModel.sortedSongs.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val sortField by viewModel.sortField.collectAsStateWithLifecycle()
    val sortAscending by viewModel.sortAscending.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val menuMessage by songMenuViewModel.message.collectAsStateWithLifecycle()
    val gridAlbums by viewModel.gridAlbums.collectAsStateWithLifecycle()

    val selection = rememberSelectionState()
    var menuSong by remember { mutableStateOf<com.localmusic.player.data.db.SongEntity?>(null) }
    var detailSong by remember { mutableStateOf<com.localmusic.player.data.db.SongEntity?>(null) }
    var artworkSong by remember { mutableStateOf<com.localmusic.player.data.db.SongEntity?>(null) }
    var lyricsEditSong by remember { mutableStateOf<com.localmusic.player.data.db.SongEntity?>(null) }
    var showBatchAddToPlaylist by remember { mutableStateOf(false) }
    var showBatchDelete by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val lyricsEditViewModel: com.localmusic.player.lyrics.LyricsEditViewModel = hiltViewModel()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }
    LaunchedEffect(menuMessage) {
        menuMessage?.let {
            snackbarHostState.showSnackbar(it)
            songMenuViewModel.consumeMessage()
        }
    }

    val tabs = LibraryTab.entries
    val pagerState = rememberPagerState(initialPage = selectedTab.ordinal) { tabs.size }
    val scope = rememberCoroutineScope()

    LaunchedEffect(selectedTab) {
        if (pagerState.currentPage != selectedTab.ordinal) {
            pagerState.scrollToPage(selectedTab.ordinal)
        }
    }
    LaunchedEffect(pagerState.currentPage) {
        val tab = tabs[pagerState.currentPage]
        if (tab != selectedTab) viewModel.setTab(tab)
    }

    val selectedSongs = remember(selection.selected, songs) {
        songs.filter { it.id in selection.selected }
    }

    Scaffold(
        topBar = {
            if (selection.enabled) {
                TopAppBar(
                    title = { Text("已选 ${selection.count} 首") },
                    navigationIcon = {
                        IconButton(onClick = { selection.clear() }) {
                            Icon(Icons.Default.Close, contentDescription = "取消选择")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            selection.selectAll(songs.map { it.id })
                        }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "全选")
                        }
                        IconButton(onClick = { showBatchAddToPlaylist = true }) {
                            Icon(
                                Icons.AutoMirrored.Filled.PlaylistAdd,
                                contentDescription = "添加到播放列表",
                            )
                        }
                        IconButton(onClick = {
                            songMenuViewModel.playNext(selectedSongs)
                            selection.clear()
                        }) {
                            Icon(Icons.Default.SkipNext, contentDescription = "下一首播放")
                        }
                        IconButton(onClick = {
                            songMenuViewModel.addToQueue(selectedSongs)
                            selection.clear()
                        }) {
                            Icon(Icons.Default.QueueMusic, contentDescription = "加入队列")
                        }
                        IconButton(onClick = { showBatchDelete = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "删除",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    },
                )
            } else {
                TopAppBar(
                    title = { Text("本地音乐 · ${songs.size}") },
                    actions = {
                        IconButton(onClick = onOpenSearch) {
                            Icon(Icons.Default.Search, contentDescription = "搜索")
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
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(index) }
                        },
                        text = { Text(tab.label) },
                    )
                }
            }

            if (selectedTab == LibraryTab.SONGS || selectedTab == LibraryTab.ALBUMS) {
                SortBar(
                    sort = sortField,
                    ascending = sortAscending,
                    onSortChange = { viewModel.setSort(it) },
                    onToggleDirection = { viewModel.toggleSortDirection() },
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = androidx.compose.ui.Alignment.Top,
                userScrollEnabled = !selection.enabled,
            ) { page ->
                when (tabs[page]) {
                    LibraryTab.SONGS -> SongsTab(
                        viewModel = viewModel,
                        songs = songs,
                        favoriteIds = favoriteIds,
                        onSongClick = onPlaySongs,
                        onSongLongClick = onSongLongClick,
                        onOpenAutoList = onOpenAutoList,
                        selection = selection,
                    )
                    LibraryTab.ALBUMS -> AlbumsTab(
                        albums = albums,
                        onOpenAlbum = onOpenAlbum,
                        grid = gridAlbums,
                    )
                    LibraryTab.ARTISTS -> ArtistsTab(
                        artists = artists,
                        onOpenArtist = onOpenArtist,
                    )
                    LibraryTab.FOLDERS -> FoldersTab(
                        folders = folders,
                        onOpenFolder = onOpenFolder,
                    )
                    LibraryTab.PLAYLISTS -> PlaylistInlineTab(
                        playlists = playlists,
                        onOpenPlaylist = onOpenPlaylist,
                        onCreatePlaylist = { viewModel.createPlaylist(it) },
                    )
                }
            }
        }
    }

    menuSong?.let { song ->
        com.localmusic.player.ui.song.SongContextMenuSheet(
            song = song,
            isFavorite = song.id in favoriteIds,
            viewModel = songMenuViewModel,
            onDismiss = { menuSong = null },
            onShowDetail = { detailSong = it },
            onEditLyrics = { lyricsEditSong = it },
            onEditArtwork = { artworkSong = it },
            onDeleted = { viewModel.refresh() },
        )
    }

    detailSong?.let { song ->
        com.localmusic.player.ui.song.SongDetailSheet(
            song = song,
            onDismiss = { detailSong = null },
        )
    }

    artworkSong?.let { song ->
        com.localmusic.player.ui.song.ArtworkPickerSheet(
            song = song,
            onDismiss = { artworkSong = null },
            onPicked = { path ->
                songMenuViewModel.setArtworkOverride(song.id, path)
                artworkSong = null
            },
        )
    }

    lyricsEditSong?.let { song ->
        com.localmusic.player.lyrics.LyricsEditSheet(
            songId = song.id,
            viewModel = lyricsEditViewModel,
            onDismiss = { lyricsEditSong = null },
            onSaved = { lyricsEditSong = null },
        )
    }

    if (showBatchAddToPlaylist) {
        var newName by remember { mutableStateOf("") }
        val songMenuPlaylists = songMenuViewModel.playlists.collectAsStateWithLifecycle().value
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showBatchAddToPlaylist = false },
            title = { Text("添加到播放列表（${selection.count} 首）") },
            text = {
                Column(modifier = Modifier.heightIn(max = 400.dp)) {
                    if (songMenuPlaylists.isEmpty()) {
                        Text("暂无播放列表")
                    } else {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            songMenuPlaylists.forEach { playlist ->
                                androidx.compose.material3.ListItem(
                                    modifier = Modifier.clickable {
                                        songMenuViewModel.addToPlaylist(playlist.id, selectedSongs)
                                        selection.clear()
                                        showBatchAddToPlaylist = false
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
                TextButton(onClick = {
                    if (newName.isNotBlank()) {
                        songMenuViewModel.createPlaylistWith(newName.trim(), selectedSongs)
                        newName = ""
                        selection.clear()
                        showBatchAddToPlaylist = false
                    }
                }) { Text("新建") }
            },
            dismissButton = {
                TextButton(onClick = { showBatchAddToPlaylist = false }) { Text("取消") }
            },
        )
    }

    if (showBatchDelete) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showBatchDelete = false },
            title = { Text("删除文件") },
            text = { Text("确定要永久删除所选的 ${selection.count} 首歌曲吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    showBatchDelete = false
                    songMenuViewModel.deleteSongs(selectedSongs, context) {
                        viewModel.refresh()
                        selection.clear()
                    }
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDelete = false }) { Text("取消") }
            },
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SongRow(
    song: com.localmusic.player.data.db.SongEntity,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    selected: Boolean = false,
    selectionMode: Boolean = false,
) {
    androidx.compose.material3.ListItem(
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
        headlineContent = {
            Text(song.title, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(
                "${song.artist} · ${song.album}",
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        },
        leadingContent = {
            if (selectionMode) {
                androidx.compose.material3.Checkbox(
                    checked = selected,
                    onCheckedChange = { onLongClick() },
                )
            } else {
                SongArtwork(
                    song = song,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
                )
            }
        },
        trailingContent = {
            androidx.compose.foundation.layout.Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
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

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PlaylistInlineTab(
    playlists: List<com.localmusic.player.data.db.PlaylistEntity>,
    onOpenPlaylist: (Long) -> Unit,
    onCreatePlaylist: (String) -> Unit,
) {
    var showCreate by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxSize()) {
        if (playlists.isEmpty()) {
            Text(
                "还没有播放列表，点击右下角新建",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(androidx.compose.ui.Alignment.Center),
            )
        } else {
                androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(playlists, key = { it.id }) { playlist ->
                    androidx.compose.material3.ListItem(
                        modifier = Modifier.combinedClickable(
                            onClick = { onOpenPlaylist(playlist.id) },
                        ),
                        headlineContent = { Text(playlist.name) },
                        leadingContent = {
                            Icon(
                                Icons.Default.PlaylistPlay,
                                contentDescription = null,
                            )
                        },
                    )
                }
            }
        }
        androidx.compose.material3.FloatingActionButton(
            onClick = { showCreate = true },
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = "新建")
        }
    }

    if (showCreate) {
        var name by remember { mutableStateOf("") }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("新建播放列表") },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("播放列表名称") },
                    singleLine = true,
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        if (name.isNotBlank()) onCreatePlaylist(name.trim())
                        showCreate = false
                    },
                ) { Text("创建") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showCreate = false }) { Text("取消") }
            },
        )
    }
}


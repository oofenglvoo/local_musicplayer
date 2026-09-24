package com.localmusic.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.localmusic.player.playback.PlayerConnection
import com.localmusic.player.ui.LibraryScreen
import com.localmusic.player.ui.PermissionGate
import com.localmusic.player.ui.folder.FolderBrowserScreen
import com.localmusic.player.ui.library.AlbumDetailScreen
import com.localmusic.player.ui.library.ArtistDetailScreen
import com.localmusic.player.ui.library.FolderDetailScreen
import com.localmusic.player.ui.library.LibraryViewModel
import com.localmusic.player.ui.player.MiniPlayer
import com.localmusic.player.ui.player.NowPlayingScreen
import com.localmusic.player.ui.player.QueueScreen
import com.localmusic.player.ui.playlist.FavoritesScreen
import com.localmusic.player.ui.playlist.PlaylistDetailScreen
import com.localmusic.player.ui.playlist.PlaylistListScreen
import com.localmusic.player.ui.settings.AudioSettingsScreen
import com.localmusic.player.ui.settings.AboutScreen
import com.localmusic.player.ui.settings.LibrarySettingsScreen
import com.localmusic.player.ui.settings.SettingsScreen
import com.localmusic.player.ui.settings.ThemeSettingsScreen
import com.localmusic.player.ui.search.SearchScreen
import com.localmusic.player.ui.theme.LocalMusicTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import java.net.URLDecoder
import java.net.URLEncoder

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @javax.inject.Inject
    lateinit var playbackCoordinator: com.localmusic.player.playback.PlaybackCoordinator

    @javax.inject.Inject
    lateinit var settingsStore: com.localmusic.player.data.SettingsStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        PlayerConnection.connect(applicationContext)
        playbackCoordinator.start()

        setContent {
            val themeMode by settingsStore.themeMode
                .collectAsStateWithLifecycle(com.localmusic.player.data.ThemeMode.SYSTEM)
            val dynamicColor by settingsStore.dynamicColor
                .collectAsStateWithLifecycle(true)
            val seedColor by settingsStore.seedColor
                .collectAsStateWithLifecycle(null)

            LocalMusicTheme(
                themeMode = themeMode,
                dynamicColor = dynamicColor,
                seedColor = seedColor,
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AppRoot()
                }
            }
        }
    }

    override fun onDestroy() {
        playbackCoordinator.saveNow()
        if (isFinishing) PlayerConnection.release()
        super.onDestroy()
    }
}

private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")
private fun dec(s: String?): String =
    if (s.isNullOrBlank()) "" else runCatching { URLDecoder.decode(s, "UTF-8") }.getOrDefault(s)

@Composable
private fun AppRoot() {
    val tabs = listOf("音乐库", "播放列表", "收藏")
    var selectedTab by remember { mutableIntStateOf(0) }
    var nowPlayingOpen by remember { mutableStateOf(false) }
    var queueOpen by remember { mutableStateOf(false) }

    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                                onClick = {
                                    selectedTab = index
                                    nowPlayingOpen = false
                                    queueOpen = false
                                    when (index) {
                                0 -> navController.navigate("library")
                                1 -> navController.navigate("playlists")
                                else -> navController.navigate("favorites")
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = when (index) {
                                    0 -> Icons.Default.LibraryMusic
                                    1 -> Icons.Default.PlaylistPlay
                                    else -> Icons.Default.Favorite
                                },
                                contentDescription = label,
                            )
                        },
                        label = { Text(label) },
                    )
                }
            }
        },
    ) { padding ->
        Surface(modifier = Modifier.fillMaxSize().padding(padding)) {
            PermissionGate(onGranted = {}) {
                if (nowPlayingOpen) {
                    BackHandler { nowPlayingOpen = false }
                    if (queueOpen) {
                        BackHandler { queueOpen = false }
                        QueueScreen(onBack = { queueOpen = false })
                    } else {
                        NowPlayingScreen(
                            onCollapse = { nowPlayingOpen = false },
                            onOpenQueue = { queueOpen = true },
                            onOpenBookmarks = { songId ->
                                nowPlayingOpen = false
                                navController.navigate("bookmarks/$songId")
                            },
                            onOpenEqualizer = {
                                nowPlayingOpen = false
                                navController.navigate("settings/audio")
                            },
                        )
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f)) {
                            NavHost(
                                navController = navController,
                                startDestination = "library",
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                composable("library") {
                                    LibraryScreen(
                                        onOpenFolderPicker = { navController.navigate("folderPicker") },
                                        onOpenSettings = { navController.navigate("settings") },
                                        onOpenSearch = { navController.navigate("search") },
                                        onPlaySongs = { list, index ->
                                            PlayerConnection.playSongs(list, index)
                                        },
                                        onOpenAlbum = { album ->
                                            navController.navigate("album/${enc(album.key)}")
                                        },
                                        onOpenArtist = { artist ->
                                            navController.navigate("artist/${enc(artist.artist)}")
                                        },
                                        onOpenFolder = { folder ->
                                            navController.navigate("folderDetail/${enc(folder.path)}")
                                        },
                                        onOpenAutoList = { list ->
                                            navController.navigate("autolist/${list.name}")
                                        },
                                        onOpenPlaylist = { id ->
                                            navController.navigate("playlist/$id")
                                        },
                                    )
                                }
                                composable("playlists") {
                                    PlaylistListScreen(
                                        onOpenPlaylist = { id -> navController.navigate("playlist/$id") },
                                    )
                                }
                                composable("favorites") { FavoritesScreen() }
                                composable("folderPicker") {
                                    FolderBrowserScreen(onBack = { navController.popBackStack() })
                                }
                                composable("settings") {
                                    SettingsScreen(
                                        onBack = { navController.popBackStack() },
                                        onOpenAudio = { navController.navigate("settings/audio") },
                                        onOpenTheme = { navController.navigate("settings/theme") },
                                        onOpenLibrary = { navController.navigate("settings/library") },
                                        onOpenAbout = { navController.navigate("settings/about") },
                                    )
                                }
                                composable("settings/audio") {
                                    AudioSettingsScreen(onBack = { navController.popBackStack() })
                                }
                                composable("settings/theme") {
                                    ThemeSettingsScreen(onBack = { navController.popBackStack() })
                                }
                                composable("settings/library") {
                                    LibrarySettingsScreen(onBack = { navController.popBackStack() })
                                }
                                composable("settings/about") {
                                    AboutScreen(onBack = { navController.popBackStack() })
                                }
                                composable("search") {
                                    SearchScreen(
                                        onBack = { navController.popBackStack() },
                                        onOpenAlbum = { key -> navController.navigate("album/${enc(key)}") },
                                        onOpenArtist = { name -> navController.navigate("artist/${enc(name)}") },
                                    )
                                }
                                composable(
                                    "bookmarks/{songId}",
                                    arguments = listOf(navArgument("songId") { type = NavType.LongType }),
                                ) { entry ->
                                    val songId = entry.arguments?.getLong("songId") ?: -1L
                                    com.localmusic.player.ui.player.BookmarksScreen(
                                        songId = songId,
                                        onBack = { navController.popBackStack() },
                                    )
                                }
                                composable("playlist/{playlistId}") { entry ->
                                    val id = entry.arguments?.getString("playlistId")?.toLongOrNull() ?: -1L
                                    PlaylistDetailScreen(
                                        playlistId = id,
                                        onBack = { navController.popBackStack() },
                                    )
                                }
                                composable(
                                    "album/{albumKey}",
                                    arguments = listOf(navArgument("albumKey") { type = NavType.StringType }),
                                ) { entry ->
                                    val key = dec(entry.arguments?.getString("albumKey"))
                                    AlbumDetailScreen(
                                        albumKey = key,
                                        viewModel = hiltViewModel(),
                                        onBack = { navController.popBackStack() },
                                    )
                                }
                                composable(
                                    "artist/{artistName}",
                                    arguments = listOf(navArgument("artistName") { type = NavType.StringType }),
                                ) { entry ->
                                    val name = dec(entry.arguments?.getString("artistName"))
                                    ArtistDetailScreen(
                                        artistName = name,
                                        viewModel = hiltViewModel(),
                                        onOpenAlbum = { album ->
                                            navController.navigate("album/${enc(album.key)}")
                                        },
                                        onBack = { navController.popBackStack() },
                                    )
                                }
                                composable(
                                    "folderDetail/{folderPath}",
                                    arguments = listOf(navArgument("folderPath") { type = NavType.StringType }),
                                ) { entry ->
                                    val path = dec(entry.arguments?.getString("folderPath"))
                                    FolderDetailScreen(
                                        folderPath = path,
                                        viewModel = hiltViewModel(),
                                        onBack = { navController.popBackStack() },
                                    )
                                }
                                composable(
                                    "autolist/{kind}",
                                    arguments = listOf(navArgument("kind") { type = NavType.StringType }),
                                ) { entry ->
                                    val name = entry.arguments?.getString("kind") ?: "RECENTLY_ADDED"
                                    val kind = runCatching {
                                        com.localmusic.player.ui.library.AutoList.valueOf(name)
                                    }.getOrDefault(com.localmusic.player.ui.library.AutoList.RECENTLY_ADDED)
                                    com.localmusic.player.ui.library.AutoListScreen(
                                        kind = kind,
                                        viewModel = hiltViewModel(),
                                        onBack = { navController.popBackStack() },
                                    )
                                }
                            }
                        }
                        MiniPlayer(onExpand = { nowPlayingOpen = true })
                    }
                }
            }
        }
    }
}

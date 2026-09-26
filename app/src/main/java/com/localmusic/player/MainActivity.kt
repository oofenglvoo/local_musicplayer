package com.localmusic.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.localmusic.player.playback.PlayerConnection
import com.localmusic.player.ui.PermissionGate
import com.localmusic.player.ui.folder.FolderBrowserScreen
import com.localmusic.player.ui.home.HomeScreen
import com.localmusic.player.ui.library.AlbumDetailScreen
import com.localmusic.player.ui.library.ArtistDetailScreen
import com.localmusic.player.ui.library.FolderDetailScreen
import com.localmusic.player.ui.library.LibraryViewModel
import com.localmusic.player.ui.local.LocalScreen
import com.localmusic.player.ui.player.MiniPlayer
import com.localmusic.player.ui.player.NowPlayingScreen
import com.localmusic.player.ui.player.QueueScreen
import com.localmusic.player.ui.playlist.FavoritesScreen
import com.localmusic.player.ui.playlist.PlaylistDetailScreen
import com.localmusic.player.ui.playlist.PlaylistHubScreen
import com.localmusic.player.ui.profile.ProfileScreen
import com.localmusic.player.ui.profile.RecentlyPlayedScreen
import com.localmusic.player.ui.settings.AudioSettingsScreen
import com.localmusic.player.ui.settings.AboutScreen
import com.localmusic.player.ui.settings.LibrarySettingsScreen
import com.localmusic.player.ui.settings.SettingsScreen
import com.localmusic.player.ui.settings.ThemeSettingsScreen
import com.localmusic.player.ui.search.SearchScreen
import com.localmusic.player.ui.theme.LocalMusicTheme
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
            val dynamicColor by settingsStore.dynamicColor.collectAsStateWithLifecycle(true)
            val seedColor by settingsStore.seedColor.collectAsStateWithLifecycle(null)

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

private data class BottomTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val icon: ImageVector,
)

@Composable
private fun AppRoot() {
    val tabs = listOf(
        BottomTab("home", "首页", Icons.Filled.Home, Icons.Outlined.Home),
        BottomTab("local", "本地", Icons.Filled.Folder, Icons.Outlined.Folder),
        BottomTab("playlists", "歌单", Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic),
        BottomTab("profile", "我的", Icons.Filled.Person, Icons.Outlined.Person),
    )
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var nowPlayingOpen by rememberSaveable { mutableStateOf(false) }
    var queueOpen by rememberSaveable { mutableStateOf(false) }

    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route
    val showBottomBar = currentRoute in tabs.map { it.route }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                if (showBottomBar && !nowPlayingOpen) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp,
                        modifier = Modifier.navigationBarsPadding(),
                    ) {
                        tabs.forEachIndexed { index, tab ->
                            NavigationBarItem(
                                selected = selectedTab == index,
                                onClick = {
                                    selectedTab = index
                                    nowPlayingOpen = false
                                    queueOpen = false
                                    navController.navigate(tab.route) {
                                        popUpTo(tabs.first().route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selectedTab == index) tab.selectedIcon else tab.icon,
                                        contentDescription = tab.label,
                                        modifier = Modifier.size(24.dp),
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                    indicatorColor = Color.Transparent,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
                    }
                }
            },
        ) { padding ->
            Surface(
                modifier = Modifier.fillMaxSize().padding(padding),
                color = Color.Transparent,
            ) {
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
                                onOpenSleepTimer = {
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
                                    startDestination = "home",
                                    modifier = Modifier.fillMaxSize(),
                                ) {
                                    composable("home") {
                                        HomeScreen(
                                            onOpenLocal = {
                                                selectedTab = 1
                                                navController.navigate("local") {
                                                    popUpTo("home") { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = false
                                                }
                                            },
                                            onOpenSearch = { navController.navigate("search") },
                                            onOpenPlaylist = { id -> navController.navigate("playlist/$id") },
                                            onPlaySongs = { list, index -> PlayerConnection.playSongs(list, index) },
                                        )
                                    }
                                    composable("local") {
                                        LocalScreen(
                                            viewModel = hiltViewModel(),
                                            onOpenFolderPicker = { navController.navigate("folderPicker") },
                                            onPlaySongs = { list, index -> PlayerConnection.playSongs(list, index) },
                                            onOpenAlbum = { album -> navController.navigate("album/${enc(album.key)}") },
                                            onOpenArtist = { artist -> navController.navigate("artist/${enc(artist.artist)}") },
                                            onOpenFolder = { folder -> navController.navigate("folderDetail/${enc(folder.path)}") },
                                        )
                                    }
                                    composable("playlists") {
                                        PlaylistHubScreen(
                                            onOpenPlaylist = { id -> navController.navigate("playlist/$id") },
                                            onOpenFavorites = { navController.navigate("favorites") },
                                        )
                                    }
                                    composable("profile") {
                                        ProfileScreen(
                                            onOpenSettings = { navController.navigate("settings") },
                                            onOpenFavorites = { navController.navigate("favorites") },
                                            onOpenMostPlayed = { navController.navigate("autolist/MOST_PLAYED") },
                                            onOpenRecentlyPlayed = { navController.navigate("recentlyPlayed") },
                                            onOpenPlaylist = { id -> navController.navigate("playlist/$id") },
                                        )
                                    }
                                    composable("recentlyPlayed") {
                                        RecentlyPlayedScreen(onBack = { navController.popBackStack() })
                                    }
                                    composable("favorites") {
                                        FavoritesScreen(onBack = { navController.popBackStack() })
                                    }
                                    composable("folderPicker?exclude={exclude}") { entry ->
                                        val exclude = entry.arguments?.getString("exclude") == "true"
                                        FolderBrowserScreen(
                                            onBack = { navController.popBackStack() },
                                            excludeMode = exclude,
                                        )
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
                                        LibrarySettingsScreen(
                                            onBack = { navController.popBackStack() },
                                            onAddExcludedFolder = { navController.navigate("folderPicker?exclude=true") },
                                        )
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
                                    composable(
                                        "playlist/{playlistId}",
                                        arguments = listOf(navArgument("playlistId") { type = NavType.LongType }),
                                    ) { entry ->
                                        val id = entry.arguments?.getLong("playlistId") ?: -1L
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
}

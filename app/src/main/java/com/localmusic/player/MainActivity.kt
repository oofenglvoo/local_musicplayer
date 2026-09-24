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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.localmusic.player.playback.PlayerConnection
import com.localmusic.player.ui.LibraryScreen
import com.localmusic.player.ui.PermissionGate
import com.localmusic.player.ui.folder.FolderBrowserScreen
import com.localmusic.player.ui.player.MiniPlayer
import com.localmusic.player.ui.player.NowPlayingScreen
import com.localmusic.player.ui.playlist.FavoritesScreen
import com.localmusic.player.ui.playlist.PlaylistDetailScreen
import com.localmusic.player.ui.playlist.PlaylistListScreen
import com.localmusic.player.ui.settings.AudioSettingsScreen
import com.localmusic.player.ui.theme.LocalMusicTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        PlayerConnection.connect(applicationContext)

        setContent {
            LocalMusicTheme {
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
        if (isFinishing) PlayerConnection.release()
        super.onDestroy()
    }
}

@Composable
private fun AppRoot() {
    val tabs = listOf("音乐库", "播放列表", "收藏")
    var selectedTab by remember { mutableIntStateOf(0) }
    var nowPlayingOpen by remember { mutableStateOf(false) }

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
                    NowPlayingScreen(onCollapse = { nowPlayingOpen = false })
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
                                        onOpenFolderPicker = {
                                            navController.navigate("folderPicker")
                                        },
                                        onOpenSettings = {
                                            navController.navigate("settings")
                                        },
                                    )
                                }
                                composable("playlists") {
                                    PlaylistListScreen(
                                        onOpenPlaylist = { id ->
                                            navController.navigate("playlist/$id")
                                        },
                                    )
                                }
                                composable("favorites") { FavoritesScreen() }
                                composable("folderPicker") {
                                    FolderBrowserScreen(
                                        onBack = { navController.popBackStack() },
                                    )
                                }
                                composable("settings") {
                                    AudioSettingsScreen(
                                        onBack = { navController.popBackStack() },
                                    )
                                }
                                composable("playlist/{playlistId}") { backStackEntry ->
                                    val id = backStackEntry.arguments
                                        ?.getString("playlistId")?.toLongOrNull() ?: -1L
                                    PlaylistDetailScreen(
                                        playlistId = id,
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

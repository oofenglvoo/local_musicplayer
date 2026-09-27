package com.localmusic.player.ui.folder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.localmusic.player.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderBrowserScreen(
    onBack: () -> Unit,
    excludeMode: Boolean = false,
    viewModel: FolderBrowserViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scanning by viewModel.scanning.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val folders by viewModel.scannedFolders.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    LaunchedEffect(Unit) {
        if (state.currentDir == null) {
            viewModel.rememberAndResolveStart()?.let { viewModel.open(it) }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.folder_pick_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    if (state.canGoUp) {
                        IconButton(onClick = { viewModel.goUp() }) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = stringResource(R.string.folder_up))
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {

                if (folders.isNotEmpty() && !viewModel.isPlaylistMode) {
                    Text(
                        stringResource(R.string.folder_scanned_title),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        folders.forEach { path ->
                            AssistChip(
                                onClick = { },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            path.substringAfterLast('/'),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = stringResource(R.string.common_remove),
                                            modifier = Modifier
                                                .padding(start = 4.dp)
                                                .size(16.dp)
                                                .clickable { viewModel.removeFolder(path) },
                                        )
                                    }
                                },
                            )
                        }
                    }
                }

                val current = state.currentDir
                if (current != null) {
                    ListItem(
                        headlineContent = {
                            Text(current.absolutePath, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        supportingContent = { Text(stringResource(R.string.folder_audio_count, state.audioCount)) },
                    )
                    Button(
                        onClick = {
                            if (excludeMode) viewModel.excludeCurrentFolder()
                            else viewModel.scanCurrentFolder()
                        },
                        enabled = !scanning,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    ) {
                        Text(
                            stringResource(
                                when {
                                    excludeMode -> R.string.folder_exclude_current
                                    viewModel.isPlaylistMode -> R.string.folder_scan_to_playlist
                                    else -> R.string.folder_scan_current
                                },
                            ),
                        )
                    }
                }

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.subDirs, key = { it.absolutePath }) { dir ->
                        ListItem(
                            modifier = Modifier.clickable { viewModel.open(dir) },
                            headlineContent = { Text(dir.name) },
                            leadingContent = {
                                Icon(Icons.Default.Folder, contentDescription = null)
                            },
                        )
                    }
                }
            }

            if (scanning) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.92f)),
                    contentAlignment = Alignment.Center,
                ) {
                    val p = progress
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (p != null && p.total > 0) {
                            LinearProgressIndicator(
                                progress = { p.scanned.toFloat() / p.total.toFloat() },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                stringResource(R.string.folder_scanning_progress, p.scanned, p.total),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                p.currentName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        } else {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(12.dp))
                            Text(
                                stringResource(R.string.folder_scanning_preparing),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

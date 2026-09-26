package com.localmusic.player.ui.folder

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderBrowserScreen(
    onBack: () -> Unit,
    excludeMode: Boolean = false,
    viewModel: FolderBrowserViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scanning by viewModel.scanning.collectAsStateWithLifecycle()
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
        if (state.currentDir == null && viewModel.shortcuts.isNotEmpty()) {
            viewModel.open(viewModel.shortcuts.first())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("选择音乐目录") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (state.canGoUp) {
                        IconButton(onClick = { viewModel.goUp() }) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = "上一级")
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {

                if (folders.isNotEmpty()) {
                    Text(
                        "已扫描目录",
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
                                            contentDescription = "移除",
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
                        supportingContent = { Text("${state.audioCount} 个音频文件") },
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
                        Text(if (excludeMode) "排除当前目录" else "扫描当前目录")
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
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

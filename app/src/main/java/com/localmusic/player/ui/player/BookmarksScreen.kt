package com.localmusic.player.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.localmusic.player.util.toDurationString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    songId: Long,
    onBack: () -> Unit,
    viewModel: BookmarksViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
) {
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(songId) { viewModel.setSong(songId) }
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("书签") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = "添加书签")
            }
        },
    ) { padding ->
        if (bookmarks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text("还没有书签，点击右下角添加到当前播放位置", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(bookmarks, key = { it.id }) { bookmark ->
                    ListItem(
                        modifier = Modifier.clickable { viewModel.seekTo(bookmark.positionMs) },
                        headlineContent = { Text(bookmark.label) },
                        supportingContent = { Text(bookmark.positionMs.toDurationString()) },
                        trailingContent = {
                            IconButton(onClick = { viewModel.delete(bookmark.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "删除")
                            }
                        },
                    )
                }
            }
        }
    }

    if (showAdd) {
        var label by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("添加书签") },
            text = {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    placeholder = { Text("书签名称") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addBookmarkAtCurrent(label)
                    showAdd = false
                }) { Text("添加") }
            },
            dismissButton = {
                TextButton(onClick = { showAdd = false }) { Text("取消") }
            },
        )
    }
}

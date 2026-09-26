package com.localmusic.player.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.localmusic.player.R
import com.localmusic.player.playback.PlayerConnection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(onBack: () -> Unit) {
    val queue by PlayerConnection.queue.collectAsStateWithLifecycle()
    val currentIndex by PlayerConnection.currentIndex.collectAsStateWithLifecycle()
    val shuffle by PlayerConnection.shuffle.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    var dragFrom by remember { mutableStateOf(-1) }

    LaunchedEffect(currentIndex) {
        if (currentIndex in queue.indices) {
            listState.animateScrollToItem(currentIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.queue_title_count, queue.size)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    IconButton(onClick = { PlayerConnection.toggleShuffle() }) {
                        Icon(
                            Icons.Default.Shuffle,
                            contentDescription = stringResource(R.string.common_shuffle),
                            tint = if (shuffle) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
    ) { padding ->
        if (queue.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.queue_empty), style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                itemsIndexed(queue, key = { index, item -> "$index-${item.songId}" }) { index, item ->
                    val isDragging = dragFrom == index
                    ListItem(
                        modifier = Modifier
                            .clickable { PlayerConnection.playAtQueueIndex(index) }
                            .then(
                                if (isDragging) {
                                    Modifier.graphicsLayer { alpha = 0.5f }
                                } else Modifier
                            ),
                        headlineContent = {
                            Text(
                                item.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = if (item.isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = if (item.isCurrent) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                            )
                        },
                        supportingContent = {
                            Text(item.artist, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        leadingContent = {
                            IconButton(
                                onClick = {
                                    if (dragFrom == -1) {
                                        dragFrom = index
                                    } else {
                                        PlayerConnection.moveQueueItem(dragFrom, index)
                                        dragFrom = -1
                                    }
                                },
                            ) {
                                Icon(
                                    Icons.Default.DragHandle,
                                    contentDescription = stringResource(R.string.queue_move),
                                    tint = if (dragFrom == index) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        trailingContent = {
                            IconButton(onClick = { PlayerConnection.removeQueueItem(index) }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.common_remove))
                            }
                        },
                    )
                }
            }
        }
    }
}

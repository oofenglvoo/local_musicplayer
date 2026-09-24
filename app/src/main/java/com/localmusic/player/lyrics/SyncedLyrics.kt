package com.localmusic.player.lyrics

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.localmusic.player.playback.PlayerConnection
import kotlinx.coroutines.delay

@Composable
fun SyncedLyrics(
    lyrics: Lyrics,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    var positionMs by remember { mutableLongStateOf(0L) }
    val controller = PlayerConnection.controller()

    LaunchedEffect(controller) {
        while (true) {
            controller?.let { positionMs = it.currentPosition }
            delay(300)
        }
    }

    if (lyrics.lines.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                lyrics.plainText ?: "暂无歌词",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(24.dp),
            )
        }
        return
    }

    val currentIndex = lyrics.lineIndexAt(positionMs)

    LaunchedEffect(currentIndex) {
        if (currentIndex >= 0) {
            listState.animateScrollToItem(currentIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 80.dp),
    ) {
        itemsIndexed(lyrics.lines) { index, line ->
            val isCurrent = index == currentIndex
            val color by animateColorAsState(
                targetValue = if (isCurrent) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                animationSpec = tween(400),
                label = "lyricColor",
            )
            val scale by animateFloatAsState(
                targetValue = if (isCurrent) 1.06f else 1f,
                animationSpec = tween(400),
                label = "lyricScale",
            )
            val alpha by animateFloatAsState(
                targetValue = if (isCurrent) 1f else 0.6f,
                animationSpec = tween(400),
                label = "lyricAlpha",
            )
            Text(
                text = line.text.ifBlank { "♪" },
                style = if (isCurrent) {
                    MaterialTheme.typography.titleLarge
                } else {
                    MaterialTheme.typography.bodyLarge
                },
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = color.copy(alpha = alpha),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(scale)
                    .padding(horizontal = 24.dp),
            )
        }
    }
}

package com.localmusic.player.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.localmusic.player.data.AlbumArtSource
import com.localmusic.player.lyrics.LyricsInlineViewModel
import com.localmusic.player.lyrics.SyncedLyrics
import com.localmusic.player.playback.PlayerConnection
import com.localmusic.player.ui.Artwork
import com.localmusic.player.util.toDurationString
import kotlinx.coroutines.delay

@Composable
fun NowPlayingScreen(onCollapse: () -> Unit) {
    val nowPlaying by PlayerConnection.nowPlaying.collectAsStateWithLifecycle()
    val isPlaying by PlayerConnection.isPlaying.collectAsStateWithLifecycle()

    val controller = PlayerConnection.controller()
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var isDragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    var showLyrics by remember { mutableStateOf(false) }

    val lyricsViewModel: LyricsInlineViewModel = hiltViewModel()
    val lyricsState by lyricsViewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(controller) {
        while (true) {
            controller?.let {
                if (!isDragging) {
                    positionMs = it.currentPosition
                    durationMs = it.duration.coerceAtLeast(0L)
                }
            }
            delay(500)
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onCollapse) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "收起")
                }
                Text(
                    "正在播放",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                IconButton(onClick = { showLyrics = !showLyrics }) {
                    Icon(
                        Icons.Default.Lyrics,
                        contentDescription = "歌词",
                        tint = if (showLyrics) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (showLyrics) {
                    val lyrics = lyricsState.lyrics
                    when {
                        lyricsState.loading -> CircularProgressIndicator()
                        lyrics != null -> SyncedLyrics(lyrics = lyrics)
                        else -> Text(
                            "暂无歌词\n（可在歌曲同目录放置同名 .lrc 文件）",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    Artwork(
                        source = AlbumArtSource(
                            artworkPath = nowPlaying.artworkPath,
                            albumId = nowPlaying.albumId,
                        ),
                        modifier = Modifier
                            .size(320.dp)
                            .clip(RoundedCornerShape(16.dp)),
                    )
                }
            }

            Text(
                text = nowPlaying.title.ifBlank { "未在播放" },
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = nowPlaying.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )

            val sliderMax = durationMs.coerceAtLeast(1L).toFloat()
            Slider(
                value = if (isDragging) dragValue else positionMs.coerceIn(0L, durationMs).toFloat(),
                onValueChange = {
                    isDragging = true
                    dragValue = it
                },
                onValueChangeFinished = {
                    PlayerConnection.seekTo(dragValue.toLong())
                    isDragging = false
                },
                valueRange = 0f..sliderMax,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TimeLabel(if (isDragging) dragValue.toLong() else positionMs)
                TimeLabel(durationMs)
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { PlayerConnection.previous() }) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "上一首",
                        modifier = Modifier.size(48.dp),
                    )
                }
                IconButton(onClick = {
                    if (nowPlaying.isEmpty) return@IconButton
                    PlayerConnection.togglePlayPause()
                }) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "播放/暂停",
                        modifier = Modifier.size(64.dp),
                    )
                }
                IconButton(onClick = { PlayerConnection.next() }) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "下一首",
                        modifier = Modifier.size(48.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeLabel(ms: Long) {
    Text(
        text = ms.toDurationString(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun MiniPlayer(onExpand: () -> Unit) {
    val nowPlaying by PlayerConnection.nowPlaying.collectAsStateWithLifecycle()
    val isPlaying by PlayerConnection.isPlaying.collectAsStateWithLifecycle()

    if (nowPlaying.isEmpty) return

    Surface(
        tonalElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onExpand),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Artwork(
                source = AlbumArtSource(
                    artworkPath = nowPlaying.artworkPath,
                    albumId = nowPlaying.albumId,
                ),
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)),
            )
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            ) {
                Text(
                    nowPlaying.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    nowPlaying.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = { PlayerConnection.togglePlayPause() }) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "播放/暂停",
                )
            }
            IconButton(onClick = { PlayerConnection.next() }) {
                Icon(Icons.Default.SkipNext, contentDescription = "下一首")
            }
        }
    }
}

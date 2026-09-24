package com.localmusic.player.ui.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode as AnimationRepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.localmusic.player.data.AlbumArtSource
import com.localmusic.player.data.toModel
import com.localmusic.player.lyrics.LyricsInlineViewModel
import com.localmusic.player.lyrics.SyncedLyrics
import com.localmusic.player.playback.PlayerConnection
import com.localmusic.player.playback.RepeatMode
import com.localmusic.player.ui.Artwork
import com.localmusic.player.util.toDurationString
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NowPlayingScreen(
    onCollapse: () -> Unit,
    onOpenQueue: () -> Unit = {},
    onOpenBookmarks: (Long) -> Unit = {},
    onOpenEqualizer: () -> Unit = {},
) {
    val nowPlaying by PlayerConnection.nowPlaying.collectAsStateWithLifecycle()
    val isPlaying by PlayerConnection.isPlaying.collectAsStateWithLifecycle()
    val shuffle by PlayerConnection.shuffle.collectAsStateWithLifecycle()
    val repeat by PlayerConnection.repeat.collectAsStateWithLifecycle()
    val queue by PlayerConnection.queue.collectAsStateWithLifecycle()
    val currentIndex by PlayerConnection.currentIndex.collectAsStateWithLifecycle()

    val controller = PlayerConnection.controller()
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var isDragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    var showLyrics by remember { mutableStateOf(false) }
    var volume by remember { mutableFloatStateOf(PlayerConnection.currentVolume()) }
    val scope = rememberCoroutineScope()

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

    val artworkSource = AlbumArtSource(nowPlaying.artworkPath, nowPlaying.albumId)
    val artworkModel = remember(artworkSource) { artworkSource.toModel() }

    val pagerState = rememberPagerState(
        initialPage = currentIndex.coerceAtLeast(0),
    ) { queue.size.coerceAtLeast(1) }

    LaunchedEffect(currentIndex) {
        if (currentIndex >= 0 && queue.isNotEmpty() && pagerState.currentPage != currentIndex) {
            pagerState.scrollToPage(currentIndex)
        }
    }
    LaunchedEffect(pagerState.currentPage) {
        if (queue.isNotEmpty() && pagerState.currentPage != currentIndex) {
            PlayerConnection.playAtQueueIndex(pagerState.currentPage)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (artworkModel != null) {
            AsyncImage(
                model = artworkModel,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(48.dp),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)),
            )
        }

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
                IconButton(onClick = onOpenQueue) {
                    Icon(Icons.Default.QueueMusic, contentDescription = "播放队列")
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 16.dp),
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
                    if (queue.size > 1) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                        ) { page ->
                            val item = queue.getOrNull(page)
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                RotatingArtwork(
                                    model = artworkModel,
                                    isPlaying = isPlaying,
                                    active = page == currentIndex,
                                    fallbackAlbumId = item?.songId ?: 0L,
                                )
                            }
                        }
                    } else {
                        RotatingArtwork(
                            model = artworkModel,
                            isPlaying = isPlaying,
                            active = true,
                            fallbackAlbumId = nowPlaying.albumId,
                        )
                    }
                }
            }

            Text(
                text = nowPlaying.title.ifBlank { "未在播放" },
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = nowPlaying.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (!nowPlaying.isEmpty) {
                    IconButton(onClick = { onOpenBookmarks(nowPlaying.songId) }) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = "书签",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

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
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TimeLabel(if (isDragging) dragValue.toLong() else positionMs)
                TimeLabel(durationMs)
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { PlayerConnection.toggleShuffle() }) {
                    Icon(
                        Icons.Default.Shuffle,
                        contentDescription = "随机播放",
                        tint = if (shuffle) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
                IconButton(onClick = { PlayerConnection.cycleRepeat() }) {
                    Icon(
                        when (repeat) {
                            RepeatMode.ONE -> Icons.Default.RepeatOne
                            else -> Icons.Default.Repeat
                        },
                        contentDescription = repeat.label,
                        tint = if (repeat != RepeatMode.OFF) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.VolumeUp, contentDescription = "音量")
                Slider(
                    value = volume,
                    onValueChange = {
                        volume = it
                        PlayerConnection.setVolume(it)
                    },
                    valueRange = 0f..1f,
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                )
                IconButton(onClick = onOpenEqualizer) {
                    Icon(Icons.Default.Tune, contentDescription = "均衡器")
                }
            }
        }
    }
}

@Composable
private fun RotatingArtwork(
    model: Any?,
    isPlaying: Boolean,
    active: Boolean,
    fallbackAlbumId: Long,
) {
    val transition = rememberInfiniteTransition(label = "artwork")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20_000, easing = LinearEasing),
            repeatMode = AnimationRepeatMode.Restart,
        ),
        label = "angle",
    )
    val shouldRotate = isPlaying && active
    Surface(
        shape = CircleShape,
        tonalElevation = 6.dp,
        modifier = Modifier
            .size(300.dp)
            .rotate(if (shouldRotate) angle else 0f),
    ) {
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
            )
        } else {
            Artwork(
                source = AlbumArtSource(null, fallbackAlbumId),
                modifier = Modifier.fillMaxSize(),
            )
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
fun MiniPlayer(onExpand: () -> Unit, onOpenQueue: () -> Unit = {}) {
    val nowPlaying by PlayerConnection.nowPlaying.collectAsStateWithLifecycle()
    val isPlaying by PlayerConnection.isPlaying.collectAsStateWithLifecycle()
    val shuffle by PlayerConnection.shuffle.collectAsStateWithLifecycle()

    val controller = PlayerConnection.controller()
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(controller, nowPlaying.songId) {
        while (true) {
            controller?.let {
                positionMs = it.currentPosition
                durationMs = it.duration.coerceAtLeast(0L)
            }
            delay(500)
        }
    }

    if (nowPlaying.isEmpty) return

    Surface(
        tonalElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onExpand),
    ) {
        Column {
            val progress = if (durationMs > 0) {
                (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
            } else 0f
            androidx.compose.material3.LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
            )
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
                IconButton(onClick = { PlayerConnection.toggleShuffle() }) {
                    Icon(
                        Icons.Default.Shuffle,
                        contentDescription = "随机",
                        tint = if (shuffle) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
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
}

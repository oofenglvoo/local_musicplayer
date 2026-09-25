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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
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
import com.localmusic.player.ui.theme.AppAccent
import com.localmusic.player.util.toDurationString
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
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
    var volume by remember { mutableFloatStateOf(PlayerConnection.currentVolume()) }
    var page by remember { mutableStateOf(0) }
    var volumePopup by remember { mutableStateOf(false) }

    val lyricsViewModel: LyricsInlineViewModel = hiltViewModel()
    val lyricsState by lyricsViewModel.state.collectAsStateWithLifecycle()
    val favoriteViewModel: com.localmusic.player.ui.playlist.FavoritesViewModel = hiltViewModel()
    val favoriteIds by favoriteViewModel.favoriteIds.collectAsStateWithLifecycle()

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

    val artworkPagerState = rememberPagerState(initialPage = 0) { 2 }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onCollapse) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "歌曲",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = if (page == 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (page == 0) AppAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable { page = 0 },
                    )
                    Spacer(Modifier.width(24.dp))
                    Text(
                        "歌词",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = if (page == 1) FontWeight.Bold else FontWeight.Normal,
                        color = if (page == 1) AppAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable { page = if (page == 1) 0 else 1 },
                    )
                }
                IconButton(onClick = onOpenQueue) {
                    Icon(Icons.Default.QueueMusic, contentDescription = "播放队列")
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (page == 1) {
                    Box(modifier = Modifier.fillMaxSize().clickable { page = 0 }) {
                        val lyrics = lyricsState.lyrics
                        when {
                            lyricsState.loading -> CircularProgressIndicator(color = AppAccent, modifier = Modifier.align(Alignment.Center))
                            lyrics != null -> SyncedLyrics(lyrics = lyrics)
                            else -> Text(
                                "暂无歌词\n（可在歌曲同目录放置同名 .lrc 文件）",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.Center),
                            )
                        }
                    }
                } else {
                    HorizontalPager(state = artworkPagerState, modifier = Modifier.fillMaxSize()) { p ->
                        if (p == 0) {
                            VinylDisc(model = artworkModel, pageModel = artworkModel, isPlaying = isPlaying,
                                active = true, fallbackAlbumId = nowPlaying.albumId,
                                onClick = { page = 1 })
                        } else {
                            AlbumCover(model = artworkModel, fallbackAlbumId = nowPlaying.albumId,
                                onClick = { page = 1 })
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        nowPlaying.title.ifBlank { "未在播放" },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        nowPlaying.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!nowPlaying.isEmpty) {
                    val isFavorite = nowPlaying.songId in favoriteIds
                    VolumeButton(
                        volume = volume,
                        expanded = volumePopup,
                        onExpandedChange = { volumePopup = it },
                        onVolumeChange = { volume = it; PlayerConnection.setVolume(it) },
                    )
                    IconButton(
                        onClick = { favoriteViewModel.toggleFavorite(nowPlaying.songId) },
                    ) {
                        Icon(
                            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "收藏",
                            tint = if (isFavorite) AppAccent
                            else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(onClick = { onOpenBookmarks(nowPlaying.songId) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.PlaylistAdd,
                            contentDescription = "添加到歌单",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            CreditsBlock(artist = nowPlaying.artist)

            SeekBar(
                positionMs = if (isDragging) dragValue.toLong() else positionMs,
                durationMs = durationMs,
                onStartDrag = { isDragging = true },
                onDrag = { dragValue = it.toFloat() },
                onEndDrag = { PlayerConnection.seekTo(dragValue.toLong()); isDragging = false },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TimeLabel(if (isDragging) dragValue.toLong() else positionMs)
                TimeLabel(durationMs)
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { PlayerConnection.toggleShuffle() }) {
                    Icon(
                        Icons.Default.Shuffle,
                        contentDescription = "随机播放",
                        tint = if (shuffle) AppAccent else MaterialTheme.colorScheme.onSurface,
                    )
                }
                IconButton(onClick = { PlayerConnection.previous() }) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "上一首",
                        modifier = Modifier.size(40.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(AppAccent)
                        .clickable {
                            if (!nowPlaying.isEmpty) PlayerConnection.togglePlayPause()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "播放/暂停",
                        tint = Color.Black,
                        modifier = Modifier.size(40.dp),
                    )
                }
                IconButton(onClick = { PlayerConnection.next() }) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "下一首",
                        modifier = Modifier.size(40.dp),
                    )
                }
                IconButton(onClick = { PlayerConnection.cycleRepeat() }) {
                    Icon(
                        when (repeat) {
                            RepeatMode.ONE -> Icons.Default.RepeatOne
                            else -> Icons.Default.Repeat
                        },
                        contentDescription = repeat.label,
                        tint = if (repeat != RepeatMode.OFF) AppAccent
                        else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun CreditsBlock(artist: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row {
            Text(
                "作词：",
                style = MaterialTheme.typography.bodyMedium,
                color = AppAccent,
            )
            Text(
                "网络佚名",
                style = MaterialTheme.typography.bodyMedium,
                color = AppAccent,
            )
        }
        Spacer(Modifier.height(6.dp))
        Row {
            Text(
                "作曲：",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                artist.ifBlank { "未知" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SeekBar(
    positionMs: Long,
    durationMs: Long,
    onStartDrag: () -> Unit,
    onDrag: (Long) -> Unit,
    onEndDrag: () -> Unit,
) {
    val duration = durationMs.coerceAtLeast(1L)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .pointerInput(duration) {
                fun valueAt(x: Float): Long =
                    (x.coerceIn(0f, size.width.toFloat()) / size.width * duration).toLong()
                detectTapGestures { offset ->
                    onStartDrag()
                    onDrag(valueAt(offset.x))
                    onEndDrag()
                }
            }
            .pointerInput(duration) {
                detectDragGestures(
                    onDragStart = { onStartDrag() },
                    onDragEnd = { onEndDrag() },
                    onDragCancel = { onEndDrag() },
                ) { change, _ ->
                    change.consume()
                    onDrag(
                        (change.position.x.coerceIn(0f, size.width.toFloat()) / size.width * duration).toLong(),
                    )
                }
            },
    ) {
        val y = size.height / 2f
        val progress = (positionMs.coerceIn(0L, duration).toFloat() / duration).coerceIn(0f, 1f)
        val x = size.width * progress
        drawLine(Color(0xFFDCE4DF), Offset(0f, y), Offset(size.width, y), strokeWidth = 3f)
        drawLine(AppAccent, Offset(0f, y), Offset(x, y), strokeWidth = 3f)
        drawCircle(AppAccent, radius = 5f, center = Offset(x, y))
    }
}

@Composable
private fun VolumeButton(
    volume: Float,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onVolumeChange: (Float) -> Unit,
) {
    Box {
        IconButton(onClick = { onExpandedChange(!expanded) }) {
            Icon(
                Icons.Default.VolumeUp,
                contentDescription = "音量",
                tint = if (expanded) AppAccent else MaterialTheme.colorScheme.onSurface,
            )
        }
        if (expanded) {
            Popup(
                alignment = Alignment.TopCenter,
                onDismissRequest = { onExpandedChange(false) },
            ) {
                Surface(
                    modifier = Modifier.padding(top = 44.dp),
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 5.dp,
                    shadowElevation = 5.dp,
                ) {
                    Canvas(
                        modifier = Modifier
                            .width(42.dp)
                            .height(128.dp)
                            .padding(horizontal = 17.dp, vertical = 12.dp)
                            .pointerInput(Unit) {
                                fun valueAt(y: Float): Float =
                                    (1f - y / size.height).coerceIn(0f, 1f)
                                detectTapGestures { offset -> onVolumeChange(valueAt(offset.y)) }
                            }
                            .pointerInput(Unit) {
                                detectDragGestures { change, _ ->
                                    change.consume()
                                    onVolumeChange((1f - change.position.y / size.height).coerceIn(0f, 1f))
                                }
                            },
                    ) {
                        val x = size.width / 2f
                        val y = size.height * (1f - volume.coerceIn(0f, 1f))
                        drawLine(Color(0xFFDCE4DF), Offset(x, 0f), Offset(x, size.height), strokeWidth = 3f)
                        drawLine(AppAccent, Offset(x, y), Offset(x, size.height), strokeWidth = 3f)
                        drawCircle(AppAccent, radius = 5f, center = Offset(x, y))
                    }
                }
            }
        }
    }
}

@Composable
private fun VinylDisc(
    model: Any?,
    pageModel: Any?,
    isPlaying: Boolean,
    active: Boolean,
    fallbackAlbumId: Long,
    onClick: () -> Unit,
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

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val discSize = minOf(maxWidth, maxHeight)
            .coerceAtMost(360.dp)
            .coerceAtLeast(180.dp)
        Box(
            modifier = Modifier
                .size(discSize)
                .clip(CircleShape)
                .background(Color(0xFF111111))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(discSize * 0.92f)
                    .clip(CircleShape)
                    .rotate(if (shouldRotate) angle else 0f),
                contentAlignment = Alignment.Center,
            ) {
                if (pageModel != null) {
                    AsyncImage(
                        model = pageModel,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                    )
                } else {
                    Artwork(
                        source = AlbumArtSource(null, fallbackAlbumId),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(discSize * 0.24f)
                    .clip(CircleShape)
                    .background(Color(0xFF111111)),
            )
        }
    }
}

@Composable
private fun AlbumCover(
    model: Any?,
    fallbackAlbumId: Long,
    onClick: () -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val coverSize = minOf(maxWidth, maxHeight).coerceAtMost(420.dp)
        Box(
            modifier = Modifier
                .size(coverSize)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = "专辑封面",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Artwork(
                source = AlbumArtSource(null, fallbackAlbumId),
                modifier = Modifier.fillMaxSize(),
            )
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
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        tonalElevation = 0.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onExpand),
    ) {
        Column {
            val progress = if (durationMs > 0) {
                (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
            } else 0f
            androidx.compose.material3.LinearProgressIndicator(
                progress = { progress },
                color = AppAccent,
                trackColor = AppAccent.copy(alpha = 0.18f),
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
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)),
                )
                Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
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
                        tint = AppAccent,
                    )
                }
                IconButton(onClick = { PlayerConnection.next() }) {
                    Icon(Icons.Default.SkipNext, contentDescription = "下一首")
                }
            }
        }
    }
}

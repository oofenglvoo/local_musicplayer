package com.localmusic.player.ui.local

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.localmusic.player.data.AlbumGroup
import com.localmusic.player.data.ArtistGroup
import com.localmusic.player.data.FolderGroup
import com.localmusic.player.data.db.SongEntity
import com.localmusic.player.ui.SongArtwork
import com.localmusic.player.ui.library.LibraryViewModel
import com.localmusic.player.ui.theme.AppAccent
import com.localmusic.player.ui.theme.AppInk
import com.localmusic.player.util.toDurationString

@Composable
fun LocalScreen(
    viewModel: LibraryViewModel,
    onOpenFolderPicker: () -> Unit,
    onPlaySongs: (List<SongEntity>, Int) -> Unit,
    onOpenAlbum: (AlbumGroup) -> Unit,
    onOpenArtist: (ArtistGroup) -> Unit,
    onOpenFolder: (FolderGroup) -> Unit,
) {
    val songs by viewModel.sortedSongs.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val displayed = remember(songs) { songs }

    if (loading) {
        Box(modifier = Modifier.fillMaxSize().background(AppInk.copy(alpha = 0.02f)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                androidx.compose.material3.CircularProgressIndicator(color = AppAccent)
                Spacer(Modifier.height(16.dp))
                Text("正在扫描音乐库…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppInk.copy(alpha = 0.02f)),
    ) {
        Text(
            "本地",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.statusBarsPadding().padding(start = 20.dp, top = 16.dp, bottom = 8.dp),
        )

        if (displayed.isEmpty()) {
            EmptyLocalState(onScan = onOpenFolderPicker)
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AppAccent)
                        .clickable { onPlaySongs(displayed, 0) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Text(
                    "${displayed.size} 首本地歌曲",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Default.Folder,
                    contentDescription = "选择目录",
                    tint = AppAccent,
                    modifier = Modifier.size(26.dp).clickable(onClick = onOpenFolderPicker),
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                itemsIndexed(displayed, key = { _, s -> s.id }) { index, song ->
                    LocalSongRow(
                        song = song,
                        onPlay = { onPlaySongs(displayed, index) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyLocalState(onScan: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(bottom = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        WaveformIllustration()
        Spacer(Modifier.height(28.dp))
        Text(
            "暂无本地歌曲",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(28.dp))
                .background(AppAccent)
                .clickable(onClick = onScan)
                .padding(horizontal = 40.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                Icons.Default.Folder,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(22.dp),
            )
            Text(
                "开始扫描",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
            )
        }
    }
}

@Composable
private fun WaveformIllustration() {
    val accent = AppAccent
    val soft = AppAccent.copy(alpha = 0.25f)
    Canvas(modifier = Modifier.fillMaxWidth().height(160.dp).padding(horizontal = 24.dp)) {
        val w = size.width
        val h = size.height
        val mid = h / 2f
        val amplitudes = listOf(
            0.55f, 0.2f, 0.95f, 0.35f, 0.8f, 0.45f, 0.7f, 0.55f,
            0.3f, 0.9f, 0.4f, 0.75f, 0.5f, 0.85f, 0.45f, 0.65f, 0.3f,
        )
        val step = w / (amplitudes.size - 1)
        val mainPath = Path()
        val mirrorPath = Path()
        amplitudes.forEachIndexed { index, amp ->
            val x = index * step
            val y = mid - amp * mid * 0.92f
            if (index == 0) {
                mainPath.moveTo(x, y)
                mirrorPath.moveTo(x, mid + (mid - y))
            } else {
                mainPath.lineTo(x, y)
                mirrorPath.lineTo(x, mid + (mid - y))
            }
        }
        drawLine(
            color = accent.copy(alpha = 0.12f),
            start = Offset(0f, mid),
            end = Offset(w, mid),
            strokeWidth = 18f,
        )
        drawPath(mainPath, color = soft, style = Stroke(width = 6f))
        drawPath(mirrorPath, color = soft, style = Stroke(width = 6f))
        drawPath(mainPath, color = accent, style = Stroke(width = 4f))
    }
}

@Composable
private fun LocalSongRow(song: SongEntity, onPlay: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SongArtwork(
            song = song,
            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)),
        )
        Column(
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
        ) {
            Text(
                song.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            song.duration.toDurationString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(AppAccent)
                .clickable(onClick = onPlay),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "播放",
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

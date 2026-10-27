package com.localmusic.player.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.localmusic.player.data.db.SongEntity
import com.localmusic.player.playback.PlayerConnection
import com.localmusic.player.util.toDurationString

/**
 * 页面级订阅当前播放歌曲 id，避免每一行 [SongRow] 各自 collect 播放状态。
 * [PlayerConnection] 的 mediaId 就是 [SongEntity.id]，可直接与列表项比较。
 */
@Composable
fun rememberCurrentSongId(): Long {
    val nowPlaying by PlayerConnection.nowPlaying.collectAsStateWithLifecycle()
    return nowPlaying.songId
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SongRow(
    song: SongEntity,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    selected: Boolean = false,
    selectionMode: Boolean = false,
    isCurrent: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    ListItem(
        modifier = Modifier
            .drawWithContent {
                drawContent()
                // 左侧强调竖条：与底色一起构成"正在播放"的强提示，三种主题下都清晰
                if (isCurrent) {
                    drawRect(
                        color = colors.primary,
                        size = Size(3.dp.toPx(), size.height),
                    )
                }
            }
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = if (isCurrent) {
            ListItemDefaults.colors(containerColor = colors.primaryContainer)
        } else {
            ListItemDefaults.colors()
        },
        headlineContent = {
            Text(
                song.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (isCurrent) FontWeight.SemiBold else null,
                color = if (isCurrent) colors.onPrimaryContainer else Color.Unspecified,
            )
        },
        supportingContent = {
            Text(
                "${song.artist} · ${song.album}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingContent = {
            if (selectionMode) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = { onLongClick() },
                )
            } else {
                SongArtwork(
                    song = song,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    song.duration.toDurationString(),
                    style = MaterialTheme.typography.labelSmall,
                )
                IconButton(onClick = onFavoriteClick) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline,
                    )
                }
            }
        },
    )
}

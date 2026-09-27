package com.localmusic.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.localmusic.player.ui.theme.AppAccent
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val THUMB_WIDTH_DP = 5f
private const val THUMB_WIDTH_DRAG_DP = 9f
private const val MIN_THUMB_DP = 36f

private fun computeFraction(
    totalItems: Int,
    visibleItems: Int,
    firstVisibleIndex: Int,
    firstVisibleOffset: Int,
    itemHeightPx: Float,
): Float {
    if (totalItems <= 0) return 0f
    val maxFirstIndex = (totalItems - visibleItems).coerceAtLeast(0)
    if (maxFirstIndex <= 0) return 0f
    val within = if (itemHeightPx > 0f) (firstVisibleOffset / itemHeightPx).coerceIn(0f, 1f) else 0f
    val position = firstVisibleIndex + within
    return (position / maxFirstIndex).coerceIn(0f, 1f)
}

@Composable
fun FastScrollbar(listState: LazyListState, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    ScrollbarTrack(
        modifier = modifier,
        totalItems = listState.layoutInfo.totalItemsCount,
        visibleItems = listState.layoutInfo.visibleItemsInfo.size,
        firstVisibleIndex = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0,
        firstVisibleOffset = listState.firstVisibleItemScrollOffset,
        itemHeightPx = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.size?.toFloat() ?: 0f,
        onSeek = { fraction ->
            val total = listState.layoutInfo.totalItemsCount
            if (total > 0) {
                val target = (fraction * (total - 1)).roundToInt().coerceIn(0, total - 1)
                scope.launch { listState.scrollToItem(target) }
            }
        },
    )
}

@Composable
fun FastScrollbar(gridState: LazyGridState, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    ScrollbarTrack(
        modifier = modifier,
        totalItems = gridState.layoutInfo.totalItemsCount,
        visibleItems = gridState.layoutInfo.visibleItemsInfo.size,
        firstVisibleIndex = gridState.layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0,
        firstVisibleOffset = gridState.firstVisibleItemScrollOffset,
        itemHeightPx = gridState.layoutInfo.visibleItemsInfo.firstOrNull()?.size?.height?.toFloat() ?: 0f,
        onSeek = { fraction ->
            val total = gridState.layoutInfo.totalItemsCount
            if (total > 0) {
                val target = (fraction * (total - 1)).roundToInt().coerceIn(0, total - 1)
                scope.launch { gridState.scrollToItem(target) }
            }
        },
    )
}

@Composable
fun ScrollableSongList(
    modifier: Modifier = Modifier,
    contentPadding: androidx.compose.foundation.layout.PaddingValues =
        androidx.compose.foundation.layout.PaddingValues(0.dp),
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            content = content,
        )
        FastScrollbar(
            listState = listState,
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
        )
    }
}

@Composable
private fun ScrollbarTrack(
    modifier: Modifier,
    totalItems: Int,
    visibleItems: Int,
    firstVisibleIndex: Int,
    firstVisibleOffset: Int,
    itemHeightPx: Float,
    onSeek: (Float) -> Unit,
) {
    if (totalItems <= 0) return

    val density = LocalDensity.current
    var trackHeightPx by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableFloatStateOf(0f) }

    val minThumbPx = with(density) { MIN_THUMB_DP.dp.toPx() }
    val visible = visibleItems.coerceAtLeast(1)
    val fraction = computeFraction(totalItems, visible, firstVisibleIndex, firstVisibleOffset, itemHeightPx)
    val thumbHeightPx = if (trackHeightPx > 0f) {
        (trackHeightPx * (visible.toFloat() / totalItems.toFloat())).coerceIn(minThumbPx, trackHeightPx)
    } else 0f
    val travelPx = (trackHeightPx - thumbHeightPx).coerceAtLeast(0f)
    val thumbTopPx = travelPx * fraction

    Box(
        modifier = modifier
            .width(with(density) { THUMB_WIDTH_DRAG_DP.dp })
            .fillMaxHeight()
            .onSizeChanged { trackHeightPx = it.height.toFloat() }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    if (trackHeightPx > 0f) onSeek((offset.y / trackHeightPx).coerceIn(0f, 1f))
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { dragging = 1f },
                    onDragEnd = { dragging = 0f },
                    onDragCancel = { dragging = 0f },
                ) { change, _ ->
                    change.consume()
                    if (trackHeightPx > 0f) {
                        onSeek((change.position.y / trackHeightPx).coerceIn(0f, 1f))
                    }
                }
            },
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .padding(top = with(density) { thumbTopPx.toDp() })
                .width(with(density) { (if (dragging > 0f) THUMB_WIDTH_DRAG_DP else THUMB_WIDTH_DP).dp })
                .height(with(density) { thumbHeightPx.toDp().coerceAtLeast(MIN_THUMB_DP.dp) })
                .clip(RoundedCornerShape(percent = 50))
                .background(if (dragging > 0f) AppAccent else AppAccent.copy(alpha = 0.45f)),
        )
    }
}

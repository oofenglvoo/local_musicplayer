package com.localmusic.player.widget

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.localmusic.player.MainActivity
import com.localmusic.player.playback.PlayerConnection
import java.io.File

class MusicWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MusicWidget()
}

class MusicWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            WidgetContent()
        }
    }
}

@Composable
private fun WidgetContent() {
    val nowPlaying = PlayerConnection.nowPlaying.value
    val isPlaying = PlayerConnection.isPlaying.value
    val shuffle = PlayerConnection.shuffle.value
    val repeat = PlayerConnection.repeat.value

    val title = if (nowPlaying.isEmpty) "本地播放器" else nowPlaying.title
    val subtitle = if (nowPlaying.isEmpty) "点击打开应用" else nowPlaying.artist

    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(Color(0xFF1B1B2F))
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val artworkPath = nowPlaying.artworkPath
            val bitmap = artworkPath?.let { path ->
                File(path).takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.absolutePath) }
            }
            if (bitmap != null) {
                Image(
                    provider = ImageProvider(bitmap),
                    contentDescription = null,
                    modifier = GlanceModifier.size(48.dp),
                )
                Spacer(GlanceModifier.width(10.dp))
            }
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = title,
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                )
                Spacer(GlanceModifier.size(2.dp))
                Text(
                    text = subtitle,
                    style = TextStyle(color = ColorProvider(Color(0xFFB0B0C0)), fontSize = 12.sp),
                    maxLines = 1,
                )
            }
        }
        Spacer(GlanceModifier.size(8.dp))
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "⤨",
                modifier = GlanceModifier
                    .padding(4.dp)
                    .clickable(actionRunCallback<ShuffleAction>()),
                style = TextStyle(
                    color = ColorProvider(if (shuffle) Color(0xFFD0BCFF) else Color(0xFF808090)),
                    fontSize = 16.sp,
                ),
            )
            Spacer(GlanceModifier.width(12.dp))
            Text(
                text = "◀◀",
                modifier = GlanceModifier
                    .padding(4.dp)
                    .clickable(actionRunCallback<PrevAction>()),
                style = TextStyle(color = ColorProvider(Color.White), fontSize = 18.sp),
            )
            Spacer(GlanceModifier.width(12.dp))
            Text(
                text = if (isPlaying) "❚❚" else "▶",
                modifier = GlanceModifier
                    .padding(4.dp)
                    .clickable(actionRunCallback<ToggleAction>()),
                style = TextStyle(color = ColorProvider(Color.White), fontSize = 18.sp),
            )
            Spacer(GlanceModifier.width(12.dp))
            Text(
                text = "▶▶",
                modifier = GlanceModifier
                    .padding(4.dp)
                    .clickable(actionRunCallback<NextAction>()),
                style = TextStyle(color = ColorProvider(Color.White), fontSize = 18.sp),
            )
            Spacer(GlanceModifier.width(12.dp))
            Text(
                text = when (repeat) {
                    com.localmusic.player.playback.RepeatMode.ONE -> "↻1"
                    com.localmusic.player.playback.RepeatMode.ALL -> "↻"
                    else -> "↻"
                },
                modifier = GlanceModifier
                    .padding(4.dp)
                    .clickable(actionRunCallback<RepeatAction>()),
                style = TextStyle(
                    color = ColorProvider(
                        if (repeat != com.localmusic.player.playback.RepeatMode.OFF) Color(0xFFD0BCFF)
                        else Color(0xFF808090)
                    ),
                    fontSize = 16.sp,
                ),
            )
        }
    }
}

private suspend fun refreshWidget(context: Context, glanceId: GlanceId) {
    MusicWidget().update(context, glanceId)
}

class ToggleAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        PlayerConnection.togglePlayPause()
        refreshWidget(context, glanceId)
    }
}

class NextAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        PlayerConnection.next()
        refreshWidget(context, glanceId)
    }
}

class PrevAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        PlayerConnection.previous()
        refreshWidget(context, glanceId)
    }
}

class ShuffleAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        PlayerConnection.toggleShuffle()
        refreshWidget(context, glanceId)
    }
}

class RepeatAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        PlayerConnection.cycleRepeat()
        refreshWidget(context, glanceId)
    }
}

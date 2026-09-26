package com.localmusic.player.widget

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import com.localmusic.player.R
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

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            DpSize(110.dp, 56.dp),
            DpSize(180.dp, 110.dp),
            DpSize(250.dp, 110.dp),
            DpSize(320.dp, 180.dp),
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val size = LocalSize.current
            WidgetContent(
                compact = size.width < 160.dp,
                defaultTitle = context.getString(R.string.app_name),
                openAppHint = context.getString(R.string.widget_open_app),
            )
        }
    }
}

@Composable
private fun WidgetContent(compact: Boolean, defaultTitle: String, openAppHint: String) {
    val nowPlaying = PlayerConnection.nowPlaying.value
    val isPlaying = PlayerConnection.isPlaying.value
    val shuffle = PlayerConnection.shuffle.value
    val repeat = PlayerConnection.repeat.value

    val title = if (nowPlaying.isEmpty) defaultTitle else nowPlaying.title
    val subtitle = if (nowPlaying.isEmpty) openAppHint else nowPlaying.artist

    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(Color(0xFF1B1B2F))
            .padding(if (compact) 8.dp else 12.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!compact) {
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
            }
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = title,
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = if (compact) 13.sp else 16.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                )
                if (!compact) {
                    Spacer(GlanceModifier.size(2.dp))
                    Text(
                        text = subtitle,
                        style = TextStyle(color = ColorProvider(Color(0xFFB0B0C0)), fontSize = 12.sp),
                        maxLines = 1,
                    )
                }
            }
        }
        Spacer(GlanceModifier.size(if (compact) 4.dp else 8.dp))
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!compact) {
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
            }
            Text(
                text = "◀◀",
                modifier = GlanceModifier
                    .padding(4.dp)
                    .clickable(actionRunCallback<PrevAction>()),
                style = TextStyle(color = ColorProvider(Color.White), fontSize = 18.sp),
            )
            Spacer(GlanceModifier.width(if (compact) 8.dp else 12.dp))
            Text(
                text = if (isPlaying) "❚❚" else "▶",
                modifier = GlanceModifier
                    .padding(4.dp)
                    .clickable(actionRunCallback<ToggleAction>()),
                style = TextStyle(color = ColorProvider(Color.White), fontSize = 18.sp),
            )
            Spacer(GlanceModifier.width(if (compact) 8.dp else 12.dp))
            Text(
                text = "▶▶",
                modifier = GlanceModifier
                    .padding(4.dp)
                    .clickable(actionRunCallback<NextAction>()),
                style = TextStyle(color = ColorProvider(Color.White), fontSize = 18.sp),
            )
            if (!compact) {
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
        PlayerConnection.runOnMain(context) { it?.let { c -> if (c.isPlaying) c.pause() else c.play() } }
        refreshWidget(context, glanceId)
    }
}

class NextAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        PlayerConnection.runOnMain(context) { it?.seekToNextMediaItem() }
        refreshWidget(context, glanceId)
    }
}

class PrevAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        PlayerConnection.runOnMain(context) { it?.seekToPreviousMediaItem() }
        refreshWidget(context, glanceId)
    }
}

class ShuffleAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        PlayerConnection.runOnMain(context) { it?.let { c -> c.shuffleModeEnabled = !c.shuffleModeEnabled } }
        refreshWidget(context, glanceId)
    }
}

class RepeatAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        PlayerConnection.runOnMain(context) { c ->
            c?.let {
                it.repeatMode = when (it.repeatMode) {
                    androidx.media3.common.Player.REPEAT_MODE_OFF -> androidx.media3.common.Player.REPEAT_MODE_ALL
                    androidx.media3.common.Player.REPEAT_MODE_ALL -> androidx.media3.common.Player.REPEAT_MODE_ONE
                    else -> androidx.media3.common.Player.REPEAT_MODE_OFF
                }
            }
        }
        refreshWidget(context, glanceId)
    }
}

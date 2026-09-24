package com.localmusic.player.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
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

    val title = if (nowPlaying.isEmpty) "本地播放器" else nowPlaying.title
    val artist = if (nowPlaying.isEmpty) "点击打开应用" else nowPlaying.artist

    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(Color(0xFF1B1B2F))
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
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
            text = artist,
            style = TextStyle(color = ColorProvider(Color(0xFFB0B0C0)), fontSize = 12.sp),
            maxLines = 1,
        )
        Spacer(GlanceModifier.size(8.dp))
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "◀◀",
                modifier = GlanceModifier
                    .padding(4.dp)
                    .clickable(actionRunCallback<PrevAction>()),
                style = TextStyle(color = ColorProvider(Color.White), fontSize = 18.sp),
            )
            Spacer(GlanceModifier.width(16.dp))
            Text(
                text = if (isPlaying) "❚❚" else "▶",
                modifier = GlanceModifier
                    .padding(4.dp)
                    .clickable(actionRunCallback<ToggleAction>()),
                style = TextStyle(color = ColorProvider(Color.White), fontSize = 18.sp),
            )
            Spacer(GlanceModifier.width(16.dp))
            Text(
                text = "▶▶",
                modifier = GlanceModifier
                    .padding(4.dp)
                    .clickable(actionRunCallback<NextAction>()),
                style = TextStyle(color = ColorProvider(Color.White), fontSize = 18.sp),
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

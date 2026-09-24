package com.localmusic.player.playback

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                attachEffects(player.audioSessionId)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                attachEffects(player.audioSessionId)
            }
        })
        attachEffects(player.audioSessionId)

        mediaSession = MediaSession.Builder(this, player).build()
    }

    private fun attachEffects(sessionId: Int) {
        if (sessionId == 0) return
        if (sessionId == attachedSessionId) return
        attachedSessionId = sessionId
        PlayerConnection.effects.release()
        PlayerConnection.effects.attach(sessionId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        PlayerConnection.effects.release()
        super.onDestroy()
    }

    companion object {
        private var attachedSessionId = 0
    }
}

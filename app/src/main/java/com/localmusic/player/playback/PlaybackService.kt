package com.localmusic.player.playback

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null

    val replayGainProcessor = ReplayGainProcessor()

    override fun onCreate() {
        super.onCreate()

        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean,
            ): AudioSink {
                val audioProcessors = arrayOf<androidx.media3.common.audio.AudioProcessor>(
                    replayGainProcessor,
                )
                return DefaultAudioSink.Builder(context)
                    .setAudioProcessors(audioProcessors)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .build()
            }
        }

        val exoPlayer = ExoPlayer.Builder(this, renderersFactory)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setPauseAtEndOfMediaItems(false)
            .build()

        player = exoPlayer
        ReplayGainManager.attach(exoPlayer, replayGainProcessor)
        PlayerConnection.crossfade.attach(exoPlayer)
        SkipSilenceBridge.register(exoPlayer)

        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                attachEffects(exoPlayer.audioSessionId)
                if (playbackState == Player.STATE_READY) {
                    ReplayGainManager.applyForCurrent(exoPlayer)
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                attachEffects(exoPlayer.audioSessionId)
                if (isPlaying) {
                    startCrossfadeTicker()
                } else {
                    stopCrossfadeTicker()
                }
            }

            override fun onMediaItemTransition(
                mediaItem: androidx.media3.common.MediaItem?,
                reason: Int,
            ) {
                ReplayGainManager.applyForCurrent(exoPlayer)
            }
        })
        attachEffects(exoPlayer.audioSessionId)

        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setCallback(SessionCallback())
            .setSessionActivity(
                android.app.PendingIntent.getActivity(
                    this,
                    0,
                    android.content.Intent(this, com.localmusic.player.MainActivity::class.java),
                    android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT,
                )
            )
            .build()
    }

    private inner class SessionCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            // Only export the session to the system UI / our own app; reject arbitrary callers
            // unless they hold the privileged MEDIA_CONTENT_CONTROL permission.
            if (!isTrustedController(controller)) {
                return MediaSession.ConnectionResult.reject()
            }
            val result = super.onConnect(session, controller)
            val sessionCommands = result.availableSessionCommands
                .buildUpon()
                .add(COMMAND_TOGGLE_SHUFFLE)
                .add(COMMAND_CYCLE_REPEAT)
                .build()
            val playerCommands = result.availablePlayerCommands
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .setAvailablePlayerCommands(playerCommands)
                .setCustomLayout(
                    listOf(
                        shuffleButton(session),
                        repeatButton(session),
                    )
                )
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: androidx.media3.session.SessionCommand,
            args: android.os.Bundle,
        ): com.google.common.util.concurrent.ListenableFuture<androidx.media3.session.SessionResult> {
            when (customCommand.customAction) {
                COMMAND_TOGGLE_SHUFFLE.customAction -> {
                    val p = session.player
                    p.shuffleModeEnabled = !p.shuffleModeEnabled
                }
                COMMAND_CYCLE_REPEAT.customAction -> {
                    val p = session.player
                    p.repeatMode = when (p.repeatMode) {
                        Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                        Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                        else -> Player.REPEAT_MODE_OFF
                    }
                }
            }
            return com.google.common.util.concurrent.Futures.immediateFuture(
                androidx.media3.session.SessionResult(androidx.media3.session.SessionResult.RESULT_SUCCESS)
            )
        }

        private fun shuffleButton(session: MediaSession): androidx.media3.session.CommandButton {
            val shuffleOn = session.player.shuffleModeEnabled
            return androidx.media3.session.CommandButton.Builder()
                .setDisplayName(getString(com.localmusic.player.R.string.common_shuffle))
                .setIconResId(
                    if (shuffleOn) android.R.drawable.ic_menu_sort_by_size
                    else android.R.drawable.ic_menu_sort_by_size
                )
                .setSessionCommand(COMMAND_TOGGLE_SHUFFLE)
                .setEnabled(true)
                .build()
        }

        private fun repeatButton(session: MediaSession): androidx.media3.session.CommandButton {
            val repeatOn = session.player.repeatMode != Player.REPEAT_MODE_OFF
            return androidx.media3.session.CommandButton.Builder()
                .setDisplayName(
                    when (session.player.repeatMode) {
                        Player.REPEAT_MODE_ONE -> getString(com.localmusic.player.R.string.repeat_one)
                        Player.REPEAT_MODE_ALL -> getString(com.localmusic.player.R.string.repeat_all)
                        else -> getString(com.localmusic.player.R.string.repeat_off)
                    }
                )
                .setIconResId(android.R.drawable.ic_menu_rotate)
                .setSessionCommand(COMMAND_CYCLE_REPEAT)
                .setEnabled(true)
                .build()
        }
    }

    private fun isTrustedController(controller: MediaSession.ControllerInfo): Boolean {
        if (controller.uid == android.os.Process.myUid()) return true
        if (controller.uid == 1000) return true
        val pkg = controller.packageName
        if (pkg == "android" || pkg == "com.android.systemui") return true
        return runCatching {
            checkCallingOrSelfPermission(android.Manifest.permission.MEDIA_CONTENT_CONTROL) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)
    }

    private var crossfadeJob: Job? = null
    private val serviceScope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main,
    )

    private fun startCrossfadeTicker() {
        if (crossfadeJob?.isActive == true) return
        crossfadeJob = serviceScope.launch {
            while (true) {
                PlayerConnection.tickCrossfade()
                delay(60)
            }
        }
    }

    private fun stopCrossfadeTicker() {
        crossfadeJob?.cancel()
        crossfadeJob = null
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
        stopCrossfadeTicker()
        SkipSilenceBridge.unregister()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        player = null
        PlayerConnection.effects.release()
        PlayerConnection.crossfade.release()
        ReplayGainManager.release()
        super.onDestroy()
    }

    companion object {
        private var attachedSessionId = 0
        private val COMMAND_TOGGLE_SHUFFLE =
            androidx.media3.session.SessionCommand("com.localmusic.player.TOGGLE_SHUFFLE", android.os.Bundle.EMPTY)
        private val COMMAND_CYCLE_REPEAT =
            androidx.media3.session.SessionCommand("com.localmusic.player.CYCLE_REPEAT", android.os.Bundle.EMPTY)
    }
}

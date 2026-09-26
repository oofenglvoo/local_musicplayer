package com.localmusic.player.playback

import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ExoPlayer has no native crossfade, so this implements a volume-ramp crossfade:
 * the outgoing track fades out near its end and the incoming track fades in at its
 * start. Requires the player to expose position/duration changes.
 */
class CrossfadeController {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var player: ExoPlayer? = null
    private var fadeJob: Job? = null

    @Volatile
    private var crossfadeMs: Int = 0

    @Volatile
    private var userVolume: Float = 1f

    fun attach(exoPlayer: ExoPlayer) {
        player = exoPlayer
    }

    fun setCrossfadeMs(ms: Int) {
        crossfadeMs = ms.coerceIn(0, 30_000)
        if (crossfadeMs == 0) resetToUserVolume()
    }

    fun effectiveCrossfadeMs(): Int = crossfadeMs

    fun tick() {
        val p = player ?: return
        if (crossfadeMs <= 0) {
            resetToUserVolume()
            return
        }
        if (!p.isPlaying) {
            // Don't keep the track ducked once playback stops.
            resetToUserVolume()
            return
        }
        if (p.mediaItemCount <= 1) {
            resetToUserVolume()
            return
        }

        val duration = p.duration
        val position = p.currentPosition
        if (duration <= 0 || position < 0) return

        val remaining = duration - position
        val fadeWindow = crossfadeMs.toLong()

        val targetVolume = when {
            remaining <= fadeWindow -> {
                (remaining.toFloat() / fadeWindow).coerceIn(0f, 1f) * userVolume
            }
            position <= fadeWindow -> {
                (position.toFloat() / fadeWindow).coerceIn(0f, 1f) * userVolume
            }
            else -> userVolume
        }
        if (kotlin.math.abs(p.volume - targetVolume) > 0.01f) {
            p.volume = targetVolume
        }
    }

    fun setUserVolume(v: Float) {
        userVolume = v.coerceIn(0f, 1f)
        player?.volume = userVolume
    }

    fun userVolume(): Float = userVolume

    fun resetToUserVolume() {
        fadeJob?.cancel()
        fadeJob = null
        if (player?.volume != userVolume) {
            player?.volume = userVolume
        }
    }

    fun release() {
        fadeJob?.cancel()
        fadeJob = null
        scope.cancel()
        player = null
    }
}

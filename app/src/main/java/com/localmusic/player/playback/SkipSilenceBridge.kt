package com.localmusic.player.playback

/**
 * Skip-silence is an ExoPlayer-only feature and is not exposed through MediaController.
 * PlaybackService registers the live ExoPlayer here so the UI/controller layer can toggle it.
 */
object SkipSilenceBridge {

    private var player: androidx.media3.exoplayer.ExoPlayer? = null

    @Volatile
    private var pending: Boolean? = null

    fun register(exoPlayer: androidx.media3.exoplayer.ExoPlayer) {
        player = exoPlayer
        pending?.let { apply(exoPlayer, it) }
        pending = null
    }

    fun unregister() {
        player = null
    }

    fun setEnabled(enabled: Boolean) {
        val p = player
        if (p == null) {
            pending = enabled
        } else {
            apply(p, enabled)
        }
    }

    private fun apply(p: androidx.media3.exoplayer.ExoPlayer, enabled: Boolean) {
        runCatching { p.skipSilenceEnabled = enabled }
    }
}

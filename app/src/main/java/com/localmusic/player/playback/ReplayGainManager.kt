package com.localmusic.player.playback

import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object ReplayGainManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var processor: ReplayGainProcessor? = null
    private var player: ExoPlayer? = null

    @Volatile
    private var mode: ReplayGainMode = ReplayGainMode.OFF

    private val cache = mutableMapOf<String, ReplayGainInfo?>()

    fun attach(exoPlayer: ExoPlayer, replayGainProcessor: ReplayGainProcessor) {
        player = exoPlayer
        processor = replayGainProcessor
    }

    fun setMode(newMode: ReplayGainMode) {
        mode = newMode
        player?.let { applyForCurrent(it) }
    }

    fun applyForCurrent(exoPlayer: ExoPlayer) {
        val path = exoPlayer.currentMediaItem?.localConfiguration?.uri?.path ?: run {
            processor?.setGainDb(0f)
            return
        }
        if (mode == ReplayGainMode.OFF) {
            processor?.setGainDb(0f)
            return
        }
        scope.launch {
            val info = cache.getOrPut(path) { ReplayGainReader.read(path) }
            val gain = computeGain(info)
            processor?.setGainDb(gain)
        }
    }

    private fun computeGain(info: ReplayGainInfo?): Float {
        if (info == null) return 0f
        val useAlbum = mode == ReplayGainMode.ALBUM || mode == ReplayGainMode.ALBUM_PREVENT_CLIP
        val preventClip = mode == ReplayGainMode.TRACK_PREVENT_CLIP ||
            mode == ReplayGainMode.ALBUM_PREVENT_CLIP

        val gain = if (useAlbum) info.albumGainDb ?: info.trackGainDb else info.trackGainDb
        val peak = if (useAlbum) info.albumPeak ?: info.trackPeak else info.trackPeak

        if (gain == null) return 0f

        var result = gain
        if (preventClip && peak != null && peak > 0f) {
            val peakDb = (20 * kotlin.math.log10(peak.toDouble())).toFloat()
            val maxGain = -peakDb
            if (result > maxGain) result = maxGain
        }
        return result
    }

    fun clearCache() {
        cache.clear()
    }
}

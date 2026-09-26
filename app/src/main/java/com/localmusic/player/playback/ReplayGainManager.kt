package com.localmusic.player.playback

import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

object ReplayGainManager {

    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var processor: ReplayGainProcessor? = null

    @Volatile
    private var player: ExoPlayer? = null

    @Volatile
    private var mode: ReplayGainMode = ReplayGainMode.OFF

    private val cache = ConcurrentHashMap<String, ReplayGainInfo?>()
    private val ioMutex = Mutex()
    private var applyJob: Job? = null

    fun attach(exoPlayer: ExoPlayer, replayGainProcessor: ReplayGainProcessor) {
        player = exoPlayer
        processor = replayGainProcessor
        cache.clear()
    }

    fun setMode(newMode: ReplayGainMode) {
        mode = newMode
        // Player access must happen on the main thread.
        val p = player
        if (p != null) scope.launch { applyForCurrent(p) }
    }

    fun applyForCurrent(exoPlayer: ExoPlayer) {
        // ExoPlayer must be accessed on the application (main) thread.
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            scope.launch { applyForCurrent(exoPlayer) }
            return
        }
        applyJob?.cancel()
        val path = exoPlayer.currentMediaItem?.localConfiguration?.uri?.path ?: run {
            processor?.setGainDb(0f)
            return
        }
        if (mode == ReplayGainMode.OFF) {
            processor?.setGainDb(0f)
            return
        }
        applyJob = ioScope.launch {
            val info = ioMutex.withLock { cache.getOrPut(path) { ReplayGainReader.read(path) } }
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

    /** Releases the shared scopes and detaches from the player. */
    fun release() {
        applyJob?.cancel()
        applyJob = null
        scope.cancel()
        ioScope.cancel()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        player = null
        processor = null
        cache.clear()
    }
}

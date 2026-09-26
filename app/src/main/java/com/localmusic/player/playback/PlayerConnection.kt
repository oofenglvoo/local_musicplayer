package com.localmusic.player.playback

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.localmusic.player.data.db.SongEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NowPlaying(
    val songId: Long = -1,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val albumId: Long = 0,
    val artworkPath: String? = null,
) {
    val isEmpty: Boolean get() = songId == -1L
}

data class QueueItem(
    val songId: Long,
    val title: String,
    val artist: String,
    val albumId: Long = 0L,
    val artworkPath: String? = null,
    val isCurrent: Boolean,
)

enum class RepeatMode(val value: Int, val label: String) {
    OFF(Player.REPEAT_MODE_OFF, "顺序播放"),
    ALL(Player.REPEAT_MODE_ALL, "列表循环"),
    ONE(Player.REPEAT_MODE_ONE, "单曲循环");

    companion object {
        fun from(value: Int): RepeatMode = entries.firstOrNull { it.value == value } ?: OFF
    }
}

object PlayerConnection {

    private var controller: MediaController? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var pauseCommitJob: Job? = null

    val effects = AudioEffectsController()
    val crossfade = CrossfadeController()

    private var appContext: Context? = null
    private var onPlayRecorded: ((Long) -> Unit)? = null
    private var onStateChanged: (() -> Unit)? = null
    private var onTrackFinished: (() -> Unit)? = null

    private val _nowPlaying = MutableStateFlow(NowPlaying())
    val nowPlaying: StateFlow<NowPlaying> = _nowPlaying.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _shuffle = MutableStateFlow(false)
    val shuffle: StateFlow<Boolean> = _shuffle.asStateFlow()

    private val _repeat = MutableStateFlow(RepeatMode.OFF)
    val repeat: StateFlow<RepeatMode> = _repeat.asStateFlow()

    private val _queue = MutableStateFlow<List<QueueItem>>(emptyList())
    val queue: StateFlow<List<QueueItem>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _skipSilence = MutableStateFlow(false)
    val skipSilence: StateFlow<Boolean> = _skipSilence.asStateFlow()

    fun controller(): MediaController? = controller

    fun setPlayRecordedCallback(cb: (Long) -> Unit) {
        onPlayRecorded = cb
    }

    fun setStateChangedCallback(cb: () -> Unit) {
        onStateChanged = cb
    }

    fun setTrackFinishedCallback(cb: () -> Unit) {
        onTrackFinished = cb
    }

    fun connect(context: Context) {
        if (controller != null) return
        appContext = context.applicationContext
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            controller = runCatching { future.get() }.getOrNull()
            controller?.addListener(Listener())
            applySpeed(_playbackSpeed.value)
            applySkipSilence(_skipSilence.value)
            syncState()
        }, MoreExecutors.directExecutor())
    }

    /**
     * Ensures a controller is connected and runs [block] on the main thread.
     * Used by entry points that may start from a cold process (e.g. the widget),
     * where [connect] has not been called yet.
     */
    fun runOnMain(context: Context, block: (MediaController?) -> Unit) {
        scope.launch {
            if (controller == null) connect(context)
            var waited = 0
            while (controller == null && waited < 3000) {
                delay(50)
                waited += 50
            }
            block(controller)
        }
    }

    fun release() {
        pauseCommitJob?.cancel()
        pauseCommitJob = null
        controller?.release()
        controller = null
        _isPlaying.value = false
    }

    private fun syncState() {
        val c = controller ?: return
        updateIsPlaying(c.isPlaying)
        _shuffle.value = c.shuffleModeEnabled
        _repeat.value = RepeatMode.from(c.repeatMode)
        _currentIndex.value = c.currentMediaItemIndex
        refreshQueue()
        val item = c.currentMediaItem
        _nowPlaying.value = if (item == null) {
            NowPlaying()
        } else {
            NowPlaying(
                songId = item.mediaId.toLongOrNull() ?: -1L,
                title = item.mediaMetadata.title?.toString().orEmpty(),
                artist = item.mediaMetadata.artist?.toString().orEmpty(),
                album = item.mediaMetadata.albumTitle?.toString().orEmpty(),
                albumId = item.mediaMetadata.extras?.getLong("albumId") ?: 0L,
                artworkPath = item.mediaMetadata.extras?.getString("artworkPath"),
            )
        }
    }

    private fun updateIsPlaying(playing: Boolean) {
        if (playing) {
            pauseCommitJob?.cancel()
            pauseCommitJob = null
            if (!_isPlaying.value) _isPlaying.value = true
        } else {
            if (_isPlaying.value) {
                pauseCommitJob?.cancel()
                pauseCommitJob = scope.launch {
                    delay(300)
                    _isPlaying.value = false
                }
            }
        }
    }

    private fun refreshQueue() {
        val c = controller ?: return
        val items = buildList {
            for (i in 0 until c.mediaItemCount) {
                val mi = c.getMediaItemAt(i)
                add(
                    QueueItem(
                        songId = mi.mediaId.toLongOrNull() ?: -1L,
                        title = mi.mediaMetadata.title?.toString().orEmpty(),
                        artist = mi.mediaMetadata.artist?.toString().orEmpty(),
                        albumId = mi.mediaMetadata.extras?.getLong("albumId") ?: 0L,
                        artworkPath = mi.mediaMetadata.extras?.getString("artworkPath"),
                        isCurrent = i == c.currentMediaItemIndex,
                    )
                )
            }
        }
        _queue.value = items
    }

    fun playSongs(songs: List<SongEntity>, startIndex: Int) {
        val c = controller ?: return
        if (songs.isEmpty()) return
        val items = songs.map { it.toMediaItem() }
        c.setMediaItems(items, startIndex.coerceIn(0, items.lastIndex), 0L)
        c.prepare()
        c.play()
    }

    fun playSongsShuffled(songs: List<SongEntity>) {
        val c = controller ?: return
        if (songs.isEmpty()) return
        val items = songs.map { it.toMediaItem() }
        c.setMediaItems(items, 0, 0L)
        c.shuffleModeEnabled = true
        c.prepare()
        c.play()
    }

    fun playNext(songs: List<SongEntity>) {
        val c = controller ?: return
        if (songs.isEmpty()) return
        val insertAt = (c.currentMediaItemIndex + 1).coerceIn(0, c.mediaItemCount)
        c.addMediaItems(insertAt, songs.map { it.toMediaItem() })
        if (c.playbackState == Player.STATE_IDLE) {
            c.prepare()
        }
        if (!c.isPlaying) c.play()
    }

    fun addToQueue(songs: List<SongEntity>) {
        val c = controller ?: return
        if (songs.isEmpty()) return
        c.addMediaItems(songs.map { it.toMediaItem() })
        if (c.playbackState == Player.STATE_IDLE) {
            c.prepare()
        }
        if (!c.isPlaying) c.play()
    }

    fun playAtQueueIndex(index: Int) {
        val c = controller ?: return
        if (index !in 0 until c.mediaItemCount) return
        c.seekTo(index, 0L)
        c.play()
    }

    fun moveQueueItem(from: Int, to: Int) {
        val c = controller ?: return
        if (from == to) return
        if (from !in 0 until c.mediaItemCount) return
        if (to !in 0 until c.mediaItemCount) return
        c.moveMediaItem(from, to)
    }

    fun removeQueueItem(index: Int) {
        val c = controller ?: return
        if (index in 0 until c.mediaItemCount) {
            c.removeMediaItem(index)
        }
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else {
            if (c.playbackState == Player.STATE_IDLE || c.mediaItemCount == 0) return
            c.play()
        }
    }

    fun next() { controller?.seekToNextMediaItem() }

    fun previous() { controller?.seekToPreviousMediaItem() }

    fun seekTo(positionMs: Long) { controller?.seekTo(positionMs) }

    fun stop() {
        controller?.stop()
        controller?.clearMediaItems()
        _nowPlaying.value = NowPlaying()
        _isPlaying.value = false
        _queue.value = emptyList()
    }

    fun setVolume(volume: Float) {
        crossfade.setUserVolume(volume)
    }

    fun currentVolume(): Float = crossfade.userVolume()

    fun tickCrossfade() {
        crossfade.tick()
    }

    fun toggleShuffle() {
        val c = controller ?: return
        c.shuffleModeEnabled = !c.shuffleModeEnabled
    }

    fun setShuffle(enabled: Boolean) {
        controller?.shuffleModeEnabled = enabled
    }

    fun cycleRepeat() {
        val c = controller ?: return
        c.repeatMode = when (RepeatMode.from(c.repeatMode)) {
            RepeatMode.OFF -> Player.REPEAT_MODE_ALL
            RepeatMode.ALL -> Player.REPEAT_MODE_ONE
            RepeatMode.ONE -> Player.REPEAT_MODE_OFF
        }
    }

    fun setRepeat(mode: RepeatMode) {
        controller?.repeatMode = mode.value
    }

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
        applySpeed(speed)
    }

    private fun applySpeed(speed: Float) {
        controller?.setPlaybackSpeed(speed.coerceIn(0.25f, 4.0f))
    }

    fun setSkipSilence(enabled: Boolean) {
        _skipSilence.value = enabled
        // Skip-silence is ExoPlayer-specific and not exposed through MediaController;
        // PlaybackService observes this flag via SkipSilenceBridge.
        SkipSilenceBridge.setEnabled(enabled)
    }

    private fun applySkipSilence(enabled: Boolean) {
        SkipSilenceBridge.setEnabled(enabled)
    }

    fun snapshotForPersistence(): PersistenceSnapshot? {
        val c = controller ?: return null
        if (c.mediaItemCount == 0) return null
        val ids = (0 until c.mediaItemCount).mapNotNull { c.getMediaItemAt(it).mediaId.toLongOrNull() }
        return PersistenceSnapshot(
            songIds = ids,
            currentIndex = c.currentMediaItemIndex,
            positionMs = c.currentPosition.coerceAtLeast(0L),
            shuffle = c.shuffleModeEnabled,
            repeat = c.repeatMode,
        )
    }

    data class PersistenceSnapshot(
        val songIds: List<Long>,
        val currentIndex: Int,
        val positionMs: Long,
        val shuffle: Boolean,
        val repeat: Int,
    )

    fun restore(songs: List<SongEntity>, index: Int, positionMs: Long, shuffle: Boolean, repeat: Int) {
        val c = controller ?: return
        if (songs.isEmpty()) return
        val items = songs.map { it.toMediaItem() }
        c.setMediaItems(items, index.coerceIn(0, items.lastIndex), positionMs)
        c.shuffleModeEnabled = shuffle
        c.repeatMode = repeat
        c.prepare()
    }

    private fun SongEntity.toMediaItem(): MediaItem =
        MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri("file://$path")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .setIsPlayable(true)
                    .setExtras(
                        Bundle().apply {
                            putLong("albumId", albumId)
                            artworkPath?.let { putString("artworkPath", it) }
                        }
                    )
                    .apply {
                        artworkPath?.let { p ->
                            runCatching { setArtworkUri(android.net.Uri.fromFile(java.io.File(p))) }
                        }
                    }
                    .build()
            )
            .build()

    private var lastRecordedSongId = -1L

    private class Listener : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            syncState()
            if (isPlaying) recordCurrent()
            onStateChanged?.invoke()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            syncState()
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                onTrackFinished?.invoke()
            }
            recordCurrent()
            onStateChanged?.invoke()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            syncState()
            onStateChanged?.invoke()
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            syncState()
            onStateChanged?.invoke()
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            syncState()
            onStateChanged?.invoke()
        }

        override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
            refreshQueue()
        }
    }

    private fun recordCurrent() {
        val id = controller?.currentMediaItem?.mediaId?.toLongOrNull() ?: return
        if (id == lastRecordedSongId) return
        lastRecordedSongId = id
        onPlayRecorded?.invoke(id)
    }
}

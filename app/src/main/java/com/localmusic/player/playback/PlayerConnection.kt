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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

object PlayerConnection {

    private var controller: MediaController? = null

    val effects = AudioEffectsController()

    private var lastAudioSessionId = 0

    private val _nowPlaying = MutableStateFlow(NowPlaying())
    val nowPlaying: StateFlow<NowPlaying> = _nowPlaying.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    fun controller(): MediaController? = controller

    fun connect(context: Context) {
        if (controller != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            controller = runCatching { future.get() }.getOrNull()
            controller?.addListener(Listener())
            syncState()
        }, MoreExecutors.directExecutor())
    }

    fun release() {
        controller?.release()
        controller = null
        _isPlaying.value = false
    }

    private fun syncState() {
        val c = controller ?: return
        _isPlaying.value = c.isPlaying
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

    fun playSongs(songs: List<SongEntity>, startIndex: Int) {
        val c = controller ?: return
        val items = songs.map { it.toMediaItem() }
        c.setMediaItems(items, startIndex.coerceIn(0, items.lastIndex.coerceAtLeast(0)), 0L)
        c.prepare()
        c.play()
    }

    fun playNext(songs: List<SongEntity>) {
        val c = controller ?: return
        c.addMediaItems(0, songs.map { it.toMediaItem() })
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun next() { controller?.seekToNextMediaItem() }

    fun previous() { controller?.seekToPreviousMediaItem() }

    fun seekTo(positionMs: Long) { controller?.seekTo(positionMs) }

    fun stop() {
        controller?.stop()
        controller?.clearMediaItems()
        _nowPlaying.value = NowPlaying()
        _isPlaying.value = false
    }

    fun setVolume(volume: Float) {
        controller?.volume = volume.coerceIn(0f, 1f)
    }

    fun currentVolume(): Float = controller?.volume ?: 1f

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
                    .build()
            )
            .build()

    private class Listener : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) = syncState()

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = syncState()

        override fun onPlaybackStateChanged(playbackState: Int) = syncState()
    }
}

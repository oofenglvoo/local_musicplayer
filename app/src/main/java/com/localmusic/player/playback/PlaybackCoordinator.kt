package com.localmusic.player.playback

import android.content.Context
import com.localmusic.player.data.MusicRepository
import com.localmusic.player.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Bridges PlayerConnection with persisted state: play tracking, queue resume,
 * and playback settings (speed / skip silence / crossfade / replaygain).
 */
class PlaybackCoordinator(
    private val context: Context,
    private val repository: MusicRepository,
    private val settingsStore: SettingsStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var saveJob: kotlinx.coroutines.Job? = null

    fun start() {
        PlayerConnection.setPlayRecordedCallback { songId ->
            scope.launch { runCatching { repository.recordPlay(songId) } }
        }
        PlayerConnection.setTrackFinishedCallback {
            SleepTimerController.onTrackFinished()
        }
        PlayerConnection.setStateChangedCallback {
            scheduleSave()
        }
        scope.launch { applySettings() }
        scope.launch { restoreQueue() }
    }

    suspend fun applySettings() {
        val speed = settingsStore.playbackSpeed.first()
        val skipSilence = settingsStore.skipSilence.first()
        val crossfade = settingsStore.crossfadeMs.first()
        val rgMode = ReplayGainMode.from(settingsStore.replayGainMode.first())
        // Player/controller access must happen on the main thread.
        withContext(Dispatchers.Main) {
            PlayerConnection.setSpeed(speed)
            PlayerConnection.setSkipSilence(skipSilence)
            PlayerConnection.crossfade.setCrossfadeMs(crossfade)
            ReplayGainManager.setMode(rgMode)
        }
    }

    fun suspendApplySettings() {
        scope.launch { applySettings() }
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = scope.launch {
            kotlinx.coroutines.delay(1500)
            persistQueue()
        }
    }

    suspend fun persistQueue() {
        // Snapshot requires MediaController access on the main thread.
        val snap = withContext(Dispatchers.Main) {
            PlayerConnection.snapshotForPersistence()
        } ?: return
        runCatching {
            repository.saveQueueState(
                songIds = snap.songIds,
                currentSongId = snap.songIds.getOrNull(snap.currentIndex) ?: return,
                positionMs = snap.positionMs,
                shuffleMode = if (snap.shuffle) 1 else 0,
                repeatMode = snap.repeat,
            )
        }
    }

    private suspend fun restoreQueue() {
        val (ids, state) = repository.loadQueueState()
        if (state == null || ids.isEmpty()) return
        val songs = ids.mapNotNull { repository.getSong(it) }
        if (songs.isEmpty()) return
        val index = ids.indexOf(state.songId).coerceAtLeast(0)
        // Controller access must happen on the main thread.
        withContext(Dispatchers.Main) {
            PlayerConnection.restore(
                songs = songs,
                index = index,
                positionMs = state.positionMs,
                shuffle = state.shuffleMode == 1,
                repeat = state.repeatMode,
            )
        }
    }

    fun saveNow() {
        scope.launch { persistQueue() }
    }
}

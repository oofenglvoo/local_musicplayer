package com.localmusic.player.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localmusic.player.data.SettingsStore
import com.localmusic.player.playback.AudioEffectsState
import com.localmusic.player.playback.PlayerConnection
import com.localmusic.player.playback.ReplayGainManager
import com.localmusic.player.playback.ReplayGainMode
import com.localmusic.player.playback.SleepTimerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AudioSettingsViewModel @Inject constructor(
    private val settingsStore: SettingsStore,
) : ViewModel() {

    val effectsState: StateFlow<AudioEffectsState> = PlayerConnection.effects.state

    val playbackSpeed: StateFlow<Float> = settingsStore.playbackSpeed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)

    val skipSilence: StateFlow<Boolean> = settingsStore.skipSilence
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val crossfadeMs: StateFlow<Int> = settingsStore.crossfadeMs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val replayGainMode: StateFlow<ReplayGainMode> = settingsStore.replayGainMode
        .map { ReplayGainMode.from(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReplayGainMode.OFF)

    val sleepTimerFade: StateFlow<Boolean> = settingsStore.sleepTimerFade
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setEffectsEnabled(enabled: Boolean) =
        PlayerConnection.effects.setEnabled(enabled)

    fun setPreset(preset: Short) = PlayerConnection.effects.setPreset(preset)

    fun setBandLevel(bandIndex: Short, levelMb: Short) =
        PlayerConnection.effects.setBandLevel(bandIndex, levelMb)

    fun setBass(strength: Short) = PlayerConnection.effects.setBassStrength(strength)

    fun setVirtualizer(strength: Short) =
        PlayerConnection.effects.setVirtualizerStrength(strength)

    fun setLoudness(gainMb: Int) = PlayerConnection.effects.setLoudnessGainMb(gainMb)

    fun setSpeed(speed: Float) {
        viewModelScope.launch { settingsStore.setPlaybackSpeed(speed) }
        PlayerConnection.setSpeed(speed)
    }

    fun setSkipSilence(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setSkipSilence(enabled) }
        PlayerConnection.setSkipSilence(enabled)
    }

    fun setCrossfade(ms: Int) {
        viewModelScope.launch { settingsStore.setCrossfadeMs(ms) }
        PlayerConnection.crossfade.setCrossfadeMs(ms)
    }

    fun setReplayGain(mode: ReplayGainMode) {
        viewModelScope.launch { settingsStore.setReplayGainMode(mode.key) }
        ReplayGainManager.setMode(mode)
    }

    fun setSleepFade(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setSleepTimerFade(enabled) }
    }

    fun startSleepTimer(minutes: Int) = SleepTimerController.start(minutes)

    fun startSleepTimerByTracks(count: Int) = SleepTimerController.startByTracks(count)

    fun cancelSleepTimer() = SleepTimerController.cancel()
}

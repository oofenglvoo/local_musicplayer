package com.localmusic.player.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localmusic.player.playback.AudioEffectsState
import com.localmusic.player.playback.PlayerConnection
import com.localmusic.player.playback.SleepTimerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class AudioSettingsViewModel @Inject constructor() : ViewModel() {

    val effectsState: StateFlow<AudioEffectsState> = PlayerConnection.effects.state

    fun setEffectsEnabled(enabled: Boolean) =
        PlayerConnection.effects.setEnabled(enabled)

    fun setPreset(preset: Short) = PlayerConnection.effects.setPreset(preset)

    fun setBandLevel(bandIndex: Short, levelMb: Short) =
        PlayerConnection.effects.setBandLevel(bandIndex, levelMb)

    fun setBass(strength: Short) = PlayerConnection.effects.setBassStrength(strength)

    fun setVirtualizer(strength: Short) =
        PlayerConnection.effects.setVirtualizerStrength(strength)

    fun setLoudness(gainMb: Int) = PlayerConnection.effects.setLoudnessGainMb(gainMb)

    fun startSleepTimer(minutes: Int) = SleepTimerController.start(minutes)

    fun cancelSleepTimer() = SleepTimerController.cancel()
}

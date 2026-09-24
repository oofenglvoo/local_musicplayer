package com.localmusic.player.playback

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EqualizerBand(
    val index: Short,
    val centerFreqHz: Int,
    val levelDb: Short,
    val minDb: Short,
    val maxDb: Short,
)

data class AudioEffectsState(
    val available: Boolean = false,
    val enabled: Boolean = false,
    val bands: List<EqualizerBand> = emptyList(),
    val presetNames: List<String> = emptyList(),
    val currentPreset: Short = -1,
    val bassStrength: Short = 0,
    val bassAvailable: Boolean = false,
    val virtualizerStrength: Short = 0,
    val virtualizerAvailable: Boolean = false,
    val loudnessGainMb: Int = 0,
    val loudnessAvailable: Boolean = false,
)

class AudioEffectsController {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudness: LoudnessEnhancer? = null

    private var globalEnabled = false

    private val _state = MutableStateFlow(AudioEffectsState())
    val state: StateFlow<AudioEffectsState> = _state.asStateFlow()

    fun attach(audioSessionId: Int) {
        if (audioSessionId == 0 || equalizer != null) return
        runCatching {
            val eq = Equalizer(0, audioSessionId)
            equalizer = eq

            val bass = runCatching { BassBoost(0, audioSessionId) }.getOrNull()
            bassBoost = bass

            val virt = runCatching { Virtualizer(0, audioSessionId) }.getOrNull()
            virtualizer = virt

            val loud = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                runCatching { LoudnessEnhancer(audioSessionId) }.getOrNull()
            } else null
            loudness = loud

            val bandCount = eq.numberOfBands
            val bands = (0 until bandCount).map { i ->
                val band = i.toShort()
                EqualizerBand(
                    index = band,
                    centerFreqHz = eq.getCenterFreq(band) / 1000,
                    levelDb = eq.getBandLevel(band),
                    minDb = eq.bandLevelRange[0],
                    maxDb = eq.bandLevelRange[1],
                )
            }
            val presets = (0 until eq.numberOfPresets).map { eq.getPresetName(it.toShort()) }

            _state.value = AudioEffectsState(
                available = true,
                enabled = globalEnabled,
                bands = bands,
                presetNames = presets,
                currentPreset = eq.currentPreset,
                bassAvailable = bass != null,
                bassStrength = 0,
                virtualizerAvailable = virt != null,
                virtualizerStrength = 0,
                loudnessAvailable = loud != null,
                loudnessGainMb = loud?.enabled?.let { if (it) 300 else 0 } ?: 0,
            )

            applyEnabled(globalEnabled)
        }.onFailure {
            _state.value = AudioEffectsState(available = false)
        }
    }

    fun setEnabled(enabled: Boolean) {
        globalEnabled = enabled
        applyEnabled(enabled)
        _state.value = _state.value.copy(enabled = enabled)
    }

    private fun applyEnabled(enabled: Boolean) {
        runCatching { equalizer?.enabled = enabled }
        runCatching { bassBoost?.enabled = enabled }
        runCatching { virtualizer?.enabled = enabled }
        runCatching { loudness?.enabled = enabled }
    }

    fun setBandLevel(bandIndex: Short, levelMb: Short) {
        runCatching { equalizer?.setBandLevel(bandIndex, levelMb) }
        _state.value = _state.value.copy(
            bands = _state.value.bands.map {
                if (it.index == bandIndex) it.copy(levelDb = levelMb) else it
            },
        )
    }

    fun setPreset(preset: Short) {
        runCatching { equalizer?.usePreset(preset) }
        val eq = equalizer ?: return
        val bands = (0 until eq.numberOfBands).map { i ->
            val band = i.toShort()
            EqualizerBand(
                index = band,
                centerFreqHz = eq.getCenterFreq(band) / 1000,
                levelDb = eq.getBandLevel(band),
                minDb = eq.bandLevelRange[0],
                maxDb = eq.bandLevelRange[1],
            )
        }
        _state.value = _state.value.copy(currentPreset = preset, bands = bands)
    }

    fun setBassStrength(strength: Short) {
        runCatching { bassBoost?.setStrength((strength.toInt() * 10).coerceIn(0, 1000).toShort()) }
        _state.value = _state.value.copy(bassStrength = strength)
    }

    fun setVirtualizerStrength(strength: Short) {
        runCatching {
            virtualizer?.setStrength((strength.toInt() * 10).coerceIn(0, 1000).toShort())
        }
        _state.value = _state.value.copy(virtualizerStrength = strength)
    }

    fun setLoudnessGainMb(gainMb: Int) {
        runCatching { loudness?.setTargetGain(gainMb) }
        _state.value = _state.value.copy(loudnessGainMb = gainMb)
    }

    fun release() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        runCatching { loudness?.release() }
        equalizer = null
        bassBoost = null
        virtualizer = null
        loudness = null
    }
}

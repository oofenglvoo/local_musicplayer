package com.localmusic.player.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.localmusic.player.R
import com.localmusic.player.playback.ReplayGainMode
import com.localmusic.player.playback.SleepTimerController

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun AudioSettingsScreen(
    onBack: () -> Unit,
    viewModel: AudioSettingsViewModel = hiltViewModel(),
) {
    val effects by viewModel.effectsState.collectAsStateWithLifecycle()
    val sleepActive by SleepTimerController.active.collectAsStateWithLifecycle()
    val sleepRemaining by SleepTimerController.remainingMs.collectAsStateWithLifecycle()
    val remainingTracks by SleepTimerController.remainingTracks.collectAsStateWithLifecycle()
    val speed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val skipSilence by viewModel.skipSilence.collectAsStateWithLifecycle()
    val crossfadeMs by viewModel.crossfadeMs.collectAsStateWithLifecycle()
    val replayGain by viewModel.replayGainMode.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.audio_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.audio_speed), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.audio_current_speed, "%.2f".format(speed)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Slider(
                value = speed,
                onValueChange = { viewModel.setSpeed(it) },
                valueRange = 0.5f..2.0f,
                steps = 5,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(stringResource(R.string.audio_skip_silence), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.audio_skip_silence_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = skipSilence,
                    onCheckedChange = { viewModel.setSkipSilence(it) },
                )
            }

            Text(stringResource(R.string.audio_crossfade), style = MaterialTheme.typography.titleMedium)
            Text(
                if (crossfadeMs == 0) stringResource(R.string.common_close) else stringResource(R.string.audio_crossfade_ms, crossfadeMs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Slider(
                value = crossfadeMs.toFloat(),
                onValueChange = { viewModel.setCrossfade(it.toInt()) },
                valueRange = 0f..10000f,
                steps = 19,
            )

            Text(stringResource(R.string.audio_replay_gain), style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReplayGainMode.entries.forEach { mode ->
                    AssistChip(
                        onClick = { viewModel.setReplayGain(mode) },
                        label = { Text(stringResource(mode.labelRes)) },
                        leadingIcon = if (replayGain == mode) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                    )
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(stringResource(R.string.audio_enable), style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (effects.available) stringResource(R.string.audio_available)
                        else stringResource(R.string.audio_unavailable),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = effects.enabled,
                    onCheckedChange = { viewModel.setEffectsEnabled(it) },
                    enabled = effects.available,
                )
            }

            HorizontalDivider()

            if (effects.available) {
                Text(stringResource(R.string.audio_eq_preset), style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    effects.presetNames.forEachIndexed { index, name ->
                        AssistChip(
                            onClick = { viewModel.setPreset(index.toShort()) },
                            label = { Text(name) },
                        )
                    }
                }

                Text(stringResource(R.string.audio_band_adjust), style = MaterialTheme.typography.titleMedium)
                effects.bands.forEach { band ->
                    var value by remember(band.index, band.levelDb) {
                        mutableFloatStateOf(band.levelDb.toFloat())
                    }
                    Column {
                        Text(
                            stringResource(R.string.audio_freq_hz, band.centerFreqHz),
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Slider(
                            value = value,
                            onValueChange = {
                                value = it
                                viewModel.setBandLevel(band.index, it.toInt().toShort())
                            },
                            valueRange = band.minDb.toFloat()..band.maxDb.toFloat(),
                        )
                    }
                }

                HorizontalDivider()

                if (effects.bassAvailable) {
                    Text(stringResource(R.string.audio_bass_boost), style = MaterialTheme.typography.titleMedium)
                    Slider(
                        value = effects.bassStrength.toFloat(),
                        onValueChange = { viewModel.setBass(it.toInt().toShort()) },
                        valueRange = 0f..100f,
                    )
                }

                if (effects.virtualizerAvailable) {
                    Text(stringResource(R.string.audio_virtualizer), style = MaterialTheme.typography.titleMedium)
                    Slider(
                        value = effects.virtualizerStrength.toFloat(),
                        onValueChange = { viewModel.setVirtualizer(it.toInt().toShort()) },
                        valueRange = 0f..100f,
                    )
                }

                if (effects.loudnessAvailable) {
                    Text(stringResource(R.string.audio_loudness), style = MaterialTheme.typography.titleMedium)
                    Slider(
                        value = effects.loudnessGainMb.toFloat(),
                        onValueChange = { viewModel.setLoudness(it.toInt()) },
                        valueRange = 0f..1500f,
                    )
                }

                HorizontalDivider()
            }

            Text(stringResource(R.string.sleep_timer_title), style = MaterialTheme.typography.titleMedium)
            if (sleepActive) {
                Text(
                    if (remainingTracks > 0) stringResource(R.string.audio_sleep_tracks_remaining, remainingTracks)
                    else stringResource(R.string.audio_sleep_time_remaining, formatMs(sleepRemaining)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Button(onClick = { viewModel.cancelSleepTimer() }) {
                    Text(stringResource(R.string.sleep_timer_cancel))
                }
            } else {
                Text(stringResource(R.string.audio_sleep_by_minutes), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 30, 45, 60).forEach { minutes ->
                        AssistChip(
                            onClick = { viewModel.startSleepTimer(minutes) },
                            label = { Text(stringResource(R.string.audio_sleep_minutes, minutes)) },
                        )
                    }
                }
                Text(stringResource(R.string.audio_sleep_by_tracks), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 3, 5, 10).forEach { count ->
                        AssistChip(
                            onClick = { viewModel.startSleepTimerByTracks(count) },
                            label = { Text(stringResource(R.string.audio_sleep_count, count)) },
                        )
                    }
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = ms / 1000
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}

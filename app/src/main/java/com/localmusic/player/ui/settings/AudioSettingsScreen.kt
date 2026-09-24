package com.localmusic.player.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("音效与定时") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("启用音效", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (effects.available) "均衡器 / 低音增强 / 环绕音" else "当前音频会话不可用",
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
                Text("均衡器预设", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    effects.presetNames.forEachIndexed { index, name ->
                        AssistChip(
                            onClick = { viewModel.setPreset(index.toShort()) },
                            label = { Text(name) },
                        )
                    }
                }

                Text("频段调节", style = MaterialTheme.typography.titleMedium)
                effects.bands.forEach { band ->
                    var value by remember(band.index, band.levelDb) {
                        mutableFloatStateOf(band.levelDb.toFloat())
                    }
                    Column {
                        Text(
                            "${band.centerFreqHz} Hz",
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
                    Text("低音增强", style = MaterialTheme.typography.titleMedium)
                    Slider(
                        value = effects.bassStrength.toFloat(),
                        onValueChange = { viewModel.setBass(it.toInt().toShort()) },
                        valueRange = 0f..100f,
                    )
                }

                if (effects.virtualizerAvailable) {
                    Text("环绕音效", style = MaterialTheme.typography.titleMedium)
                    Slider(
                        value = effects.virtualizerStrength.toFloat(),
                        onValueChange = { viewModel.setVirtualizer(it.toInt().toShort()) },
                        valueRange = 0f..100f,
                    )
                }

                if (effects.loudnessAvailable) {
                    Text("音量增强", style = MaterialTheme.typography.titleMedium)
                    Slider(
                        value = effects.loudnessGainMb.toFloat(),
                        onValueChange = { viewModel.setLoudness(it.toInt()) },
                        valueRange = 0f..1500f,
                    )
                }

                HorizontalDivider()
            }

            Text("睡眠定时", style = MaterialTheme.typography.titleMedium)
            if (sleepActive) {
                Text(
                    "剩余 ${formatMs(sleepRemaining)}，到点自动暂停",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Button(onClick = { viewModel.cancelSleepTimer() }) {
                    Text("取消定时")
                }
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 30, 45, 60).forEach { minutes ->
                        AssistChip(
                            onClick = { viewModel.startSleepTimer(minutes) },
                            label = { Text("$minutes 分钟") },
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

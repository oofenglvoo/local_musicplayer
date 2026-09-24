package com.localmusic.player.lyrics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsEditSheet(
    songId: Long,
    viewModel: LyricsEditViewModel,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(songId) { viewModel.load(songId) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("编辑歌词", style = MaterialTheme.typography.titleLarge)
            Text(
                if (state.lrcPath.isBlank()) "将新建同名 .lrc 文件"
                else state.lrcPath,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = state.content,
                onValueChange = { viewModel.updateContent(it) },
                label = { Text("LRC 歌词内容") },
                placeholder = { Text("[00:00.00]歌词第一行\n[00:05.00]歌词第二行") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 360.dp),
            )

            Text("歌词时间偏移：${state.offsetMs} ms", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { viewModel.adjustOffset(-500) }) { Text("-0.5s") }
                OutlinedButton(onClick = { viewModel.adjustOffset(-100) }) { Text("-0.1s") }
                OutlinedButton(onClick = { viewModel.adjustOffset(100) }) { Text("+0.1s") }
                OutlinedButton(onClick = { viewModel.adjustOffset(500) }) { Text("+0.5s") }
            }
            Text(
                "正值表示歌词延后显示",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            state.message?.let {
                LaunchedEffect(it) {
                    viewModel.consumeMessage()
                }
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) { Text("关闭") }
                Button(
                    onClick = { viewModel.save(onSaved) },
                    modifier = Modifier.padding(start = 8.dp),
                ) { Text("保存") }
            }
        }
    }
}

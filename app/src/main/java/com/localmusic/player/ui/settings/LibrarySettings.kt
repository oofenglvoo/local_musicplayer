package com.localmusic.player.ui.settings

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val MIN_DURATION_OPTIONS = listOf(0 to "不过滤", 10 to "10 秒", 30 to "30 秒", 60 to "60 秒")

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun LibrarySettingsScreen(
    onBack: () -> Unit,
    onAddExcludedFolder: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val minDuration by viewModel.minDurationSec.collectAsStateWithLifecycle()
    val excluded by viewModel.excludedFolders.collectAsStateWithLifecycle()
    val scanned by viewModel.scannedFolders.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("音乐库") },
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
            Text("忽略短音频", style = MaterialTheme.typography.titleMedium)
            Text(
                "扫描时跳过时长低于该值的音频文件",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MIN_DURATION_OPTIONS.forEach { (sec, label) ->
                    AssistChip(
                        onClick = { viewModel.setMinDurationSec(sec) },
                        label = { Text(label) },
                        leadingIcon = if (minDuration == sec) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.padding(end = 2.dp)) }
                        } else null,
                    )
                }
            }

            HorizontalDivider()

            Text("扫描目录", style = MaterialTheme.typography.titleMedium)
            if (scanned.isEmpty()) {
                Text(
                    "尚未手动添加扫描目录（默认扫描媒体库）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                scanned.forEach { path ->
                    ListItem(
                        headlineContent = { Text(path, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        trailingContent = {
                            IconButton(onClick = { viewModel.removeScannedFolder(path) }) {
                                Icon(Icons.Default.Delete, contentDescription = "移除")
                            }
                        },
                    )
                }
            }

            HorizontalDivider()

            Text("排除目录", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Text(
                    "扫描时会跳过这些目录",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                androidx.compose.material3.TextButton(onClick = onAddExcludedFolder) {
                    Text("添加排除目录")
                }
            }
            if (excluded.isEmpty()) {
                Text(
                    "没有排除任何目录",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                excluded.forEach { path ->
                    ListItem(
                        headlineContent = { Text(path, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        trailingContent = {
                            IconButton(onClick = { viewModel.removeExcludedFolder(path) }) {
                                Icon(Icons.Default.Delete, contentDescription = "移除")
                            }
                        },
                    )
                }
            }
        }
    }
}

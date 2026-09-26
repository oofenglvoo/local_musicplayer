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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.localmusic.player.R

private val MIN_DURATION_VALUES = listOf(0, 10, 30, 60)

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
                title = { Text(stringResource(R.string.settings_library)) },
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
            Text(stringResource(R.string.library_ignore_short), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.library_ignore_short_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MIN_DURATION_VALUES.forEach { sec ->
                    AssistChip(
                        onClick = { viewModel.setMinDurationSec(sec) },
                        label = {
                            Text(
                                if (sec == 0) stringResource(R.string.library_min_duration_none)
                                else stringResource(R.string.library_min_duration_sec, sec)
                            )
                        },
                        leadingIcon = if (minDuration == sec) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.padding(end = 2.dp)) }
                        } else null,
                    )
                }
            }

            HorizontalDivider()

            Text(stringResource(R.string.library_scan_folders), style = MaterialTheme.typography.titleMedium)
            if (scanned.isEmpty()) {
                Text(
                    stringResource(R.string.library_no_scan_folder),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                scanned.forEach { path ->
                    ListItem(
                        headlineContent = { Text(path, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        trailingContent = {
                            IconButton(onClick = { viewModel.removeScannedFolder(path) }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.common_remove))
                            }
                        },
                    )
                }
            }

            HorizontalDivider()

            Text(stringResource(R.string.library_excluded_folders), style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.library_excluded_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                androidx.compose.material3.TextButton(onClick = onAddExcludedFolder) {
                    Text(stringResource(R.string.library_add_excluded_folder))
                }
            }
            if (excluded.isEmpty()) {
                Text(
                    stringResource(R.string.library_no_excluded),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                excluded.forEach { path ->
                    ListItem(
                        headlineContent = { Text(path, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        trailingContent = {
                            IconButton(onClick = { viewModel.removeExcludedFolder(path) }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.common_remove))
                            }
                        },
                    )
                }
            }
        }
    }
}

package com.localmusic.player.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.localmusic.player.R
import com.localmusic.player.data.ThemeMode
import com.localmusic.player.data.BackgroundMode

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ThemeSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val dynamicColor by viewModel.dynamicColor.collectAsStateWithLifecycle()
    val seedColor by viewModel.seedColor.collectAsStateWithLifecycle()
    val gridAlbums by viewModel.gridAlbums.collectAsStateWithLifecycle()
    val backgroundMode by viewModel.backgroundMode.collectAsStateWithLifecycle()
    val backgroundImage by viewModel.backgroundImage.collectAsStateWithLifecycle()
    val backgroundBlur by viewModel.backgroundBlur.collectAsStateWithLifecycle()
    val backgroundDim by viewModel.backgroundDim.collectAsStateWithLifecycle()
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.setBackgroundImage(it.toString()) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_theme)) },
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
            Text(stringResource(R.string.settings_theme_mode), style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    AssistChip(
                        onClick = { viewModel.setThemeMode(mode) },
                        label = { Text(stringResource(modeLabelRes(mode))) },
                        leadingIcon = if (themeMode == mode) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                    )
                }
            }

            HorizontalDivider()

            Text(stringResource(R.string.settings_background), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.settings_background_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BackgroundMode.entries.forEach { mode ->
                    AssistChip(
                        onClick = { viewModel.setBackgroundMode(mode) },
                        label = { Text(stringResource(backgroundLabelRes(mode))) },
                        leadingIcon = if (backgroundMode == mode) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                    )
                }
            }
            if (backgroundMode == BackgroundMode.LOCAL_IMAGE) {
                AssistChip(
                    onClick = { imagePicker.launch(arrayOf("image/*")) },
                    label = { Text(stringResource(if (backgroundImage == null) R.string.settings_background_pick else R.string.settings_background_change)) },
                )
            }
            if (backgroundMode == BackgroundMode.GRADIENT) {
                Text(stringResource(R.string.settings_gradient), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SeedColorPicker(
                        colors = BACKGROUND_COLORS,
                        selected = null,
                        onSelect = { viewModel.setBackgroundColor(it); viewModel.setBackgroundSecondaryColor(it xor 0x00303030) },
                    )
                }
            }
            Text(stringResource(R.string.settings_blur, backgroundBlur), style = MaterialTheme.typography.labelLarge)
            androidx.compose.material3.Slider(
                value = backgroundBlur.toFloat(), onValueChange = { viewModel.setBackgroundBlur(it.toInt()) }, valueRange = 0f..80f,
            )
            Text(stringResource(R.string.settings_dim, backgroundDim), style = MaterialTheme.typography.labelLarge)
            androidx.compose.material3.Slider(
                value = backgroundDim.toFloat(), onValueChange = { viewModel.setBackgroundDim(it.toInt()) }, valueRange = 0f..95f,
            )

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(stringResource(R.string.settings_dynamic_color_full), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.settings_dynamic_color_full_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = dynamicColor && seedColor == null,
                    onCheckedChange = {
                        viewModel.setDynamicColor(it)
                        if (it) viewModel.setSeedColor(null)
                    },
                )
            }

            Text(stringResource(R.string.settings_accent), style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SeedColorPicker(
                    colors = PRESET_COLORS,
                    selected = seedColor,
                    onSelect = {
                        viewModel.setDynamicColor(false)
                        viewModel.setSeedColor(it)
                    },
                )
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(stringResource(R.string.settings_album_grid), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.settings_album_grid_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = gridAlbums,
                    onCheckedChange = { viewModel.setGridAlbums(it) },
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun SeedColorPicker(
    colors: List<Int>,
    selected: Int?,
    onSelect: (Int) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        colors.forEach { color ->
            val isSelected = selected == color
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(color))
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape,
                    )
                    .clickable { onSelect(color) },
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                    )
                }
            }
        }
    }
}

private val PRESET_COLORS = listOf(
    0xFF6750A4.toInt(),
    0xFF2196F3.toInt(),
    0xFF00BCD4.toInt(),
    0xFF4CAF50.toInt(),
    0xFFFF9800.toInt(),
    0xFFF44336.toInt(),
    0xFFE91E63.toInt(),
    0xFF9C27B0.toInt(),
)

@androidx.annotation.StringRes
private fun modeLabelRes(mode: ThemeMode): Int = when (mode) {
    ThemeMode.SYSTEM -> R.string.settings_theme_system
    ThemeMode.LIGHT -> R.string.settings_theme_light
    ThemeMode.DARK -> R.string.settings_theme_dark
    ThemeMode.BLACK -> R.string.settings_black
}

@androidx.annotation.StringRes
private fun backgroundLabelRes(mode: BackgroundMode): Int = when (mode) {
    BackgroundMode.ARTWORK -> R.string.background_artwork
    BackgroundMode.LOCAL_IMAGE -> R.string.background_local_image
    BackgroundMode.GRADIENT -> R.string.background_gradient
    BackgroundMode.SOLID -> R.string.background_solid
}

private val BACKGROUND_COLORS = listOf(
    0xFF15121C.toInt(), 0xFF102027.toInt(), 0xFF17231B.toInt(),
    0xFF24131A.toInt(), 0xFF1A1530.toInt(), 0xFF202020.toInt(),
)

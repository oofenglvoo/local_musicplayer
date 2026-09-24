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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
                title = { Text("外观") },
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
            Text("主题模式", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    AssistChip(
                        onClick = { viewModel.setThemeMode(mode) },
                        label = { Text(modeLabel(mode)) },
                        leadingIcon = if (themeMode == mode) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                    )
                }
            }

            HorizontalDivider()

            Text("播放背景", style = MaterialTheme.typography.titleMedium)
            Text(
                "播放页和全局界面的沉浸式背景",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BackgroundMode.entries.forEach { mode ->
                    AssistChip(
                        onClick = { viewModel.setBackgroundMode(mode) },
                        label = { Text(backgroundLabel(mode)) },
                        leadingIcon = if (backgroundMode == mode) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                    )
                }
            }
            if (backgroundMode == BackgroundMode.LOCAL_IMAGE) {
                AssistChip(
                    onClick = { imagePicker.launch(arrayOf("image/*")) },
                    label = { Text(if (backgroundImage == null) "选择本地图片" else "更换背景图片") },
                )
            }
            if (backgroundMode == BackgroundMode.GRADIENT) {
                Text("渐变色", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SeedColorPicker(
                        colors = BACKGROUND_COLORS,
                        selected = null,
                        onSelect = { viewModel.setBackgroundColor(it); viewModel.setBackgroundSecondaryColor(it xor 0x00303030) },
                    )
                }
            }
            Text("背景模糊：$backgroundBlur", style = MaterialTheme.typography.labelLarge)
            androidx.compose.material3.Slider(
                value = backgroundBlur.toFloat(), onValueChange = { viewModel.setBackgroundBlur(it.toInt()) }, valueRange = 0f..80f,
            )
            Text("背景暗度：$backgroundDim%", style = MaterialTheme.typography.labelLarge)
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
                    Text("动态取色", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "使用壁纸颜色生成主题（Android 12+）",
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

            Text("强调色", style = MaterialTheme.typography.titleMedium)
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
                    Text("专辑网格布局", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "专辑列表以网格方式展示",
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

private fun modeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> "跟随系统"
    ThemeMode.LIGHT -> "浅色"
    ThemeMode.DARK -> "深色"
    ThemeMode.BLACK -> "纯黑"
}

private fun backgroundLabel(mode: BackgroundMode): String = when (mode) {
    BackgroundMode.ARTWORK -> "封面动态"
    BackgroundMode.LOCAL_IMAGE -> "本地图片"
    BackgroundMode.GRADIENT -> "渐变"
    BackgroundMode.SOLID -> "纯色"
}

private val BACKGROUND_COLORS = listOf(
    0xFF15121C.toInt(), 0xFF102027.toInt(), 0xFF17231B.toInt(),
    0xFF24131A.toInt(), 0xFF1A1530.toInt(), 0xFF202020.toInt(),
)

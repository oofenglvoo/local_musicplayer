package com.localmusic.player.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
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
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenAudio: () -> Unit,
    onOpenTheme: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenAbout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
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
                .verticalScroll(rememberScrollState()),
        ) {
            ListItem(
                modifier = Modifier.clickable(onClick = onOpenTheme),
                headlineContent = { Text("外观") },
                supportingContent = { Text("主题模式、动态取色、网格布局") },
                leadingContent = { Icon(Icons.Default.Palette, contentDescription = null) },
            )
            ListItem(
                modifier = Modifier.clickable(onClick = onOpenAudio),
                headlineContent = { Text("音效与播放") },
                supportingContent = { Text("均衡器、播放速度、交叉淡入淡出、ReplayGain") },
                leadingContent = { Icon(Icons.Default.Equalizer, contentDescription = null) },
            )
            ListItem(
                modifier = Modifier.clickable(onClick = onOpenLibrary),
                headlineContent = { Text("音乐库") },
                supportingContent = { Text("扫描目录、排除目录、忽略短音频") },
                leadingContent = { Icon(Icons.Default.Folder, contentDescription = null) },
            )
            HorizontalDivider()
            ListItem(
                modifier = Modifier.clickable(onClick = onOpenAbout),
                headlineContent = { Text("关于") },
                supportingContent = { Text("版本信息与开源许可") },
                leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
            )
        }
    }
}

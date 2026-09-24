package com.localmusic.player.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.0"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("关于") },
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
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("本地音乐播放器", style = MaterialTheme.typography.headlineSmall)
            Text("版本 $versionName", style = MaterialTheme.typography.bodyMedium)
            HorizontalDivider()
            Text(
                "一款纯本地的 Android 音乐播放器，不上传、不联网，所有数据保存在设备本地。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider()
            Text("开源组件", style = MaterialTheme.typography.titleMedium)
            LicenseItem("Jetpack Compose", "Apache License 2.0")
            LicenseItem("AndroidX Media3", "Apache License 2.0")
            LicenseItem("Room", "Apache License 2.0")
            LicenseItem("Hilt", "Apache License 2.0")
            LicenseItem("Coil", "Apache License 2.0")
            LicenseItem("Glance", "Apache License 2.0")
            LicenseItem("Kotlin Coroutines", "Apache License 2.0")
        }
    }
}

@Composable
private fun LicenseItem(name: String, license: String) {
    Column {
        Text(name, style = MaterialTheme.typography.bodyMedium)
        Text(
            license,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

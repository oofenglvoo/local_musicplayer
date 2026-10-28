package com.localmusic.player.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.localmusic.player.R
import com.localmusic.player.data.db.SongEntity

/**
 * 列表内歌曲搜索的共享状态。各个歌单 / 收藏 / 专辑 / 艺术家 / 文件夹 / 自动列表页
 * 复用同一套交互：顶栏放大镜展开输入框、实时内存过滤、返回键先关搜索。
 */
@Stable
class SongSearchState(
    active: Boolean = false,
    query: String = "",
) {
    var active by mutableStateOf(active)
        private set

    var query by mutableStateOf(query)

    internal val focusRequester = FocusRequester()

    fun open() {
        active = true
    }

    fun close() {
        active = false
        query = ""
    }

    fun clearQuery() {
        query = ""
    }

    companion object {
        val Saver = listSaver<SongSearchState, Any>(
            save = { listOf(it.active, it.query) },
            restore = { SongSearchState(it[0] as Boolean, it[1] as String) },
        )
    }
}

@Composable
fun rememberSongSearchState(): SongSearchState {
    val state = rememberSaveable(saver = SongSearchState.Saver) { SongSearchState() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(state.active) {
        if (state.active) {
            state.focusRequester.requestFocus()
            keyboard?.show()
        }
    }

    // 搜索态下返回键先退出搜索，而不是直接离开页面
    BackHandler(enabled = state.active) { state.close() }

    return state
}

/** 按标题 / 艺术家 / 专辑 / 文件名做大小写不敏感的内存过滤。 */
fun filterSongs(songs: List<SongEntity>, query: String): List<SongEntity> {
    val q = query.trim()
    if (q.isEmpty()) return songs
    return songs.filter {
        it.title.contains(q, true) || it.artist.contains(q, true) ||
            it.album.contains(q, true) || it.displayName.contains(q, true)
    }
}

/** 放在 TopAppBar 的 title 槽位里使用的搜索输入框。 */
@Composable
fun SongSearchField(
    state: SongSearchState,
    modifier: Modifier = Modifier,
    placeholder: String = stringResource(R.string.home_search_hint),
) {
    val keyboard = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        value = state.query,
        onValueChange = { state.query = it },
        placeholder = { Text(placeholder) },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (state.query.isNotEmpty()) {
                IconButton(onClick = { state.clearQuery() }) {
                    Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.common_clear_input))
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(state.focusRequester),
    )
}

/** 顶栏右上角的搜索入口按钮。 */
@Composable
fun SongSearchAction(state: SongSearchState, enabled: Boolean = true) {
    if (!enabled || state.active) return
    IconButton(onClick = { state.open() }) {
        Icon(Icons.Default.Search, contentDescription = stringResource(R.string.common_search))
    }
}

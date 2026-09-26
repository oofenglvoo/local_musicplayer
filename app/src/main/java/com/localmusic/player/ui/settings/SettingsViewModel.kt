package com.localmusic.player.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localmusic.player.data.SettingsStore
import com.localmusic.player.data.ThemeMode
import com.localmusic.player.data.BackgroundMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val settingsStore: SettingsStore,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settingsStore.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    val dynamicColor: StateFlow<Boolean> = settingsStore.dynamicColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val seedColor: StateFlow<Int?> = settingsStore.seedColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val gridAlbums: StateFlow<Boolean> = settingsStore.gridAlbums
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val backgroundMode = settingsStore.backgroundMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BackgroundMode.ARTWORK)
    val backgroundImage = settingsStore.backgroundImage.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val backgroundColor = settingsStore.backgroundColor.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val backgroundSecondaryColor = settingsStore.backgroundSecondaryColor.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val backgroundBlur = settingsStore.backgroundBlur.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 42)
    val backgroundDim = settingsStore.backgroundDim.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 72)

    val minDurationSec: StateFlow<Int> = settingsStore.minDurationSec
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val excludedFolders: StateFlow<Set<String>> = settingsStore.excludedFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val scannedFolders: StateFlow<Set<String>> = settingsStore.scannedFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsStore.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setDynamicColor(enabled) }
    }

    fun setSeedColor(color: Int?) {
        viewModelScope.launch { settingsStore.setSeedColor(color) }
    }

    fun setGridAlbums(grid: Boolean) {
        viewModelScope.launch { settingsStore.setGridAlbums(grid) }
    }

    fun setBackgroundMode(mode: BackgroundMode) { viewModelScope.launch { settingsStore.setBackgroundMode(mode) } }
    fun setBackgroundImage(path: String?) { viewModelScope.launch { settingsStore.setBackgroundImage(path) } }
    fun setBackgroundColor(color: Int) { viewModelScope.launch { settingsStore.setBackgroundColor(color) } }
    fun setBackgroundSecondaryColor(color: Int) { viewModelScope.launch { settingsStore.setBackgroundSecondaryColor(color) } }
    fun setBackgroundBlur(value: Int) { viewModelScope.launch { settingsStore.setBackgroundBlur(value) } }
    fun setBackgroundDim(value: Int) { viewModelScope.launch { settingsStore.setBackgroundDim(value) } }

    fun setMinDurationSec(sec: Int) {
        viewModelScope.launch { settingsStore.setMinDurationSec(sec) }
    }

    fun removeExcludedFolder(path: String) {
        viewModelScope.launch { settingsStore.removeExcludedFolder(path) }
    }

    fun removeScannedFolder(path: String) {
        viewModelScope.launch { settingsStore.removeScannedFolder(path) }
    }
}

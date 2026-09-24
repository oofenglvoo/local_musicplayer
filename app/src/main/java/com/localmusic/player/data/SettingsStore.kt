package com.localmusic.player.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode { SYSTEM, LIGHT, DARK, BLACK }

enum class BackgroundMode { ARTWORK, LOCAL_IMAGE, GRADIENT, SOLID }

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val scannedFoldersKey = stringSetPreferencesKey("scanned_folders")
    private val excludedFoldersKey = stringSetPreferencesKey("excluded_folders")
    private val minDurationKey = intPreferencesKey("min_duration_sec")
    private val sortFieldKey = stringPreferencesKey("sort_field")
    private val sortAscendingKey = booleanPreferencesKey("sort_ascending")
    private val libraryTabKey = stringPreferencesKey("library_tab")
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val dynamicColorKey = booleanPreferencesKey("dynamic_color")
    private val seedColorKey = intPreferencesKey("seed_color")
    private val skipSilenceKey = booleanPreferencesKey("skip_silence")
    private val playbackSpeedKey = floatPreferencesKey("playback_speed")
    private val crossfadeKey = intPreferencesKey("crossfade_ms")
    private val replayGainKey = stringPreferencesKey("replaygain_mode")
    private val sleepTimerFadeKey = booleanPreferencesKey("sleep_timer_fade")
    private val searchHistoryKey = stringSetPreferencesKey("search_history")
    private val gridAlbumsKey = booleanPreferencesKey("grid_albums")
    private val artworkOverridesKey = stringSetPreferencesKey("artwork_overrides")
    private val backgroundModeKey = stringPreferencesKey("background_mode")
    private val backgroundImageKey = stringPreferencesKey("background_image")
    private val backgroundColorKey = intPreferencesKey("background_color")
    private val backgroundSecondaryColorKey = intPreferencesKey("background_secondary_color")
    private val backgroundBlurKey = intPreferencesKey("background_blur")
    private val backgroundDimKey = intPreferencesKey("background_dim")

    val scannedFolders: Flow<Set<String>> =
        context.dataStore.data.map { it[scannedFoldersKey] ?: emptySet() }

    val excludedFolders: Flow<Set<String>> =
        context.dataStore.data.map { it[excludedFoldersKey] ?: emptySet() }

    val minDurationSec: Flow<Int> =
        context.dataStore.data.map { it[minDurationKey] ?: 0 }

    val sortField: Flow<SongSort> =
        context.dataStore.data.map { SongSort.valueOf(it[sortFieldKey] ?: SongSort.TITLE.name) }

    val sortAscending: Flow<Boolean> =
        context.dataStore.data.map { it[sortAscendingKey] ?: true }

    val libraryTab: Flow<String> =
        context.dataStore.data.map { it[libraryTabKey] ?: "SONGS" }

    val themeMode: Flow<ThemeMode> =
        context.dataStore.data.map { ThemeMode.valueOf(it[themeModeKey] ?: ThemeMode.SYSTEM.name) }

    val dynamicColor: Flow<Boolean> =
        context.dataStore.data.map { it[dynamicColorKey] ?: true }

    val seedColor: Flow<Int?> =
        context.dataStore.data.map { it[seedColorKey] }

    val skipSilence: Flow<Boolean> =
        context.dataStore.data.map { it[skipSilenceKey] ?: false }

    val playbackSpeed: Flow<Float> =
        context.dataStore.data.map { it[playbackSpeedKey] ?: 1.0f }

    val crossfadeMs: Flow<Int> =
        context.dataStore.data.map { it[crossfadeKey] ?: 0 }

    val replayGainMode: Flow<String> =
        context.dataStore.data.map { it[replayGainKey] ?: "off" }

    val sleepTimerFade: Flow<Boolean> =
        context.dataStore.data.map { it[sleepTimerFadeKey] ?: true }

    val searchHistory: Flow<List<String>> =
        context.dataStore.data.map { (it[searchHistoryKey] ?: emptySet()).toList().take(20) }

    val gridAlbums: Flow<Boolean> =
        context.dataStore.data.map { it[gridAlbumsKey] ?: true }

    val backgroundMode: Flow<BackgroundMode> = context.dataStore.data.map {
        runCatching { BackgroundMode.valueOf(it[backgroundModeKey] ?: BackgroundMode.ARTWORK.name) }
            .getOrDefault(BackgroundMode.ARTWORK)
    }
    val backgroundImage: Flow<String?> = context.dataStore.data.map { it[backgroundImageKey] }
    val backgroundColor: Flow<Int> = context.dataStore.data.map { it[backgroundColorKey] ?: 0xFF15121C.toInt() }
    val backgroundSecondaryColor: Flow<Int> = context.dataStore.data.map { it[backgroundSecondaryColorKey] ?: 0xFF332044.toInt() }
    val backgroundBlur: Flow<Int> = context.dataStore.data.map { it[backgroundBlurKey] ?: 42 }
    val backgroundDim: Flow<Int> = context.dataStore.data.map { it[backgroundDimKey] ?: 72 }

    suspend fun addScannedFolder(path: String) = context.dataStore.edit {
        it[scannedFoldersKey] = (it[scannedFoldersKey] ?: emptySet()) + path
    }

    suspend fun removeScannedFolder(path: String) = context.dataStore.edit {
        it[scannedFoldersKey] = (it[scannedFoldersKey] ?: emptySet()) - path
    }

    suspend fun clearScannedFolders() = context.dataStore.edit { it.remove(scannedFoldersKey) }

    suspend fun addExcludedFolder(path: String) = context.dataStore.edit {
        it[excludedFoldersKey] = (it[excludedFoldersKey] ?: emptySet()) + path
    }

    suspend fun removeExcludedFolder(path: String) = context.dataStore.edit {
        it[excludedFoldersKey] = (it[excludedFoldersKey] ?: emptySet()) - path
    }

    suspend fun setMinDurationSec(sec: Int) = context.dataStore.edit { it[minDurationKey] = sec }

    suspend fun setSortField(field: SongSort) = context.dataStore.edit { it[sortFieldKey] = field.name }

    suspend fun setSortAscending(asc: Boolean) = context.dataStore.edit { it[sortAscendingKey] = asc }

    suspend fun setLibraryTab(tab: String) = context.dataStore.edit { it[libraryTabKey] = tab }

    suspend fun setThemeMode(mode: ThemeMode) = context.dataStore.edit { it[themeModeKey] = mode.name }

    suspend fun setDynamicColor(enabled: Boolean) = context.dataStore.edit { it[dynamicColorKey] = enabled }

    suspend fun setSeedColor(color: Int?) = context.dataStore.edit {
        if (color == null) it.remove(seedColorKey) else it[seedColorKey] = color
    }

    suspend fun setSkipSilence(enabled: Boolean) = context.dataStore.edit { it[skipSilenceKey] = enabled }

    suspend fun setPlaybackSpeed(speed: Float) = context.dataStore.edit { it[playbackSpeedKey] = speed }

    suspend fun setCrossfadeMs(ms: Int) = context.dataStore.edit { it[crossfadeKey] = ms }

    suspend fun setReplayGainMode(mode: String) = context.dataStore.edit { it[replayGainKey] = mode }

    suspend fun setSleepTimerFade(enabled: Boolean) = context.dataStore.edit { it[sleepTimerFadeKey] = enabled }

    suspend fun setGridAlbums(grid: Boolean) = context.dataStore.edit { it[gridAlbumsKey] = grid }

    suspend fun setBackgroundMode(mode: BackgroundMode) = context.dataStore.edit { it[backgroundModeKey] = mode.name }
    suspend fun setBackgroundImage(path: String?) = context.dataStore.edit {
        if (path.isNullOrBlank()) it.remove(backgroundImageKey) else it[backgroundImageKey] = path
    }
    suspend fun setBackgroundColor(color: Int) = context.dataStore.edit { it[backgroundColorKey] = color }
    suspend fun setBackgroundSecondaryColor(color: Int) = context.dataStore.edit { it[backgroundSecondaryColorKey] = color }
    suspend fun setBackgroundBlur(value: Int) = context.dataStore.edit { it[backgroundBlurKey] = value.coerceIn(0, 80) }
    suspend fun setBackgroundDim(value: Int) = context.dataStore.edit { it[backgroundDimKey] = value.coerceIn(0, 95) }

    suspend fun addSearchTerm(term: String) = context.dataStore.edit { prefs ->
        val current = prefs[searchHistoryKey] ?: emptySet()
        prefs[searchHistoryKey] = (listOf(term) + current.toList()).distinct().take(20).toSet()
    }

    suspend fun clearSearchHistory() = context.dataStore.edit { it.remove(searchHistoryKey) }

    fun artworkOverride(songId: Long): Flow<String?> =
        context.dataStore.data.map { prefs ->
            (prefs[artworkOverridesKey] ?: emptySet())
                .firstOrNull { it.startsWith("$songId=") }
                ?.substringAfter("=")
                ?.takeIf { it.isNotBlank() }
        }

    val artworkOverrides: Flow<Map<Long, String>> =
        context.dataStore.data.map { prefs ->
            (prefs[artworkOverridesKey] ?: emptySet()).mapNotNull { entry ->
                val id = entry.substringBefore("=").toLongOrNull()
                val path = entry.substringAfter("=", "")
                if (id != null && path.isNotBlank()) id to path else null
            }.toMap()
        }

    suspend fun setArtworkOverride(songId: Long, path: String) = context.dataStore.edit { prefs ->
        val current = (prefs[artworkOverridesKey] ?: emptySet())
            .filterNot { it.startsWith("$songId=") }
            .toSet()
        prefs[artworkOverridesKey] = current + "$songId=$path"
    }

    suspend fun clearArtworkOverride(songId: Long) = context.dataStore.edit { prefs ->
        prefs[artworkOverridesKey] = (prefs[artworkOverridesKey] ?: emptySet())
            .filterNot { it.startsWith("$songId=") }
            .toSet()
    }
}

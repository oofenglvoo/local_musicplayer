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

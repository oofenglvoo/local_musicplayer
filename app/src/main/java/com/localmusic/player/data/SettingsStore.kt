package com.localmusic.player.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val scannedFoldersKey = stringSetPreferencesKey("scanned_folders")

    val scannedFolders: Flow<Set<String>> =
        context.dataStore.data.map { it[scannedFoldersKey] ?: emptySet() }

    suspend fun addScannedFolder(path: String) {
        context.dataStore.edit { prefs ->
            prefs[scannedFoldersKey] = (prefs[scannedFoldersKey] ?: emptySet()) + path
        }
    }

    suspend fun removeScannedFolder(path: String) {
        context.dataStore.edit { prefs ->
            prefs[scannedFoldersKey] = (prefs[scannedFoldersKey] ?: emptySet()) - path
        }
    }

    suspend fun clearScannedFolders() {
        context.dataStore.edit { it.remove(scannedFoldersKey) }
    }
}

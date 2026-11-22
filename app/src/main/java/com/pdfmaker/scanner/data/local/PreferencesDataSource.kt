package com.pdfmaker.scanner.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pdfmaker.scanner.data.model.CompressionLevel
import com.pdfmaker.scanner.data.model.SortOption
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pdf_maker_prefs")

@Singleton
class PreferencesDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val defaultCompressionKey = stringPreferencesKey("default_compression")
    private val librarySortKey = stringPreferencesKey("library_sort")

    val defaultCompression: Flow<CompressionLevel> = context.dataStore.data.map { prefs ->
        val raw = prefs[defaultCompressionKey]
        CompressionLevel.entries.firstOrNull { it.name == raw } ?: CompressionLevel.MEDIUM
    }

    val librarySort: Flow<SortOption> = context.dataStore.data.map { prefs ->
        val raw = prefs[librarySortKey]
        SortOption.entries.firstOrNull { it.name == raw } ?: SortOption.DATE_DESC
    }

    suspend fun setDefaultCompression(level: CompressionLevel) {
        context.dataStore.edit { it[defaultCompressionKey] = level.name }
    }

    suspend fun setLibrarySort(sortOption: SortOption) {
        context.dataStore.edit { it[librarySortKey] = sortOption.name }
    }
}

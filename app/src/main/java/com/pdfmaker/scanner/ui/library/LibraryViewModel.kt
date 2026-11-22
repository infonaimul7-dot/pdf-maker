package com.pdfmaker.scanner.ui.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdfmaker.scanner.data.model.PdfDocumentItem
import com.pdfmaker.scanner.data.model.SortOption
import com.pdfmaker.scanner.data.repository.PdfRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LibraryFilter(val label: String) {
    ALL("All"),
    PROTECTED("Password Protected")
}

data class LibraryUiState(
    val documents: List<PdfDocumentItem> = emptyList(),
    val searchQuery: String = "",
    val sortOption: SortOption = SortOption.DATE_DESC,
    val filter: LibraryFilter = LibraryFilter.ALL,
    val errorMessage: String? = null
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: PdfRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val filterState = MutableStateFlow(LibraryFilter.ALL)
    private val errorState = MutableStateFlow<String?>(null)

    val uiState: StateFlow<LibraryUiState> = combine(
        repository.libraryFlow,
        repository.sortOptionFlow,
        searchQuery,
        filterState,
        errorState
    ) { docs, sort, query, filter, err ->
        val filtered = docs.filter { doc ->
            val matchesQuery = query.isBlank() || doc.fileName.contains(query, ignoreCase = true)
            val matchesFilter = when (filter) {
                LibraryFilter.ALL -> true
                LibraryFilter.PROTECTED -> doc.isEncrypted
            }
            matchesQuery && matchesFilter
        }
        val sorted = when (sort) {
            SortOption.DATE_DESC -> filtered.sortedByDescending { it.lastModifiedMillis }
            SortOption.DATE_ASC -> filtered.sortedBy { it.lastModifiedMillis }
            SortOption.NAME_ASC -> filtered.sortedBy { it.fileName.lowercase() }
            SortOption.NAME_DESC -> filtered.sortedByDescending { it.fileName.lowercase() }
            SortOption.SIZE_DESC -> filtered.sortedByDescending { it.sizeBytes }
            SortOption.SIZE_ASC -> filtered.sortedBy { it.sizeBytes }
        }
        LibraryUiState(
            documents = sorted,
            searchQuery = query,
            sortOption = sort,
            filter = filter,
            errorMessage = err
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    init {
        viewModelScope.launch { repository.refreshLibrary() }
    }

    fun setSearchQuery(q: String) {
        searchQuery.value = q
    }

    fun setSortOption(option: SortOption) {
        viewModelScope.launch { repository.updateSortOption(option) }
    }

    fun setFilter(filter: LibraryFilter) {
        filterState.value = filter
    }

    fun renamePdf(filePath: String, newName: String) {
        viewModelScope.launch {
            repository.renamePdf(filePath, newName).onFailure {
                errorState.value = it.message ?: "Rename failed."
            }
        }
    }

    fun deletePdf(filePath: String) {
        viewModelScope.launch {
            if (!repository.deletePdf(filePath)) {
                errorState.value = "Unable to delete file."
            }
        }
    }

    fun importExistingPdf(uri: Uri, name: String) {
        viewModelScope.launch {
            repository.importExternalPdf(uri, name).onFailure {
                errorState.value = it.message ?: "Failed to import PDF."
            }
        }
    }

    fun clearError() {
        errorState.value = null
    }
}

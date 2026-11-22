package com.pdfmaker.scanner.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdfmaker.scanner.data.model.PdfDocumentItem
import com.pdfmaker.scanner.data.repository.PdfRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: PdfRepository
) : ViewModel() {

    val recentFiles: StateFlow<List<PdfDocumentItem>> = repository.libraryFlow
        .map { list -> list.take(5) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            repository.refreshLibrary()
        }
    }
}

package com.pdfmaker.scanner.ui.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdfmaker.scanner.data.model.CompressionLevel
import com.pdfmaker.scanner.data.model.PdfCompressionResult
import com.pdfmaker.scanner.data.model.PdfDocumentItem
import com.pdfmaker.scanner.data.repository.PdfRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ToolsUiState(
    val availablePdfs: List<PdfDocumentItem> = emptyList(),
    val isWorking: Boolean = false,
    val compressionResult: PdfCompressionResult? = null,
    val mergedDocument: PdfDocumentItem? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class PdfToolsViewModel @Inject constructor(
    private val repository: PdfRepository
) : ViewModel() {

    private val isWorking = MutableStateFlow(false)
    private val compressionResult = MutableStateFlow<PdfCompressionResult?>(null)
    private val mergedDocument = MutableStateFlow<PdfDocumentItem?>(null)
    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ToolsUiState> = combine(
        repository.libraryFlow,
        isWorking,
        compressionResult,
        mergedDocument,
        errorMessage
    ) { docs, working, comp, merged, err ->
        ToolsUiState(
            availablePdfs = docs,
            isWorking = working,
            compressionResult = comp,
            mergedDocument = merged,
            errorMessage = err
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ToolsUiState())

    init {
        viewModelScope.launch { repository.refreshLibrary() }
    }

    fun compressPdf(filePath: String, level: CompressionLevel) {
        viewModelScope.launch {
            isWorking.value = true
            errorMessage.value = null
            compressionResult.value = null
            repository.compressExistingPdf(filePath, level).fold(
                onSuccess = { compressionResult.value = it },
                onFailure = { errorMessage.value = it.message ?: "Compression failed." }
            )
            isWorking.value = false
        }
    }

    fun mergePdfs(paths: List<String>, outputTitle: String) {
        viewModelScope.launch {
            isWorking.value = true
            errorMessage.value = null
            mergedDocument.value = null
            repository.mergePdfs(paths, outputTitle).fold(
                onSuccess = { mergedDocument.value = it },
                onFailure = { errorMessage.value = it.message ?: "Merge failed." }
            )
            isWorking.value = false
        }
    }
}

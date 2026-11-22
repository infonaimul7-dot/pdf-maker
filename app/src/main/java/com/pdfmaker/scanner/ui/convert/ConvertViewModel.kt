package com.pdfmaker.scanner.ui.convert

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdfmaker.scanner.data.model.CompressionLevel
import com.pdfmaker.scanner.data.model.EditablePage
import com.pdfmaker.scanner.data.model.PdfCreationResult
import com.pdfmaker.scanner.domain.usecase.CreatePdfUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConvertUiState(
    val documentTitle: String = "New Document.pdf",
    val compressionLevel: CompressionLevel = CompressionLevel.MEDIUM,
    val enablePassword: Boolean = false,
    val password: String = "",
    val confirmPassword: String = "",
    val isConverting: Boolean = false,
    val progress: Float = 0f,
    val errorMessage: String? = null
)

@HiltViewModel
class ConvertViewModel @Inject constructor(
    private val createPdfUseCase: CreatePdfUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConvertUiState())
    val uiState: StateFlow<ConvertUiState> = _uiState.asStateFlow()

    fun updateTitle(title: String) {
        _uiState.update { it.copy(documentTitle = title, errorMessage = null) }
    }

    fun updateCompression(level: CompressionLevel) {
        _uiState.update { it.copy(compressionLevel = level) }
    }

    fun togglePassword(enabled: Boolean) {
        _uiState.update { it.copy(enablePassword = enabled, errorMessage = null) }
    }

    fun updatePassword(pwd: String) {
        _uiState.update { it.copy(password = pwd, errorMessage = null) }
    }

    fun updateConfirmPassword(pwd: String) {
        _uiState.update { it.copy(confirmPassword = pwd, errorMessage = null) }
    }

    fun convert(
        pages: List<EditablePage>,
        onSuccess: (PdfCreationResult) -> Unit
    ) {
        val current = _uiState.value
        if (current.isConverting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isConverting = true, progress = 0.05f, errorMessage = null) }
            val result = createPdfUseCase(
                pages = pages,
                rawTitle = current.documentTitle,
                compressionLevel = current.compressionLevel,
                password = current.password,
                confirmPassword = current.confirmPassword,
                requirePassword = current.enablePassword,
                onProgress = { pct ->
                    _uiState.update { it.copy(progress = pct) }
                }
            )
            result.fold(
                onSuccess = { created ->
                    _uiState.update { it.copy(isConverting = false, progress = 1f) }
                    onSuccess(created)
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isConverting = false,
                            progress = 0f,
                            errorMessage = err.message ?: "Failed to create PDF."
                        )
                    }
                }
            )
        }
    }
}

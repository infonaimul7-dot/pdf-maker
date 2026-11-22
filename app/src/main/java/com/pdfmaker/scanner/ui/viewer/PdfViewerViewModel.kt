package com.pdfmaker.scanner.ui.viewer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdfmaker.scanner.data.repository.PdfRepository
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class ViewerUiState(
    val fileName: String = "",
    val filePath: String = "",
    val pageCount: Int = 0,
    val isLoading: Boolean = true,
    val requiresPassword: Boolean = false,
    val passwordError: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class PdfViewerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: PdfRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ViewerUiState())
    val uiState: StateFlow<ViewerUiState> = _uiState.asStateFlow()

    private val renderMutex = Mutex()
    private var pfd: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null
    private var decryptedTempFile: File? = null

    fun openDocument(filePath: String, password: String? = null) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    filePath = filePath,
                    fileName = File(filePath).name,
                    isLoading = true,
                    errorMessage = null,
                    passwordError = null
                )
            }
            withContext(Dispatchers.IO) {
                closeInternal()
                val file = File(filePath)
                if (!file.exists()) {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "PDF file could not be found on device.")
                    }
                    return@withContext
                }

                try {
                    val targetFile = if (password != null) {
                        val temp = File(context.cacheDir, "decrypted_view_${System.currentTimeMillis()}.pdf")
                        PDDocument.load(file, password).use { doc ->
                            doc.isAllSecurityToBeRemoved = true
                            doc.save(temp)
                        }
                        decryptedTempFile = temp
                        temp
                    } else {
                        file
                    }

                    val descriptor = ParcelFileDescriptor.open(targetFile, ParcelFileDescriptor.MODE_READ_ONLY)
                    val pdfRenderer = PdfRenderer(descriptor)
                    pfd = descriptor
                    renderer = pdfRenderer

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            requiresPassword = false,
                            pageCount = pdfRenderer.pageCount
                        )
                    }
                } catch (_: SecurityException) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            requiresPassword = true,
                            passwordError = if (password != null) "Incorrect password." else null
                        )
                    }
                } catch (_: InvalidPasswordException) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            requiresPassword = true,
                            passwordError = "Incorrect password. Please try again."
                        )
                    }
                } catch (t: Throwable) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = t.message ?: "Invalid or corrupted PDF document."
                        )
                    }
                }
            }
        }
    }

    /**
     * Lazily renders a single page on demand so large multi-page PDFs never load all pages into RAM at once.
     */
    suspend fun renderPage(pageIndex: Int, targetWidthPx: Int): Bitmap? = withContext(Dispatchers.IO) {
        renderMutex.withLock {
            val activeRenderer = renderer ?: return@withLock null
            if (pageIndex !in 0 until activeRenderer.pageCount) return@withLock null
            activeRenderer.openPage(pageIndex).use { page ->
                val aspect = page.height.toFloat() / page.width.toFloat().coerceAtLeast(1f)
                val w = targetWidthPx.coerceIn(400, 1800)
                val h = (w * aspect).toInt().coerceAtLeast(1)
                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                bmp.eraseColor(Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bmp
            }
        }
    }

    fun renameDocument(newName: String) {
        val path = _uiState.value.filePath
        viewModelScope.launch {
            repository.renamePdf(path, newName).onSuccess { updated ->
                _uiState.update { it.copy(filePath = updated.filePath, fileName = updated.fileName) }
            }
        }
    }

    fun deleteDocument(onDeleted: () -> Unit) {
        val path = _uiState.value.filePath
        viewModelScope.launch {
            if (repository.deletePdf(path)) {
                onDeleted()
            }
        }
    }

    private fun closeInternal() {
        runCatching { renderer?.close() }
        runCatching { pfd?.close() }
        decryptedTempFile?.delete()
        renderer = null
        pfd = null
        decryptedTempFile = null
    }

    override fun onCleared() {
        closeInternal()
        super.onCleared()
    }
}

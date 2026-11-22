package com.pdfmaker.scanner.data.repository

import android.net.Uri
import com.pdfmaker.scanner.data.model.CompressionLevel
import com.pdfmaker.scanner.data.model.EditablePage
import com.pdfmaker.scanner.data.model.PdfCompressionResult
import com.pdfmaker.scanner.data.model.PdfCreationResult
import com.pdfmaker.scanner.data.model.PdfDocumentItem
import com.pdfmaker.scanner.data.model.SortOption
import kotlinx.coroutines.flow.Flow
import java.io.File

interface PdfRepository {
    val libraryFlow: Flow<List<PdfDocumentItem>>
    val sortOptionFlow: Flow<SortOption>

    suspend fun refreshLibrary()
    suspend fun updateSortOption(option: SortOption)
    suspend fun createPdfFromPages(
        pages: List<EditablePage>,
        desiredTitle: String,
        compressionLevel: CompressionLevel,
        password: String?,
        onProgress: (Float) -> Unit
    ): Result<PdfCreationResult>

    suspend fun compressExistingPdf(
        sourcePath: String,
        level: CompressionLevel
    ): Result<PdfCompressionResult>

    suspend fun importExternalPdf(uri: Uri, desiredName: String): Result<PdfDocumentItem>
    suspend fun exportPdfToUri(filePath: String, targetUri: Uri): Result<Unit>
    suspend fun renamePdf(filePath: String, newRawName: String): Result<PdfDocumentItem>
    suspend fun deletePdf(filePath: String): Boolean
    suspend fun mergePdfs(sourcePaths: List<String>, outputTitle: String): Result<PdfDocumentItem>
    suspend fun getFileByPath(filePath: String): File?
}

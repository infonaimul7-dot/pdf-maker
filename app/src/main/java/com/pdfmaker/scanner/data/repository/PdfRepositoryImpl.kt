package com.pdfmaker.scanner.data.repository

import android.net.Uri
import com.pdfmaker.scanner.data.local.PdfFileStorageDataSource
import com.pdfmaker.scanner.data.local.PreferencesDataSource
import com.pdfmaker.scanner.data.model.CompressionLevel
import com.pdfmaker.scanner.data.model.EditablePage
import com.pdfmaker.scanner.data.model.PdfCompressionResult
import com.pdfmaker.scanner.data.model.PdfCreationResult
import com.pdfmaker.scanner.data.model.PdfDocumentItem
import com.pdfmaker.scanner.data.model.SortOption
import com.pdfmaker.scanner.util.FilenameValidator
import com.pdfmaker.scanner.util.PdfCompressor
import com.pdfmaker.scanner.util.PdfGenerator
import com.pdfmaker.scanner.util.PdfPageManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfRepositoryImpl @Inject constructor(
    private val storage: PdfFileStorageDataSource,
    private val prefs: PreferencesDataSource,
    private val pdfGenerator: PdfGenerator,
    private val pdfCompressor: PdfCompressor,
    private val pdfPageManager: PdfPageManager
) : PdfRepository {

    private val _libraryFlow = MutableStateFlow<List<PdfDocumentItem>>(emptyList())
    override val libraryFlow: Flow<List<PdfDocumentItem>> = _libraryFlow.asStateFlow()
    override val sortOptionFlow: Flow<SortOption> = prefs.librarySort

    override suspend fun refreshLibrary() {
        _libraryFlow.value = storage.listPdfDocuments()
    }

    override suspend fun updateSortOption(option: SortOption) {
        prefs.setLibrarySort(option)
    }

    override suspend fun createPdfFromPages(
        pages: List<EditablePage>,
        desiredTitle: String,
        compressionLevel: CompressionLevel,
        password: String?,
        onProgress: (Float) -> Unit
    ): Result<PdfCreationResult> {
        val outputFile = FilenameValidator.resolveCollisionSafeFile(
            directory = storage.getPdfDirectory(),
            desiredPdfName = desiredTitle
        )
        val genResult = pdfGenerator.generatePdfFromPages(
            pages = pages,
            outputFile = outputFile,
            compressionLevel = compressionLevel,
            password = password,
            onProgress = onProgress
        )
        return genResult.fold(
            onSuccess = { savedFile ->
                val item = storage.inspectPdfFile(savedFile)
                    ?: PdfDocumentItem(
                        id = savedFile.absolutePath,
                        fileName = savedFile.name,
                        filePath = savedFile.absolutePath,
                        sizeBytes = savedFile.length(),
                        lastModifiedMillis = savedFile.lastModified(),
                        pageCount = pages.size,
                        isEncrypted = !password.isNullOrEmpty()
                    )
                refreshLibrary()
                Result.success(PdfCreationResult(item, compressionLevel))
            },
            onFailure = { Result.failure(it) }
        )
    }

    override suspend fun compressExistingPdf(
        sourcePath: String,
        level: CompressionLevel
    ): Result<PdfCompressionResult> {
        val sourceFile = File(sourcePath)
        val baseName = sourceFile.nameWithoutExtension
        val targetFile = FilenameValidator.resolveCollisionSafeFile(
            directory = storage.getPdfDirectory(),
            desiredPdfName = "${baseName}_${level.label.lowercase()}_compressed.pdf"
        )
        val outcome = pdfCompressor.compressPdf(sourceFile, targetFile, level)
        return outcome.fold(
            onSuccess = { res ->
                val item = storage.inspectPdfFile(res.outputFile)
                    ?: return@fold Result.failure(IllegalStateException("Compressed PDF could not be verified."))
                refreshLibrary()
                Result.success(
                    PdfCompressionResult(
                        document = item,
                        originalSizeBytes = res.originalBytes,
                        compressedSizeBytes = res.compressedBytes,
                        reductionPercent = res.reductionPercent,
                        notice = res.notice
                    )
                )
            },
            onFailure = { Result.failure(it) }
        )
    }

    override suspend fun importExternalPdf(uri: Uri, desiredName: String): Result<PdfDocumentItem> {
        val res = storage.importExternalPdf(uri, desiredName)
        if (res.isSuccess) refreshLibrary()
        return res
    }

    override suspend fun exportPdfToUri(filePath: String, targetUri: Uri): Result<Unit> {
        return storage.exportPdfToUri(File(filePath), targetUri)
    }

    override suspend fun renamePdf(filePath: String, newRawName: String): Result<PdfDocumentItem> {
        val res = storage.renamePdf(filePath, newRawName)
        if (res.isSuccess) refreshLibrary()
        return res
    }

    override suspend fun deletePdf(filePath: String): Boolean {
        val deleted = storage.deletePdf(filePath)
        if (deleted) refreshLibrary()
        return deleted
    }

    override suspend fun mergePdfs(sourcePaths: List<String>, outputTitle: String): Result<PdfDocumentItem> {
        val outFile = FilenameValidator.resolveCollisionSafeFile(storage.getPdfDirectory(), outputTitle)
        val res = pdfPageManager.mergePdfs(sourcePaths.map { File(it) }, outFile)
        return res.fold(
            onSuccess = { file ->
                val item = storage.inspectPdfFile(file)
                    ?: return@fold Result.failure(IllegalStateException("Merged PDF could not be read."))
                refreshLibrary()
                Result.success(item)
            },
            onFailure = { Result.failure(it) }
        )
    }

    override suspend fun getFileByPath(filePath: String): File? {
        val file = File(filePath)
        return if (file.exists()) file else null
    }
}

package com.pdfmaker.scanner.data.local

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.pdfmaker.scanner.data.model.PdfDocumentItem
import com.pdfmaker.scanner.util.FilenameValidator
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfFileStorageDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getPdfDirectory(): File {
        return File(context.filesDir, "pdfs").apply {
            if (!exists()) mkdirs()
        }
    }

    suspend fun listPdfDocuments(): List<PdfDocumentItem> = withContext(Dispatchers.IO) {
        val dir = getPdfDirectory()
        val files = dir.listFiles { file ->
            file.isFile && file.extension.equals("pdf", ignoreCase = true)
        } ?: emptyArray()

        files.mapNotNull { file ->
            inspectPdfFile(file)
        }.sortedByDescending { it.lastModifiedMillis }
    }

    fun inspectPdfFile(file: File): PdfDocumentItem? {
        if (!file.exists()) return null
        var pageCount = 0
        var isEncrypted = false

        try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    pageCount = renderer.pageCount
                }
            }
        } catch (_: SecurityException) {
            isEncrypted = true
        } catch (_: Throwable) {
            // Fallback inspection via PDFBox
            try {
                PDDocument.load(file).use { doc ->
                    pageCount = doc.numberOfPages
                    isEncrypted = doc.isEncrypted
                }
            } catch (_: InvalidPasswordException) {
                isEncrypted = true
            } catch (_: Throwable) {
                pageCount = 0
            }
        }

        return PdfDocumentItem(
            id = file.absolutePath,
            fileName = file.name,
            filePath = file.absolutePath,
            sizeBytes = file.length(),
            lastModifiedMillis = file.lastModified(),
            pageCount = pageCount,
            isEncrypted = isEncrypted
        )
    }

    suspend fun importExternalPdf(uri: Uri, desiredName: String): Result<PdfDocumentItem> =
        withContext(Dispatchers.IO) {
            try {
                val targetFile = FilenameValidator.resolveCollisionSafeFile(getPdfDirectory(), desiredName)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: return@withContext Result.failure(IllegalStateException("Unable to read selected PDF."))

                val item = inspectPdfFile(targetFile)
                    ?: return@withContext Result.failure(IllegalArgumentException("Selected file is not a valid PDF."))
                Result.success(item)
            } catch (t: Throwable) {
                Result.failure(t)
            }
        }

    suspend fun exportPdfToUri(sourceFile: File, targetUri: Uri): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                if (!sourceFile.exists()) {
                    return@withContext Result.failure(IllegalStateException("Source file not found."))
                }
                context.contentResolver.openOutputStream(targetUri)?.use { out ->
                    sourceFile.inputStream().use { input -> input.copyTo(out) }
                } ?: return@withContext Result.failure(IllegalStateException("Unable to open destination URI."))
                Result.success(Unit)
            } catch (t: Throwable) {
                Result.failure(t)
            }
        }

    suspend fun renamePdf(filePath: String, newRawName: String): Result<PdfDocumentItem> =
        withContext(Dispatchers.IO) {
            val source = File(filePath)
            if (!source.exists()) {
                return@withContext Result.failure(IllegalStateException("File no longer exists."))
            }
            val target = FilenameValidator.resolveCollisionSafeFile(getPdfDirectory(), newRawName)
            if (!source.renameTo(target)) {
                return@withContext Result.failure(IllegalStateException("Unable to rename file."))
            }
            val inspected = inspectPdfFile(target)
                ?: return@withContext Result.failure(IllegalStateException("Renamed file could not be read."))
            Result.success(inspected)
        }

    suspend fun deletePdf(filePath: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(filePath)
        file.exists() && file.delete()
    }
}

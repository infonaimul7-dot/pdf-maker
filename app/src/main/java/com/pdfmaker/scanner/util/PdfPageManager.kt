package com.pdfmaker.scanner.util

import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfPageManager @Inject constructor() {

    suspend fun mergePdfs(
        sources: List<File>,
        outputFile: File
    ): Result<File> = withContext(Dispatchers.IO) {
        if (sources.size < 2) {
            return@withContext Result.failure(IllegalArgumentException("Select at least 2 PDF files to merge."))
        }
        val tempFile = File(outputFile.parentFile, "${outputFile.name}.tmp")
        try {
            val merger = PDFMergerUtility()
            sources.forEach { merger.addSource(it) }
            merger.destinationFileName = tempFile.absolutePath
            merger.mergeDocuments(null)

            if (!tempFile.renameTo(outputFile)) {
                tempFile.copyTo(outputFile, overwrite = true)
                tempFile.delete()
            }
            Result.success(outputFile)
        } catch (t: Throwable) {
            tempFile.delete()
            Result.failure(t)
        }
    }

    suspend fun extractPages(
        sourceFile: File,
        zeroBasedPageIndices: List<Int>,
        outputFile: File,
        password: String? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        if (zeroBasedPageIndices.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Select at least one page to extract."))
        }
        val tempFile = File(outputFile.parentFile, "${outputFile.name}.tmp")
        try {
            PDDocument.load(sourceFile, password ?: "").use { sourceDoc ->
                PDDocument().use { targetDoc ->
                    zeroBasedPageIndices.forEach { pageIndex ->
                        if (pageIndex in 0 until sourceDoc.numberOfPages) {
                            targetDoc.importPage(sourceDoc.getPage(pageIndex))
                        }
                    }
                    FileOutputStream(tempFile).use { targetDoc.save(it) }
                }
            }
            if (!tempFile.renameTo(outputFile)) {
                tempFile.copyTo(outputFile, overwrite = true)
                tempFile.delete()
            }
            Result.success(outputFile)
        } catch (t: Throwable) {
            tempFile.delete()
            Result.failure(t)
        }
    }

    suspend fun rotateAndReorderPages(
        sourceFile: File,
        pageOps: List<Pair<Int, Int>>, // (zeroBasedOriginalIndex, additionalRotationDegrees)
        outputFile: File,
        password: String? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        val tempFile = File(outputFile.parentFile, "${outputFile.name}.tmp")
        try {
            PDDocument.load(sourceFile, password ?: "").use { sourceDoc ->
                PDDocument().use { targetDoc ->
                    pageOps.forEach { (idx, extraRotation) ->
                        if (idx in 0 until sourceDoc.numberOfPages) {
                            val imported = targetDoc.importPage(sourceDoc.getPage(idx))
                            imported.rotation = (imported.rotation + extraRotation) % 360
                        }
                    }
                    FileOutputStream(tempFile).use { targetDoc.save(it) }
                }
            }
            if (!tempFile.renameTo(outputFile)) {
                tempFile.copyTo(outputFile, overwrite = true)
                tempFile.delete()
            }
            Result.success(outputFile)
        } catch (t: Throwable) {
            tempFile.delete()
            Result.failure(t)
        }
    }
}

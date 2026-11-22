package com.pdfmaker.scanner.util

import android.content.Context
import android.graphics.Bitmap
import com.pdfmaker.scanner.data.model.CompressionLevel
import com.pdfmaker.scanner.data.model.EditablePage
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfGenerator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageFilterProcessor: ImageFilterProcessor
) {
    /**
     * Builds a real multi-page PDF with meaningful JPEG downsampling/quality per CompressionLevel
     * and genuine PDF standard encryption when a password is supplied.
     */
    suspend fun generatePdfFromPages(
        pages: List<EditablePage>,
        outputFile: File,
        compressionLevel: CompressionLevel,
        password: String?,
        onProgress: (Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        if (pages.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("At least one page is required to create a PDF."))
        }

        val tempFile = File(outputFile.parentFile, "${outputFile.name}.tmp")
        val document = PDDocument()

        try {
            pages.forEachIndexed { index, page ->
                val sourceUri = page.originalUri
                val sampled = imageFilterProcessor.decodeSampledBitmap(
                    uri = sourceUri,
                    maxDimensionPx = compressionLevel.maxDimensionPx
                )
                val edited = imageFilterProcessor.applyEdits(
                    source = sampled,
                    cropQuad = page.cropQuad,
                    rotationDegrees = page.rotationDegrees,
                    filter = page.filter,
                    brightness = page.brightness,
                    contrast = page.contrast
                )
                if (edited != sampled) sampled.recycle()

                // Fit image into standard A4 proportions while matching portrait/landscape orientation
                val isLandscape = edited.width > edited.height
                val pageRect = if (isLandscape) {
                    PDRectangle(PDRectangle.A4.height, PDRectangle.A4.width)
                } else {
                    PDRectangle.A4
                }

                val pdPage = PDPage(pageRect)
                document.addPage(pdPage)

                val pdImage = JPEGFactory.createFromImage(
                    document,
                    edited,
                    compressionLevel.jpegQuality / 100f
                )
                edited.recycle()

                val pageWidth = pageRect.width
                val pageHeight = pageRect.height
                val scale = minOf(pageWidth / pdImage.width, pageHeight / pdImage.height)
                val drawW = pdImage.width * scale
                val drawH = pdImage.height * scale
                val offsetX = (pageWidth - drawW) / 2f
                val offsetY = (pageHeight - drawH) / 2f

                PDPageContentStream(document, pdPage).use { content ->
                    content.drawImage(pdImage, offsetX, offsetY, drawW, drawH)
                }

                onProgress((index + 1).toFloat() / pages.size.toFloat())
            }

            if (!password.isNullOrEmpty()) {
                val permissions = AccessPermission().apply {
                    setCanPrint(true)
                    setCanModify(false)
                    setCanExtractContent(false)
                }
                val policy = StandardProtectionPolicy(password, password, permissions).apply {
                    encryptionKeyLength = 128
                }
                document.protect(policy)
            }

            FileOutputStream(tempFile).use { fos ->
                document.save(fos)
            }

            if (!tempFile.renameTo(outputFile)) {
                tempFile.copyTo(outputFile, overwrite = true)
                tempFile.delete()
            }

            Result.success(outputFile)
        } catch (t: Throwable) {
            tempFile.delete()
            Result.failure(t)
        } finally {
            runCatching { document.close() }
        }
    }
}

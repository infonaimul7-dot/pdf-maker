package com.pdfmaker.scanner.util

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.pdfmaker.scanner.data.model.CompressionLevel
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

data class CompressedFileOutcome(
    val outputFile: File,
    val originalBytes: Long,
    val compressedBytes: Long,
    val reductionPercent: Int,
    val notice: String
)

@Singleton
class PdfCompressor @Inject constructor() {

    /**
     * Compresses an existing PDF by rasterizing pages at level-appropriate DPI and JPEG quality.
     * Explicitly notifies the caller that rasterization flattens searchable text layers.
     */
    suspend fun compressPdf(
        sourceFile: File,
        targetFile: File,
        level: CompressionLevel
    ): Result<CompressedFileOutcome> = withContext(Dispatchers.IO) {
        if (!sourceFile.exists()) {
            return@withContext Result.failure(IllegalStateException("Source PDF no longer exists."))
        }

        val originalBytes = sourceFile.length()
        val pfd = ParcelFileDescriptor.open(sourceFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val outDoc = PDDocument()
        val tempOut = File(targetFile.parentFile, "${targetFile.name}.tmp")

        val targetScale = when (level) {
            CompressionLevel.LOW -> 1.8f
            CompressionLevel.MEDIUM -> 1.3f
            CompressionLevel.HIGH -> 0.9f
        }

        try {
            val pageCount = renderer.pageCount
            for (i in 0 until pageCount) {
                renderer.openPage(i).use { page ->
                    val renderW = (page.width * targetScale).roundToInt().coerceIn(300, level.maxDimensionPx)
                    val renderH = (page.height * targetScale).roundToInt().coerceIn(300, level.maxDimensionPx)
                    val bmp = Bitmap.createBitmap(renderW, renderH, Bitmap.Config.ARGB_8888)
                    bmp.eraseColor(android.graphics.Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

                    val rect = PDRectangle(page.width.toFloat(), page.height.toFloat())
                    val pdPage = PDPage(rect)
                    outDoc.addPage(pdPage)

                    val pdImg = JPEGFactory.createFromImage(outDoc, bmp, level.jpegQuality / 100f)
                    bmp.recycle()

                    PDPageContentStream(outDoc, pdPage).use { stream ->
                        stream.drawImage(pdImg, 0f, 0f, rect.width, rect.height)
                    }
                }
            }

            FileOutputStream(tempOut).use { outDoc.save(it) }
            if (!tempOut.renameTo(targetFile)) {
                tempOut.copyTo(targetFile, overwrite = true)
                tempOut.delete()
            }

            val compressedBytes = targetFile.length()
            val reduction = if (originalBytes > 0) {
                (((originalBytes - compressedBytes).toDouble() / originalBytes.toDouble()) * 100.0).roundToInt()
            } else 0

            val notice = if (compressedBytes >= originalBytes) {
                "This PDF was already tightly compressed; image re-encoding did not reduce size further."
            } else {
                "Compressed by rasterizing pages at ${level.label} setting (searchable text layers are flattened)."
            }

            Result.success(
                CompressedFileOutcome(
                    outputFile = targetFile,
                    originalBytes = originalBytes,
                    compressedBytes = compressedBytes,
                    reductionPercent = reduction,
                    notice = notice
                )
            )
        } catch (t: Throwable) {
            tempOut.delete()
            Result.failure(t)
        } finally {
            runCatching { outDoc.close() }
            runCatching { renderer.close() }
            runCatching { pfd.close() }
        }
    }
}

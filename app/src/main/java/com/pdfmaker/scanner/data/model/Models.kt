package com.pdfmaker.scanner.data.model

import android.net.Uri
import java.util.UUID

enum class CompressionLevel(
    val label: String,
    val jpegQuality: Int,
    val maxDimensionPx: Int,
    val description: String
) {
    LOW(
        label = "Low",
        jpegQuality = 92,
        maxDimensionPx = 2400,
        description = "Highest visual clarity with minimal file shrinkage"
    ),
    MEDIUM(
        label = "Medium",
        jpegQuality = 72,
        maxDimensionPx = 1650,
        description = "Balanced file size and sharp document text"
    ),
    HIGH(
        label = "High",
        jpegQuality = 48,
        maxDimensionPx = 1150,
        description = "Compact file size optimized for quick sharing"
    )
}

enum class ScanFilter(val label: String) {
    ORIGINAL("Original"),
    MAGIC_COLOR("Magic color"),
    INVERT("Invert"),
    GRAYSCALE("Grey"),
    BLACK_AND_WHITE("B&W")
}

enum class SortOption(val label: String) {
    DATE_DESC("Last modified (newest)"),
    DATE_ASC("Last modified (oldest)"),
    NAME_ASC("Name (A–Z)"),
    NAME_DESC("Name (Z–A)"),
    SIZE_DESC("Size (largest)"),
    SIZE_ASC("Size (smallest)")
}

data class QuadPoints(
    val topLeft: NormalizedPoint = NormalizedPoint(0.12f, 0.14f),
    val topRight: NormalizedPoint = NormalizedPoint(0.88f, 0.14f),
    val bottomRight: NormalizedPoint = NormalizedPoint(0.88f, 0.86f),
    val bottomLeft: NormalizedPoint = NormalizedPoint(0.12f, 0.86f)
)

data class NormalizedPoint(val x: Float, val y: Float)

data class EditablePage(
    val id: String = UUID.randomUUID().toString(),
    val originalUri: Uri,
    val processedUri: Uri? = null,
    val rotationDegrees: Int = 0,
    val filter: ScanFilter = ScanFilter.ORIGINAL,
    val brightness: Float = 0f, // -50f .. +50f
    val contrast: Float = 1f,   // 0.5f .. 1.8f
    val cropQuad: QuadPoints? = null
)

data class PdfDocumentItem(
    val id: String,
    val fileName: String,
    val filePath: String,
    val sizeBytes: Long,
    val lastModifiedMillis: Long,
    val pageCount: Int,
    val isEncrypted: Boolean
)

data class PdfCreationResult(
    val document: PdfDocumentItem,
    val compressionLevel: CompressionLevel
)

data class PdfCompressionResult(
    val document: PdfDocumentItem,
    val originalSizeBytes: Long,
    val compressedSizeBytes: Long,
    val reductionPercent: Int,
    val notice: String
)

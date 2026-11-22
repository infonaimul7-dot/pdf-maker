package com.pdfmaker.scanner.util

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.pdfmaker.scanner.data.model.NormalizedPoint
import com.pdfmaker.scanner.data.model.QuadPoints
import kotlin.math.abs

/**
 * Lightweight real-time luminance edge scanner for CameraX ImageAnalysis.
 * Estimates document sheet bounds from high-contrast border gradients on the Y plane.
 */
class DocumentEdgeDetector(
    private val onQuadDetected: (QuadPoints) -> Unit
) : ImageAnalysis.Analyzer {

    private var lastAnalyzedTimestamp = 0L

    override fun analyze(image: ImageProxy) {
        val now = System.currentTimeMillis()
        if (now - lastAnalyzedTimestamp < 220L) {
            image.close()
            return
        }
        lastAnalyzedTimestamp = now

        try {
            val yPlane = image.planes.firstOrNull() ?: return
            val buffer = yPlane.buffer
            val rowStride = yPlane.rowStride
            val width = image.width
            val height = image.height
            if (width < 40 || height < 40) return

            val step = 6
            var minX = width
            var maxX = 0
            var minY = height
            var maxY = 0
            var strongEdges = 0

            for (y in step until height - step step step) {
                for (x in step until width - step step step) {
                    val center = buffer.get(y * rowStride + x).toInt() and 0xFF
                    val right = buffer.get(y * rowStride + (x + step)).toInt() and 0xFF
                    val down = buffer.get((y + step) * rowStride + x).toInt() and 0xFF
                    val grad = abs(center - right) + abs(center - down)
                    if (grad > 68) {
                        strongEdges++
                        if (x < minX) minX = x
                        if (x > maxX) maxX = x
                        if (y < minY) minY = y
                        if (y > maxY) maxY = y
                    }
                }
            }

            if (strongEdges > 24 && (maxX - minX) > width * 0.25f && (maxY - minY) > height * 0.25f) {
                val left = (minX.toFloat() / width).coerceIn(0.05f, 0.40f)
                val right = (maxX.toFloat() / width).coerceIn(0.60f, 0.95f)
                val top = (minY.toFloat() / height).coerceIn(0.05f, 0.40f)
                val bottom = (maxY.toFloat() / height).coerceIn(0.60f, 0.95f)

                onQuadDetected(
                    QuadPoints(
                        topLeft = NormalizedPoint(left, top),
                        topRight = NormalizedPoint(right, top),
                        bottomRight = NormalizedPoint(right, bottom),
                        bottomLeft = NormalizedPoint(left, bottom)
                    )
                )
            }
        } finally {
            image.close()
        }
    }
}

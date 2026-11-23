package com.pdfmaker.scanner.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import android.net.Uri
import com.pdfmaker.scanner.data.model.QuadPoints
import com.pdfmaker.scanner.data.model.ScanFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Singleton
class ImageFilterProcessor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Memory-safe bitmap decoding using inSampleSize so large gallery photos do not trigger OOM.
     */
    fun decodeSampledBitmap(uri: Uri, maxDimensionPx: Int): Bitmap {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Unable to read image at $uri")
        boundsStream.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOpts)
        }

        val srcW = boundsOpts.outWidth
        val srcH = boundsOpts.outHeight
        if (srcW <= 0 || srcH <= 0) {
            throw IllegalArgumentException("Unsupported or corrupted image format.")
        }

        var sampleSize = 1
        while (max(srcW / sampleSize, srcH / sampleSize) > maxDimensionPx * 2) {
            sampleSize *= 2
        }

        val exifOrientation = runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        val decodeStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Unable to read image at $uri")
        val rawDecoded = decodeStream.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
        } ?: throw IllegalStateException("Failed to decode image stream.")

        val exifDegrees = when (exifOrientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }

        val oriented = if (exifDegrees != 0f) {
            val m = Matrix().apply { postRotate(exifDegrees) }
            val rotated = Bitmap.createBitmap(rawDecoded, 0, 0, rawDecoded.width, rawDecoded.height, m, true)
            if (rotated != rawDecoded) rawDecoded.recycle()
            rotated
        } else {
            rawDecoded
        }

        // Scale cleanly down (never upscale low-res scans)
        val maxActual = max(oriented.width, oriented.height)
        if (maxActual <= maxDimensionPx) return oriented

        val ratio = maxDimensionPx.toFloat() / maxActual.toFloat()
        val targetW = (oriented.width * ratio).roundToInt().coerceAtLeast(1)
        val targetH = (oriented.height * ratio).roundToInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(oriented, targetW, targetH, true)
        if (scaled != oriented) oriented.recycle()
        return scaled
    }

    fun applyEdits(
        source: Bitmap,
        cropQuad: QuadPoints?,
        rotationDegrees: Int,
        filter: ScanFilter,
        brightness: Float,
        contrast: Float
    ): Bitmap {
        var working = if (cropQuad != null) {
            applyPerspectiveCorrection(source, cropQuad)
        } else {
            source.copy(Bitmap.Config.ARGB_8888, true)
        }

        val normalizedRot = ((rotationDegrees % 360) + 360) % 360
        if (normalizedRot != 0) {
            val matrix = Matrix().apply { postRotate(normalizedRot.toFloat()) }
            val rotated = Bitmap.createBitmap(working, 0, 0, working.width, working.height, matrix, true)
            if (rotated != working) working.recycle()
            working = rotated
        }

        val filtered = when (filter) {
            ScanFilter.BLACK_AND_WHITE -> applyAdaptiveThresholdBw(working, brightness, contrast)
            ScanFilter.MAGIC_COLOR -> applyColorMatrixFilter(
                working,
                buildColorMatrix(saturation = 1.28f, contrast = contrast * 1.22f, brightness = brightness + 10f)
            )
            ScanFilter.GRAYSCALE -> {
                val cm = buildColorMatrix(saturation = 0f, contrast = contrast, brightness = brightness)
                applyColorMatrixFilter(working, cm)
            }
            ScanFilter.INVERT -> {
                val invertMatrix = ColorMatrix(
                    floatArrayOf(
                        -1f, 0f, 0f, 0f, 255f,
                        0f, -1f, 0f, 0f, 255f,
                        0f, 0f, -1f, 0f, 255f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                val adjust = buildColorMatrix(saturation = 1f, contrast = contrast, brightness = brightness)
                adjust.postConcat(invertMatrix)
                applyColorMatrixFilter(working, adjust)
            }
            ScanFilter.ORIGINAL -> {
                if (brightness == 0f && contrast == 1f) {
                    working
                } else {
                    applyColorMatrixFilter(working, buildColorMatrix(1f, contrast, brightness))
                }
            }
        }

        if (filtered != working) working.recycle()
        return filtered
    }

    /**
     * Genuine 4-point perspective warp using Android Matrix.setPolyToPoly.
     */
    fun applyPerspectiveCorrection(source: Bitmap, quad: QuadPoints): Bitmap {
        val w = source.width.toFloat()
        val h = source.height.toFloat()

        val src = floatArrayOf(
            quad.topLeft.x * w, quad.topLeft.y * h,
            quad.topRight.x * w, quad.topRight.y * h,
            quad.bottomRight.x * w, quad.bottomRight.y * h,
            quad.bottomLeft.x * w, quad.bottomLeft.y * h
        )

        val topWidth = hypot((src[2] - src[0]).toDouble(), (src[3] - src[1]).toDouble())
        val bottomWidth = hypot((src[4] - src[6]).toDouble(), (src[5] - src[7]).toDouble())
        val leftHeight = hypot((src[6] - src[0]).toDouble(), (src[7] - src[1]).toDouble())
        val rightHeight = hypot((src[4] - src[2]).toDouble(), (src[5] - src[3]).toDouble())

        val outW = max(topWidth, bottomWidth).roundToInt().coerceIn(64, source.width)
        val outH = max(leftHeight, rightHeight).roundToInt().coerceIn(64, source.height)

        val dst = floatArrayOf(
            0f, 0f,
            outW.toFloat(), 0f,
            outW.toFloat(), outH.toFloat(),
            0f, outH.toFloat()
        )

        val matrix = Matrix()
        val mapped = matrix.setPolyToPoly(src, 0, dst, 0, 4)
        if (!mapped) return source.copy(Bitmap.Config.ARGB_8888, true)

        val result = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(source, matrix, paint)
        return result
    }

    private fun buildColorMatrix(saturation: Float, contrast: Float, brightness: Float): ColorMatrix {
        val satMatrix = ColorMatrix().apply { setSaturation(saturation) }
        val translate = (1f - contrast) * 128f + brightness
        val bcMatrix = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        satMatrix.postConcat(bcMatrix)
        return satMatrix
    }

    private fun applyColorMatrixFilter(source: Bitmap, matrix: ColorMatrix): Bitmap {
        val out = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(matrix)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return out
    }

    /**
     * Integral-image adaptive thresholding for crisp document B&W scans.
     */
    private fun applyAdaptiveThresholdBw(source: Bitmap, brightness: Float, contrast: Float): Bitmap {
        val w = source.width
        val h = source.height
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)

        val lum = IntArray(w * h)
        val integral = LongArray(w * h)

        for (y in 0 until h) {
            var rowSum = 0L
            for (x in 0 until w) {
                val idx = y * w + x
                val c = pixels[idx]
                val rawGray = ((Color.red(c) * 299 + Color.green(c) * 587 + Color.blue(c) * 114) / 1000)
                val adjusted = (((rawGray - 128) * contrast) + 128 + brightness).roundToInt().coerceIn(0, 255)
                lum[idx] = adjusted
                rowSum += adjusted
                integral[idx] = rowSum + if (y > 0) integral[(y - 1) * w + x] else 0L
            }
        }

        val radius = max(8, min(w, h) / 28)
        val out = IntArray(w * h)
        for (y in 0 until h) {
            val y1 = max(0, y - radius)
            val y2 = min(h - 1, y + radius)
            for (x in 0 until w) {
                val x1 = max(0, x - radius)
                val x2 = min(w - 1, x + radius)
                val count = (x2 - x1 + 1) * (y2 - y1 + 1)
                val a = if (x1 > 0 && y1 > 0) integral[(y1 - 1) * w + (x1 - 1)] else 0L
                val b = if (y1 > 0) integral[(y1 - 1) * w + x2] else 0L
                val c = if (x1 > 0) integral[y2 * w + (x1 - 1)] else 0L
                val d = integral[y2 * w + x2]
                val localMean = (d - b - c + a) / count
                val idx = y * w + x
                out[idx] = if (lum[idx] < localMean * 0.90) Color.BLACK else Color.WHITE
            }
        }

        return Bitmap.createBitmap(out, w, h, Bitmap.Config.ARGB_8888)
    }

    fun persistTempScanBitmap(bitmap: Bitmap, fileName: String): Uri {
        val dir = File(context.cacheDir, "scans").apply { mkdirs() }
        val file = File(dir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        return Uri.fromFile(file)
    }
}

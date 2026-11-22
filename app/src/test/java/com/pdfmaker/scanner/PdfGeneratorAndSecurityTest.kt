package com.pdfmaker.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.pdfmaker.scanner.data.model.CompressionLevel
import com.pdfmaker.scanner.data.model.EditablePage
import com.pdfmaker.scanner.util.ImageFilterProcessor
import com.pdfmaker.scanner.util.PdfGenerator
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
class PdfGeneratorAndSecurityTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var pdfGenerator: PdfGenerator

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        PDFBoxResourceLoader.init(context)
        pdfGenerator = PdfGenerator(context, ImageFilterProcessor(context))
    }

    private fun createTempImageUri(name: String, color: Int): Uri {
        val file = File(tempFolder.root, name)
        val bmp = Bitmap.createBitmap(800, 1000, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(color)
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        bmp.recycle()
        return Uri.fromFile(file)
    }

    @Test
    fun `creates valid single-image and multi-image PDFs`() = runBlocking {
        val img1 = createTempImageUri("p1.jpg", Color.WHITE)
        val img2 = createTempImageUri("p2.jpg", Color.LTGRAY)

        val singleOut = File(tempFolder.root, "single.pdf")
        val singleRes = pdfGenerator.generatePdfFromPages(
            pages = listOf(EditablePage(originalUri = img1)),
            outputFile = singleOut,
            compressionLevel = CompressionLevel.LOW,
            password = null
        )
        assertTrue(singleRes.isSuccess)
        PDDocument.load(singleOut).use { assertEquals(1, it.numberOfPages) }

        val multiOut = File(tempFolder.root, "multi.pdf")
        val multiRes = pdfGenerator.generatePdfFromPages(
            pages = listOf(EditablePage(originalUri = img1), EditablePage(originalUri = img2)),
            outputFile = multiOut,
            compressionLevel = CompressionLevel.MEDIUM,
            password = null
        )
        assertTrue(multiRes.isSuccess)
        PDDocument.load(multiOut).use { assertEquals(2, it.numberOfPages) }
    }

    @Test
    fun `high compression produces smaller output than low compression`() = runBlocking {
        val img = createTempImageUri("dense.jpg", Color.DKGRAY)
        val lowOut = File(tempFolder.root, "low.pdf")
        val highOut = File(tempFolder.root, "high.pdf")

        pdfGenerator.generatePdfFromPages(listOf(EditablePage(originalUri = img)), lowOut, CompressionLevel.LOW, null)
        pdfGenerator.generatePdfFromPages(listOf(EditablePage(originalUri = img)), highOut, CompressionLevel.HIGH, null)

        assertTrue(highOut.length() < lowOut.length())
    }

    @Test
    fun `password protected PDF rejects wrong password and opens with correct password`() = runBlocking {
        val img = createTempImageUri("secret.jpg", Color.WHITE)
        val out = File(tempFolder.root, "protected.pdf")

        val res = pdfGenerator.generatePdfFromPages(
            pages = listOf(EditablePage(originalUri = img)),
            outputFile = out,
            compressionLevel = CompressionLevel.MEDIUM,
            password = "MySecretPassword"
        )
        assertTrue(res.isSuccess)

        var wrongPasswordRejected = false
        try {
            PDDocument.load(out, "wrong").close()
        } catch (_: InvalidPasswordException) {
            wrongPasswordRejected = true
        }
        assertTrue(wrongPasswordRejected)

        PDDocument.load(out, "MySecretPassword").use { doc ->
            assertEquals(1, doc.numberOfPages)
        }
    }
}

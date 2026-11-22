package com.pdfmaker.scanner

import com.pdfmaker.scanner.util.FilenameValidationResult
import com.pdfmaker.scanner.util.FilenameValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FilenameValidatorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `empty and whitespace filenames are rejected`() {
        assertTrue(FilenameValidator.validate("") is FilenameValidationResult.Invalid)
        assertTrue(FilenameValidator.validate("   ") is FilenameValidationResult.Invalid)
        assertTrue(FilenameValidator.validate(".pdf") is FilenameValidationResult.Invalid)
    }

    @Test
    fun `illegal filesystem characters are rejected`() {
        assertTrue(FilenameValidator.validate("invoice/2026") is FilenameValidationResult.Invalid)
        assertTrue(FilenameValidator.validate("doc:name") is FilenameValidationResult.Invalid)
        assertTrue(FilenameValidator.validate("a*b?c") is FilenameValidationResult.Invalid)
    }

    @Test
    fun `valid filename appends pdf suffix when omitted`() {
        val result = FilenameValidator.validate("  Quarterly Report ")
        assertEquals(FilenameValidationResult.Valid("Quarterly Report.pdf"), result)
    }

    @Test
    fun `collision safe resolution never overwrites existing files`() {
        val dir = tempFolder.newFolder("pdfs")
        File(dir, "New Document.pdf").writeText("existing-1")
        File(dir, "New Document (1).pdf").writeText("existing-2")

        val resolved = FilenameValidator.resolveCollisionSafeFile(dir, "New Document.pdf")
        assertEquals("New Document (2).pdf", resolved.name)
    }
}

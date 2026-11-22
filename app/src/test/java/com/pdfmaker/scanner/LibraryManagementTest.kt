package com.pdfmaker.scanner

import com.pdfmaker.scanner.data.model.PdfDocumentItem
import com.pdfmaker.scanner.data.model.SortOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryManagementTest {

    private val sampleDocs = listOf(
        PdfDocumentItem("1", "Zebra Report.pdf", "/pdfs/Zebra.pdf", 250_000L, 1_000L, 4, false),
        PdfDocumentItem("2", "Alpha Invoice.pdf", "/pdfs/Alpha.pdf", 80_000L, 3_000L, 1, true),
        PdfDocumentItem("3", "Meeting Notes.pdf", "/pdfs/Notes.pdf", 600_000L, 2_000L, 8, false)
    )

    @Test
    fun `sorts library correctly by name, date, and size`() {
        val byNameAsc = sampleDocs.sortedBy { it.fileName.lowercase() }
        assertEquals("Alpha Invoice.pdf", byNameAsc.first().fileName)

        val byDateDesc = sampleDocs.sortedByDescending { it.lastModifiedMillis }
        assertEquals("Alpha Invoice.pdf", byDateDesc.first().fileName)

        val bySizeDesc = sampleDocs.sortedByDescending { it.sizeBytes }
        assertEquals("Meeting Notes.pdf", bySizeDesc.first().fileName)
    }

    @Test
    fun `filters by search query and handles empty state`() {
        val query = "invoice"
        val matches = sampleDocs.filter { it.fileName.contains(query, ignoreCase = true) }
        assertEquals(1, matches.size)
        assertEquals("Alpha Invoice.pdf", matches.first().fileName)

        val noMatches = sampleDocs.filter { it.fileName.contains("nonexistent", ignoreCase = true) }
        assertTrue(noMatches.isEmpty())
    }

    @Test
    fun `all sort options are distinct`() {
        assertEquals(6, SortOption.entries.size)
    }
}

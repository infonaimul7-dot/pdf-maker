package com.pdfmaker.scanner

import android.net.Uri
import com.pdfmaker.scanner.data.model.EditablePage
import com.pdfmaker.scanner.navigation.WorkflowSessionViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PageOrderingAndSessionTest {

    @Test
    fun `moving and removing pages preserves expected document order`() {
        val session = WorkflowSessionViewModel()
        val p1 = EditablePage(id = "p1", originalUri = Uri.parse("file:///1.jpg"))
        val p2 = EditablePage(id = "p2", originalUri = Uri.parse("file:///2.jpg"))
        val p3 = EditablePage(id = "p3", originalUri = Uri.parse("file:///3.jpg"))

        session.setPages(listOf(p1, p2, p3))
        session.movePage(0, 2)
        assertEquals(listOf("p2", "p3", "p1"), session.draftPages.map { it.id })

        session.removePage("p3")
        assertEquals(listOf("p2", "p1"), session.draftPages.map { it.id })

        session.clearDraft()
        assertTrue(session.draftPages.isEmpty())
    }
}

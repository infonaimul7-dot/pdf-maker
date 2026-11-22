package com.pdfmaker.scanner.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.pdfmaker.scanner.data.model.CompressionLevel
import com.pdfmaker.scanner.data.model.EditablePage
import com.pdfmaker.scanner.data.model.PdfCreationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Activity-scoped shared session ViewModel preserving imported/scanned pages and
 * editing progress across Import -> Scan -> High-Quality Editor -> Convert -> Share.
 */
@HiltViewModel
class WorkflowSessionViewModel @Inject constructor() : ViewModel() {

    val draftPages = mutableStateListOf<EditablePage>()
    val activeEditPageId = mutableStateOf<String?>(null)
    val lastCreationResult = mutableStateOf<PdfCreationResult?>(null)
    val lastCompressionLevel = mutableStateOf(CompressionLevel.MEDIUM)

    fun setPages(pages: List<EditablePage>) {
        draftPages.clear()
        draftPages.addAll(pages)
    }

    fun appendPages(pages: List<EditablePage>) {
        draftPages.addAll(pages)
    }

    fun updatePage(updated: EditablePage) {
        val idx = draftPages.indexOfFirst { it.id == updated.id }
        if (idx >= 0) {
            draftPages[idx] = updated
        }
    }

    fun removePage(id: String) {
        draftPages.removeAll { it.id == id }
    }

    fun movePage(fromIndex: Int, toIndex: Int) {
        if (fromIndex in draftPages.indices && toIndex in draftPages.indices && fromIndex != toIndex) {
            val item = draftPages.removeAt(fromIndex)
            draftPages.add(toIndex, item)
        }
    }

    fun clearDraft() {
        draftPages.clear()
        activeEditPageId.value = null
    }
}

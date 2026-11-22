package com.pdfmaker.scanner.ui.library

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pdfmaker.scanner.data.model.PdfDocumentItem
import com.pdfmaker.scanner.data.model.SortOption
import com.pdfmaker.scanner.ui.components.ConfirmActionDialog
import com.pdfmaker.scanner.ui.components.EmptyLibraryState
import com.pdfmaker.scanner.ui.components.FacetedHeaderScaffold
import com.pdfmaker.scanner.ui.components.formatBytes
import com.pdfmaker.scanner.ui.components.formatRelativeDate
import com.pdfmaker.scanner.ui.theme.BrandRed
import com.pdfmaker.scanner.util.ShareHelper
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onBack: () -> Unit,
    onOpenPdf: (String) -> Unit,
    onCreateFromImages: () -> Unit,
    onScanDocument: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showSearch by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showFabSheet by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<PdfDocumentItem?>(null) }
    var deleteTarget by remember { mutableStateOf<PdfDocumentItem?>(null) }
    var infoTarget by remember { mutableStateOf<PdfDocumentItem?>(null) }

    val importPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            var displayName = "Imported Document.pdf"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) {
                    displayName = cursor.getString(idx) ?: displayName
                }
            }
            viewModel.importExistingPdf(uri, displayName)
        }
    }

    if (renameTarget != null) {
        val doc = renameTarget!!
        var draft by remember(doc.id) { mutableStateOf(doc.fileName.removeSuffix(".pdf")) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename PDF") },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    label = { Text("Filename") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.renamePdf(doc.filePath, draft)
                    renameTarget = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("Cancel") }
            }
        )
    }

    if (deleteTarget != null) {
        val doc = deleteTarget!!
        ConfirmActionDialog(
            title = "Delete PDF",
            message = "Delete \"${doc.fileName}\" from your library?",
            confirmLabel = "Delete",
            isDestructive = true,
            onConfirm = {
                viewModel.deletePdf(doc.filePath)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null }
        )
    }

    if (infoTarget != null) {
        val doc = infoTarget!!
        AlertDialog(
            onDismissRequest = { infoTarget = null },
            title = { Text("File Information") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Name: ${doc.fileName}")
                    Text("Size: ${formatBytes(doc.sizeBytes)}")
                    Text("Pages: ${if (doc.pageCount > 0) doc.pageCount else "Encrypted / Unknown"}")
                    Text("Encrypted: ${if (doc.isEncrypted) "Yes" else "No"}")
                    Text("Modified: ${formatRelativeDate(doc.lastModifiedMillis)}")
                    Text("Path: ${doc.filePath}", style = MaterialTheme.typography.labelSmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { infoTarget = null }) { Text("Close") }
            }
        )
    }

    if (showFabSheet) {
        ModalBottomSheet(onDismissRequest = { showFabSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = "Add to PDF Library",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
                ListItem(
                    headlineContent = { Text("Create PDF from images") },
                    leadingContent = { Icon(Icons.Outlined.Image, contentDescription = null, tint = BrandRed) },
                    modifier = Modifier.clickable {
                        showFabSheet = false
                        onCreateFromImages()
                    }
                )
                ListItem(
                    headlineContent = { Text("Scan a document") },
                    leadingContent = { Icon(Icons.Outlined.DocumentScanner, contentDescription = null, tint = BrandRed) },
                    modifier = Modifier.clickable {
                        showFabSheet = false
                        onScanDocument()
                    }
                )
                ListItem(
                    headlineContent = { Text("Import an existing PDF") },
                    leadingContent = { Icon(Icons.Outlined.FileOpen, contentDescription = null, tint = BrandRed) },
                    modifier = Modifier.clickable {
                        showFabSheet = false
                        importPdfLauncher.launch(arrayOf("application/pdf"))
                    }
                )
            }
        }
    }

    FacetedHeaderScaffold(
        title = "List all generated PDF files",
        onBack = onBack
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("My PDF Library", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "${state.documents.size} local file(s)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row {
                        IconButton(onClick = { showSearch = !showSearch }) {
                            Icon(Icons.Default.Search, contentDescription = "Search PDFs")
                        }
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.Outlined.Sort, contentDescription = "Sort PDFs")
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                SortOption.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = option.label,
                                                fontWeight = if (state.sortOption == option) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            viewModel.setSortOption(option)
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                if (showSearch) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = viewModel::setSearchQuery,
                        placeholder = { Text("Search by filename...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LibraryFilter.entries.forEach { f ->
                        FilterChip(
                            selected = state.filter == f,
                            onClick = { viewModel.setFilter(f) },
                            label = { Text(f.label) }
                        )
                    }
                }

                if (state.documents.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyLibraryState(
                            title = "No PDF files yet",
                            subtitle = "Create a PDF from gallery images, scan a document, or import an existing PDF.",
                            primaryLabel = "Create PDF",
                            onPrimaryClick = { showFabSheet = true }
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.documents, key = { it.id }) { doc ->
                            var itemMenuExpanded by remember { mutableStateOf(false) }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenPdf(doc.filePath) },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.PictureAsPdf,
                                        contentDescription = null,
                                        tint = BrandRed,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = doc.fileName,
                                                style = MaterialTheme.typography.titleMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            if (doc.isEncrypted) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Lock,
                                                    contentDescription = "Password protected",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                        val pageLabel = if (doc.pageCount > 0) "${doc.pageCount}p" else "Locked"
                                        Text(
                                            text = "PDF · ${formatRelativeDate(doc.lastModifiedMillis)} · ${formatBytes(doc.sizeBytes)} · $pageLabel",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Box {
                                        IconButton(onClick = { itemMenuExpanded = true }) {
                                            Icon(Icons.Default.MoreVert, contentDescription = "File options")
                                        }
                                        DropdownMenu(
                                            expanded = itemMenuExpanded,
                                            onDismissRequest = { itemMenuExpanded = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Open") },
                                                onClick = {
                                                    itemMenuExpanded = false
                                                    onOpenPdf(doc.filePath)
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Share") },
                                                onClick = {
                                                    itemMenuExpanded = false
                                                    val f = File(doc.filePath)
                                                    if (f.exists()) {
                                                        context.startActivity(ShareHelper.createSharePdfIntent(context, f))
                                                    }
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Rename") },
                                                onClick = {
                                                    itemMenuExpanded = false
                                                    renameTarget = doc
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("File info") },
                                                onClick = {
                                                    itemMenuExpanded = false
                                                    infoTarget = doc
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Delete") },
                                                onClick = {
                                                    itemMenuExpanded = false
                                                    deleteTarget = doc
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = { showFabSheet = true },
                shape = CircleShape,
                containerColor = BrandRed,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(20.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create or import PDF")
            }
        }
    }
}

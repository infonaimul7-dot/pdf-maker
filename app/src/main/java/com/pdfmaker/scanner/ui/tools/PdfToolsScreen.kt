package com.pdfmaker.scanner.ui.tools

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pdfmaker.scanner.data.model.CompressionLevel
import com.pdfmaker.scanner.ui.components.EmptyLibraryState
import com.pdfmaker.scanner.ui.components.FacetedHeaderScaffold
import com.pdfmaker.scanner.ui.components.formatBytes
import com.pdfmaker.scanner.ui.theme.BrandRed

@Composable
fun PdfToolsScreen(
    onBack: () -> Unit,
    onOpenPdf: (String) -> Unit,
    viewModel: PdfToolsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedLevel by remember { mutableStateOf(CompressionLevel.MEDIUM) }
    val mergeSelection = remember { mutableStateListOf<String>() }

    FacetedHeaderScaffold(
        title = "Compress & Merge",
        onBack = onBack
    ) {
        if (state.availablePdfs.isEmpty()) {
            EmptyLibraryState(
                title = "No PDFs available yet",
                subtitle = "Create or import PDFs first before compressing or merging.",
                primaryLabel = "Go Back",
                onPrimaryClick = onBack,
                modifier = Modifier.fillMaxSize()
            )
            return@FacetedHeaderScaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("Compression Level", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    CompressionLevel.entries.forEachIndexed { idx, level ->
                        SegmentedButton(
                            selected = selectedLevel == level,
                            onClick = { selectedLevel = level },
                            shape = SegmentedButtonDefaults.itemShape(idx, CompressionLevel.entries.size)
                        ) {
                            Text(level.label)
                        }
                    }
                }
            }

            if (state.isWorking) {
                item {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = BrandRed)
                }
            }

            state.compressionResult?.let { res ->
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Compressed Copy Saved: ${res.document.fileName}", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Original: ${formatBytes(res.originalSizeBytes)} -> Compressed: ${formatBytes(res.compressedSizeBytes)} (${res.reductionPercent}% reduction)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(res.notice, style = MaterialTheme.typography.labelSmall)
                            Button(
                                onClick = { onOpenPdf(res.document.filePath) },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
                            ) {
                                Text("Open Compressed PDF")
                            }
                        }
                    }
                }
            }

            if (state.errorMessage != null) {
                item {
                    Text(
                        text = state.errorMessage.orEmpty(),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Select PDF to Compress or Merge", style = MaterialTheme.typography.titleMedium)
                    if (mergeSelection.size >= 2) {
                        Button(
                            onClick = {
                                viewModel.mergePdfs(mergeSelection.toList(), "Merged Document.pdf")
                                mergeSelection.clear()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
                        ) {
                            Text("Merge (${mergeSelection.size})")
                        }
                    }
                }
            }

            items(state.availablePdfs, key = { it.id }) { doc ->
                val isChecked = mergeSelection.contains(doc.filePath)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isChecked) mergeSelection.remove(doc.filePath)
                            else mergeSelection.add(doc.filePath)
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                if (checked) mergeSelection.add(doc.filePath)
                                else mergeSelection.remove(doc.filePath)
                            }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(doc.fileName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${formatBytes(doc.sizeBytes)} · ${doc.pageCount}p",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = { viewModel.compressPdf(doc.filePath, selectedLevel) },
                            enabled = !doc.isEncrypted && !state.isWorking,
                            colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
                        ) {
                            Text("Compress")
                        }
                    }
                }
            }
        }
    }
}

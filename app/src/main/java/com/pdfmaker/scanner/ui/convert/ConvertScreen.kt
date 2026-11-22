package com.pdfmaker.scanner.ui.convert

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.pdfmaker.scanner.data.model.CompressionLevel
import com.pdfmaker.scanner.data.model.PdfCreationResult
import com.pdfmaker.scanner.navigation.WorkflowSessionViewModel
import com.pdfmaker.scanner.ui.components.FacetedHeaderScaffold
import com.pdfmaker.scanner.ui.theme.BrandRed
import kotlin.math.roundToInt

@Composable
fun ConvertScreen(
    sessionViewModel: WorkflowSessionViewModel,
    onBack: () -> Unit,
    onAddMoreImages: () -> Unit,
    onEditPage: (String) -> Unit,
    onPdfCreated: (PdfCreationResult) -> Unit,
    viewModel: ConvertViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pages = sessionViewModel.draftPages

    FacetedHeaderScaffold(
        title = "Convert",
        onBack = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Save File", style = MaterialTheme.typography.titleLarge)
                if (pages.isNotEmpty()) {
                    IconButton(onClick = { onEditPage(pages.first().id) }) {
                        Icon(Icons.Default.Crop, contentDescription = "Crop or filter page")
                    }
                }
            }

            OutlinedTextField(
                value = state.documentTitle,
                onValueChange = viewModel::updateTitle,
                label = { Text("Document title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Selected Images (${pages.size}) — Long press & drag to reorder",
                    style = MaterialTheme.typography.titleMedium
                )
                LazyRow(
                    contentPadding = PaddingValues(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .size(width = 86.dp, height = 112.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                .clickable(onClick = onAddMoreImages),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Add, contentDescription = "Add more images")
                                Text("Add", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    itemsIndexed(pages, key = { _, page -> page.id }) { index, page ->
                        var dragAccumulatedX by remember { mutableFloatStateOf(0f) }
                        Box(
                            modifier = Modifier
                                .size(width = 92.dp, height = 112.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                .pointerInput(page.id, pages.size) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { dragAccumulatedX = 0f },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragAccumulatedX += dragAmount.x
                                            if (dragAccumulatedX > 90f && index < pages.lastIndex) {
                                                sessionViewModel.movePage(index, index + 1)
                                                dragAccumulatedX = 0f
                                            } else if (dragAccumulatedX < -90f && index > 0) {
                                                sessionViewModel.movePage(index, index - 1)
                                                dragAccumulatedX = 0f
                                            }
                                        }
                                    )
                                }
                                .clickable { onEditPage(page.id) }
                        ) {
                            AsyncImage(
                                model = page.originalUri,
                                contentDescription = "Page ${index + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .rotate(page.rotationDegrees.toFloat())
                            )

                            IconButton(
                                onClick = { sessionViewModel.removePage(page.id) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(BrandRed)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove page",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    sessionViewModel.updatePage(
                                        page.copy(rotationDegrees = (page.rotationDegrees + 90) % 360)
                                    )
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(4.dp)
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.65f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RotateRight,
                                    contentDescription = "Rotate 90 degrees",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }

                            Text(
                                text = "${index + 1}",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(6.dp)
                                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { viewModel.togglePassword(!state.enablePassword) }
            ) {
                Checkbox(
                    checked = state.enablePassword,
                    onCheckedChange = viewModel::togglePassword
                )
                Text("Add Password (Optional)", style = MaterialTheme.typography.bodyLarge)
            }

            if (state.enablePassword) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = viewModel::updatePassword,
                        label = { Text("PDF Password (min 4 chars)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.confirmPassword,
                        onValueChange = viewModel::updateConfirmPassword,
                        label = { Text("Confirm Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Compress Image", style = MaterialTheme.typography.titleMedium)
                val entries = CompressionLevel.entries
                Slider(
                    value = entries.indexOf(state.compressionLevel).toFloat(),
                    onValueChange = { raw ->
                        val idx = raw.roundToInt().coerceIn(0, entries.lastIndex)
                        viewModel.updateCompression(entries[idx])
                    },
                    valueRange = 0f..entries.lastIndex.toFloat(),
                    steps = entries.size - 2,
                    colors = SliderDefaults.colors(
                        thumbColor = BrandRed,
                        activeTrackColor = BrandRed
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    entries.forEach { level ->
                        Text(
                            text = level.label.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (state.compressionLevel == level) FontWeight.Bold else FontWeight.Normal,
                            color = if (state.compressionLevel == level) BrandRed else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = state.compressionLevel.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (state.isConverting) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = BrandRed
                    )
                    Text(
                        text = "Generating PDF (${(state.progress * 100).roundToInt()}%)...",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    enabled = !state.isConverting,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = { viewModel.convert(pages, onPdfCreated) },
                    enabled = pages.isNotEmpty() && !state.isConverting,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRed),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text("Convert")
                }
            }
        }
    }
}

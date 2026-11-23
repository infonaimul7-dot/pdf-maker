package com.pdfmaker.scanner.ui.convert

import android.graphics.Bitmap
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.pdfmaker.scanner.data.model.CompressionLevel
import com.pdfmaker.scanner.data.model.EditablePage
import com.pdfmaker.scanner.data.model.PdfCreationResult
import com.pdfmaker.scanner.data.model.ScanFilter
import com.pdfmaker.scanner.navigation.WorkflowSessionViewModel
import com.pdfmaker.scanner.ui.components.FacetedHeaderScaffold
import com.pdfmaker.scanner.ui.theme.BrandRed
import com.pdfmaker.scanner.util.ImageFilterProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val context = LocalContext.current
    val processor = remember(context) { ImageFilterProcessor(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var isAutoCropping by remember { mutableStateOf(false) }

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Selected Images (${pages.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (pages.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (pages.any { it.cropQuad != null }) {
                                TextButton(
                                    onClick = {
                                        pages.toList().forEach { page ->
                                            if (page.cropQuad != null) {
                                                sessionViewModel.updatePage(page.copy(cropQuad = null))
                                            }
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("Full", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    if (!isAutoCropping) {
                                        isAutoCropping = true
                                        scope.launch {
                                            val snapshot = pages.toList()
                                            val updatedList = withContext(Dispatchers.IO) {
                                                snapshot.map { page ->
                                                    runCatching {
                                                        val bmp = processor.decodeSampledBitmap(page.originalUri, maxDimensionPx = 640)
                                                        val detected = processor.detectDocumentBounds(bmp)
                                                        bmp.recycle()
                                                        page.copy(cropQuad = detected)
                                                    }.getOrDefault(page)
                                                }
                                            }
                                            updatedList.forEach { sessionViewModel.updatePage(it) }
                                            isAutoCropping = false
                                        }
                                    }
                                },
                                enabled = !isAutoCropping && !state.isConverting,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                if (isAutoCropping) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = BrandRed
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.AutoFixHigh,
                                        contentDescription = "Auto Crop all images",
                                        tint = BrandRed,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Text(
                                    text = if (isAutoCropping) " Cropping..." else " Auto Crop",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
                Text(
                    text = "Tap any image to crop/filter, long press & drag to reorder",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    contentPadding = PaddingValues(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .size(width = 88.dp, height = 122.dp)
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
                                .size(width = 96.dp, height = 122.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(
                                    width = if (page.cropQuad != null) 1.5.dp else 1.dp,
                                    color = if (page.cropQuad != null) BrandRed else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(12.dp)
                                )
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
                            ConvertPageThumbnail(
                                page = page,
                                index = index,
                                processor = processor
                            )

                            if (page.cropQuad != null) {
                                Icon(
                                    imageVector = Icons.Default.Crop,
                                    contentDescription = "Auto-cropped",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(4.dp)
                                        .background(BrandRed.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                        .size(12.dp)
                                )
                            }

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

@Composable
private fun ConvertPageThumbnail(
    page: EditablePage,
    index: Int,
    processor: ImageFilterProcessor
) {
    val hasEdits = page.cropQuad != null ||
        page.rotationDegrees != 0 ||
        page.filter != ScanFilter.ORIGINAL ||
        page.brightness != 0f ||
        page.contrast != 1f

    if (!hasEdits) {
        AsyncImage(
            model = page.originalUri,
            contentDescription = "Page ${index + 1}",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp)
        )
        return
    }

    val editedBitmap by produceState<Bitmap?>(
        initialValue = null,
        key1 = page.id,
        key2 = page.cropQuad,
        key3 = "${page.rotationDegrees}_${page.filter}_${page.brightness}_${page.contrast}"
    ) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val sampled = processor.decodeSampledBitmap(page.originalUri, maxDimensionPx = 360)
                val rendered = processor.applyEdits(
                    source = sampled,
                    cropQuad = page.cropQuad,
                    rotationDegrees = page.rotationDegrees,
                    filter = page.filter,
                    brightness = page.brightness,
                    contrast = page.contrast
                )
                if (rendered != sampled) sampled.recycle()
                rendered
            }.getOrNull()
        }
    }

    val bmp = editedBitmap
    if (bmp != null) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = "Page ${index + 1}",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp)
        )
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = BrandRed
            )
        }
    }
}

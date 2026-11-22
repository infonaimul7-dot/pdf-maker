package com.pdfmaker.scanner.ui.editor

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pdfmaker.scanner.data.model.ScanFilter
import com.pdfmaker.scanner.navigation.WorkflowSessionViewModel
import com.pdfmaker.scanner.ui.components.FacetedHeaderScaffold
import com.pdfmaker.scanner.ui.theme.BrandRed
import com.pdfmaker.scanner.util.ImageFilterProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun HighQualityScanScreen(
    sessionViewModel: WorkflowSessionViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val processor = remember(context) { ImageFilterProcessor(context.applicationContext) }

    val pageId = sessionViewModel.activeEditPageId.value
    val originalPage = sessionViewModel.draftPages.firstOrNull { it.id == pageId }
        ?: sessionViewModel.draftPages.firstOrNull()

    if (originalPage == null) {
        FacetedHeaderScaffold(title = "HIGH-QUALITY Scan", onBack = onBack) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No page selected for editing.")
            }
        }
        return
    }

    var selectedFilter by remember(originalPage.id) { mutableStateOf(originalPage.filter) }
    var rotation by remember(originalPage.id) { mutableIntStateOf(originalPage.rotationDegrees) }
    var brightness by remember(originalPage.id) { mutableFloatStateOf(originalPage.brightness) }
    var contrast by remember(originalPage.id) { mutableFloatStateOf(originalPage.contrast) }
    var showBefore by remember { mutableStateOf(false) }

    var basePreviewBitmap by remember(originalPage.id) { mutableStateOf<Bitmap?>(null) }
    var processedPreviewBitmap by remember(originalPage.id) { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(true) }

    LaunchedEffect(originalPage.originalUri) {
        withContext(Dispatchers.IO) {
            basePreviewBitmap = processor.decodeSampledBitmap(originalPage.originalUri, maxDimensionPx = 1280)
        }
    }

    LaunchedEffect(basePreviewBitmap, selectedFilter, rotation, brightness, contrast, showBefore) {
        val base = basePreviewBitmap ?: return@LaunchedEffect
        isProcessing = true
        val rendered = withContext(Dispatchers.IO) {
            if (showBefore) {
                base
            } else {
                processor.applyEdits(
                    source = base,
                    cropQuad = originalPage.cropQuad,
                    rotationDegrees = rotation,
                    filter = selectedFilter,
                    brightness = brightness,
                    contrast = contrast
                )
            }
        }
        processedPreviewBitmap = rendered
        isProcessing = false
    }

    FacetedHeaderScaffold(title = "HIGH-QUALITY Scan") {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = if (showBefore) "Original Preview" else "Enhanced Preview",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showBefore = !showBefore }) {
                    Icon(Icons.Default.Compare, contentDescription = "Compare before and after")
                }
                IconButton(onClick = { rotation = (rotation + 90) % 360 }) {
                    Icon(Icons.Default.RotateRight, contentDescription = "Rotate 90 degrees")
                }
                IconButton(onClick = {
                    selectedFilter = ScanFilter.ORIGINAL
                    rotation = 0
                    brightness = 0f
                    contrast = 1f
                }) {
                    Icon(Icons.Default.RestartAlt, contentDescription = "Reset edits")
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                val bmp = processedPreviewBitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Edited scan preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                if (isProcessing) {
                    CircularProgressIndicator()
                }
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("Brightness: ${brightness.toInt()}", style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = brightness,
                    onValueChange = { brightness = it },
                    valueRange = -50f..50f,
                    colors = SliderDefaults.colors(thumbColor = BrandRed, activeTrackColor = BrandRed)
                )
                Text("Contrast: ${String.format("%.2f", contrast)}x", style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = contrast,
                    onValueChange = { contrast = it },
                    valueRange = 0.6f..1.8f,
                    colors = SliderDefaults.colors(thumbColor = BrandRed, activeTrackColor = BrandRed)
                )
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(ScanFilter.entries) { filter ->
                    val isSelected = selectedFilter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) BrandRed else MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) BrandRed else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedFilter = filter }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = filter.label,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        sessionViewModel.updatePage(
                            originalPage.copy(
                                filter = selectedFilter,
                                rotationDegrees = rotation,
                                brightness = brightness,
                                contrast = contrast
                            )
                        )
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRed),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save Page")
                }
            }
        }
    }
}

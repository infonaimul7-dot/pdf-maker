package com.pdfmaker.scanner.ui.editor

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pdfmaker.scanner.data.model.NormalizedPoint
import com.pdfmaker.scanner.data.model.QuadPoints
import com.pdfmaker.scanner.data.model.ScanFilter
import com.pdfmaker.scanner.navigation.WorkflowSessionViewModel
import com.pdfmaker.scanner.ui.components.FacetedHeaderScaffold
import com.pdfmaker.scanner.ui.theme.BrandRed
import com.pdfmaker.scanner.util.ImageFilterProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.hypot
import kotlin.math.min

private val FULL_FRAME_QUAD = QuadPoints(
    topLeft = NormalizedPoint(0.04f, 0.04f),
    topRight = NormalizedPoint(0.96f, 0.04f),
    bottomRight = NormalizedPoint(0.96f, 0.96f),
    bottomLeft = NormalizedPoint(0.04f, 0.96f)
)

@Composable
fun HighQualityScanScreen(
    sessionViewModel: WorkflowSessionViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val processor = remember(context) { ImageFilterProcessor(context.applicationContext) }
    val scope = rememberCoroutineScope()

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
    var cropQuad by remember(originalPage.id) { mutableStateOf(originalPage.cropQuad) }
    var showBefore by remember { mutableStateOf(false) }
    var isCropping by remember(originalPage.id) { mutableStateOf(false) }
    var isAutoCropping by remember { mutableStateOf(false) }

    var basePreviewBitmap by remember(originalPage.id) { mutableStateOf<Bitmap?>(null) }
    var processedPreviewBitmap by remember(originalPage.id) { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(true) }

    LaunchedEffect(originalPage.originalUri) {
        withContext(Dispatchers.IO) {
            basePreviewBitmap = processor.decodeSampledBitmap(originalPage.originalUri, maxDimensionPx = 1280)
        }
    }

    LaunchedEffect(basePreviewBitmap, cropQuad, selectedFilter, rotation, brightness, contrast, showBefore, isCropping) {
        val base = basePreviewBitmap ?: return@LaunchedEffect
        if (isCropping) {
            isProcessing = false
            return@LaunchedEffect
        }
        isProcessing = true
        val rendered = withContext(Dispatchers.IO) {
            if (showBefore) {
                base
            } else {
                processor.applyEdits(
                    source = base,
                    cropQuad = cropQuad,
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
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = when {
                        isCropping -> "Drag corners to crop"
                        showBefore -> "Original Preview"
                        cropQuad != null -> "Auto-Cropped Preview"
                        else -> "Enhanced Preview"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    isCropping = false
                    showBefore = !showBefore
                }) {
                    Icon(Icons.Default.Compare, contentDescription = "Compare before and after")
                }
                IconButton(onClick = {
                    isCropping = false
                    rotation = (rotation + 90) % 360
                }) {
                    Icon(Icons.Default.RotateRight, contentDescription = "Rotate 90 degrees")
                }
                IconButton(onClick = {
                    isCropping = false
                    cropQuad = null
                    selectedFilter = ScanFilter.ORIGINAL
                    rotation = 0
                    brightness = 0f
                    contrast = 1f
                }) {
                    Icon(Icons.Default.RestartAlt, contentDescription = "Reset edits")
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        val base = basePreviewBitmap
                        if (base != null && !isAutoCropping) {
                            isAutoCropping = true
                            showBefore = false
                            scope.launch {
                                val detected = withContext(Dispatchers.IO) {
                                    processor.detectDocumentBounds(base)
                                }
                                cropQuad = detected
                                isAutoCropping = false
                            }
                        }
                    },
                    enabled = basePreviewBitmap != null && !isAutoCropping,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
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
                            contentDescription = "Auto crop document edges",
                            tint = BrandRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = if (isAutoCropping) " Detecting..." else " Auto Crop",
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                OutlinedButton(
                    onClick = {
                        showBefore = false
                        if (!isCropping && cropQuad == null) {
                            cropQuad = FULL_FRAME_QUAD
                        }
                        isCropping = !isCropping
                    },
                    enabled = basePreviewBitmap != null,
                    colors = if (isCropping) {
                        ButtonDefaults.outlinedButtonColors(
                            containerColor = BrandRed,
                            contentColor = Color.White
                        )
                    } else {
                        ButtonDefaults.outlinedButtonColors()
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = if (isCropping) Icons.Default.Check else Icons.Default.Crop,
                        contentDescription = if (isCropping) "Apply crop" else "Adjust crop corners",
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isCropping) " Apply Crop" else " Adjust Crop",
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                if (cropQuad != null) {
                    TextButton(
                        onClick = {
                            cropQuad = null
                            isCropping = false
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CropFree,
                            contentDescription = "Reset to full image",
                            modifier = Modifier.size(15.dp)
                        )
                        Text(" Full", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                val base = basePreviewBitmap
                if (isCropping && base != null) {
                    InteractiveCropOverlay(
                        bitmap = base,
                        quad = cropQuad ?: FULL_FRAME_QUAD,
                        onQuadChange = { cropQuad = it }
                    )
                } else {
                    val bmp = processedPreviewBitmap
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Edited scan preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp)
                        )
                    }
                    if (isProcessing) {
                        CircularProgressIndicator(color = BrandRed)
                    }
                }
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
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
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
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
                            .clickable {
                                isCropping = false
                                selectedFilter = filter
                            }
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
                    .padding(horizontal = 16.dp, vertical = 10.dp),
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
                                cropQuad = cropQuad,
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

@Composable
private fun InteractiveCropOverlay(
    bitmap: Bitmap,
    quad: QuadPoints,
    onQuadChange: (QuadPoints) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(6.dp)
    ) {
        val cw = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val ch = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val bw = bitmap.width.toFloat().coerceAtLeast(1f)
        val bh = bitmap.height.toFloat().coerceAtLeast(1f)
        val fitScale = min(cw / bw, ch / bh)
        val rw = (bw * fitScale).coerceAtLeast(1f)
        val rh = (bh * fitScale).coerceAtLeast(1f)
        val ox = (cw - rw) / 2f
        val oy = (ch - rh) / 2f

        val latestQuad by rememberUpdatedState(quad)
        val latestOnQuadChange by rememberUpdatedState(onQuadChange)
        var draggingCorner by remember { mutableIntStateOf(-1) }

        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Original image for cropping",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(ox, oy, rw, rh) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            val q = latestQuad
                            val corners = listOf(
                                Offset(ox + q.topLeft.x * rw, oy + q.topLeft.y * rh),
                                Offset(ox + q.topRight.x * rw, oy + q.topRight.y * rh),
                                Offset(ox + q.bottomRight.x * rw, oy + q.bottomRight.y * rh),
                                Offset(ox + q.bottomLeft.x * rw, oy + q.bottomLeft.y * rh)
                            )
                            var bestIdx = -1
                            var bestDist = 96f
                            corners.forEachIndexed { idx, cornerPos ->
                                val d = hypot(
                                    (cornerPos.x - startOffset.x).toDouble(),
                                    (cornerPos.y - startOffset.y).toDouble()
                                ).toFloat()
                                if (d < bestDist) {
                                    bestDist = d
                                    bestIdx = idx
                                }
                            }
                            draggingCorner = bestIdx
                        },
                        onDragEnd = { draggingCorner = -1 },
                        onDragCancel = { draggingCorner = -1 },
                        onDrag = { change, dragAmount ->
                            if (draggingCorner < 0) return@detectDragGestures
                            change.consume()
                            val dx = dragAmount.x / rw
                            val dy = dragAmount.y / rh
                            val q = latestQuad
                            val updated = when (draggingCorner) {
                                0 -> q.copy(
                                    topLeft = NormalizedPoint(
                                        (q.topLeft.x + dx).coerceIn(0.01f, 0.99f),
                                        (q.topLeft.y + dy).coerceIn(0.01f, 0.99f)
                                    )
                                )
                                1 -> q.copy(
                                    topRight = NormalizedPoint(
                                        (q.topRight.x + dx).coerceIn(0.01f, 0.99f),
                                        (q.topRight.y + dy).coerceIn(0.01f, 0.99f)
                                    )
                                )
                                2 -> q.copy(
                                    bottomRight = NormalizedPoint(
                                        (q.bottomRight.x + dx).coerceIn(0.01f, 0.99f),
                                        (q.bottomRight.y + dy).coerceIn(0.01f, 0.99f)
                                    )
                                )
                                3 -> q.copy(
                                    bottomLeft = NormalizedPoint(
                                        (q.bottomLeft.x + dx).coerceIn(0.01f, 0.99f),
                                        (q.bottomLeft.y + dy).coerceIn(0.01f, 0.99f)
                                    )
                                )
                                else -> q
                            }
                            latestOnQuadChange(updated)
                        }
                    )
                }
        ) {
            val tl = Offset(ox + quad.topLeft.x * rw, oy + quad.topLeft.y * rh)
            val tr = Offset(ox + quad.topRight.x * rw, oy + quad.topRight.y * rh)
            val br = Offset(ox + quad.bottomRight.x * rw, oy + quad.bottomRight.y * rh)
            val bl = Offset(ox + quad.bottomLeft.x * rw, oy + quad.bottomLeft.y * rh)

            val quadPath = Path().apply {
                moveTo(tl.x, tl.y)
                lineTo(tr.x, tr.y)
                lineTo(br.x, br.y)
                lineTo(bl.x, bl.y)
                close()
            }

            drawPath(quadPath, color = BrandRed.copy(alpha = 0.14f))
            drawPath(quadPath, color = BrandRed, style = Stroke(width = 5f))

            listOf(tl, tr, br, bl).forEach { pt ->
                drawCircle(color = Color.Black.copy(alpha = 0.35f), radius = 21f, center = pt)
                drawCircle(color = Color.White, radius = 17f, center = pt)
                drawCircle(color = BrandRed, radius = 10f, center = pt)
            }
        }
    }
}

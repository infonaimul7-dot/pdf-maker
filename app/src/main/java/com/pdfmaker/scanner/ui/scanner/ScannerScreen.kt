package com.pdfmaker.scanner.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.pdfmaker.scanner.R
import com.pdfmaker.scanner.data.model.EditablePage
import com.pdfmaker.scanner.data.model.QuadPoints
import com.pdfmaker.scanner.navigation.WorkflowSessionViewModel
import com.pdfmaker.scanner.ui.components.FacetedHeaderScaffold
import com.pdfmaker.scanner.ui.theme.BrandRed
import com.pdfmaker.scanner.util.DocumentEdgeDetector
import java.io.File
import java.util.concurrent.Executors

@Composable
fun ScannerScreen(
    sessionViewModel: WorkflowSessionViewModel,
    onBack: () -> Unit,
    onEditLastScan: (String) -> Unit,
    onProceedToConvert: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    var flashEnabled by remember { mutableStateOf(false) }
    var detectedQuad by remember { mutableStateOf(QuadPoints()) }
    var lastCapturedPage by remember { mutableStateOf<EditablePage?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var cameraRef by remember { mutableStateOf<Camera?>(null) }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()
    }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose { analysisExecutor.shutdown() }
    }

    FacetedHeaderScaffold(title = "Scan") {
        if (!hasCameraPermission) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = BrandRed,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Camera Permission Required",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.camera_permission_rationale),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
                ) {
                    Text("Grant Camera Access")
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onBack) {
                    Text("Back to Home")
                }
            }
            return@FacetedHeaderScaffold
        }

        // Captured single-page confirmation view (Retake / Crop & Filter / Add Page / Next)
        val previewPage = lastCapturedPage
        if (previewPage != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { lastCapturedPage = null }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to camera")
                    }
                    Text(
                        text = "Page ${sessionViewModel.draftPages.size} Captured",
                        style = MaterialTheme.typography.titleMedium
                    )
                    TextButton(onClick = { onEditLastScan(previewPage.id) }) {
                        Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(" Crop & Filters")
                    }
                }

                AsyncImage(
                    model = previewPage.originalUri,
                    contentDescription = "Captured document preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.05f))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            sessionViewModel.removePage(previewPage.id)
                            lastCapturedPage = null
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(" Retake")
                    }
                    OutlinedButton(
                        onClick = { lastCapturedPage = null },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Add Page")
                    }
                    Button(
                        onClick = onProceedToConvert,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandRed),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Next")
                    }
                }
            }
            return@FacetedHeaderScaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Align document inside frame",
                    style = MaterialTheme.typography.bodyMedium
                )
                IconButton(onClick = {
                    flashEnabled = !flashEnabled
                    cameraRef?.cameraControl?.enableTorch(flashEnabled)
                }) {
                    Icon(
                        imageVector = if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Toggle flash"
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            try {
                                val provider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.surfaceProvider = previewView.surfaceProvider
                                }
                                val analysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                    .also {
                                        it.setAnalyzer(
                                            analysisExecutor,
                                            DocumentEdgeDetector { quad -> detectedQuad = quad }
                                        )
                                    }

                                provider.unbindAll()
                                cameraRef = provider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageCapture,
                                    analysis
                                )
                            } catch (t: Throwable) {
                                errorMessage = t.message ?: "Camera initialization failed."
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Detected document bounding polygon overlay
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val tl = Offset(detectedQuad.topLeft.x * size.width, detectedQuad.topLeft.y * size.height)
                    val tr = Offset(detectedQuad.topRight.x * size.width, detectedQuad.topRight.y * size.height)
                    val br = Offset(detectedQuad.bottomRight.x * size.width, detectedQuad.bottomRight.y * size.height)
                    val bl = Offset(detectedQuad.bottomLeft.x * size.width, detectedQuad.bottomLeft.y * size.height)

                    val path = Path().apply {
                        moveTo(tl.x, tl.y)
                        lineTo(tr.x, tr.y)
                        lineTo(br.x, br.y)
                        lineTo(bl.x, bl.y)
                        close()
                    }
                    drawPath(path, color = BrandRed, style = Stroke(width = 6f))
                    listOf(tl, tr, br, bl).forEach { pt ->
                        drawCircle(color = Color.White, radius = 14f, center = pt)
                        drawCircle(color = BrandRed, radius = 8f, center = pt)
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage.orEmpty(),
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(10.dp))
                            .padding(16.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(enabled = sessionViewModel.draftPages.isNotEmpty()) {
                            sessionViewModel.draftPages.lastOrNull()?.let { onEditLastScan(it.id) }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${sessionViewModel.draftPages.size}",
                        fontWeight = FontWeight.Bold,
                        color = BrandRed
                    )
                }

                // Shutter button
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .border(4.dp, BrandRed, CircleShape)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(BrandRed)
                        .clickable {
                            val scanDir = File(context.cacheDir, "scans").apply { mkdirs() }
                            val outFile = File(scanDir, "scan_${System.currentTimeMillis()}.jpg")
                            val outputOptions = ImageCapture.OutputFileOptions.Builder(outFile).build()
                            imageCapture.takePicture(
                                outputOptions,
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                        val page = EditablePage(
                                            originalUri = Uri.fromFile(outFile),
                                            cropQuad = detectedQuad
                                        )
                                        sessionViewModel.appendPages(listOf(page))
                                        lastCapturedPage = page
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        errorMessage = exception.message ?: "Capture failed."
                                    }
                                }
                            )
                        }
                )

                TextButton(
                    onClick = onProceedToConvert,
                    enabled = sessionViewModel.draftPages.isNotEmpty()
                ) {
                    Text("Next")
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}

package com.pdfmaker.scanner.ui.importscreen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pdfmaker.scanner.data.model.EditablePage
import com.pdfmaker.scanner.navigation.WorkflowSessionViewModel
import com.pdfmaker.scanner.ui.components.EmptyLibraryState
import com.pdfmaker.scanner.ui.components.FacetedHeaderScaffold
import com.pdfmaker.scanner.ui.theme.BrandRed

@Composable
fun ImportScreen(
    sessionViewModel: WorkflowSessionViewModel,
    onBack: () -> Unit,
    onProceedToConvert: () -> Unit
) {
    // Preserve imported URIs and allow explicit deselection while retaining order
    val pickedUris = remember {
        mutableStateListOf<Uri>().apply {
            addAll(sessionViewModel.draftPages.map { it.originalUri })
        }
    }
    val selectedOrder = remember {
        mutableStateListOf<Uri>().apply {
            addAll(pickedUris)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 50)
    ) { uris ->
        uris.forEach { uri ->
            if (!pickedUris.contains(uri)) {
                pickedUris.add(uri)
                selectedOrder.add(uri)
            }
        }
    }

    fun launchPicker() {
        photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    FacetedHeaderScaffold(
        title = "Import",
        onBack = onBack
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "All Images (${selectedOrder.size} selected)",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = ::launchPicker) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(" Pick Photos", modifier = Modifier.padding(start = 4.dp))
                    }
                    if (selectedOrder.isNotEmpty()) {
                        TextButton(onClick = { selectedOrder.clear() }) {
                            Text("Clear")
                        }
                    }
                }
            }

            if (pickedUris.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyLibraryState(
                        title = "Select photos to convert",
                        subtitle = "Uses Android Photo Picker — no broad storage permission required.",
                        primaryLabel = "Choose from Gallery",
                        onPrimaryClick = ::launchPicker
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 104.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(pickedUris, key = { _, uri -> uri.toString() }) { _, uri ->
                        val orderIdx = selectedOrder.indexOf(uri)
                        val isSelected = orderIdx >= 0

                        Box(
                            modifier = Modifier
                                .aspectRatio(0.82f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) BrandRed else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    if (isSelected) {
                                        selectedOrder.remove(uri)
                                    } else {
                                        selectedOrder.add(uri)
                                    }
                                }
                        ) {
                            AsyncImage(
                                model = uri,
                                contentDescription = "Imported photo",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(2.dp)
                            )

                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) BrandRed else Color.Black.copy(alpha = 0.45f))
                                    .border(1.5.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Text(
                                        text = "${orderIdx + 1}",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Not selected",
                                        tint = Color.White.copy(alpha = 0.55f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val pages = selectedOrder.map { uri ->
                        sessionViewModel.draftPages.firstOrNull { it.originalUri == uri }
                            ?: EditablePage(originalUri = uri)
                    }
                    sessionViewModel.setPages(pages)
                    onProceedToConvert()
                },
                enabled = selectedOrder.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = BrandRed),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .height(52.dp)
            ) {
                Text(
                    text = "Import (${selectedOrder.size})",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

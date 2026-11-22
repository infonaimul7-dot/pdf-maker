package com.pdfmaker.scanner.ui.home

import android.content.Intent
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pdfmaker.scanner.ui.components.EmptyLibraryState
import com.pdfmaker.scanner.ui.components.FacetedHeaderScaffold
import com.pdfmaker.scanner.ui.components.formatBytes
import com.pdfmaker.scanner.ui.components.formatRelativeDate
import com.pdfmaker.scanner.ui.theme.BrandRed
import com.pdfmaker.scanner.util.ShareHelper
import java.io.File

@Composable
fun HomeScreen(
    onOpenImageToPdf: () -> Unit,
    onOpenScanner: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenTools: () -> Unit,
    onOpenViewer: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val recentFiles by viewModel.recentFiles.collectAsStateWithLifecycle()
    val context = LocalContext.current

    FacetedHeaderScaffold(title = "PDF Maker") {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DashboardActionCard(
                            title = "Image to PDF",
                            subtitle = "Gallery photos",
                            icon = Icons.Outlined.Image,
                            onClick = onOpenImageToPdf,
                            modifier = Modifier.weight(1f)
                        )
                        DashboardActionCard(
                            title = "Scan Document",
                            subtitle = "Camera & filters",
                            icon = Icons.Outlined.DocumentScanner,
                            onClick = onOpenScanner,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DashboardActionCard(
                            title = "View PDFs",
                            subtitle = "All local files",
                            icon = Icons.Outlined.FolderOpen,
                            onClick = onOpenLibrary,
                            modifier = Modifier.weight(1f)
                        )
                        DashboardActionCard(
                            title = "Compress PDF",
                            subtitle = "Shrink & merge",
                            icon = Icons.Outlined.Compress,
                            onClick = onOpenTools,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    DashboardActionCard(
                        title = "Share PDF",
                        subtitle = if (recentFiles.isNotEmpty()) "Share latest: ${recentFiles.first().fileName}" else "Choose a PDF from your library",
                        icon = Icons.Outlined.Share,
                        onClick = {
                            val latest = recentFiles.firstOrNull()
                            if (latest != null) {
                                val file = File(latest.filePath)
                                if (file.exists()) {
                                    context.startActivity(ShareHelper.createSharePdfIntent(context, file))
                                } else {
                                    onOpenLibrary()
                                }
                            } else {
                                onOpenLibrary()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Files",
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (recentFiles.isNotEmpty()) {
                        TextButton(onClick = onOpenLibrary) {
                            Text("See all")
                        }
                    }
                }
            }

            if (recentFiles.isEmpty()) {
                item {
                    EmptyLibraryState(
                        title = "No PDF files yet",
                        subtitle = "Convert photos or scan a paper document to build your local PDF library.",
                        primaryLabel = "Create First PDF",
                        onPrimaryClick = onOpenImageToPdf
                    )
                }
            } else {
                items(recentFiles, key = { it.id }) { doc ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenViewer(doc.filePath) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
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
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = doc.fileName,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${formatRelativeDate(doc.lastModifiedMillis)} · ${formatBytes(doc.sizeBytes)} · ${doc.pageCount}p",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BrandRed,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

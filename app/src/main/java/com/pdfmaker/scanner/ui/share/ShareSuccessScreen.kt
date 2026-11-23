package com.pdfmaker.scanner.ui.share

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pdfmaker.scanner.data.model.PdfCreationResult
import com.pdfmaker.scanner.ui.components.AdMobBannerAd
import com.pdfmaker.scanner.ui.components.AdMobInterstitialManager
import com.pdfmaker.scanner.ui.components.FacetedHeaderScaffold
import com.pdfmaker.scanner.ui.components.formatBytes
import com.pdfmaker.scanner.ui.theme.BrandRed
import com.pdfmaker.scanner.util.ShareHelper
import java.io.File

@Composable
fun ShareSuccessScreen(
    result: PdfCreationResult?,
    onOpenPdf: (String) -> Unit,
    onReturnHome: () -> Unit
) {
    val context = LocalContext.current

    FacetedHeaderScaffold(
        title = "Share",
        onBack = onReturnHome
    ) {
        if (result == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No PDF summary available.")
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onReturnHome) { Text("Return Home") }
            }
            return@FacetedHeaderScaffold
        }

        val doc = result.document
        var adShownForDoc by rememberSaveable(doc.filePath) { mutableStateOf(false) }
        LaunchedEffect(doc.filePath) {
            if (!adShownForDoc) {
                adShownForDoc = true
                AdMobInterstitialManager.loadAndShow(context)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF16A34A),
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = "PDF Created Successfully",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, tint = BrandRed)
                            Text(
                                text = doc.fileName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "${doc.pageCount} page(s) · ${formatBytes(doc.sizeBytes)} · ${result.compressionLevel.label} compression",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Saved at: ${doc.filePath}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { onOpenPdf(doc.filePath) },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRed),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  Open PDF", style = MaterialTheme.typography.labelLarge)
                }

                OutlinedButton(
                    onClick = {
                        val file = File(doc.filePath)
                        if (file.exists()) {
                            context.startActivity(ShareHelper.createSharePdfIntent(context, file))
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  Share PDF (WhatsApp, Gmail, Drive, Bluetooth)", style = MaterialTheme.typography.labelLarge)
                }

                TextButton(onClick = onReturnHome) {
                    Icon(Icons.Outlined.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  Return to Home")
                }
            }

            AdMobBannerAd(
                modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
            )
        }
    }
}

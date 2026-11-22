package com.pdfmaker.scanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.pdfmaker.scanner.navigation.PdfNavHost
import com.pdfmaker.scanner.ui.theme.PdfMakerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PdfMakerTheme {
                PdfNavHost()
            }
        }
    }
}

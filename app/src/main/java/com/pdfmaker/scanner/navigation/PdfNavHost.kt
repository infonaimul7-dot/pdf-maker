package com.pdfmaker.scanner.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pdfmaker.scanner.ui.convert.ConvertScreen
import com.pdfmaker.scanner.ui.editor.HighQualityScanScreen
import com.pdfmaker.scanner.ui.home.HomeScreen
import com.pdfmaker.scanner.ui.importscreen.ImportScreen
import com.pdfmaker.scanner.ui.library.LibraryScreen
import com.pdfmaker.scanner.ui.scanner.ScannerScreen
import com.pdfmaker.scanner.ui.share.ShareSuccessScreen
import com.pdfmaker.scanner.ui.tools.PdfToolsScreen
import com.pdfmaker.scanner.ui.viewer.PdfViewerScreen

object PdfRoutes {
    const val HOME = "home"
    const val IMPORT = "import"
    const val CONVERT = "convert"
    const val SHARE_SUCCESS = "share_success"
    const val LIBRARY = "library"
    const val SCANNER = "scanner"
    const val HIGH_QUALITY_EDIT = "editor"
    const val TOOLS = "tools"
    const val VIEWER = "viewer/{filePath}"

    fun viewerRoute(filePath: String): String = "viewer/${Uri.encode(filePath)}"
}

@Composable
fun PdfNavHost() {
    val navController = rememberNavController()
    val sessionViewModel: WorkflowSessionViewModel = hiltViewModel()
    val lastNavClickMs = remember { mutableLongStateOf(0L) }

    fun safeNavigate(route: String) {
        val now = System.currentTimeMillis()
        if (now - lastNavClickMs.longValue < 350L) return
        lastNavClickMs.longValue = now
        navController.navigate(route) {
            launchSingleTop = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = PdfRoutes.HOME
    ) {
        composable(PdfRoutes.HOME) {
            HomeScreen(
                onOpenImageToPdf = { safeNavigate(PdfRoutes.IMPORT) },
                onOpenScanner = { safeNavigate(PdfRoutes.SCANNER) },
                onOpenLibrary = { safeNavigate(PdfRoutes.LIBRARY) },
                onOpenTools = { safeNavigate(PdfRoutes.TOOLS) },
                onOpenViewer = { path -> safeNavigate(PdfRoutes.viewerRoute(path)) }
            )
        }

        composable(PdfRoutes.IMPORT) {
            ImportScreen(
                sessionViewModel = sessionViewModel,
                onBack = { navController.popBackStack() },
                onProceedToConvert = { safeNavigate(PdfRoutes.CONVERT) }
            )
        }

        composable(PdfRoutes.CONVERT) {
            ConvertScreen(
                sessionViewModel = sessionViewModel,
                onBack = { navController.popBackStack() },
                onAddMoreImages = { safeNavigate(PdfRoutes.IMPORT) },
                onEditPage = { pageId ->
                    sessionViewModel.activeEditPageId.value = pageId
                    safeNavigate(PdfRoutes.HIGH_QUALITY_EDIT)
                },
                onPdfCreated = { result ->
                    sessionViewModel.lastCreationResult.value = result
                    navController.navigate(PdfRoutes.SHARE_SUCCESS) {
                        popUpTo(PdfRoutes.HOME)
                    }
                }
            )
        }

        composable(PdfRoutes.SHARE_SUCCESS) {
            ShareSuccessScreen(
                result = sessionViewModel.lastCreationResult.value,
                onOpenPdf = { path -> safeNavigate(PdfRoutes.viewerRoute(path)) },
                onReturnHome = {
                    sessionViewModel.clearDraft()
                    navController.popBackStack(PdfRoutes.HOME, inclusive = false)
                }
            )
        }

        composable(PdfRoutes.LIBRARY) {
            LibraryScreen(
                onBack = { navController.popBackStack() },
                onOpenPdf = { path -> safeNavigate(PdfRoutes.viewerRoute(path)) },
                onCreateFromImages = { safeNavigate(PdfRoutes.IMPORT) },
                onScanDocument = { safeNavigate(PdfRoutes.SCANNER) }
            )
        }

        composable(PdfRoutes.SCANNER) {
            ScannerScreen(
                sessionViewModel = sessionViewModel,
                onBack = { navController.popBackStack() },
                onEditLastScan = { pageId ->
                    sessionViewModel.activeEditPageId.value = pageId
                    safeNavigate(PdfRoutes.HIGH_QUALITY_EDIT)
                },
                onProceedToConvert = { safeNavigate(PdfRoutes.CONVERT) }
            )
        }

        composable(PdfRoutes.HIGH_QUALITY_EDIT) {
            HighQualityScanScreen(
                sessionViewModel = sessionViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(PdfRoutes.TOOLS) {
            PdfToolsScreen(
                onBack = { navController.popBackStack() },
                onOpenPdf = { path -> safeNavigate(PdfRoutes.viewerRoute(path)) }
            )
        }

        composable(
            route = PdfRoutes.VIEWER,
            arguments = listOf(navArgument("filePath") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("filePath").orEmpty()
            val filePath = Uri.decode(encodedPath)
            PdfViewerScreen(
                filePath = filePath,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

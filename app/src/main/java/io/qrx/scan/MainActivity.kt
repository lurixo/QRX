package io.qrx.scan

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.qrx.scan.ui.animation.MD3Transitions
import io.qrx.scan.ui.screens.BarcodeGenerateScreen
import io.qrx.scan.ui.screens.HistoryScreen
import io.qrx.scan.ui.screens.QRCodeGenerateScreen
import io.qrx.scan.ui.screens.ScanScreen
import io.qrx.scan.ui.theme.QRXTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val isDark = isSystemInDarkTheme()

            LaunchedEffect(isDark) {
                setupSystemBars(isDark)
            }

            QRXTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    QRXApp()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val isDark = resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        setupSystemBars(isDark)
    }

    private fun setupSystemBars(isDark: Boolean) {
        if (isDark) {
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
            )
        } else {
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
            )
        }
    }
}

@Composable
fun QRXApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "scan",
        enterTransition = { MD3Transitions.containerTransformIn() },
        exitTransition = { MD3Transitions.containerTransformOut() },
        popEnterTransition = { MD3Transitions.containerTransformIn() },
        popExitTransition = { MD3Transitions.containerTransformOut() }
    ) {
        composable("scan") {
            ScanScreen(
                onNavigateToHistory = { navController.navigate("history") },
                onNavigateToQRGenerate = { navController.navigate("qr_generate") },
                onNavigateToBarcodeGenerate = { navController.navigate("barcode_generate") }
            )
        }
        composable("history") {
            HistoryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("qr_generate") {
            QRCodeGenerateScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("barcode_generate") {
            BarcodeGenerateScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

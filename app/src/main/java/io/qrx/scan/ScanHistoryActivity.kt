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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import io.qrx.scan.data.ScanSource
import io.qrx.scan.ui.screens.ScanHistoryListScreen
import io.qrx.scan.ui.theme.QRXTheme

class ScanHistoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val isDark = resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        setupSystemBars(isDark)

        val source = intent.getStringExtra(EXTRA_SOURCE)
            ?.let { runCatching { ScanSource.valueOf(it) }.getOrNull() }
            ?: ScanSource.CAMERA

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
                    ScanHistoryListScreen(
                        source = source,
                        onNavigateBack = { finish() }
                    )
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

    companion object {
        const val EXTRA_SOURCE = "extra_source"
    }
}

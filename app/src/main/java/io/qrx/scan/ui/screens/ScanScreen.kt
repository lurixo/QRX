package io.qrx.scan.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.ViewWeek
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.qrx.scan.R
import io.qrx.scan.ScanActivity
import io.qrx.scan.ui.animation.MD3StateAnimations
import io.qrx.scan.ui.components.MD3ActionButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    onNavigateToHistory: () -> Unit,
    onNavigateToImageScan: () -> Unit,
    onNavigateToQRGenerate: () -> Unit,
    onNavigateToBarcodeGenerate: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("QRX") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(
                            Icons.Default.History,
                            stringResource(R.string.history),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedVisibility(
                visible = true,
                enter = MD3StateAnimations.emptyStateEnter(),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.size(120.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.scan_or_generate),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.scan_batch_support),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        MD3ActionButton(
                            icon = Icons.Default.QrCodeScanner,
                            text = stringResource(R.string.scan),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                context.startActivity(Intent(context, ScanActivity::class.java))
                            }
                        )
                        MD3ActionButton(
                            icon = Icons.Outlined.Photo,
                            text = stringResource(R.string.select_image),
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToImageScan
                        )
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Row(modifier = Modifier.fillMaxWidth()) {
                        MD3ActionButton(
                            icon = Icons.Default.QrCode2,
                            text = stringResource(R.string.generate_qrcode),
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToQRGenerate
                        )
                        MD3ActionButton(
                            icon = Icons.Outlined.ViewWeek,
                            text = stringResource(R.string.generate_barcode),
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToBarcodeGenerate
                        )
                    }
                }
            }
        }
    }
}

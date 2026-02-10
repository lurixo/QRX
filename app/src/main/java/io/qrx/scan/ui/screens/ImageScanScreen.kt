package io.qrx.scan.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import io.qrx.scan.QRXApplication
import io.qrx.scan.R
import io.qrx.scan.data.ScanHistoryEntity
import io.qrx.scan.data.ScanSource
import io.qrx.scan.ui.animation.MD3ListAnimations
import io.qrx.scan.ui.animation.MD3StateAnimations
import io.qrx.scan.ui.animation.MD3Transitions
import io.qrx.scan.ui.components.MD3PressableSurface
import io.qrx.scan.ui.components.QRXSnackbar
import io.qrx.scan.ui.components.ScanResultCard
import io.qrx.scan.ui.components.SnackbarData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class ScanResult(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val codes: List<String> = emptyList(),
    val isProcessing: Boolean = false,
    val error: String? = null,
    val savedPath: String? = null,
    val historyId: Long? = null
)

fun copyImageToInternal(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val fileName = "qrx_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg"
        val file = File(context.filesDir, fileName)
        file.outputStream().use { output ->
            inputStream.copyTo(output)
        }
        inputStream.close()
        file.absolutePath
    } catch (e: Exception) {
        null
    }
}

@Suppress("LocalContextGetResourceValueCall")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageScanScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val database = (context.applicationContext as QRXApplication).database

    val strImagesAlreadySelected = stringResource(R.string.images_already_selected)
    val strRecognizeFailed = stringResource(R.string.recognize_failed)
    val strProcessFailed = stringResource(R.string.process_failed)
    val strProcessImageError = stringResource(R.string.process_image_error)

    var scanResults by remember { mutableStateOf<List<ScanResult>>(emptyList()) }
    var isScanning by remember { mutableStateOf(false) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var snackbarData by remember { mutableStateOf<SnackbarData?>(null) }
    var hasLaunchedPicker by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = isSelectionMode) {
        isSelectionMode = false
        selectedIds = emptySet()
    }

    fun processImages(uris: List<Uri>) {
        if (uris.isEmpty()) return
        isScanning = true

        val existingUris = scanResults.map { it.uri }.toSet()
        val newUris = uris.filter { it !in existingUris }

        if (newUris.isEmpty()) {
            snackbarData = SnackbarData(strImagesAlreadySelected, false)
            isScanning = false
            return
        }

        val newResults = newUris.map { uri ->
            ScanResult(id = UUID.randomUUID().toString(), uri = uri, isProcessing = true)
        }
        val uriToIdMap = newResults.associate { it.uri to it.id }

        scanResults = scanResults + newResults

        scope.launch {
            try {
                val scanner = BarcodeScanning.getClient()
                val processedCodeSets = scanResults
                    .filter { !it.isProcessing && it.codes.isNotEmpty() }
                    .map { it.codes.toSet() }
                    .toMutableList()

                for (uri in newUris) {
                    val itemId = uriToIdMap[uri] ?: continue

                    try {
                        val processedResult = withContext(Dispatchers.IO) {
                            try {
                                val image = InputImage.fromFilePath(context, uri)
                                val barcodes = scanner.process(image).await()
                                val codes = barcodes.mapNotNull { barcode -> barcode.rawValue }
                                val savedPath = copyImageToInternal(context, uri)
                                ScanResult(id = itemId, uri = uri, codes = codes, savedPath = savedPath)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                ScanResult(id = itemId, uri = uri, error = e.message ?: strRecognizeFailed)
                            }
                        }

                        scanResults = scanResults.map { existing ->
                            if (existing.id == itemId) processedResult else existing
                        }

                        if (processedResult.codes.isNotEmpty()) {
                            val currentCodeSet = processedResult.codes.toSet()
                            val isDuplicate = processedCodeSets.any { it == currentCodeSet }

                            if (isDuplicate) {
                                scanResults = scanResults.filter { it.id != itemId }
                            } else {
                                processedCodeSets.add(currentCodeSet)
                                try {
                                    val historyId = withContext(Dispatchers.IO) {
                                        database.scanHistoryDao().insert(
                                            ScanHistoryEntity(
                                                codes = processedResult.codes,
                                                imageUri = processedResult.savedPath ?: uri.toString(),
                                                source = ScanSource.IMAGE
                                            )
                                        )
                                    }
                                    scanResults = scanResults.map { existing ->
                                        if (existing.id == itemId) existing.copy(historyId = historyId) else existing
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        scanResults = scanResults.map { existing ->
                            if (existing.id == itemId) ScanResult(id = itemId, uri = uri, error = strProcessFailed) else existing
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                snackbarData = SnackbarData(strProcessImageError, false)
            } finally {
                isScanning = false
            }
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(100)
    ) { uris ->
        if (uris.isEmpty()) {
            if (scanResults.isEmpty()) {
                onNavigateBack()
            }
            return@rememberLauncherForActivityResult
        }
        processImages(uris)
    }

    LaunchedEffect(Unit) {
        if (!hasLaunchedPicker) {
            hasLaunchedPicker = true
            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    fun exitSelectionMode() {
        isSelectionMode = false
        selectedIds = emptySet()
    }

    fun deleteSelected() {
        val itemsToDelete = scanResults.filter { it.id in selectedIds }
        scope.launch {
            withContext(Dispatchers.IO) {
                itemsToDelete.forEach { result ->
                    result.historyId?.let { id ->
                        database.scanHistoryDao().deleteById(id)
                    }
                    result.savedPath?.let { path ->
                        try { File(path).delete() } catch (_: Exception) {}
                    }
                }
            }
        }
        scanResults = scanResults.filter { it.id !in selectedIds }
        exitSelectionMode()
    }

    fun deleteItem(result: ScanResult) {
        scope.launch {
            withContext(Dispatchers.IO) {
                result.historyId?.let { id ->
                    database.scanHistoryDao().deleteById(id)
                }
                result.savedPath?.let { path ->
                    try { File(path).delete() } catch (_: Exception) {}
                }
            }
        }
        scanResults = scanResults.filter { it.id != result.id }
        if (scanResults.isEmpty()) {
            isSelectionMode = false
            selectedIds = emptySet()
        }
    }

    fun copySelected() {
        val codes = scanResults
            .filter { it.id in selectedIds }
            .flatMap { it.codes }
            .distinct()
        if (codes.isNotEmpty()) {
            clipboardManager.setText(AnnotatedString(codes.joinToString("\n")))
            snackbarData = SnackbarData(context.getString(R.string.copied_results, codes.size), true)
        }
        exitSelectionMode()
    }

    fun copyAll() {
        val codes = scanResults
            .flatMap { it.codes }
            .distinct()
        if (codes.isNotEmpty()) {
            clipboardManager.setText(AnnotatedString(codes.joinToString("\n")))
            snackbarData = SnackbarData(context.getString(R.string.copied_results, codes.size), true)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            if (isSelectionMode) stringResource(R.string.selected_count, selectedIds.size)
                            else stringResource(R.string.image_scan)
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                    navigationIcon = {
                        IconButton(onClick = { if (isSelectionMode) exitSelectionMode() else onNavigateBack() }) {
                            Icon(
                                if (isSelectionMode) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                                stringResource(R.string.back)
                            )
                        }
                    },
                    actions = {
                        if (isSelectionMode) {
                            IconButton(onClick = { selectedIds = scanResults.filter { !it.isProcessing }.map { it.id }.toSet() }) {
                                Icon(Icons.Default.SelectAll, stringResource(R.string.select_all), tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { copySelected() }, enabled = selectedIds.isNotEmpty()) {
                                Icon(Icons.Outlined.ContentCopy, stringResource(R.string.copy), tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { deleteSelected() }, enabled = selectedIds.isNotEmpty()) {
                                Icon(Icons.Default.Delete, stringResource(R.string.delete), tint = MaterialTheme.colorScheme.primary)
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }
                            ) {
                                Icon(Icons.Outlined.Photo, stringResource(R.string.select_image), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                )
            },
            floatingActionButton = {
                if (!isSelectionMode && scanResults.any { it.codes.isNotEmpty() }) {
                    MD3PressableSurface(
                        onClick = { copyAll() },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            tonalElevation = 2.dp,
                            shadowElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.ContentCopy,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.copy_all),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            AnimatedContent(
                targetState = scanResults.isEmpty() && !isScanning,
                transitionSpec = { MD3Transitions.fadeThrough() },
                label = "imageScanContentTransition"
            ) { isEmpty ->
                if (isEmpty) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedVisibility(
                            visible = true,
                            enter = MD3StateAnimations.emptyStateEnter()
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Outlined.Photo,
                                    contentDescription = null,
                                    modifier = Modifier.size(100.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(
                                    text = stringResource(R.string.no_barcode_found),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(paddingValues),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (!isScanning && scanResults.isNotEmpty() && !isSelectionMode) {
                            item {
                                StatisticsCard(scanResults)
                            }
                        }

                        itemsIndexed(scanResults, key = { _, it -> it.id }) { index, result ->
                            ScanResultCard(
                                result = result,
                                clipboardManager = clipboardManager,
                                context = context,
                                onDelete = { deleteItem(result) },
                                isSelectionMode = isSelectionMode,
                                isSelected = result.id in selectedIds,
                                onToggleSelect = {
                                    selectedIds = if (result.id in selectedIds) {
                                        selectedIds - result.id
                                    } else {
                                        selectedIds + result.id
                                    }
                                },
                                onLongPress = {
                                    if (!isSelectionMode && !result.isProcessing) {
                                        isSelectionMode = true
                                        selectedIds = setOf(result.id)
                                    }
                                },
                                onShowSnackbar = { message, isSuccess ->
                                    snackbarData = SnackbarData(message, isSuccess)
                                },
                                modifier = Modifier.animateItem(
                                    fadeInSpec = MD3ListAnimations.fadeInSpec(index),
                                    fadeOutSpec = MD3ListAnimations.fadeOutSpec(),
                                    placementSpec = MD3ListAnimations.placementSpec()
                                )
                            )
                        }

                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }

        QRXSnackbar(
            snackbarData = snackbarData,
            onDismiss = { snackbarData = null }
        )
    }
}

@Composable
fun StatisticsCard(results: List<ScanResult>) {
    val totalImages = results.size
    val successCount = results.count { it.codes.isNotEmpty() }
    val totalCodes = results.flatMap { it.codes }.distinct().size

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(stringResource(R.string.stat_images), totalImages.toString())
            StatItem(stringResource(R.string.stat_success), successCount.toString())
            StatItem(stringResource(R.string.stat_codes), totalCodes.toString())
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

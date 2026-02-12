package io.qrx.scan.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ViewWeek
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Size
import io.qrx.scan.QRXApplication
import io.qrx.scan.R
import io.qrx.scan.data.GenerateHistoryEntity
import io.qrx.scan.data.GenerateType
import io.qrx.scan.data.ScanSource
import io.qrx.scan.ui.animation.rememberRubberBandOverscrollEffect
import io.qrx.scan.ui.animation.rubberBandOffset
import io.qrx.scan.ui.animation.MD3FabAnimations
import io.qrx.scan.ui.animation.MD3ListAnimations
import io.qrx.scan.ui.animation.MD3Motion
import io.qrx.scan.ui.animation.MD3StateAnimations
import io.qrx.scan.ui.animation.MD3Transitions
import io.qrx.scan.ui.components.HistoryCard
import io.qrx.scan.ui.components.MD3PressableSurface
import io.qrx.scan.ui.components.MD3SelectionIcon
import io.qrx.scan.ui.components.QRXSnackbar
import io.qrx.scan.ui.components.SnackbarData
import io.qrx.scan.util.formatTimestamp
import io.qrx.scan.util.saveToGalleryOnly
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryMainScreen(
    onNavigateBack: () -> Unit,
    onSelectScanSource: (ScanSource) -> Unit,
    onSelectGenerateType: (GenerateType) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = (context.applicationContext as QRXApplication).database

    val cameraCount by database.scanHistoryDao().getCountBySource(ScanSource.CAMERA).collectAsState(initial = 0)
    val imageCount by database.scanHistoryDao().getCountBySource(ScanSource.IMAGE).collectAsState(initial = 0)
    val qrCodeCount by database.generateHistoryDao().getCountByType(GenerateType.QR_CODE).collectAsState(initial = 0)
    val barcodeCount by database.generateHistoryDao().getCountByType(GenerateType.BARCODE).collectAsState(initial = 0)

    val scanHistoryList by database.scanHistoryDao().getAllHistory().collectAsState(initial = emptyList())
    val generateHistoryList by database.generateHistoryDao().getAll().collectAsState(initial = emptyList())

    val totalCount = cameraCount + imageCount + qrCodeCount + barcodeCount
    var snackbarData by remember { mutableStateOf<SnackbarData?>(null) }

    fun deleteAll() {
        scope.launch {
            withContext(Dispatchers.IO) {
                scanHistoryList.forEach { try { File(it.imageUri).delete() } catch (_: Exception) {} }
                generateHistoryList.forEach { try { File(it.imagePath).delete() } catch (_: Exception) {} }
                database.scanHistoryDao().deleteAll()
                database.generateHistoryDao().deleteAll()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.history)) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                        }
                    },
                    actions = {
                        if (totalCount > 0) {
                            IconButton(onClick = { deleteAll() }) {
                                Icon(Icons.Default.Delete, stringResource(R.string.clear), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                )
            }
        ) { paddingValues ->
            AnimatedContent(
                targetState = totalCount == 0,
                transitionSpec = { MD3Transitions.fadeThrough() },
                label = "historyContentTransition"
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
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(100.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(
                                    text = stringResource(R.string.no_history_plain),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                } else {
                    val rubberBandOverscroll = rememberRubberBandOverscrollEffect()
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(paddingValues).rubberBandOffset(rubberBandOverscroll),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        overscrollEffect = rubberBandOverscroll
                    ) {
                        item {
                            Text(
                                text = stringResource(R.string.scan_history),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column {
                                    CategoryListItem(
                                        icon = Icons.Default.CameraAlt,
                                        title = stringResource(R.string.camera_scan),
                                        count = cameraCount,
                                        onClick = { onSelectScanSource(ScanSource.CAMERA) }
                                    )
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                    CategoryListItem(
                                        icon = Icons.Default.Photo,
                                        title = stringResource(R.string.image_scan),
                                        count = imageCount,
                                        onClick = { onSelectScanSource(ScanSource.IMAGE) }
                                    )
                                }
                            }
                        }

                        item {
                            Text(
                                text = stringResource(R.string.history),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                            )
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column {
                                    CategoryListItem(
                                        icon = Icons.Default.QrCode2,
                                        title = stringResource(R.string.qrcode_generate),
                                        count = qrCodeCount,
                                        onClick = { onSelectGenerateType(GenerateType.QR_CODE) }
                                    )
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                    CategoryListItem(
                                        icon = Icons.Outlined.ViewWeek,
                                        title = stringResource(R.string.barcode_generate),
                                        count = barcodeCount,
                                        onClick = { onSelectGenerateType(GenerateType.BARCODE) }
                                    )
                                }
                            }
                        }
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
fun CategoryListItem(
    icon: ImageVector,
    title: String,
    count: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.record_count, count),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline
        )
    }
}

@Suppress("LocalContextGetResourceValueCall")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanHistoryListScreen(
    source: ScanSource,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val database = (context.applicationContext as QRXApplication).database

    val historyList by database.scanHistoryDao().getHistoryBySource(source).collectAsState(initial = emptyList())
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var snackbarData by remember { mutableStateOf<SnackbarData?>(null) }

    val title = stringResource(if (source == ScanSource.CAMERA) R.string.camera_scan else R.string.image_scan)

    BackHandler(enabled = isSelectionMode) {
        isSelectionMode = false
        selectedIds = emptySet()
    }

    fun exitSelectionMode() { isSelectionMode = false; selectedIds = emptySet() }

    fun deleteAll() {
        scope.launch {
            withContext(Dispatchers.IO) {
                historyList.forEach { try { File(it.imageUri).delete() } catch (_: Exception) {} }
                database.scanHistoryDao().deleteBySource(source)
            }
        }
    }

    fun deleteSelected() {
        val itemsToDelete = historyList.filter { it.id in selectedIds }
        scope.launch {
            withContext(Dispatchers.IO) {
                itemsToDelete.forEach { try { File(it.imageUri).delete() } catch (_: Exception) {} }
                itemsToDelete.forEach { database.scanHistoryDao().delete(it) }
            }
        }
        exitSelectionMode()
    }

    fun copySelected() {
        val selectedItems = historyList.filter { it.id in selectedIds }
        val allCodes = selectedItems.flatMap { it.codes }.distinct()
        if (allCodes.isNotEmpty()) {
            clipboardManager.setText(AnnotatedString(allCodes.joinToString("\n")))
            snackbarData = SnackbarData(context.getString(R.string.copied_results, allCodes.size), true)
        }
        exitSelectionMode()
    }

    fun saveSelectedToGallery() {
        val itemsToSave = historyList.filter { it.id in selectedIds }
        if (itemsToSave.isEmpty()) return

        scope.launch {
            var savedCount = 0
            itemsToSave.forEach { item ->
                val saved = withContext(Dispatchers.IO) {
                    try {
                        val file = File(item.imageUri)
                        if (file.exists()) {
                            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                            if (bitmap != null) {
                                saveToGalleryOnly(context, bitmap, "QRX_${System.currentTimeMillis()}")
                            } else false
                        } else false
                    } catch (e: Exception) {
                        false
                    }
                }
                if (saved) savedCount++
            }

            withContext(Dispatchers.Main) {
                if (savedCount > 0) {
                    snackbarData = SnackbarData(context.getString(R.string.saved_items_to_gallery, savedCount), true)
                } else {
                    snackbarData = SnackbarData(context.getString(R.string.save_failed), false)
                }
            }
            exitSelectionMode()
        }
    }

    fun saveAllToGallery() {
        if (historyList.isEmpty()) return

        scope.launch {
            var savedCount = 0
            historyList.forEach { item ->
                val saved = withContext(Dispatchers.IO) {
                    try {
                        val file = File(item.imageUri)
                        if (file.exists()) {
                            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                            if (bitmap != null) {
                                saveToGalleryOnly(context, bitmap, "QRX_${System.currentTimeMillis()}")
                            } else false
                        } else false
                    } catch (e: Exception) {
                        false
                    }
                }
                if (saved) savedCount++
            }

            withContext(Dispatchers.Main) {
                if (savedCount > 0) {
                    snackbarData = SnackbarData(context.getString(R.string.saved_items_to_gallery, savedCount), true)
                } else {
                    snackbarData = SnackbarData(context.getString(R.string.save_failed), false)
                }
            }
        }
    }

    fun saveSingleItem(imageUri: String) {
        scope.launch {
            val saved = withContext(Dispatchers.IO) {
                try {
                    val file = File(imageUri)
                    if (file.exists()) {
                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        if (bitmap != null) {
                            saveToGalleryOnly(context, bitmap, "QRX_${System.currentTimeMillis()}")
                        } else false
                    } else false
                } catch (e: Exception) {
                    false
                }
            }

            withContext(Dispatchers.Main) {
                if (saved) {
                    snackbarData = SnackbarData(context.getString(R.string.saved_to_gallery), true)
                } else {
                    snackbarData = SnackbarData(context.getString(R.string.save_failed), false)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (isSelectionMode) stringResource(R.string.selected_count, selectedIds.size) else title) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                    navigationIcon = {
                        IconButton(onClick = { if (isSelectionMode) exitSelectionMode() else onNavigateBack() }) {
                            Icon(if (isSelectionMode) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                        }
                    },
                    actions = {
                        AnimatedContent(
                            targetState = isSelectionMode,
                            transitionSpec = { MD3Transitions.fadeThrough() },
                            label = "scanHistoryActionsTransition"
                        ) { selectionMode ->
                            Row {
                                if (selectionMode) {
                                    IconButton(onClick = { selectedIds = historyList.map { it.id }.toSet() }) {
                                        Icon(Icons.Default.SelectAll, stringResource(R.string.select_all), tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { copySelected() }, enabled = selectedIds.isNotEmpty()) {
                                        Icon(Icons.Outlined.ContentCopy, stringResource(R.string.copy), tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { saveSelectedToGallery() }, enabled = selectedIds.isNotEmpty()) {
                                        Icon(Icons.Default.Save, stringResource(R.string.save), tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { deleteSelected() }, enabled = selectedIds.isNotEmpty()) {
                                        Icon(Icons.Default.Delete, stringResource(R.string.delete), tint = MaterialTheme.colorScheme.primary)
                                    }
                                } else if (historyList.isNotEmpty()) {
                                    IconButton(onClick = { deleteAll() }) {
                                        Icon(Icons.Default.Delete, stringResource(R.string.clear), tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                )
            },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = !isSelectionMode && historyList.isNotEmpty(),
                    enter = MD3FabAnimations.enter(),
                    exit = MD3FabAnimations.exit()
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MD3PressableSurface(
                            onClick = {
                                val allCodes = historyList.flatMap { it.codes }.distinct()
                                clipboardManager.setText(AnnotatedString(allCodes.joinToString("\n")))
                                snackbarData = SnackbarData(context.getString(R.string.copied_results, allCodes.size), true)
                            },
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
                        MD3PressableSurface(
                            onClick = { saveAllToGallery() },
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
                                        Icons.Default.Save,
                                        null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        stringResource(R.string.save_all),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            AnimatedContent(
                targetState = historyList.isEmpty(),
                transitionSpec = { MD3Transitions.fadeThrough() },
                label = "scanHistoryContentTransition"
            ) { isEmpty ->
                if (isEmpty) {
                    Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                        AnimatedVisibility(
                            visible = true,
                            enter = MD3StateAnimations.emptyStateEnter()
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = if (source == ScanSource.CAMERA) Icons.Default.CameraAlt else Icons.Default.Photo,
                                    contentDescription = null, modifier = Modifier.size(100.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(stringResource(R.string.no_history_for_type, title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                } else {
                    val rubberBandOverscroll = rememberRubberBandOverscrollEffect()
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(paddingValues).rubberBandOffset(rubberBandOverscroll),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        overscrollEffect = rubberBandOverscroll
                    ) {
                        itemsIndexed(historyList, key = { _, it -> it.id }) { index, history ->
                            HistoryCard(
                                imageUri = history.imageUri,
                                codes = history.codes,
                                timestamp = history.timestamp,
                                clipboardManager = clipboardManager,
                                context = context,
                                onDelete = {
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            try { File(history.imageUri).delete() } catch (_: Exception) {}
                                            database.scanHistoryDao().delete(history)
                                        }
                                    }
                                },
                                onSave = { saveSingleItem(history.imageUri) },
                                onCopy = {
                                    clipboardManager.setText(AnnotatedString(history.codes.joinToString("\n")))
                                    snackbarData = SnackbarData(context.getString(R.string.copied_results, history.codes.size), true)
                                },
                                isSelectionMode = isSelectionMode,
                                isSelected = history.id in selectedIds,
                                onToggleSelect = { selectedIds = if (history.id in selectedIds) selectedIds - history.id else selectedIds + history.id },
                                onLongPress = { if (!isSelectionMode) { isSelectionMode = true; selectedIds = setOf(history.id) } },
                                onShowSnackbar = { message, isSuccess -> snackbarData = SnackbarData(message, isSuccess) },
                                modifier = Modifier.animateItem(
                                    fadeInSpec = MD3ListAnimations.fadeInSpec(index),
                                    fadeOutSpec = MD3ListAnimations.fadeOutSpec(),
                                    placementSpec = MD3ListAnimations.placementSpec()
                                )
                            )
                        }
                        item { Spacer(modifier = Modifier.height(150.dp)) }
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

@Suppress("LocalContextGetResourceValueCall")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateHistoryListScreen(
    type: GenerateType,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val database = (context.applicationContext as QRXApplication).database

    val historyList by database.generateHistoryDao().getByType(type).collectAsState(initial = emptyList())
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var expandedContentIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var snackbarData by remember { mutableStateOf<SnackbarData?>(null) }

    val title = if (type == GenerateType.QR_CODE) stringResource(R.string.qrcode_generate) else stringResource(R.string.barcode_generate)

    BackHandler(enabled = isSelectionMode) {
        isSelectionMode = false
        selectedIds = emptySet()
    }

    fun exitSelectionMode() { isSelectionMode = false; selectedIds = emptySet() }

    fun deleteAll() {
        scope.launch {
            withContext(Dispatchers.IO) {
                historyList.forEach { try { File(it.imagePath).delete() } catch (_: Exception) {} }
                database.generateHistoryDao().deleteByType(type)
            }
        }
    }

    fun deleteSelected() {
        val itemsToDelete = historyList.filter { it.id in selectedIds }
        scope.launch {
            withContext(Dispatchers.IO) {
                itemsToDelete.forEach { try { File(it.imagePath).delete() } catch (_: Exception) {} }
                itemsToDelete.forEach { database.generateHistoryDao().delete(it) }
            }
        }
        exitSelectionMode()
    }

    fun copySelected() {
        val selectedItems = historyList.filter { it.id in selectedIds }
        val allContent = selectedItems.map { it.content }.distinct()
        if (allContent.isNotEmpty()) {
            clipboardManager.setText(AnnotatedString(allContent.joinToString("\n")))
            snackbarData = SnackbarData(context.getString(R.string.copied_results, allContent.size), true)
        }
        exitSelectionMode()
    }

    fun saveSelectedToGallery() {
        val itemsToSave = historyList.filter { it.id in selectedIds }
        if (itemsToSave.isEmpty()) return

        scope.launch {
            var savedCount = 0
            itemsToSave.forEach { item ->
                val saved = withContext(Dispatchers.IO) {
                    try {
                        val file = File(item.imagePath)
                        if (file.exists()) {
                            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                            if (bitmap != null) {
                                saveToGalleryOnly(context, bitmap, "QRX_${System.currentTimeMillis()}")
                            } else false
                        } else false
                    } catch (e: Exception) {
                        false
                    }
                }
                if (saved) savedCount++
            }

            withContext(Dispatchers.Main) {
                if (savedCount > 0) {
                    snackbarData = SnackbarData(context.getString(R.string.saved_items_to_gallery, savedCount), true)
                } else {
                    snackbarData = SnackbarData(context.getString(R.string.save_failed), false)
                }
            }
            exitSelectionMode()
        }
    }

    fun saveAllToGallery() {
        if (historyList.isEmpty()) return

        scope.launch {
            var savedCount = 0
            historyList.forEach { item ->
                val saved = withContext(Dispatchers.IO) {
                    try {
                        val file = File(item.imagePath)
                        if (file.exists()) {
                            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                            if (bitmap != null) {
                                saveToGalleryOnly(context, bitmap, "QRX_${System.currentTimeMillis()}")
                            } else false
                        } else false
                    } catch (e: Exception) {
                        false
                    }
                }
                if (saved) savedCount++
            }

            withContext(Dispatchers.Main) {
                if (savedCount > 0) {
                    snackbarData = SnackbarData(context.getString(R.string.saved_items_to_gallery, savedCount), true)
                } else {
                    snackbarData = SnackbarData(context.getString(R.string.save_failed), false)
                }
            }
        }
    }

    fun saveSingleItem(item: GenerateHistoryEntity) {
        scope.launch {
            val saved = withContext(Dispatchers.IO) {
                try {
                    val file = File(item.imagePath)
                    if (file.exists()) {
                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        if (bitmap != null) {
                            saveToGalleryOnly(context, bitmap, "QRX_${System.currentTimeMillis()}")
                        } else false
                    } else false
                } catch (e: Exception) {
                    false
                }
            }

            withContext(Dispatchers.Main) {
                if (saved) {
                    snackbarData = SnackbarData(context.getString(R.string.saved_to_gallery), true)
                } else {
                    snackbarData = SnackbarData(context.getString(R.string.save_failed), false)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (isSelectionMode) stringResource(R.string.selected_count, selectedIds.size) else title) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                    navigationIcon = {
                        IconButton(onClick = { if (isSelectionMode) exitSelectionMode() else onNavigateBack() }) {
                            Icon(if (isSelectionMode) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                        }
                    },
                    actions = {
                        AnimatedContent(
                            targetState = isSelectionMode,
                            transitionSpec = { MD3Transitions.fadeThrough() },
                            label = "generateHistoryActionsTransition"
                        ) { selectionMode ->
                            Row {
                                if (selectionMode) {
                                    IconButton(onClick = { selectedIds = historyList.map { it.id }.toSet() }) {
                                        Icon(Icons.Default.SelectAll, stringResource(R.string.select_all), tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { copySelected() }, enabled = selectedIds.isNotEmpty()) {
                                        Icon(Icons.Outlined.ContentCopy, stringResource(R.string.copy), tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { saveSelectedToGallery() }, enabled = selectedIds.isNotEmpty()) {
                                        Icon(Icons.Default.Save, stringResource(R.string.save), tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { deleteSelected() }, enabled = selectedIds.isNotEmpty()) {
                                        Icon(Icons.Default.Delete, stringResource(R.string.delete), tint = MaterialTheme.colorScheme.primary)
                                    }
                                } else if (historyList.isNotEmpty()) {
                                    IconButton(onClick = { deleteAll() }) {
                                        Icon(Icons.Default.Delete, stringResource(R.string.clear), tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                )
            },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = !isSelectionMode && historyList.isNotEmpty(),
                    enter = MD3FabAnimations.enter(),
                    exit = MD3FabAnimations.exit()
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MD3PressableSurface(
                            onClick = {
                                val allContent = historyList.map { it.content }.distinct()
                                clipboardManager.setText(AnnotatedString(allContent.joinToString("\n")))
                                snackbarData = SnackbarData(context.getString(R.string.copied_results, allContent.size), true)
                            },
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
                        MD3PressableSurface(
                            onClick = { saveAllToGallery() },
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
                                        Icons.Default.Save,
                                        null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        stringResource(R.string.save_all),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            AnimatedContent(
                targetState = historyList.isEmpty(),
                transitionSpec = { MD3Transitions.fadeThrough() },
                label = "generateHistoryContentTransition"
            ) { isEmpty ->
                if (isEmpty) {
                    Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                        AnimatedVisibility(
                            visible = true,
                            enter = MD3StateAnimations.emptyStateEnter()
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = if (type == GenerateType.QR_CODE) Icons.Default.QrCode2 else Icons.Outlined.ViewWeek,
                                    contentDescription = null, modifier = Modifier.size(100.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(stringResource(R.string.no_history_for_type, title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                } else {
                    val rubberBandOverscroll = rememberRubberBandOverscrollEffect()
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(paddingValues).rubberBandOffset(rubberBandOverscroll),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        overscrollEffect = rubberBandOverscroll
                    ) {
                        itemsIndexed(historyList, key = { _, it -> it.id }) { index, history ->
                            GenerateHistoryCard(
                                history = history,
                                context = context,
                                onDelete = {
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            try { File(history.imagePath).delete() } catch (_: Exception) {}
                                            database.generateHistoryDao().delete(history)
                                        }
                                    }
                                },
                                onSave = { saveSingleItem(history) },
                                onCopy = {
                                    clipboardManager.setText(AnnotatedString(history.content))
                                    snackbarData = SnackbarData(context.getString(R.string.copied), true)
                                },
                                isSelectionMode = isSelectionMode,
                                isSelected = history.id in selectedIds,
                                isContentExpanded = history.id in expandedContentIds,
                                onToggleContentExpand = {
                                    expandedContentIds = if (history.id in expandedContentIds)
                                        expandedContentIds - history.id
                                    else
                                        expandedContentIds + history.id
                                },
                                onToggleSelect = { selectedIds = if (history.id in selectedIds) selectedIds - history.id else selectedIds + history.id },
                                onLongPress = { if (!isSelectionMode) { isSelectionMode = true; selectedIds = setOf(history.id) } },
                                modifier = Modifier.animateItem(
                                    fadeInSpec = MD3ListAnimations.fadeInSpec(index),
                                    fadeOutSpec = MD3ListAnimations.fadeOutSpec(),
                                    placementSpec = MD3ListAnimations.placementSpec()
                                )
                            )
                        }
                        item { Spacer(modifier = Modifier.height(150.dp)) }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GenerateHistoryCard(
    history: GenerateHistoryEntity,
    context: android.content.Context,
    onDelete: () -> Unit,
    onSave: () -> Unit,
    onCopy: () -> Unit = {},
    modifier: Modifier = Modifier,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    isContentExpanded: Boolean = false,
    onToggleContentExpand: () -> Unit = {},
    onToggleSelect: () -> Unit = {},
    onLongPress: () -> Unit = {}
) {
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) lerp(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.colorScheme.primaryContainer, 0.15f)
        else MaterialTheme.colorScheme.surfaceContainerLow,
        animationSpec = MD3Motion.standardSpec(),
        label = "generateCardColor"
    )

    var hasOverflow by remember { mutableStateOf(false) }

    val deleteScale = remember { Animatable(1f) }
    val copyScale = remember { Animatable(1f) }
    val saveScale = remember { Animatable(1f) }
    val pulseScope = rememberCoroutineScope()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { if (isSelectionMode) onToggleSelect() },
                onLongClick = onLongPress
            ),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    val imageFile = File(history.imagePath)
                if (imageFile.exists()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageFile)
                            .size(Size.ORIGINAL)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(if (history.generateType == GenerateType.QR_CODE) 60.dp else 80.dp)
                            .height(if (history.generateType == GenerateType.QR_CODE) 60.dp else 40.dp)
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = if (history.generateType == GenerateType.QR_CODE) ContentScale.Fit else ContentScale.FillWidth,
                        filterQuality = FilterQuality.None
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formatTimestamp(history.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = history.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = if (isContentExpanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis,
                        onTextLayout = { result ->
                            if (!isContentExpanded) {
                                hasOverflow = result.hasVisualOverflow
                            }
                        }
                    )
                    if (hasOverflow || isContentExpanded) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(
                                onClick = onToggleContentExpand,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    if (isContentExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    if (history.barcodeFormat != null) {
                        Text(
                            text = history.barcodeFormat.name.replace("_", "-"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Box(modifier = Modifier.size(width = 32.dp, height = 96.dp)) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = !isSelectionMode,
                        enter = scaleIn(
                            animationSpec = MD3Motion.emphasizedDecelerateSpec(MD3Motion.Duration.SHORT3),
                            initialScale = 0.6f
                        ) + fadeIn(animationSpec = MD3Motion.standardSpec()),
                        exit = scaleOut(
                            animationSpec = MD3Motion.emphasizedAccelerateSpec(MD3Motion.Duration.SHORT2),
                            targetScale = 0.6f
                        ) + fadeOut(animationSpec = MD3Motion.standardSpec())
                    ) {
                        Column {
                            IconButton(
                                onClick = {
                                    pulseScope.launch {
                                        deleteScale.animateTo(0.75f, tween(50))
                                        deleteScale.animateTo(1f, tween(150, easing = MD3Motion.EmphasizedDecelerate))
                                    }
                                    onDelete()
                                },
                                modifier = Modifier.size(32.dp).scale(deleteScale.value)
                            ) {
                                Icon(Icons.Default.Close, stringResource(R.string.delete), modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(
                                onClick = {
                                    pulseScope.launch {
                                        copyScale.animateTo(0.75f, tween(50))
                                        copyScale.animateTo(1f, tween(150, easing = MD3Motion.EmphasizedDecelerate))
                                    }
                                    onCopy()
                                },
                                modifier = Modifier.size(32.dp).scale(copyScale.value)
                            ) {
                                Icon(Icons.Outlined.ContentCopy, stringResource(R.string.copy), modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(
                                onClick = {
                                    pulseScope.launch {
                                        saveScale.animateTo(0.75f, tween(50))
                                        saveScale.animateTo(1f, tween(150, easing = MD3Motion.EmphasizedDecelerate))
                                    }
                                    onSave()
                                },
                                modifier = Modifier.size(32.dp).scale(saveScale.value)
                            ) {
                                Icon(Icons.Default.Save, stringResource(R.string.save), modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }

            androidx.compose.animation.AnimatedVisibility(
                visible = isSelectionMode,
                modifier = Modifier.align(Alignment.TopEnd),
                enter = scaleIn(
                    animationSpec = MD3Motion.emphasizedDecelerateSpec(MD3Motion.Duration.SHORT3),
                    initialScale = 0.6f
                ) + fadeIn(animationSpec = MD3Motion.standardSpec()),
                exit = scaleOut(
                    animationSpec = MD3Motion.emphasizedAccelerateSpec(MD3Motion.Duration.SHORT2),
                    targetScale = 0.6f
                ) + fadeOut(animationSpec = MD3Motion.standardSpec())
            ) {
                MD3SelectionIcon(
                    selected = isSelected,
                    modifier = Modifier
                        .padding(6.dp)
                        .size(22.dp)
                )
            }
        }
    }
}

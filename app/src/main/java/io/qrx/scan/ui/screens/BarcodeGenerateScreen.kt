package io.qrx.scan.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.ViewWeek
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.qrx.scan.QRXApplication
import io.qrx.scan.R
import io.qrx.scan.data.BarcodeFormat
import io.qrx.scan.data.GenerateHistoryEntity
import io.qrx.scan.data.GenerateType
import io.qrx.scan.ui.animation.MD3FabAnimations
import io.qrx.scan.ui.animation.MD3ListAnimations
import io.qrx.scan.ui.animation.MD3Motion
import io.qrx.scan.ui.components.MD3SelectionIcon
import io.qrx.scan.ui.components.MD3TextField
import io.qrx.scan.ui.components.QRXSnackbar
import io.qrx.scan.ui.components.SnackbarData
import io.qrx.scan.util.BarcodeGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class BarcodeItem(
    val id: String = UUID.randomUUID().toString(),
    val content: String = "",
    val format: BarcodeFormat = BarcodeFormat.CODE_128,
    val bitmap: Bitmap? = null,
    val isGenerating: Boolean = false,
    val error: String? = null,
    val historyId: Long? = null
)

val barcodeFormatNameIds = mapOf(
    BarcodeFormat.CODE_128 to R.string.format_code128,
    BarcodeFormat.CODE_39 to R.string.format_code39,
    BarcodeFormat.CODE_93 to R.string.format_code93,
    BarcodeFormat.EAN_13 to R.string.format_ean13,
    BarcodeFormat.EAN_8 to R.string.format_ean8,
    BarcodeFormat.UPC_A to R.string.format_upca,
    BarcodeFormat.UPC_E to R.string.format_upce,
    BarcodeFormat.ITF to R.string.format_itf,
    BarcodeFormat.CODABAR to R.string.format_codabar
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeGenerateScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val database = (context.applicationContext as QRXApplication).database
    val listState = rememberLazyListState()
    val density = LocalDensity.current

    val items = remember { mutableStateListOf(BarcodeItem()) }
    var isSaving by remember { mutableStateOf(false) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var snackbarData by remember { mutableStateOf<SnackbarData?>(null) }

    val imeBottom = WindowInsets.ime.getBottom(density)
    val imeHeightDp = with(density) { imeBottom.toDp() }

    BackHandler(enabled = isSelectionMode) {
        isSelectionMode = false
        selectedIds = emptySet()
    }

    fun addNewItem() {
        items.add(BarcodeItem())
        scope.launch {
            listState.animateScrollToItem(items.size - 1)
        }
    }

    fun removeItem(index: Int) {
        if (index >= items.size) return
        val item = items[index]
        if (items.size == 1 && item.content.isBlank() && item.bitmap == null) {
            onNavigateBack()
            return
        }
        if (items.size == 1) {
            if (item.historyId != null) {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        database.generateHistoryDao().deleteById(item.historyId)
                    }
                }
            }
            items[index] = BarcodeItem()
            return
        }
        if (item.historyId != null) {
            scope.launch {
                withContext(Dispatchers.IO) {
                    database.generateHistoryDao().deleteById(item.historyId)
                }
            }
        }
        items.removeAt(index)
        if (items.isEmpty()) {
            items.add(BarcodeItem())
        }
    }

    fun updateContent(index: Int, content: String) {
        if (index < items.size) {
            items[index] = items[index].copy(content = content, bitmap = null, error = null, historyId = null)
        }
    }

    fun updateFormat(index: Int, format: BarcodeFormat) {
        if (index < items.size) {
            items[index] = items[index].copy(format = format, bitmap = null, error = null, historyId = null)
        }
    }

    fun generateBarcode(index: Int) {
        if (index >= items.size) return
        val item = items[index]
        if (item.content.isBlank()) {
            items[index] = item.copy(error = context.getString(R.string.error_empty_content))
            return
        }

        val validation = BarcodeGenerator.validateBarcodeContent(item.content, item.format)
        if (!validation.isValid) {
            items[index] = item.copy(error = validation.errorResId?.let { context.getString(it) })
            return
        }

        items[index] = item.copy(isGenerating = true, error = null)

        scope.launch {
            val bitmap = withContext(Dispatchers.Default) {
                BarcodeGenerator.generateBarcode(item.content, item.format)
            }

            if (bitmap != null) {
                var historyId: Long? = null
                withContext(Dispatchers.IO) {
                    try {
                        val fileName = "QRX_Barcode_${System.currentTimeMillis()}"
                        val internalFile = java.io.File(context.filesDir, "$fileName.png")
                        java.io.FileOutputStream(internalFile).use { stream ->
                            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
                        }
                        historyId = database.generateHistoryDao().insert(
                            GenerateHistoryEntity(
                                content = item.content,
                                imagePath = internalFile.absolutePath,
                                generateType = GenerateType.BARCODE,
                                barcodeFormat = item.format
                            )
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                items[index] = items[index].copy(bitmap = bitmap, isGenerating = false, historyId = historyId)
            } else {
                items[index] = items[index].copy(error = context.getString(R.string.error_generate_check_format), isGenerating = false)
            }
        }
    }

    fun generateAll() {
        focusManager.clearFocus()
        items.forEachIndexed { index, item ->
            if (item.content.isNotBlank() && item.bitmap == null) {
                generateBarcode(index)
            }
        }
    }

    fun exitSelectionMode() {
        isSelectionMode = false
        selectedIds = emptySet()
    }

    fun selectAll() {
        selectedIds = items.filter { it.bitmap != null }.map { it.id }.toSet()
    }

    fun deleteSelected() {
        val idsToRemove = selectedIds.toList()
        idsToRemove.forEach { id ->
            val index = items.indexOfFirst { it.id == id }
            if (index >= 0) {
                val item = items[index]
                if (item.historyId != null) {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            database.generateHistoryDao().deleteById(item.historyId)
                        }
                    }
                }
                items.removeAt(index)
            }
        }
        if (items.isEmpty()) {
            items.add(BarcodeItem())
        }
        exitSelectionMode()
    }

    fun saveSingleItem(item: BarcodeItem) {
        val bitmap = item.bitmap ?: return

        scope.launch {
            val saved = withContext(Dispatchers.IO) {
                saveToGalleryOnly(context, bitmap, "QRX_Barcode_${System.currentTimeMillis()}")
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

    fun saveSelectedToGallery() {
        val itemsToSave = items.filter { it.id in selectedIds && it.bitmap != null }
        if (itemsToSave.isEmpty()) {
            snackbarData = SnackbarData(context.getString(R.string.no_barcode_to_save), false)
            return
        }

        isSaving = true
        scope.launch {
            var savedCount = 0
            itemsToSave.forEach { item ->
                item.bitmap?.let { bitmap ->
                    val saved = withContext(Dispatchers.IO) {
                        saveToGalleryOnly(context, bitmap, "QRX_Barcode_${System.currentTimeMillis()}")
                    }
                    if (saved) savedCount++
                }
            }

            withContext(Dispatchers.Main) {
                isSaving = false
                if (savedCount > 0) {
                    snackbarData = SnackbarData(context.getString(R.string.saved_barcodes_to_gallery, savedCount), true)
                } else {
                    snackbarData = SnackbarData(context.getString(R.string.save_failed), false)
                }
            }
            exitSelectionMode()
        }
    }

    fun saveAllToGallery() {
        val bitmapsToSave = items.filter { it.bitmap != null }
        if (bitmapsToSave.isEmpty()) {
            snackbarData = SnackbarData(context.getString(R.string.no_barcode_to_save), false)
            return
        }

        isSaving = true
        scope.launch {
            var savedCount = 0
            bitmapsToSave.forEach { item ->
                item.bitmap?.let { bitmap ->
                    val saved = withContext(Dispatchers.IO) {
                        saveToGalleryOnly(context, bitmap, "QRX_Barcode_${System.currentTimeMillis()}")
                    }
                    if (saved) savedCount++
                }
            }

            withContext(Dispatchers.Main) {
                isSaving = false
                if (savedCount > 0) {
                    snackbarData = SnackbarData(context.getString(R.string.saved_barcodes_to_gallery, savedCount), true)
                } else {
                    snackbarData = SnackbarData(context.getString(R.string.save_failed), false)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (isSelectionMode) stringResource(R.string.selected_count, selectedIds.size) else stringResource(R.string.generate_barcode))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                navigationIcon = {
                    IconButton(onClick = {
                        if (isSelectionMode) exitSelectionMode() else onNavigateBack()
                    }) {
                        Icon(
                            if (isSelectionMode) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        IconButton(onClick = { selectAll() }) {
                            Icon(Icons.Default.SelectAll, stringResource(R.string.select_all), tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(
                            onClick = { saveSelectedToGallery() },
                            enabled = selectedIds.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Save, stringResource(R.string.save), tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(
                            onClick = { deleteSelected() },
                            enabled = selectedIds.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Delete, stringResource(R.string.delete), tint = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        Surface(
                            onClick = { generateAll() },
                            modifier = Modifier.padding(end = 8.dp),
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
                                    Icons.Outlined.ViewWeek,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.generate),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = 160.dp + imeHeightDp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                    BarcodeItemCard(
                        item = item,
                        index = index,
                        showDelete = !isSelectionMode,
                        onContentChange = { updateContent(index, it) },
                        onFormatChange = { updateFormat(index, it) },
                        onGenerate = { generateBarcode(index) },
                        onDelete = { removeItem(index) },
                        onSave = { saveSingleItem(item) },
                        isSelectionMode = isSelectionMode,
                        isSelected = item.id in selectedIds,
                        onToggleSelect = {
                            selectedIds = if (item.id in selectedIds) {
                                selectedIds - item.id
                            } else {
                                selectedIds + item.id
                            }
                        },
                        onLongPress = {
                            if (!isSelectionMode && item.bitmap != null) {
                                isSelectionMode = true
                                selectedIds = setOf(item.id)
                            }
                        },
                        modifier = Modifier.animateItem(
                            fadeInSpec = MD3ListAnimations.fadeInSpec(index),
                            fadeOutSpec = MD3ListAnimations.fadeOutSpec(),
                            placementSpec = MD3ListAnimations.placementSpec()
                        )
                    )
                }
            }

            AnimatedVisibility(
                visible = !isSelectionMode,
                enter = MD3FabAnimations.enter(),
                exit = MD3FabAnimations.exit(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .windowInsetsPadding(WindowInsets.ime)
                    .padding(16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AnimatedVisibility(
                        visible = items.any { it.bitmap != null },
                        enter = MD3FabAnimations.enter(),
                        exit = MD3FabAnimations.exit()
                    ) {
                        Surface(
                            onClick = { saveAllToGallery() },
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

                    Surface(
                        onClick = { addNewItem() },
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
                                Icons.Outlined.AddCircle,
                                null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.add),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            QRXSnackbar(
                snackbarData = snackbarData,
                onDismiss = { snackbarData = null },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BarcodeItemCard(
    item: BarcodeItem,
    index: Int,
    showDelete: Boolean,
    onContentChange: (String) -> Unit,
    onFormatChange: (BarcodeFormat) -> Unit,
    onGenerate: () -> Unit,
    onDelete: () -> Unit,
    onSave: () -> Unit,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onLongPress: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var expanded by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (item.bitmap != null) 1f else 0.95f,
        animationSpec = MD3Motion.emphasizedDecelerateSpec(),
        label = "scale"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { if (isSelectionMode && item.bitmap != null) onToggleSelect() },
                onLongClick = onLongPress
            ),
        color = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else
            MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isSelectionMode && item.bitmap != null) {
                        MD3SelectionIcon(
                            selected = isSelected,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = "#${index + 1}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (showDelete) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.delete),
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!isSelectionMode) {
                Box {
                    Surface(
                        onClick = { expanded = true },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        tonalElevation = 2.dp,
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(barcodeFormatNameIds[item.format]!!),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f),
                        shape = RoundedCornerShape(16.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        BarcodeFormat.entries.forEach { format ->
                            DropdownMenuItem(
                                text = { Text(stringResource(barcodeFormatNameIds[format]!!)) },
                                onClick = {
                                    onFormatChange(format)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = item.content,
                    onValueChange = onContentChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.input_content)) },
                    placeholder = {
                        Text(
                            when (item.format) {
                                BarcodeFormat.EAN_13 -> stringResource(R.string.hint_12_or_13_digits)
                                BarcodeFormat.EAN_8 -> stringResource(R.string.hint_7_or_8_digits)
                                BarcodeFormat.UPC_A -> stringResource(R.string.hint_11_or_12_digits)
                                BarcodeFormat.UPC_E -> stringResource(R.string.hint_7_or_8_digits)
                                BarcodeFormat.ITF -> stringResource(R.string.hint_even_digits)
                                BarcodeFormat.CODE_39, BarcodeFormat.CODE_93 -> stringResource(R.string.hint_alphanumeric)
                                BarcodeFormat.CODE_128 -> stringResource(R.string.any_text)
                                BarcodeFormat.CODABAR -> stringResource(R.string.hint_codabar_chars)
                            }
                        )
                    },
                    singleLine = false,
                    isError = item.error != null,
                    supportingText = item.error?.let { { Text(it) } },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedLabelColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }

            AnimatedVisibility(
                visible = item.bitmap != null,
                enter = MD3FabAnimations.enter(),
                exit = MD3FabAnimations.exit()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!isSelectionMode) {
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    item.bitmap?.let { bitmap ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = stringResource(R.string.barcode),
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(barcodeFormatNameIds[item.format]!!),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!isSelectionMode && item.bitmap != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        FilledTonalIconButton(
                            onClick = onSave,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.Save,
                                contentDescription = stringResource(R.string.save_to_gallery),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

package io.qrx.scan.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.outlined.AddCircle
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.qrx.scan.QRXApplication
import io.qrx.scan.R
import io.qrx.scan.data.GenerateHistoryEntity
import io.qrx.scan.data.GenerateType
import io.qrx.scan.ui.animation.MD3FabAnimations
import io.qrx.scan.ui.animation.MD3Motion
import io.qrx.scan.ui.animation.MD3StateAnimations
import io.qrx.scan.ui.animation.MD3Transitions
import io.qrx.scan.ui.components.MD3PressableSurface
import io.qrx.scan.ui.components.MD3SelectionIcon
import io.qrx.scan.ui.components.QRXSnackbar
import io.qrx.scan.ui.components.SnackbarData
import io.qrx.scan.ui.components.suppressBringIntoView
import io.qrx.scan.util.BarcodeGenerator
import io.qrx.scan.util.saveToGalleryOnly
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class QRCodeItem(
    val id: String = UUID.randomUUID().toString(),
    val content: String = "",
    val highErrorCorrection: Boolean = false,
    val bitmap: Bitmap? = null,
    val isGenerating: Boolean = false,
    val error: String? = null,
    val historyId: Long? = null
)

@Suppress("LocalContextGetResourceValueCall")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRCodeGenerateScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val database = (context.applicationContext as QRXApplication).database
    val scrollState = rememberScrollState()

    val items = remember { mutableStateListOf(QRCodeItem()) }
    var isSaving by remember { mutableStateOf(false) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var snackbarData by remember { mutableStateOf<SnackbarData?>(null) }
    val isKeyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    BackHandler(enabled = isSelectionMode) {
        isSelectionMode = false
        selectedIds = emptySet()
    }

    fun addNewItem() {
        items.add(QRCodeItem())
        scope.launch {
            scrollState.animateScrollTo(scrollState.maxValue)
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
            items[index] = QRCodeItem()
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
            items.add(QRCodeItem())
        }
    }

    fun updateContent(index: Int, content: String) {
        if (index < items.size) {
            items[index] = items[index].copy(content = content, bitmap = null, error = null, historyId = null)
        }
    }

    fun updateErrorCorrection(index: Int, highErrorCorrection: Boolean) {
        if (index < items.size) {
            items[index] = items[index].copy(highErrorCorrection = highErrorCorrection, bitmap = null, error = null, historyId = null)
        }
    }

    fun generateQRCode(index: Int) {
        if (index >= items.size) return
        val item = items[index]
        if (item.content.isBlank()) {
            items[index] = item.copy(error = context.getString(R.string.error_empty_content))
            return
        }

        items[index] = item.copy(isGenerating = true, error = null)

        scope.launch {
            val bitmap = withContext(Dispatchers.Default) {
                BarcodeGenerator.generateQRCode(item.content, highErrorCorrection = item.highErrorCorrection)
            }

            if (bitmap != null) {
                var historyId: Long? = null
                withContext(Dispatchers.IO) {
                    try {
                        val fileName = "QRX_QR_${System.currentTimeMillis()}"
                        val internalFile = File(context.filesDir, "$fileName.png")
                        FileOutputStream(internalFile).use { stream ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                        }
                        historyId = database.generateHistoryDao().insert(
                            GenerateHistoryEntity(
                                content = item.content,
                                imagePath = internalFile.absolutePath,
                                generateType = GenerateType.QR_CODE
                            )
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                items[index] = items[index].copy(bitmap = bitmap, isGenerating = false, historyId = historyId)
            } else {
                items[index] = items[index].copy(error = context.getString(R.string.error_generate_failed), isGenerating = false)
            }
        }
    }

    fun generateAll() {
        focusManager.clearFocus()
        items.forEachIndexed { index, item ->
            if (item.content.isNotBlank() && item.bitmap == null) {
                generateQRCode(index)
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
            items.add(QRCodeItem())
        }
        exitSelectionMode()
    }

    fun saveSingleItem(item: QRCodeItem) {
        val bitmap = item.bitmap ?: return

        scope.launch {
            val saved = withContext(Dispatchers.IO) {
                saveToGalleryOnly(context, bitmap, "QRX_QR_${System.currentTimeMillis()}")
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
            snackbarData = SnackbarData(context.getString(R.string.no_qrcode_to_save), false)
            return
        }

        isSaving = true
        scope.launch {
            var savedCount = 0
            itemsToSave.forEach { item ->
                item.bitmap?.let { bitmap ->
                    val saved = withContext(Dispatchers.IO) {
                        saveToGalleryOnly(context, bitmap, "QRX_QR_${System.currentTimeMillis()}")
                    }
                    if (saved) savedCount++
                }
            }

            withContext(Dispatchers.Main) {
                isSaving = false
                if (savedCount > 0) {
                    snackbarData = SnackbarData(context.getString(R.string.saved_qrcodes_to_gallery, savedCount), true)
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
            snackbarData = SnackbarData(context.getString(R.string.no_qrcode_to_save), false)
            return
        }

        isSaving = true
        scope.launch {
            var savedCount = 0
            bitmapsToSave.forEach { item ->
                item.bitmap?.let { bitmap ->
                    val saved = withContext(Dispatchers.IO) {
                        saveToGalleryOnly(context, bitmap, "QRX_QR_${System.currentTimeMillis()}")
                    }
                    if (saved) savedCount++
                }
            }

            withContext(Dispatchers.Main) {
                isSaving = false
                if (savedCount > 0) {
                    snackbarData = SnackbarData(context.getString(R.string.saved_qrcodes_to_gallery, savedCount), true)
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
                    title = {
                        Text(if (isSelectionMode) stringResource(R.string.selected_count, selectedIds.size) else stringResource(R.string.generate_qrcode))
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
                        AnimatedContent(
                            targetState = isSelectionMode,
                            transitionSpec = { MD3Transitions.fadeThrough() },
                            label = "qrGenerateActionsTransition"
                        ) { selectionMode ->
                            Row {
                                if (selectionMode) {
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
                                                Icons.Default.QrCode2,
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
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .imePadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .suppressBringIntoView(scrollState)
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                            bottom = 160.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items.forEachIndexed { index, item ->
                        key(item.id) {
                            QRCodeItemCard(
                                item = item,
                                index = index,
                                showDelete = !isSelectionMode,
                                onContentChange = { updateContent(index, it) },
                                onErrorCorrectionChange = { updateErrorCorrection(index, it) },
                                onGenerate = { generateQRCode(index) },
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
                                }
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = !isSelectionMode && !isKeyboardVisible,
                    enter = MD3FabAnimations.enter(),
                    exit = MD3FabAnimations.exit(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
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

                        MD3PressableSurface(
                            onClick = { addNewItem() },
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
fun QRCodeItemCard(
    item: QRCodeItem,
    index: Int,
    showDelete: Boolean,
    onContentChange: (String) -> Unit,
    onErrorCorrectionChange: (Boolean) -> Unit,
    onGenerate: () -> Unit,
    onDelete: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onLongPress: () -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    var expanded by remember { mutableStateOf(false) }

    val containerColor by animateColorAsState(
        targetValue = if (isSelected)
            lerp(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.colorScheme.primaryContainer, 0.15f)
        else
            MaterialTheme.colorScheme.surfaceContainerLow,
        animationSpec = MD3Motion.standardSpec(),
        label = "qrCardColor"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { if (isSelectionMode && item.bitmap != null) onToggleSelect() },
                onLongClick = onLongPress
            ),
        color = containerColor,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Box {
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
                        Text(
                            text = "#${index + 1}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                AnimatedVisibility(
                    visible = showDelete,
                    enter = fadeIn(animationSpec = MD3Motion.standardSpec()),
                    exit = fadeOut(animationSpec = MD3Motion.standardSpec())
                ) {
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

            AnimatedVisibility(
                visible = !isSelectionMode,
                enter = expandVertically(
                    animationSpec = MD3Motion.emphasizedDecelerateSpec(MD3Motion.Duration.MEDIUM1)
                ) + fadeIn(animationSpec = MD3Motion.standardSpec()),
                exit = shrinkVertically(
                    animationSpec = MD3Motion.emphasizedAccelerateSpec(MD3Motion.Duration.SHORT4)
                ) + fadeOut(animationSpec = MD3Motion.standardSpec())
            ) {
            Column {
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
                                text = if (item.highErrorCorrection) stringResource(R.string.error_correction_high) else stringResource(R.string.error_correction_low),
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
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.error_correction_low)) },
                            onClick = {
                                onErrorCorrectionChange(false)
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.error_correction_high)) },
                            onClick = {
                                onErrorCorrectionChange(true)
                                expanded = false
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = item.content,
                    onValueChange = onContentChange,
                    modifier = Modifier
                        .fillMaxWidth(),
                    label = { Text(stringResource(R.string.input_content)) },
                    placeholder = { Text(stringResource(R.string.text_url_contact)) },
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
            }

            AnimatedVisibility(
                visible = item.bitmap != null,
                enter = MD3StateAnimations.contentEnter(),
                exit = MD3StateAnimations.contentExit()
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
                            modifier = Modifier.size(200.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = stringResource(R.string.qrcode),
                                    modifier = Modifier.fillMaxSize().background(Color.White),
                                    filterQuality = FilterQuality.None
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (item.highErrorCorrection) "QR CODE (High)" else "QR CODE (Low)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    AnimatedVisibility(
                        visible = !isSelectionMode && item.bitmap != null,
                        enter = expandVertically(
                            animationSpec = MD3Motion.emphasizedDecelerateSpec(MD3Motion.Duration.MEDIUM1)
                        ) + fadeIn(animationSpec = MD3Motion.standardSpec()),
                        exit = shrinkVertically(
                            animationSpec = MD3Motion.emphasizedAccelerateSpec(MD3Motion.Duration.SHORT4)
                        ) + fadeOut(animationSpec = MD3Motion.standardSpec())
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
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

            AnimatedVisibility(
                visible = isSelectionMode && item.bitmap != null,
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



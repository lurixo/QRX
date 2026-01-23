package io.qrx.scan.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import io.qrx.scan.QRXApplication
import io.qrx.scan.R
import io.qrx.scan.data.ScanHistoryEntity
import io.qrx.scan.data.ScanSource
import io.qrx.scan.ui.animation.MD3Motion
import io.qrx.scan.ui.animation.MD3StateAnimations
import io.qrx.scan.ui.components.MD3ToggleIcon
import io.qrx.scan.ui.components.QRXSnackbar
import io.qrx.scan.ui.components.SnackbarData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executors

data class DetectedBarcode(
    val value: String,
    val centerX: Float,
    val centerY: Float
)

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScanScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val database = (context.applicationContext as QRXApplication).database
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    
    val strBack = stringResource(R.string.back)
    val strScan = stringResource(R.string.scan)
    val strFlashlight = stringResource(R.string.flashlight)
    val strScanHint = stringResource(R.string.scan_hint)

    var scannedCode by remember { mutableStateOf<String?>(null) }
    var detectedBarcodes by remember { mutableStateOf<List<DetectedBarcode>>(emptyList()) }
    var isFlashOn by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    var showSuccessAnimation by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var previewSize by remember { mutableStateOf(Pair(1, 1)) }
    var snackbarData by remember { mutableStateOf<SnackbarData?>(null) }

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val primaryColor = MaterialTheme.colorScheme.primary

    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    fun saveToHistory(code: String, imageCapture: ImageCapture?) {
        scope.launch(Dispatchers.IO) {
            var imagePath = ""

            if (imageCapture != null) {
                try {
                    val photoFile = File(
                        context.filesDir,
                        "qrx_camera_${System.currentTimeMillis()}.jpg"
                    )
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                    imageCapture.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                scope.launch(Dispatchers.IO) {
                                    database.scanHistoryDao().insert(
                                        ScanHistoryEntity(
                                            codes = listOf(code),
                                            imageUri = photoFile.absolutePath,
                                            source = ScanSource.CAMERA
                                        )
                                    )
                                }
                            }

                            override fun onError(exception: ImageCaptureException) {
                                scope.launch(Dispatchers.IO) {
                                    database.scanHistoryDao().insert(
                                        ScanHistoryEntity(
                                            codes = listOf(code),
                                            imageUri = "",
                                            source = ScanSource.CAMERA
                                        )
                                    )
                                }
                            }
                        }
                    )
                } catch (e: Exception) {
                    database.scanHistoryDao().insert(
                        ScanHistoryEntity(
                            codes = listOf(code),
                            imageUri = "",
                            source = ScanSource.CAMERA
                        )
                    )
                }
            } else {
                database.scanHistoryDao().insert(
                    ScanHistoryEntity(
                        codes = listOf(code),
                        imageUri = "",
                        source = ScanSource.CAMERA
                    )
                )
            }
        }
    }

    fun onBarcodeSelected(code: String, imageCapture: ImageCapture?) {
        scannedCode = code
        showSuccessAnimation = true
        isPaused = true
        saveToHistory(code, imageCapture)
        detectedBarcodes = emptyList()
    }

    fun resetScan() {
        scannedCode = null
        showSuccessAnimation = false
        detectedBarcodes = emptyList()
        isPaused = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            cameraPermissionState.status.isGranted -> {
                val previewView = remember { PreviewView(context) }
                val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
                val barcodeScanner = remember { BarcodeScanning.getClient() }
                var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

                DisposableEffect(Unit) {
                    onDispose {
                        cameraExecutor.shutdown()
                    }
                }

                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                ) { view ->
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = view.surfaceProvider
                        }

                        val capture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()
                        imageCapture = capture

                        val resolutionStrategy = androidx.camera.core.resolutionselector.ResolutionStrategy(
                            android.util.Size(1440, 1080),
                            androidx.camera.core.resolutionselector.ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                        )
                        val resolutionSelector = androidx.camera.core.resolutionselector.ResolutionSelector.Builder()
                            .setAspectRatioStrategy(androidx.camera.core.resolutionselector.AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                            .setResolutionStrategy(resolutionStrategy)
                            .build()

                        val imageAnalyzer = ImageAnalysis.Builder()
                            .setResolutionSelector(resolutionSelector)
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                            .build()
                            .also { analysis ->
                                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                    if (isPaused) {
                                        imageProxy.close()
                                        return@setAnalyzer
                                    }

                                    val mediaImage = imageProxy.image
                                    if (mediaImage != null && scannedCode == null) {
                                        previewSize = Pair(imageProxy.width, imageProxy.height)

                                        val image = InputImage.fromMediaImage(
                                            mediaImage,
                                            imageProxy.imageInfo.rotationDegrees
                                        )
                                        barcodeScanner.process(image)
                                            .addOnSuccessListener { barcodes ->
                                                if (barcodes.isNotEmpty() && scannedCode == null && !isPaused) {
                                                    val decodedBarcodes = barcodes.filter { it.rawValue != null }

                                                    if (decodedBarcodes.isNotEmpty()) {
                                                        if (decodedBarcodes.size == 1) {
                                                            decodedBarcodes[0].rawValue?.let { value ->
                                                                onBarcodeSelected(value, imageCapture)
                                                            }
                                                        } else {
                                                            val detected = decodedBarcodes.mapNotNull { barcode ->
                                                                barcode.rawValue?.let { value ->
                                                                    val boundingBox = barcode.boundingBox
                                                                    if (boundingBox != null) {
                                                                        val centerX = (boundingBox.left + boundingBox.right) / 2f
                                                                        val centerY = (boundingBox.top + boundingBox.bottom) / 2f

                                                                        val scaleX = screenWidthPx / previewSize.second.toFloat()
                                                                        val scaleY = screenHeightPx / previewSize.first.toFloat()

                                                                        DetectedBarcode(
                                                                            value = value,
                                                                            centerX = centerX * scaleX,
                                                                            centerY = centerY * scaleY
                                                                        )
                                                                    } else null
                                                                }
                                                            }
                                                            if (detected.isNotEmpty()) {
                                                                detectedBarcodes = detected
                                                                isPaused = true
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            .addOnCompleteListener {
                                                imageProxy.close()
                                            }
                                    } else {
                                        imageProxy.close()
                                    }
                                }
                            }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                capture,
                                imageAnalyzer
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(context))
                }

                ScanOverlay(
                    scanBoxSize = 280.dp,
                    cornerRadius = 12.dp,
                    borderColor = primaryColor,
                    isScanning = scannedCode == null && detectedBarcodes.isEmpty(),
                    showSuccess = showSuccessAnimation
                )

                if (detectedBarcodes.isNotEmpty() && scannedCode == null) {
                    detectedBarcodes.forEach { barcode ->
                        BarcodeMarker(
                            barcode = barcode,
                            onClick = { onBarcodeSelected(barcode.value, imageCapture) }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 100.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color.Black.copy(alpha = 0.7f)
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.scan_detected_codes, detectedBarcodes.size),
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strBack,
                            tint = Color.White
                        )
                    }

                    Text(
                        text = strScan,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )

                    IconButton(
                        onClick = {
                            isFlashOn = !isFlashOn
                            camera?.cameraControl?.enableTorch(isFlashOn)
                        }
                    ) {
                        MD3ToggleIcon(
                            isOn = isFlashOn,
                            onIcon = Icons.Default.FlashOn,
                            offIcon = Icons.Default.FlashOff,
                            contentDescription = strFlashlight,
                            tint = Color.White
                        )
                    }
                }

                if (scannedCode == null && detectedBarcodes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 100.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Text(
                            text = strScanHint,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                AnimatedVisibility(
                    visible = scannedCode != null,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 60.dp),
                    enter = fadeIn(MD3Motion.emphasizedDecelerateSpec()),
                    exit = fadeOut(MD3Motion.emphasizedAccelerateSpec(MD3Motion.Duration.SHORT4))
                ) {
                    scannedCode?.let { code ->
                        ScanResultBottomCard(
                            code = code,
                            context = context,
                            onScanAgain = { resetScan() },
                            onClose = onNavigateBack,
                            onShowSnackbar = { message, isSuccess ->
                                snackbarData = SnackbarData(message, isSuccess)
                            }
                        )
                    }
                }
            }
            cameraPermissionState.status.shouldShowRationale -> {
                PermissionRationale(
                    onRequestPermission = { cameraPermissionState.launchPermissionRequest() }
                )
            }
            else -> {
                PermissionDenied()
            }
        }

        QRXSnackbar(
            snackbarData = snackbarData,
            onDismiss = { snackbarData = null },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun BarcodeMarker(
    barcode: DetectedBarcode,
    onClick: () -> Unit
) {
    val density = LocalDensity.current
    val offsetX = with(density) { barcode.centerX.toDp() - 24.dp }
    val offsetY = with(density) { barcode.centerY.toDp() - 24.dp }

    Box(
        modifier = Modifier
            .offset(x = offsetX, y = offsetY)
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .border(2.dp, Color.White, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.QrCode,
            contentDescription = stringResource(R.string.select_this_code),
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun ScanOverlay(
    scanBoxSize: Dp,
    cornerRadius: Dp,
    borderColor: Color,
    isScanning: Boolean,
    showSuccess: Boolean
) {
    val density = LocalDensity.current
    val scanBoxSizePx = with(density) { scanBoxSize.toPx() }
    val cornerRadiusPx = with(density) { cornerRadius.toPx() }
    val borderWidth = with(density) { 3.dp.toPx() }
    val cornerLength = with(density) { 24.dp.toPx() }

    val infiniteTransition = rememberInfiniteTransition(label = "scanLine")
    val scanLinePosition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanLinePosition"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val boxLeft = (canvasWidth - scanBoxSizePx) / 2
                val boxTop = (canvasHeight - scanBoxSizePx) / 2
                val boxRight = boxLeft + scanBoxSizePx
                val boxBottom = boxTop + scanBoxSizePx

                drawRect(
                    color = Color.Black.copy(alpha = 0.6f),
                    topLeft = Offset.Zero,
                    size = Size(canvasWidth, boxTop)
                )
                drawRect(
                    color = Color.Black.copy(alpha = 0.6f),
                    topLeft = Offset(0f, boxBottom),
                    size = Size(canvasWidth, canvasHeight - boxBottom)
                )
                drawRect(
                    color = Color.Black.copy(alpha = 0.6f),
                    topLeft = Offset(0f, boxTop),
                    size = Size(boxLeft, scanBoxSizePx)
                )
                drawRect(
                    color = Color.Black.copy(alpha = 0.6f),
                    topLeft = Offset(boxRight, boxTop),
                    size = Size(canvasWidth - boxRight, scanBoxSizePx)
                )

                drawRoundRect(
                    color = borderColor.copy(alpha = 0.5f),
                    topLeft = Offset(boxLeft, boxTop),
                    size = Size(scanBoxSizePx, scanBoxSizePx),
                    cornerRadius = CornerRadius(cornerRadiusPx),
                    style = Stroke(width = borderWidth)
                )

                val corners = listOf(
                    Pair(Offset(boxLeft, boxTop + cornerLength), Offset(boxLeft, boxTop)),
                    Pair(Offset(boxLeft, boxTop), Offset(boxLeft + cornerLength, boxTop)),
                    Pair(Offset(boxRight - cornerLength, boxTop), Offset(boxRight, boxTop)),
                    Pair(Offset(boxRight, boxTop), Offset(boxRight, boxTop + cornerLength)),
                    Pair(Offset(boxLeft, boxBottom - cornerLength), Offset(boxLeft, boxBottom)),
                    Pair(Offset(boxLeft, boxBottom), Offset(boxLeft + cornerLength, boxBottom)),
                    Pair(Offset(boxRight - cornerLength, boxBottom), Offset(boxRight, boxBottom)),
                    Pair(Offset(boxRight, boxBottom - cornerLength), Offset(boxRight, boxBottom))
                )

                corners.forEach { (start, end) ->
                    drawLine(
                        color = borderColor,
                        start = start,
                        end = end,
                        strokeWidth = borderWidth * 2
                    )
                }

                if (isScanning) {
                    val lineY = boxTop + (scanBoxSizePx * scanLinePosition)
                    val gradient = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            borderColor.copy(alpha = 0.8f),
                            borderColor,
                            borderColor.copy(alpha = 0.8f),
                            Color.Transparent
                        ),
                        startX = boxLeft,
                        endX = boxRight
                    )
                    drawLine(
                        brush = gradient,
                        start = Offset(boxLeft + 10, lineY),
                        end = Offset(boxRight - 10, lineY),
                        strokeWidth = 3.dp.toPx()
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = showSuccess,
            enter = MD3StateAnimations.contentEnter(),
            exit = MD3StateAnimations.contentExit()
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(80.dp)
            )
        }
    }
}

@Composable
fun ScanResultBottomCard(
    code: String,
    context: Context,
    onScanAgain: () -> Unit,
    onClose: () -> Unit,
    onShowSnackbar: (String, Boolean) -> Unit
) {
    val strScanSuccess = stringResource(R.string.scan_success)
    val strCopy = stringResource(R.string.copy)
    val strCopied = stringResource(R.string.copied)
    val strOpen = stringResource(R.string.open)
    val strCannotOpenLink = stringResource(R.string.cannot_open_link)
    val strContinueScan = stringResource(R.string.continue_scan)
    
    val isUrl = code.startsWith("http://") || code.startsWith("https://") ||
            android.util.Patterns.WEB_URL.matcher(code).matches()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strScanSuccess,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 120.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = code,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                ActionButtonHorizontal(
                    icon = Icons.Default.ContentCopy,
                    text = strCopy,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("QR Code", code))
                        onShowSnackbar(strCopied, true)
                    }
                )

                if (isUrl) {
                    ActionButtonHorizontal(
                        icon = Icons.Default.OpenInBrowser,
                        text = strOpen,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            try {
                                val url = if (code.startsWith("http://") || code.startsWith("https://"))
                                    code else "https://$code"
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            } catch (e: Exception) {
                                onShowSnackbar(strCannotOpenLink, false)
                            }
                        }
                    )
                }

                ActionButtonHorizontal(
                    icon = Icons.Default.Refresh,
                    text = strContinueScan,
                    modifier = Modifier.weight(1f),
                    onClick = onScanAgain
                )
            }
        }
    }
}

@Composable
fun ActionButtonHorizontal(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = MD3Motion.pressSpec(isPressed),
        label = "actionButtonHorizontalScale"
    )

    Surface(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(12.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun PermissionRationale(onRequestPermission: () -> Unit) {
    val strCameraPermissionRequired = stringResource(R.string.camera_permission_required)
    val strCameraPermissionRationale = stringResource(R.string.camera_permission_rationale)
    val strGrantPermission = stringResource(R.string.grant_permission)
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = strCameraPermissionRequired,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = strCameraPermissionRationale,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                modifier = Modifier.clickable(onClick = onRequestPermission),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = strGrantPermission,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
fun PermissionDenied() {
    val strCameraPermissionDenied = stringResource(R.string.camera_permission_denied)
    val strCameraPermissionSettings = stringResource(R.string.camera_permission_settings)
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = strCameraPermissionDenied,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = strCameraPermissionSettings,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

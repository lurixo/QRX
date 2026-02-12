package io.qrx.scan.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

enum class PixelScaleMode { CROP, FIT, FILL_WIDTH }

@Composable
fun PixelPerfectImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    targetWidth: Dp,
    targetHeight: Dp,
    scaleMode: PixelScaleMode = PixelScaleMode.FIT
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val tw = with(density) { targetWidth.roundToPx() }
    val th = with(density) { targetHeight.roundToPx() }

    val bitmap by produceState<ImageBitmap?>(null, model, tw, th) {
        if (tw <= 0 || th <= 0 || model == null) {
            value = null
            return@produceState
        }
        value = withContext(Dispatchers.IO) {
            val src = loadSourceBitmap(context, model) ?: return@withContext null
            val result = when (scaleMode) {
                PixelScaleMode.CROP -> centerCropScale(src, tw, th)
                PixelScaleMode.FIT -> fitScale(src, tw, th)
                PixelScaleMode.FILL_WIDTH -> fillWidthScale(src, tw, th)
            }
            if (result !== src) src.recycle()
            result.asImageBitmap()
        }
    }

    bitmap?.let {
        Image(
            bitmap = it,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.None,
            filterQuality = FilterQuality.None
        )
    } ?: Box(modifier = modifier)
}

private fun loadSourceBitmap(context: android.content.Context, model: Any): Bitmap? {
    return try {
        when (model) {
            is File -> if (model.exists()) BitmapFactory.decodeFile(model.absolutePath) else null
            is String -> {
                val file = File(model)
                if (file.exists()) BitmapFactory.decodeFile(model) else null
            }
            is Uri -> context.contentResolver.openInputStream(model)?.use { BitmapFactory.decodeStream(it) }
            else -> null
        }
    } catch (_: Exception) {
        null
    }
}

private fun centerCropScale(src: Bitmap, tw: Int, th: Int): Bitmap {
    val srcRatio = src.width.toFloat() / src.height
    val dstRatio = tw.toFloat() / th
    val cropW: Int
    val cropH: Int
    if (srcRatio > dstRatio) {
        cropH = src.height
        cropW = (cropH * dstRatio).toInt().coerceIn(1, src.width)
    } else {
        cropW = src.width
        cropH = (cropW / dstRatio).toInt().coerceIn(1, src.height)
    }
    val x = (src.width - cropW) / 2
    val y = (src.height - cropH) / 2
    val cropped = Bitmap.createBitmap(src, x, y, cropW, cropH)
    val scaled = Bitmap.createScaledBitmap(cropped, tw, th, false)
    if (cropped !== src && cropped !== scaled) cropped.recycle()
    return scaled
}

private fun fitScale(src: Bitmap, tw: Int, th: Int): Bitmap {
    val scale = minOf(tw.toFloat() / src.width, th.toFloat() / src.height)
    val w = (src.width * scale).toInt().coerceAtLeast(1)
    val h = (src.height * scale).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(src, w, h, false)
}

private fun fillWidthScale(src: Bitmap, tw: Int, th: Int): Bitmap {
    val scale = tw.toFloat() / src.width
    val w = tw
    val h = (src.height * scale).toInt().coerceIn(1, th)
    return Bitmap.createScaledBitmap(src, w, h, false)
}

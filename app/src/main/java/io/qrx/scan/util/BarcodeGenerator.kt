package io.qrx.scan.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import io.qrx.barcode.QrCodeGenerator
import io.qrx.barcode.BarcodeGenerator as QrxBarcodeGenerator
import io.qrx.barcode.BarcodeType
import io.qrx.barcode.ErrorCorrectionLevel
import io.qrx.scan.R
import io.qrx.scan.data.BarcodeFormat as AppBarcodeFormat

object BarcodeGenerator {

    fun generateQRCode(
        content: String,
        width: Int = 512,
        height: Int = 512,
        margin: Int = 1,
        highErrorCorrection: Boolean = true
    ): Bitmap? {
        return try {
            val level = if (highErrorCorrection) ErrorCorrectionLevel.High else ErrorCorrectionLevel.Low
            val bitmap = QrCodeGenerator.generateBitmap(content, width, level)
            addPaddingToBitmap(bitmap, 40)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun generateBarcode(
        content: String,
        format: AppBarcodeFormat,
        width: Int = 600,
        height: Int = 200,
        margin: Int = 0
    ): Bitmap? {
        return try {
            val barcodeType = when (format) {
                AppBarcodeFormat.CODE_128 -> BarcodeType.Code128
                AppBarcodeFormat.CODE_39 -> BarcodeType.Code39
                AppBarcodeFormat.CODE_93 -> BarcodeType.Code93
                AppBarcodeFormat.EAN_13 -> BarcodeType.EAN13
                AppBarcodeFormat.EAN_8 -> BarcodeType.EAN8
                AppBarcodeFormat.UPC_A -> BarcodeType.UPCA
                AppBarcodeFormat.UPC_E -> BarcodeType.UPCE
                AppBarcodeFormat.ITF -> BarcodeType.ITF
                AppBarcodeFormat.CODABAR -> BarcodeType.Codabar
            }
            
            val processedContent = when (format) {
                AppBarcodeFormat.EAN_13 -> {
                    if (content.length == 12) content + calculateEAN13CheckDigit(content)
                    else content
                }
                AppBarcodeFormat.EAN_8 -> {
                    if (content.length == 7) content + calculateEAN8CheckDigit(content)
                    else content
                }
                AppBarcodeFormat.UPC_A -> {
                    if (content.length == 11) content + calculateUPCACheckDigit(content)
                    else content
                }
                AppBarcodeFormat.UPC_E -> {
                    if (content.length == 7) content + calculateUPCECheckDigit(content)
                    else content
                }
                else -> content
            }
            
            val bitmap = QrxBarcodeGenerator.generateBitmap(processedContent, barcodeType, width, height)
            addPaddingToBitmap(bitmap, 50)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun calculateEAN13CheckDigit(data: String): Char {
        var sum = 0
        for (i in 0 until 12) {
            val digit = data[i].digitToInt()
            sum += if (i % 2 == 0) digit else digit * 3
        }
        val checkDigit = (10 - (sum % 10)) % 10
        return checkDigit.digitToChar()
    }
    
    private fun calculateEAN8CheckDigit(data: String): Char {
        var sum = 0
        for (i in 0 until 7) {
            val digit = data[i].digitToInt()
            sum += if (i % 2 == 0) digit * 3 else digit
        }
        val checkDigit = (10 - (sum % 10)) % 10
        return checkDigit.digitToChar()
    }
    
    private fun calculateUPCACheckDigit(data: String): Char {
        var sum = 0
        for (i in 0 until 11) {
            val digit = data[i].digitToInt()
            sum += if (i % 2 == 0) digit * 3 else digit
        }
        val checkDigit = (10 - (sum % 10)) % 10
        return checkDigit.digitToChar()
    }
    
    private fun calculateUPCECheckDigit(data: String): Char {
        val expanded = expandUPCEtoUPCA(data)
        return calculateUPCACheckDigit(expanded)
    }
    
    private fun expandUPCEtoUPCA(upce: String): String {
        if (upce.length < 6) {
            return "00000000000"
        }
        val manufacturer: String
        val product: String
        when (upce[5]) {
            '0', '1', '2' -> {
                manufacturer = upce.substring(0, 2) + upce[5] + "00"
                product = "00" + upce.substring(2, 5)
            }
            '3' -> {
                manufacturer = upce.substring(0, 3) + "00"
                product = "000" + upce.substring(3, 5)
            }
            '4' -> {
                manufacturer = upce.substring(0, 4) + "0"
                product = "0000" + upce[4]
            }
            else -> {
                manufacturer = upce.substring(0, 5)
                product = "0000" + upce[5]
            }
        }
        return "0" + manufacturer + product
    }

    private fun addPaddingToBitmap(source: Bitmap, padding: Int): Bitmap {
        val newWidth = source.width + padding * 2
        val newHeight = source.height + padding * 2
        val result = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(source, padding.toFloat(), padding.toFloat(), null)
        source.recycle()
        return result
    }

    fun validateBarcodeContent(content: String, format: AppBarcodeFormat): ValidationResult {
        return when (format) {
            AppBarcodeFormat.EAN_13 -> {
                if (!content.all { it.isDigit() }) {
                    ValidationResult(false, R.string.error_ean13_digits)
                } else if (content.length != 12 && content.length != 13) {
                    ValidationResult(false, R.string.error_ean13_length)
                } else if (content.length == 13 && !verifyEAN13CheckDigit(content)) {
                    ValidationResult(false, R.string.error_check_digit)
                } else {
                    ValidationResult(true, null)
                }
            }
            AppBarcodeFormat.EAN_8 -> {
                if (!content.all { it.isDigit() }) {
                    ValidationResult(false, R.string.error_ean8_digits)
                } else if (content.length != 7 && content.length != 8) {
                    ValidationResult(false, R.string.error_ean8_length)
                } else if (content.length == 8 && !verifyEAN8CheckDigit(content)) {
                    ValidationResult(false, R.string.error_check_digit)
                } else {
                    ValidationResult(true, null)
                }
            }
            AppBarcodeFormat.UPC_A -> {
                if (!content.all { it.isDigit() }) {
                    ValidationResult(false, R.string.error_upca_digits)
                } else if (content.length != 11 && content.length != 12) {
                    ValidationResult(false, R.string.error_upca_length)
                } else if (content.length == 12 && !verifyUPCACheckDigit(content)) {
                    ValidationResult(false, R.string.error_check_digit)
                } else {
                    ValidationResult(true, null)
                }
            }
            AppBarcodeFormat.UPC_E -> {
                if (!content.all { it.isDigit() }) {
                    ValidationResult(false, R.string.error_upce_digits)
                } else if (content.length != 7 && content.length != 8) {
                    ValidationResult(false, R.string.error_upce_length)
                } else if (content.length == 8 && !verifyUPCECheckDigit(content)) {
                    ValidationResult(false, R.string.error_check_digit)
                } else {
                    ValidationResult(true, null)
                }
            }
            AppBarcodeFormat.ITF -> {
                if (!content.all { it.isDigit() }) {
                    ValidationResult(false, R.string.error_itf_digits)
                } else if (content.length < 2) {
                    ValidationResult(false, R.string.error_itf_min_length)
                } else if (content.length % 2 != 0) {
                    ValidationResult(false, R.string.error_itf_even)
                } else {
                    ValidationResult(true, null)
                }
            }
            AppBarcodeFormat.CODE_39 -> {
                val validChars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. $/+%"
                if (content.isEmpty()) {
                    ValidationResult(false, R.string.error_empty_content)
                } else if (!content.uppercase().all { it in validChars }) {
                    ValidationResult(false, R.string.error_code39_chars)
                } else {
                    ValidationResult(true, null)
                }
            }
            AppBarcodeFormat.CODE_93 -> {
                val validChars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. $/+%"
                if (content.isEmpty()) {
                    ValidationResult(false, R.string.error_empty_content)
                } else if (!content.uppercase().all { it in validChars }) {
                    ValidationResult(false, R.string.error_code93_chars)
                } else {
                    ValidationResult(true, null)
                }
            }
            AppBarcodeFormat.CODE_128 -> {
                if (content.isEmpty()) {
                    ValidationResult(false, R.string.error_empty_content)
                } else if (!content.all { it.code in 0..127 }) {
                    ValidationResult(false, R.string.error_code128_ascii)
                } else {
                    ValidationResult(true, null)
                }
            }
            AppBarcodeFormat.CODABAR -> {
                val validChars = "0123456789-$:/.+ABCD"
                if (content.isEmpty()) {
                    ValidationResult(false, R.string.error_empty_content)
                } else if (!content.uppercase().all { it in validChars }) {
                    ValidationResult(false, R.string.error_codabar_chars)
                } else {
                    ValidationResult(true, null)
                }
            }
        }
    }
    
    private fun verifyEAN13CheckDigit(data: String): Boolean {
        val calculated = calculateEAN13CheckDigit(data.substring(0, 12))
        return data[12] == calculated
    }
    
    private fun verifyEAN8CheckDigit(data: String): Boolean {
        val calculated = calculateEAN8CheckDigit(data.substring(0, 7))
        return data[7] == calculated
    }
    
    private fun verifyUPCACheckDigit(data: String): Boolean {
        val calculated = calculateUPCACheckDigit(data.substring(0, 11))
        return data[11] == calculated
    }
    
    private fun verifyUPCECheckDigit(data: String): Boolean {
        val calculated = calculateUPCECheckDigit(data.substring(0, 7))
        return data[7] == calculated
    }

    data class ValidationResult(
        val isValid: Boolean,
        val errorResId: Int?
    )
}

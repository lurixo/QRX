package io.qrx.scan.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

enum class GenerateType {
    QR_CODE,
    BARCODE
}

enum class BarcodeFormat {
    CODE_128,
    CODE_39,
    CODE_93,
    EAN_13,
    EAN_8,
    UPC_A,
    UPC_E,
    ITF,
    CODABAR
}

@Entity(tableName = "generate_history")
@TypeConverters(GenerateConverters::class)
data class GenerateHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val imagePath: String,
    val generateType: GenerateType,
    val barcodeFormat: BarcodeFormat? = null,
    val timestamp: Long = System.currentTimeMillis()
)

class GenerateConverters {
    @TypeConverter
    fun fromGenerateType(value: GenerateType): String = value.name

    @TypeConverter
    fun toGenerateType(value: String): GenerateType = try {
        GenerateType.valueOf(value)
    } catch (e: Exception) {
        GenerateType.QR_CODE
    }

    @TypeConverter
    fun fromBarcodeFormat(value: BarcodeFormat?): String? = value?.name

    @TypeConverter
    fun toBarcodeFormat(value: String?): BarcodeFormat? = value?.let {
        try {
            BarcodeFormat.valueOf(it)
        } catch (e: Exception) {
            null
        }
    }
}

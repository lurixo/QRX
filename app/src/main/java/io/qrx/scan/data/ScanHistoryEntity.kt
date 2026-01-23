package io.qrx.scan.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

enum class ScanSource {
    CAMERA,
    IMAGE
}

@Entity(tableName = "scan_history")
@TypeConverters(Converters::class)
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val codes: List<String>,
    val imageUri: String,
    val timestamp: Long = System.currentTimeMillis(),
    val source: ScanSource = ScanSource.IMAGE
)

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, type)
    }

    @TypeConverter
    fun fromScanSource(value: ScanSource): String {
        return value.name
    }

    @TypeConverter
    fun toScanSource(value: String): ScanSource {
        return try {
            ScanSource.valueOf(value)
        } catch (e: Exception) {
            ScanSource.IMAGE
        }
    }
}

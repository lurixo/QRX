package io.qrx.scan.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanHistoryDao {
    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<ScanHistoryEntity>>

    @Query("SELECT * FROM scan_history WHERE source = :source ORDER BY timestamp DESC")
    fun getHistoryBySource(source: ScanSource): Flow<List<ScanHistoryEntity>>

    @Query("SELECT COUNT(*) FROM scan_history WHERE source = :source")
    fun getCountBySource(source: ScanSource): Flow<Int>

    @Insert
    suspend fun insert(history: ScanHistoryEntity): Long

    @Delete
    suspend fun delete(history: ScanHistoryEntity)

    @Query("DELETE FROM scan_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM scan_history WHERE source = :source")
    suspend fun deleteBySource(source: ScanSource)

    @Query("DELETE FROM scan_history")
    suspend fun deleteAll()
}

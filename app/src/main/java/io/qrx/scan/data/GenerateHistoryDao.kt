package io.qrx.scan.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GenerateHistoryDao {
    @Query("SELECT * FROM generate_history ORDER BY timestamp DESC")
    fun getAll(): Flow<List<GenerateHistoryEntity>>

    @Query("SELECT * FROM generate_history WHERE generateType = :type ORDER BY timestamp DESC")
    fun getByType(type: GenerateType): Flow<List<GenerateHistoryEntity>>

    @Query("SELECT COUNT(*) FROM generate_history WHERE generateType = :type")
    fun getCountByType(type: GenerateType): Flow<Int>

    @Insert
    suspend fun insert(history: GenerateHistoryEntity): Long

    @Delete
    suspend fun delete(history: GenerateHistoryEntity)

    @Query("DELETE FROM generate_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM generate_history WHERE generateType = :type")
    suspend fun deleteByType(type: GenerateType)

    @Query("DELETE FROM generate_history")
    suspend fun deleteAll()
}

package com.calculator.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.calculator.app.data.local.db.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM calculation_history ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<HistoryEntity>>

    @Insert
    suspend fun insert(entry: HistoryEntity)

    @Query("DELETE FROM calculation_history")
    suspend fun clearAll()

    @Query("DELETE FROM calculation_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query(
        """
        DELETE FROM calculation_history WHERE id NOT IN (
            SELECT id FROM calculation_history ORDER BY timestamp DESC LIMIT :keepCount
        )
        """
    )
    suspend fun trimToSize(keepCount: Int = 100)
}

package com.calculator.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.calculator.app.data.local.db.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    /**
     * Reads the most-recent [limit] rows. The composite index on
     * `(timestamp DESC, id DESC)` covers this query end-to-end (no rowid lookups).
     */
    @Query("SELECT * FROM calculation_history ORDER BY timestamp DESC, id DESC LIMIT :limit")
    fun observeAll(limit: Int = 100): Flow<List<HistoryEntity>>

    @Insert
    suspend fun insert(entry: HistoryEntity)

    @Query("DELETE FROM calculation_history")
    suspend fun clearAll()

    @Query("DELETE FROM calculation_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query(
        """
        DELETE FROM calculation_history WHERE id NOT IN (
            SELECT id FROM calculation_history ORDER BY timestamp DESC, id DESC LIMIT :keepCount
        )
        """
    )
    suspend fun trimToSize(keepCount: Int = 100)

    @Query("SELECT COUNT(*) FROM calculation_history")
    suspend fun countRows(): Int

    /**
     * Inserts a new entry and trims the table to [keepCount] rows in a single
     * transaction. Without this, the two operations emit two separate
     * invalidation signals, causing the observe Flow to publish twice per
     * `=` press and flickering the history `LazyColumn`.
     *
     * We gate [trimToSize] on the row count so the NOT-IN subquery scan only
     * runs when we actually need to drop rows. SQLite COUNT on the indexed
     * table is microseconds; the gated trim avoids a no-op WAL append every
     * insert until the table fills.
     */
    @Transaction
    suspend fun insertAndTrim(entry: HistoryEntity, keepCount: Int = 100) {
        insert(entry)
        if (countRows() > keepCount) trimToSize(keepCount)
    }
}

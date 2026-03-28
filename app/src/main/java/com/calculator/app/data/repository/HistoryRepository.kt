package com.calculator.app.data.repository

import com.calculator.app.data.local.db.dao.HistoryDao
import com.calculator.app.data.local.db.entity.HistoryEntity
import com.calculator.app.domain.model.HistoryItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HistoryRepository(private val historyDao: HistoryDao) {

    companion object {
        private const val MAX_HISTORY_ENTRIES = 100
    }

    fun observeHistory(): Flow<List<HistoryItem>> =
        historyDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun addEntry(expression: String, result: String) {
        historyDao.insert(HistoryEntity(expression = expression, result = result))
        historyDao.trimToSize(MAX_HISTORY_ENTRIES)
    }

    suspend fun clearHistory() = historyDao.clearAll()

    suspend fun deleteEntry(id: Long) = historyDao.deleteById(id)

    private fun HistoryEntity.toDomain() = HistoryItem(
        id = id,
        expression = expression,
        result = result,
        timestamp = timestamp,
    )
}

package com.calculator.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.calculator.app.data.local.db.dao.HistoryDao
import com.calculator.app.data.local.db.entity.HistoryEntity

@Database(
    entities = [HistoryEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class CalculatorDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
}

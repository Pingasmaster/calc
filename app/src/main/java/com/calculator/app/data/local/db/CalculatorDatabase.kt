package com.calculator.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.calculator.app.data.local.db.dao.HistoryDao
import com.calculator.app.data.local.db.entity.HistoryEntity

@Database(
    entities = [HistoryEntity::class],
    version = 3,
    exportSchema = true,
)
abstract class CalculatorDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    companion object {
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_calculation_history_timestamp " +
                        "ON calculation_history(timestamp)",
                )
            }
        }

        /**
         * Replaces the single-column `timestamp` index with a composite
         * `(timestamp DESC, id DESC)` index that covers `observeAll`'s SELECT
         * end-to-end, letting SQLite serve the page without rowid lookups.
         */
        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("DROP INDEX IF EXISTS index_calculation_history_timestamp")
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_calculation_history_timestamp_id " +
                        "ON calculation_history(timestamp DESC, id DESC)",
                )
            }
        }
    }
}

package com.calculator.app.data.local.db

import androidx.sqlite.db.SupportSQLiteDatabase
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class CalculatorDatabaseMigrationTest {

    // ---------- MIGRATION_1_2 ----------

    @Test
    fun `migration 1 to 2 creates timestamp index`() {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        CalculatorDatabase.MIGRATION_1_2.migrate(db)
        verify {
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_calculation_history_timestamp " +
                        "ON calculation_history(timestamp)"
            )
        }
    }

    @Test
    fun `migration 1 to 2 targets correct versions`() {
        assert(CalculatorDatabase.MIGRATION_1_2.startVersion == 1)
        assert(CalculatorDatabase.MIGRATION_1_2.endVersion == 2)
    }

    @Test
    fun `migration 1 to 2 is idempotent (uses IF NOT EXISTS)`() {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        CalculatorDatabase.MIGRATION_1_2.migrate(db)
        CalculatorDatabase.MIGRATION_1_2.migrate(db)
        verify(exactly = 2) {
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_calculation_history_timestamp " +
                        "ON calculation_history(timestamp)"
            )
        }
    }

    // ---------- MIGRATION_2_3 ----------

    @Test
    fun `migration 2 to 3 drops old index and creates composite index`() {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        CalculatorDatabase.MIGRATION_2_3.migrate(db)
        verify {
            db.execSQL("DROP INDEX IF EXISTS index_calculation_history_timestamp")
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_calculation_history_timestamp_id " +
                        "ON calculation_history(timestamp DESC, id DESC)"
            )
        }
    }

    @Test
    fun `migration 2 to 3 targets correct versions`() {
        assert(CalculatorDatabase.MIGRATION_2_3.startVersion == 2)
        assert(CalculatorDatabase.MIGRATION_2_3.endVersion == 3)
    }

    @Test
    fun `migration 2 to 3 is idempotent`() {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        CalculatorDatabase.MIGRATION_2_3.migrate(db)
        CalculatorDatabase.MIGRATION_2_3.migrate(db)
        verify(exactly = 2) {
            db.execSQL("DROP INDEX IF EXISTS index_calculation_history_timestamp")
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_calculation_history_timestamp_id " +
                        "ON calculation_history(timestamp DESC, id DESC)"
            )
        }
    }
}

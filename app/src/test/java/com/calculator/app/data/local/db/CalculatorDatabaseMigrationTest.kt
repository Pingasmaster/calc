package com.calculator.app.data.local.db

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class CalculatorDatabaseMigrationTest {

    /**
     * Stubs the no-arg `SQLiteConnection.execSQL` extension by mocking the
     * underlying `prepare(...)` + step()/close() path on the connection +
     * statement pair. Lets us assert that the migration issues the expected
     * DDL strings.
     */
    private fun mockConnection(): SQLiteConnection {
        val conn = mockk<SQLiteConnection>(relaxed = true)
        val stmt = mockk<SQLiteStatement>(relaxed = true)
        every { conn.prepare(any()) } returns stmt
        return conn
    }

    private fun verifyExecSql(conn: SQLiteConnection, sql: String, exactly: Int = 1) {
        verify(exactly = exactly) { conn.prepare(sql) }
    }

    // ---------- MIGRATION_1_2 ----------

    @Test
    fun `migration 1 to 2 creates timestamp index`() {
        val conn = mockConnection()
        CalculatorDatabase.MIGRATION_1_2.migrate(conn)
        verifyExecSql(
            conn,
            "CREATE INDEX IF NOT EXISTS index_calculation_history_timestamp " +
                "ON calculation_history(timestamp)",
        )
    }

    @Test
    fun `migration 1 to 2 targets correct versions`() {
        assert(CalculatorDatabase.MIGRATION_1_2.startVersion == 1)
        assert(CalculatorDatabase.MIGRATION_1_2.endVersion == 2)
    }

    @Test
    fun `migration 1 to 2 is idempotent (uses IF NOT EXISTS)`() {
        val conn = mockConnection()
        CalculatorDatabase.MIGRATION_1_2.migrate(conn)
        CalculatorDatabase.MIGRATION_1_2.migrate(conn)
        verifyExecSql(
            conn,
            "CREATE INDEX IF NOT EXISTS index_calculation_history_timestamp " +
                "ON calculation_history(timestamp)",
            exactly = 2,
        )
    }

    // ---------- MIGRATION_2_3 ----------

    @Test
    fun `migration 2 to 3 drops old index and creates composite index`() {
        val conn = mockConnection()
        CalculatorDatabase.MIGRATION_2_3.migrate(conn)
        verifyExecSql(conn, "DROP INDEX IF EXISTS index_calculation_history_timestamp")
        verifyExecSql(
            conn,
            "CREATE INDEX IF NOT EXISTS index_calculation_history_timestamp_id " +
                "ON calculation_history(timestamp DESC, id DESC)",
        )
    }

    @Test
    fun `migration 2 to 3 targets correct versions`() {
        assert(CalculatorDatabase.MIGRATION_2_3.startVersion == 2)
        assert(CalculatorDatabase.MIGRATION_2_3.endVersion == 3)
    }

    @Test
    fun `migration 2 to 3 is idempotent`() {
        val conn = mockConnection()
        CalculatorDatabase.MIGRATION_2_3.migrate(conn)
        CalculatorDatabase.MIGRATION_2_3.migrate(conn)
        verifyExecSql(conn, "DROP INDEX IF EXISTS index_calculation_history_timestamp", exactly = 2)
        verifyExecSql(
            conn,
            "CREATE INDEX IF NOT EXISTS index_calculation_history_timestamp_id " +
                "ON calculation_history(timestamp DESC, id DESC)",
            exactly = 2,
        )
    }
}

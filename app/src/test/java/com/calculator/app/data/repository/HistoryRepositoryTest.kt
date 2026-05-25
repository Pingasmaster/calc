package com.calculator.app.data.repository

import app.cash.turbine.test
import com.calculator.app.data.local.db.dao.HistoryDao
import com.calculator.app.data.local.db.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HistoryRepositoryTest {

    private class FakeHistoryDao : HistoryDao {
        private val entries = MutableStateFlow<List<HistoryEntity>>(emptyList())
        private var nextId = 1L
        var insertCount = 0
            private set
        var trimCalls = mutableListOf<Int>()
            private set
        var clearCount = 0
            private set
        var deletedIds = mutableListOf<Long>()
            private set

        override fun observeAll(limit: Int): Flow<List<HistoryEntity>> = entries.asStateFlow()

        override suspend fun insert(entry: HistoryEntity) {
            insertCount++
            val stored = entry.copy(id = if (entry.id == 0L) nextId++ else entry.id)
            entries.value = (entries.value + stored).sortedByDescending { it.timestamp }
        }

        override suspend fun clearAll() {
            clearCount++
            entries.value = emptyList()
        }

        override suspend fun deleteById(id: Long) {
            deletedIds.add(id)
            entries.value = entries.value.filterNot { it.id == id }
        }

        override suspend fun trimToSize(keepCount: Int) {
            trimCalls.add(keepCount)
            entries.value = entries.value.sortedByDescending { it.timestamp }.take(keepCount)
        }

        fun seed(vararg items: HistoryEntity) {
            entries.value = items.toList().sortedByDescending { it.timestamp }
            nextId = (items.maxOfOrNull { it.id } ?: 0L) + 1
        }
    }

    private lateinit var dao: FakeHistoryDao
    private lateinit var repo: HistoryRepository

    @Before
    fun setUp() {
        dao = FakeHistoryDao()
        repo = HistoryRepository(dao)
    }

    @Test
    fun `addEntry inserts into DAO`() = runTest {
        repo.addEntry("1+1", "2")
        assertEquals(1, dao.insertCount)
    }

    @Test
    fun `addEntry calls trimToSize with 100`() = runTest {
        repo.addEntry("1+1", "2")
        assertEquals(listOf(100), dao.trimCalls)
    }

    @Test
    fun `addEntry triggers trimToSize on every insert`() = runTest {
        repo.addEntry("1", "1")
        repo.addEntry("2", "2")
        repo.addEntry("3", "3")
        assertEquals(3, dao.insertCount)
        assertEquals(listOf(100, 100, 100), dao.trimCalls)
    }

    @Test
    fun `clearHistory calls clearAll`() = runTest {
        repo.clearHistory()
        assertEquals(1, dao.clearCount)
    }

    @Test
    fun `deleteEntry passes id through`() = runTest {
        repo.deleteEntry(42L)
        assertEquals(listOf(42L), dao.deletedIds)
    }

    @Test
    fun `observeHistory maps entities to domain items`() = runTest {
        dao.seed(
            HistoryEntity(id = 1L, expression = "2+2", result = "4", timestamp = 1_000L),
        )
        repo.observeHistory().test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals(1L, items[0].id)
            assertEquals("2+2", items[0].expression)
            assertEquals("4", items[0].result)
            assertEquals(1_000L, items[0].timestamp)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeHistory reflects inserts live`() = runTest {
        repo.observeHistory().test {
            assertTrue(awaitItem().isEmpty())
            repo.addEntry("1+1", "2")
            val after = awaitItem()
            assertEquals(1, after.size)
            assertEquals("1+1", after[0].expression)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeHistory empty when DAO empty`() = runTest {
        repo.observeHistory().test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `addEntry timestamp defaults to current time`() = runTest {
        val before = System.currentTimeMillis()
        repo.addEntry("a", "b")
        val after = System.currentTimeMillis()

        repo.observeHistory().test {
            val items = awaitItem()
            assertEquals(1, items.size)
            val ts = items[0].timestamp
            assertTrue("ts=$ts before=$before", ts >= before)
            assertTrue("ts=$ts after=$after", ts <= after)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deleteEntry removes only the matching row`() = runTest {
        dao.seed(
            HistoryEntity(id = 1L, expression = "a", result = "1", timestamp = 1L),
            HistoryEntity(id = 2L, expression = "b", result = "2", timestamp = 2L),
            HistoryEntity(id = 3L, expression = "c", result = "3", timestamp = 3L),
        )
        repo.deleteEntry(2L)

        repo.observeHistory().test {
            val items = awaitItem()
            assertEquals(2, items.size)
            assertEquals(setOf(1L, 3L), items.map { it.id }.toSet())
            cancelAndIgnoreRemainingEvents()
        }
    }
}

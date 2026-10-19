package com.usuallycoding.portfolio.server.data

import com.usuallycoding.portfolio.server.domain.LinkFields
import com.usuallycoding.portfolio.server.domain.ProfileFields
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class SqliteTransactorTest {
    private fun newTransactor() = SqliteTransactor(Database(Files.createTempDirectory("tx-test").resolve("p.db")))

    @Test
    fun aUnitOfWorkSpanningRepositoriesIsKeptWhole() = runTest {
        val transactor = newTransactor()
        transactor.transaction {
            profile.save(ProfileFields("Ada", "d", null, null))
            links.create(LinkFields("GitHub", "https://github.com/a", true, null))
        }
        assertEquals("Ada", transactor.transaction { profile.get() }?.name)
        assertEquals(1, transactor.transaction { links.list(publicOnly = false) }.size)
    }

    @Test
    fun aFailureRollsBackEveryRepositoryInTheUnit() = runTest {
        val transactor = newTransactor()
        assertFailsWith<IllegalStateException> {
            transactor.transaction {
                profile.save(ProfileFields("Ada", "d", null, null))
                links.create(LinkFields("GitHub", "https://github.com/a", true, null))
                error("boom")
            }
        }
        assertNull(transactor.transaction { profile.get() })
        assertEquals(0, transactor.transaction { links.list(publicOnly = false) }.size)
    }

    @Test
    fun reorderingChangesTheStoredOrder() = runTest {
        val transactor = newTransactor()
        val (a, b) = transactor.transaction {
            links.create(LinkFields("A", "https://a.b", true, null)).id to links.create(LinkFields("B", "https://a.b", true, null)).id
        }
        transactor.transaction { links.reorder(listOf(b, a)) }
        assertEquals(listOf("B", "A"), transactor.transaction { links.list(publicOnly = false) }.map { it.name })
    }
}

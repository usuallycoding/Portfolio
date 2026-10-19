package com.usuallycoding.portfolio.server

import com.usuallycoding.portfolio.server.data.*
import com.usuallycoding.portfolio.server.domain.Clock
import com.usuallycoding.portfolio.server.domain.Transactor
import com.usuallycoding.portfolio.server.service.*
import io.ktor.server.testing.testApplication
import org.koin.dsl.module
import org.koin.ktor.ext.getKoin
import java.sql.SQLException
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class ServerWiringTest {
    @Test
    fun theDatabaseIsClosedWhenTheApplicationStops() {
        lateinit var database: Database
        withServer { database = this.database }
        assertFailsWith<SQLException> { database.transactionBlocking { it.createStatement().execute("SELECT 1") } }
    }

    @Test
    fun eachServerHasItsOwnInstances() {
        var first: Database? = null
        var second: Database? = null
        withServer { first = database }
        withServer { second = database }
        assertNotSame(first, second)
    }

    @Test
    fun singletonsAreSharedWithinOneServer() {
        withServer { assertSame(koin.get<AuthService>(), koin.get<AuthService>()) }
    }

    @Test
    fun everyServiceTheRoutesNeedCanBeBuilt() = withServer {
        listOf(
            ProfileService::class, LinkService::class, ProjectService::class, ImageService::class,
            ImageCleanupService::class, AuthService::class, Transactor::class,
        ).forEach { type -> assertNotNull(koin.get<Any>(type), "$type should be provided") }
    }

    @Test
    fun overridesReplaceDefinitions() {
        val fixed = Clock { Instant.parse("2030-05-06T00:00:00Z") }
        var seen: Clock? = null
        testApplication {
            application {
                serverApplication(tempConfig(), overrides = listOf(module { single<Clock> { fixed } }))
                seen = getKoin().get()
            }
            startApplication()
        }
        assertSame(fixed, seen)
    }
}

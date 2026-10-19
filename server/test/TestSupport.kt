package com.usuallycoding.portfolio.server

import com.usuallycoding.portfolio.PortfolioApi
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant

const val TEST_PASSWORD = "correct horse"

class MutableClock(var instant: Instant = Instant.parse("2026-01-01T00:00:00Z")) : Clock {
    override fun now(): Instant = instant
    fun advance(duration: Duration) {
        instant = instant.plus(duration)
    }
}

fun tempConfig(dir: Path = Files.createTempDirectory("portfolio-test"), password: String = TEST_PASSWORD) =
    ServerConfig(
        ownerPassword = password,
        databasePath = dir.resolve("portfolio.db"),
        imagesDir = dir.resolve("images"),
    )

class TestEnv(
    val context: ServerContext,
    val clock: MutableClock,
    val builder: ApplicationTestBuilder,
) {
    val client: HttpClient = builder.createClient { install(ContentNegotiation) { json() } }
    val api = PortfolioApi("http://localhost", client)
    val raw: HttpClient get() = builder.client
}

/** Runs [block] against a fresh server with its own database and images directory. */
fun withServer(
    config: ServerConfig = tempConfig(),
    clock: MutableClock = MutableClock(),
    block: suspend TestEnv.() -> Unit,
) {
    val context = createContext(config, clock)
    try {
        testApplication {
            application { module(context) }
            TestEnv(context, clock, this).block()
        }
    } finally {
        context.database.close()
    }
}

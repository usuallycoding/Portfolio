package com.usuallycoding.portfolio.server

import com.usuallycoding.portfolio.server.data.*
import com.usuallycoding.portfolio.server.service.*

import com.usuallycoding.portfolio.server.config.*
import com.usuallycoding.portfolio.server.domain.*
import com.usuallycoding.portfolio.PortfolioApi
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.koin.core.Koin
import org.koin.ktor.ext.getKoin
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
    val koin: Koin,
    val clock: MutableClock,
    val builder: ApplicationTestBuilder,
) {
    val client: HttpClient = builder.createClient { install(ContentNegotiation) { json() } }
    val api = PortfolioApi("http://localhost", client)
    val raw: HttpClient get() = builder.client

    val config: ServerConfig get() = koin.get()
    val database: Database get() = koin.get()
    val cleanup: ImageCleanupService get() = koin.get()

    /** Where an uploaded image (or its thumbnail) lives on disk. */
    fun imageFile(fileName: String, thumbnail: Boolean = false): Path =
        config.imagesDir.resolve(if (thumbnail) thumbnailName(fileName) else fileName)
}

/** Runs [block] against a fresh server with its own database, images directory and Koin. */
fun withServer(
    config: ServerConfig = tempConfig(),
    clock: MutableClock = MutableClock(),
    block: suspend TestEnv.() -> Unit,
) {
    testApplication {
        lateinit var koin: Koin
        application {
            serverApplication(config, clock)
            koin = getKoin()
        }
        startApplication()
        TestEnv(koin, clock, this).block()
    }
}

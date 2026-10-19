package com.usuallycoding.portfolio.server

import com.usuallycoding.portfolio.server.data.*
import com.usuallycoding.portfolio.server.service.*

import com.usuallycoding.portfolio.ApiException
import com.usuallycoding.portfolio.ImageDto
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Uploads a JPEG of the given size as the owner and returns it. */
suspend fun TestEnv.upload(width: Int, height: Int): ImageDto {
    if (api.token == null) api.login(TEST_PASSWORD)
    return api.uploadImage(TestImages.jpeg(width, height), "img.jpg", "image/jpeg")
}

suspend fun TestEnv.square() = upload(400, 400)
suspend fun TestEnv.cover() = upload(900, 300)
suspend fun TestEnv.wide() = upload(1600, 900)

/** Calls [block] as a visitor: a client that never logged in. */
suspend fun <T> TestEnv.asVisitor(block: suspend com.usuallycoding.portfolio.PortfolioApi.() -> T): T =
    com.usuallycoding.portfolio.PortfolioApi("http://localhost", client).block()

suspend fun assertStatus(expected: Int, block: suspend () -> Unit) {
    assertEquals(expected, assertFailsWith<ApiException> { block() }.status)
}

fun TestEnv.imageRowExists(id: String): Boolean =
    database.transactionBlocking { conn -> SqliteImageRepository(conn).find(id) != null }

/** Ages everything past the grace period and runs the cleanup. */
suspend fun TestEnv.ageAndCleanUp(): CleanupResult {
    clock.advance(Duration.ofHours(2))
    return cleanup.run()
}

suspend fun TestEnv.statusOf(path: String): HttpStatusCode = raw.get(path).status

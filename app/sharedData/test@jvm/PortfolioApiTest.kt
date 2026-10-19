package com.usuallycoding.portfolio

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class PortfolioApiTest {
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private fun api(engine: MockEngine) = PortfolioApi(
        "http://host:8080/",
        HttpClient(engine) { install(ContentNegotiation) { json(Json) } },
    )

    @Test
    fun publicCallsSendNoToken() = runTest {
        val engine = MockEngine { request ->
            assertNull(request.headers[HttpHeaders.Authorization])
            assertEquals("/api/profile", request.url.encodedPath)
            respond("""{"name":"n","description":"d"}""", headers = jsonHeaders)
        }
        val api = api(engine)
        api.token = "ignored-for-public"
        assertEquals("n", api.profile().name)
    }

    @Test
    fun loginStoresTokenAndAdminCallsUseIt() = runTest {
        val engine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/admin/login" -> respond("""{"token":"abc","expiresAtEpochSeconds":9}""", headers = jsonHeaders)
                else -> {
                    assertEquals("Bearer abc", request.headers[HttpHeaders.Authorization])
                    assertEquals(HttpMethod.Put, request.method)
                    assertEquals("/admin/links/order", request.url.encodedPath)
                    respond("[]", headers = jsonHeaders)
                }
            }
        }
        val api = api(engine)
        api.login("pw")
        assertEquals("abc", api.token)
        api.reorderLinks(listOf(2, 1))
    }

    @Test
    fun errorsBecomeApiException() = runTest {
        val api = api(MockEngine { respondError(HttpStatusCode.Unauthorized, "no") })
        val e = assertFailsWith<ApiException> { api.login("bad") }
        assertEquals(401, e.status)
    }

    @Test
    fun imageUrlsUseOriginalOrThumbnail() {
        val api = api(MockEngine { respond("") })
        assertEquals("http://host:8080/images/x.jpg", api.imageUrl("x.jpg"))
        assertEquals("http://host:8080/images/x.jpg?size=thumb", api.imageUrl("x.jpg", thumbnail = true))
    }

    @Test
    fun uploadSendsMultipartWithToken() = runTest {
        val engine = MockEngine { request ->
            assertEquals("Bearer t", request.headers[HttpHeaders.Authorization])
            assertEquals("/admin/images", request.url.encodedPath)
            respond("""{"id":"i","fileName":"i.png","width":1,"height":1}""", headers = jsonHeaders)
        }
        val api = api(engine)
        api.token = "t"
        assertEquals("i.png", api.uploadImage(byteArrayOf(1, 2, 3), "p.png", "image/png").fileName)
    }
}

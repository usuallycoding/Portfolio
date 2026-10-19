package com.usuallycoding.portfolio.server

import io.ktor.client.request.header
import io.ktor.client.request.options
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CorsTest {
    private suspend fun TestEnv.preflight(origin: String, method: String = "PUT", headers: String = "authorization,content-type") =
        raw.options("/admin/links/order") {
            header(HttpHeaders.Origin, origin)
            header(HttpHeaders.AccessControlRequestMethod, method)
            header(HttpHeaders.AccessControlRequestHeaders, headers)
        }

    @Test
    fun browserAppsMayCallTheApiWithTokenAndJson() = withServer {
        val response = preflight("http://localhost:8081")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("*", response.headers[HttpHeaders.AccessControlAllowOrigin])
    }

    @Test
    fun configuredOriginsRestrictAccess() = withServer(config = tempConfig().copy(corsOrigins = listOf("http://localhost:8081"))) {
        assertEquals("http://localhost:8081", preflight("http://localhost:8081").headers[HttpHeaders.AccessControlAllowOrigin])
        assertNull(preflight("http://evil.example").headers[HttpHeaders.AccessControlAllowOrigin])
    }

    @Test
    fun originsAreParsedFromTheEnvironment() {
        val config = ServerConfig.fromEnvironment(
            mapOf("PORTFOLIO_PASSWORD" to "x", "PORTFOLIO_CORS_ORIGINS" to "http://a.test:1, https://b.test ,"),
        )
        assertEquals(listOf("http://a.test:1", "https://b.test"), config.corsOrigins)
    }
}

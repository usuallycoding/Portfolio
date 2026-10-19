package com.usuallycoding.portfolio.server

import com.usuallycoding.portfolio.server.config.*
import com.usuallycoding.portfolio.ApiException
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AuthTest {
    @Test
    fun correctPasswordIssuesAWorkingToken() = withServer {
        val login = api.login(TEST_PASSWORD)
        assertTrue(login.token.isNotBlank())
        val response = raw.get("/admin/session") { bearerAuth(login.token) }
        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun wrongPasswordIs401AndIssuesNoToken() = withServer {
        val e = assertFailsWith<ApiException> { api.login("wrong") }
        assertEquals(401, e.status)
        assertEquals(null, api.token)
    }

    @Test
    fun adminRouteWithoutTokenIs401() = withServer {
        assertEquals(HttpStatusCode.Unauthorized, raw.get("/admin/session").status)
    }

    @Test
    fun adminRouteWithUnknownTokenIs401() = withServer {
        assertEquals(HttpStatusCode.Unauthorized, raw.get("/admin/session") { bearerAuth("made-up") }.status)
    }

    @Test
    fun expiredTokenIs401() = withServer {
        val token = api.login(TEST_PASSWORD).token
        clock.advance(config.tokenTtl.plusSeconds(1))
        assertEquals(HttpStatusCode.Unauthorized, raw.get("/admin/session") { bearerAuth(token) }.status)
    }

    @Test
    fun tokensDifferPerLogin() = withServer {
        assertNotEquals(api.login(TEST_PASSWORD).token, api.login(TEST_PASSWORD).token)
    }

    @Test
    fun tokenIsNotStoredInClearText() = withServer {
        val token = api.login(TEST_PASSWORD).token
        val stored = database.transaction { conn ->
            conn.createStatement().use { st ->
                st.executeQuery("SELECT token_hash FROM auth_tokens").use { rs -> buildList { while (rs.next()) add(rs.getString(1)) } }
            }
        }
        assertFalse(token in stored)
    }

    @Test
    fun repeatedFailuresAreThrottledThenRecover() = withServer {
        repeat(5) { assertEquals(401, assertFailsWith<ApiException> { api.login("wrong") }.status) }
        // Even the right password is rejected while throttled.
        assertEquals(429, assertFailsWith<ApiException> { api.login(TEST_PASSWORD) }.status)
        clock.advance(Duration.ofMinutes(11))
        assertTrue(api.login(TEST_PASSWORD).token.isNotBlank())
    }

    @Test
    fun serverRefusesToStartWithoutPassword() {
        val e = assertFailsWith<IllegalStateException> { ServerConfig.fromEnvironment(emptyMap()) }
        assertTrue("PORTFOLIO_PASSWORD" in e.message.orEmpty())
        assertFailsWith<IllegalStateException> { ServerConfig.fromEnvironment(mapOf("PORTFOLIO_PASSWORD" to "  ")) }
    }

    @Test
    fun publicRootNeedsNoToken() = withServer {
        assertEquals(HttpStatusCode.OK, raw.get("/").status)
    }
}

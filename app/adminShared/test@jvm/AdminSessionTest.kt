package com.usuallycoding.portfolio.admin

import com.usuallycoding.portfolio.admin.data.*
import com.usuallycoding.portfolio.admin.presentation.*

import com.usuallycoding.portfolio.ApiException
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AdminSessionTest {
    private val loginOk = """{"token":"tok","expiresAtEpochSeconds":1}"""

    @Test
    fun startsLoggedOut() {
        assertFalse(testSession { json("{}") }.loggedIn.value)
    }

    @Test
    fun loginStoresTheTokenAndUsesItForAdminCalls() = runTest {
        val log = mutableListOf<HttpRequestData>()
        val session = testSession(log) { request ->
            if (request.url.encodedPath == "/admin/login") json(loginOk) else json("""{"name":"n","description":"d"}""")
        }
        session.login("pw")
        assertTrue(session.loggedIn.value)

        session.call { adminProfile() }

        assertEquals("Bearer tok", log.last().headers[HttpHeaders.Authorization])
    }

    @Test
    fun wrongPasswordKeepsTheSessionLoggedOut() = runTest {
        val session = testSession { status(HttpStatusCode.Unauthorized) }
        assertEquals(401, assertFailsWith<ApiException> { session.login("bad") }.status)
        assertFalse(session.loggedIn.value)
    }

    @Test
    fun aRejectedTokenLogsOutAndRethrows() = runTest {
        val session = testSession { request ->
            if (request.url.encodedPath == "/admin/login") json(loginOk) else status(HttpStatusCode.Unauthorized)
        }
        session.login("pw")

        assertEquals(401, assertFailsWith<ApiException> { session.call { adminLinks() } }.status)

        assertFalse(session.loggedIn.value)
        assertNull(session.api.token)
    }

    @Test
    fun otherErrorsDoNotLogOut() = runTest {
        val session = testSession { request ->
            if (request.url.encodedPath == "/admin/login") json(loginOk) else status(HttpStatusCode.BadRequest)
        }
        session.login("pw")
        assertFailsWith<ApiException> { session.call { adminLinks() } }
        assertTrue(session.loggedIn.value)
    }

    @Test
    fun logoutClearsTheTokenForLaterRequests() = runTest {
        val log = mutableListOf<HttpRequestData>()
        val session = testSession(log) { request ->
            if (request.url.encodedPath == "/admin/login") json(loginOk) else json("[]")
        }
        session.login("pw")
        session.logout()
        session.call { links() }
        assertNull(log.last().headers[HttpHeaders.Authorization])
    }

    @Test
    fun errorMessagesAreHumanFriendly() {
        assertEquals("Your session expired. Please log in again.", ApiException(401, "").adminMessage())
        assertEquals("Too many attempts. Try again in a few minutes.", ApiException(429, "").adminMessage())
        assertEquals("title must not be empty", ApiException(400, """{"message":"title must not be empty"}""").adminMessage())
        assertTrue("Could not reach the server" in java.io.IOException("x").adminMessage())
    }
}

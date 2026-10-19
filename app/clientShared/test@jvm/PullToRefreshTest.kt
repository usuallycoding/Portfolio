package com.usuallycoding.portfolio.client

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.test.swipeDown
import com.usuallycoding.portfolio.ui.ThemePreference
import com.usuallycoding.portfolio.ui.ThemeStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class PullToRefreshTest {
    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private class MemoryStore : ThemeStore {
        override fun load() = ThemePreference.System
        override fun save(preference: ThemePreference) {}
    }

    private class Server {
        var name = "Ada"
        var summary = "first edition"
        var down = false
        val requests = mutableListOf<String>()

        fun client() = HttpClient(MockEngine) {
            engine {
                dispatcher = Dispatchers.Unconfined
                addHandler { request ->
                    val path = request.url.encodedPath
                    requests += path
                    if (path.startsWith("/images")) return@addHandler respondError(HttpStatusCode.NotFound)
                    if (down) return@addHandler respondError(HttpStatusCode.ServiceUnavailable)
                    val body = when (path) {
                        "/api/profile" -> """{"name":"$name","description":"d"}"""
                        "/api/links" -> "[]"
                        "/api/projects" -> """[{"id":7,"title":"Chapter 1","createdAt":"2026-01-02","isPublic":true,"summary":"$summary"}]"""
                        "/api/projects/7" -> """{"id":7,"title":"Chapter 1","createdAt":"2026-01-02","isPublic":true,"summary":"$summary","images":[{"id":"a","fileName":"a.jpg","width":800,"height":600}]}"""
                        else -> return@addHandler respondError(HttpStatusCode.NotFound)
                    }
                    respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                }
            }
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        fun apiRequests() = requests.filter { it.startsWith("/api") }
    }

    private fun ComposeUiTest.app(server: Server, pullToRefresh: Boolean) {
        val client = server.client()
        setContent {
            // Inspection mode keeps Coil from loading images on its own threads.
            CompositionLocalProvider(LocalInspectionMode provides true) {
                ClientApp("http://server", themeStore = MemoryStore(), platform = if (pullToRefresh) ClientPlatform.Mobile else ClientPlatform.Web, httpClient = client)
            }
        }
    }

    private fun ComposeUiTest.pullDown() {
        onRoot().performTouchInput { swipeDown(startY = height * 0.15f, endY = height * 0.8f, durationMillis = 400) }
        waitForIdle()
    }

    @Test
    fun pullingDownOnTheProfileReloadsItAndShowsNewContent() = runDesktopComposeUiTest(480, 900) {
        val server = Server()
        app(server, pullToRefresh = true)
        onNodeWithText("Ada").assertIsDisplayed()
        val before = server.apiRequests().size

        server.name = "Ada Lovelace"
        pullDown()

        onNodeWithText("Ada Lovelace").assertIsDisplayed()
        assertEquals(3, server.apiRequests().size - before, "profile, links and projects were requested again")
    }

    @Test
    fun pullingDownOnAProjectReloadsItAndKeepsTheReaderView() = runDesktopComposeUiTest(480, 900) {
        val server = Server()
        app(server, pullToRefresh = true)
        onNodeWithText("Chapter 1").performClick()
        onNodeWithText("first edition").assertIsDisplayed()
        onNodeWithText("Reader").performClick()
        onNodeWithTag("reader").assertIsDisplayed()

        server.summary = "second edition"
        pullDown()

        onNodeWithText("second edition").assertIsDisplayed()
        onNodeWithTag("reader").assertIsDisplayed() // still the reader, not reset to the grid
    }

    @Test
    fun aFailedRefreshKeepsTheScreenAndOffersRetry() = runDesktopComposeUiTest(480, 900) {
        val server = Server()
        app(server, pullToRefresh = true)
        onNodeWithText("Ada").assertIsDisplayed()

        server.down = true
        pullDown()

        onNodeWithText("Ada").assertIsDisplayed()
        awaitText("Retry")
        val failedRequests = server.apiRequests().size

        server.down = false
        server.name = "Back"
        onNodeWithText("Retry").performClick()
        awaitText("Back")
        assertTrue(server.apiRequests().size > failedRequests)
    }

    @Test
    fun withoutPullToRefreshNothingReloads() = runDesktopComposeUiTest(480, 900) {
        val server = Server()
        app(server, pullToRefresh = false)
        onNodeWithText("Ada").assertIsDisplayed()
        val before = server.apiRequests().size

        server.name = "Changed"
        pullDown()

        assertEquals(before, server.apiRequests().size, "the web client must not refresh on pull")
        onNodeWithText("Ada").assertIsDisplayed()
    }
}

package com.usuallycoding.portfolio.client

import com.usuallycoding.portfolio.client.presentation.*

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
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

/** A stand-in for the browser's address bar and history. */
class FakeRouteHost(initialHash: String = "") : RouteHost {
    private val history = mutableListOf(Routes.parse(initialHash))
    private var index = 0
    private val listeners = mutableListOf<(Route) -> Unit>()
    val pushes = mutableListOf<Route>()

    val entries: List<Route> get() = history.toList()
    override fun current(): Route = history[index]
    override fun push(route: Route) {
        if (route == current()) return
        pushes += route
        while (history.size > index + 1) history.removeLast()
        history += route
        index++
        // A real browser also reports the change it just made; the app must cope with that echo.
        listeners.toList().forEach { it(route) }
    }
    override fun replace(route: Route) { history[index] = route }
    override fun onChanged(listener: (Route) -> Unit): () -> Unit { listeners += listener; return { listeners -= listener } }

    fun back() { if (index > 0) { index--; listeners.toList().forEach { it(current()) } } }
    fun forward() { if (index < history.size - 1) { index++; listeners.toList().forEach { it(current()) } } }
    /** The visitor edits the address by hand. */
    fun navigateTo(hash: String) = push(Routes.parse(hash))
    val listenerCount: Int get() = listeners.size
}

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class RouteNavigationTest {
    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private class MemoryStore : ThemeStore {
        override fun load() = ThemePreference.System
        override fun save(preference: ThemePreference) {}
    }

    private fun client() = HttpClient(MockEngine) {
        engine {
            dispatcher = Dispatchers.Unconfined
            addHandler { request ->
                val body = when (request.url.encodedPath) {
                    "/api/profile" -> """{"name":"Ada","description":"d"}"""
                    "/api/links" -> "[]"
                    "/api/projects" -> """[{"id":7,"title":"Chapter 1","createdAt":"2026-01-02","isPublic":true,"summary":"first"},{"id":8,"title":"Chapter 2","createdAt":"2026-01-03","isPublic":true,"summary":"second"}]"""
                    "/api/projects/7" -> project(7, "Chapter 1", "first chapter text")
                    "/api/projects/8" -> project(8, "Chapter 2", "second chapter text")
                    else -> return@addHandler respondError(HttpStatusCode.NotFound)
                }
                respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    private fun project(id: Int, title: String, summary: String) =
        """{"id":$id,"title":"$title","createdAt":"2026-01-02","isPublic":true,"summary":"$summary","images":[]}"""

    private fun ComposeUiTest.app(host: RouteHost?, platform: ClientPlatform = ClientPlatform.Web) {
        val client = client()
        setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                ClientApp("http://server", themeStore = MemoryStore(), platform = platform, routeHost = host, httpClient = client)
            }
        }
    }

    @Test
    fun openingAProjectAddsAHistoryEntryForIt() = runDesktopComposeUiTest(900, 900) {
        val host = FakeRouteHost()
        app(host)
        onNodeWithText("Ada").assertIsDisplayed()
        assertEquals<List<Route>>(listOf(Route.Home), host.entries)

        onNodeWithText("Chapter 2").performClick()

        onNodeWithText("second chapter text").assertIsDisplayed()
        assertEquals<List<Route>>(listOf(Route.Project(8)), host.pushes)
        assertEquals(Route.Project(8), host.current())
    }

    @Test
    fun theBrowsersBackAndForwardMoveBetweenTheProfileAndTheProject() = runDesktopComposeUiTest(900, 900) {
        val host = FakeRouteHost()
        app(host)
        onNodeWithText("Chapter 1").performClick()
        onNodeWithText("first chapter text").assertIsDisplayed()

        host.back()
        waitForIdle()
        onNodeWithText("Ada").assertIsDisplayed()
        assertTrue(runCatching { onNodeWithText("first chapter text").assertIsDisplayed() }.isFailure)

        host.forward()
        waitForIdle()
        onNodeWithText("first chapter text").assertIsDisplayed()
    }

    @Test
    fun aDeepLinkOpensThatProjectDirectly() = runDesktopComposeUiTest(900, 900) {
        app(FakeRouteHost("#/projects/8"))
        onNodeWithText("second chapter text").assertIsDisplayed()
        onNodeWithTag("grid").assertIsDisplayed()
    }

    @Test
    fun anUnknownOrEmptyAddressShowsTheProfile() {
        for (hash in listOf("", "#/", "#/nonsense", "#/projects/abc", "#/projects/7/extra")) {
            runDesktopComposeUiTest(900, 900) {
                app(FakeRouteHost(hash))
                onNodeWithText("Ada").assertIsDisplayed()
            }
        }
    }

    @Test
    fun anEditedAddressSwitchesPages() = runDesktopComposeUiTest(900, 900) {
        val host = FakeRouteHost()
        app(host)
        onNodeWithText("Ada").assertIsDisplayed()

        host.navigateTo("#/projects/7")
        waitForIdle()
        onNodeWithText("first chapter text").assertIsDisplayed()

        host.navigateTo("#/projects/8")
        waitForIdle()
        onNodeWithText("second chapter text").assertIsDisplayed()

        host.navigateTo("#/whatever")
        waitForIdle()
        onNodeWithText("Ada").assertIsDisplayed()
    }

    @Test
    fun aMissingProjectShowsTheRecoverableErrorAndTheProfileIsStillReachable() = runDesktopComposeUiTest(900, 900) {
        val host = FakeRouteHost("#/projects/999")
        app(host)
        awaitText("Retry")
        host.back() // nothing before it: the app stays put, no crash
        host.navigateTo("#/")
        waitForIdle()
        onNodeWithText("Ada").assertIsDisplayed()
    }

    @Test
    fun theAppsOwnPushIsNotMistakenForAnExternalChange() = runDesktopComposeUiTest(900, 900) {
        val host = FakeRouteHost()
        app(host)
        onNodeWithText("Chapter 1").performClick()
        onNodeWithText("Reader").performClick()
        waitForIdle()
        // The echo of the push must not have reset anything; the reader choice stands.
        onNodeWithTag("reader").assertIsDisplayed()
    }

    @Test
    fun withoutAHostNavigationStaysInMemoryLikeOnPhones() = runDesktopComposeUiTest(900, 900) {
        app(null, ClientPlatform.Mobile)
        onNodeWithText("Chapter 1").performClick()
        onNodeWithText("first chapter text").assertIsDisplayed()
        onNodeWithContentDescription("Back").performClick()
        onNodeWithText("Ada").assertIsDisplayed()
    }

    @Test
    fun theAppStopsListeningWhenItLeavesTheComposition() = runDesktopComposeUiTest(900, 900) {
        val host = FakeRouteHost()
        val show = androidx.compose.runtime.mutableStateOf(true)
        val client = client()
        setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                if (show.value) ClientApp("http://server", themeStore = MemoryStore(), routeHost = host, httpClient = client)
            }
        }
        waitForIdle()
        assertEquals(1, host.listenerCount)
        show.value = false
        waitForIdle()
        assertEquals(0, host.listenerCount, "a leaked listener would keep driving a dead screen")
    }

    @Test
    fun theWebProjectPageHasNoBackControlButKeepsItsTitle() = runDesktopComposeUiTest(900, 900) {
        app(FakeRouteHost(), ClientPlatform.Web)
        onNodeWithText("Chapter 1").performClick()
        onNodeWithText("first chapter text").assertIsDisplayed()
        assertTrue(onAllNodesWithContentDescription("Back").fetchSemanticsNodes().isEmpty(), "the browser's back button is used on the web")
        onAllNodesWithText("Chapter 1").fetchSemanticsNodes().let { assertTrue(it.isNotEmpty(), "the title is still shown") }
    }

    @Test
    fun thePhoneProjectPageKeepsItsBackControlAndItWorks() = runDesktopComposeUiTest(900, 900) {
        app(null, ClientPlatform.Mobile)
        onNodeWithText("Chapter 2").performClick()
        onNodeWithText("second chapter text").assertIsDisplayed()
        onNodeWithContentDescription("Back").assertIsDisplayed()
        onNodeWithContentDescription("Back").performClick()
        onNodeWithText("Ada").assertIsDisplayed()
    }

    @Test
    fun onThePhoneTheBackControlStillReturnsWhenAHostIsPresent() = runDesktopComposeUiTest(900, 900) {
        // A phone-sized browser still has the browser's own back; the control is a platform rule, not a host rule.
        val host = FakeRouteHost()
        app(host, ClientPlatform.Mobile)
        onNodeWithText("Chapter 1").performClick()
        onNodeWithContentDescription("Back").performClick()
        onNodeWithText("Ada").assertIsDisplayed()
        assertEquals(Route.Home, host.current(), "going back via the control also moves the address back to the profile")
    }
}

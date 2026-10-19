package com.usuallycoding.portfolio.client

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
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

/** The expanded sections must survive opening a project and coming back, since the profile leaves the screen. */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class ExpandedStateNavigationTest {
    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private class MemoryStore : ThemeStore {
        override fun load() = ThemePreference.System
        override fun save(preference: ThemePreference) {}
    }

    private val projectCount = 12 // two per row on a phone: six rows
    private val linkCount = 8

    private fun client() = HttpClient(MockEngine) {
        engine {
            dispatcher = Dispatchers.Unconfined
            addHandler { request ->
                val path = request.url.encodedPath
                val body = when {
                    path == "/api/profile" -> """{"name":"Ada","description":"d"}"""
                    path == "/api/links" -> (1..linkCount).joinToString(",", "[", "]") { """{"id":$it,"name":"Link $it","address":"https://a.b/$it","isPublic":true}""" }
                    path == "/api/projects" -> (1..projectCount).joinToString(",", "[", "]") { """{"id":$it,"title":"Project $it","createdAt":"2026-01-02","isPublic":true,"summary":"s$it"}""" }
                    path.startsWith("/api/projects/") -> {
                        val id = path.substringAfterLast('/')
                        """{"id":$id,"title":"Project $id","createdAt":"2026-01-02","isPublic":true,"summary":"project $id text","images":[]}"""
                    }
                    else -> return@addHandler respondError(HttpStatusCode.NotFound)
                }
                respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    private fun ComposeUiTest.app() {
        val client = client()
        setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                ClientApp("http://server", themeStore = MemoryStore(), platform = ClientPlatform.Mobile, httpClient = client)
            }
        }
    }

    private fun ComposeUiTest.cards() = (1..projectCount).filter { onAllNodesWithTag("project-card-$it").fetchSemanticsNodes().isNotEmpty() }
    private fun ComposeUiTest.has(description: String) = onAllNodesWithContentDescription(description).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun theAppStartsWithEverySectionCollapsed() = runDesktopComposeUiTest(411, 6000) {
        app()
        assertEquals((1..4).toList(), cards(), "two rows of two projects")
        onNodeWithContentDescription("Show more projects").assertIsDisplayed()
        onNodeWithContentDescription("Show more socials").assertIsDisplayed()
    }

    @Test
    fun expandedSocialsStayExpandedAfterOpeningAProjectAndComingBack() = runDesktopComposeUiTest(411, 6000) {
        app()
        onNodeWithContentDescription("Show more socials").performClick()
        waitForIdle()
        assertEquals(true, onAllNodesWithTag("link-8").fetchSemanticsNodes().isNotEmpty(), "the eighth link is shown once expanded")

        onNodeWithTag("project-card-1").performClick()
        onNodeWithText("project 1 text").assertIsDisplayed()
        onNodeWithContentDescription("Back").performClick()
        waitForIdle()

        onNodeWithContentDescription("Show fewer socials").assertIsDisplayed()
        assertEquals(true, onAllNodesWithTag("link-8").fetchSemanticsNodes().isNotEmpty())
        onNodeWithContentDescription("Show more projects").assertIsDisplayed() // the projects were not touched
    }

    @Test
    fun anExpandedProjectListStaysExpandedAfterOpeningAProjectAndComingBack() = runDesktopComposeUiTest(411, 6000) {
        app()
        onNodeWithContentDescription("Show more projects").performClick()
        waitForIdle()
        assertEquals((1..12).toList(), cards())

        onNodeWithTag("project-card-11").performClick() // one that was hidden before expanding
        onNodeWithText("project 11 text").assertIsDisplayed()
        onNodeWithContentDescription("Back").performClick()
        waitForIdle()

        assertEquals((1..12).toList(), cards(), "still showing every project")
        onNodeWithContentDescription("Show fewer projects").assertIsDisplayed()
    }

    @Test
    fun aCollapsedListStaysCollapsedAfterTheSameTrip() = runDesktopComposeUiTest(411, 6000) {
        app()
        onNodeWithTag("project-card-3").performClick()
        onNodeWithContentDescription("Back").performClick()
        waitForIdle()
        assertEquals((1..4).toList(), cards())
        onNodeWithContentDescription("Show more projects").assertIsDisplayed()
    }

    @Test
    fun theTwoSectionsKeepTheirOwnStateAcrossTheTrip() = runDesktopComposeUiTest(411, 6000) {
        app()
        onNodeWithContentDescription("Show more projects").performClick()
        waitForIdle()
        onNodeWithTag("project-card-1").performClick()
        onNodeWithContentDescription("Back").performClick()
        waitForIdle()
        onNodeWithContentDescription("Show fewer projects").assertIsDisplayed()
        onNodeWithContentDescription("Show more socials").assertIsDisplayed() // the socials were never expanded
        assertEquals(false, has("Show fewer socials"))
    }
}

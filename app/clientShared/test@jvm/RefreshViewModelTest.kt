package com.usuallycoding.portfolio.client

import com.usuallycoding.portfolio.client.presentation.*
import com.usuallycoding.portfolio.client.domain.HomeData

import com.usuallycoding.portfolio.PortfolioApi
import com.usuallycoding.portfolio.ProjectDto
import com.usuallycoding.portfolio.ui.Load
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CompletableDeferred
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
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RefreshViewModelTest {
    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    /** A server whose answers the test controls: what the profile says, whether it is up, and when it replies. */
    private class Server {
        var name = "Ada"
        var projectSummary = "v1"
        var imageOrder = listOf("a", "b")
        var down = false
        var gate: CompletableDeferred<Unit>? = null
        var requests = 0

        fun api() = PortfolioApi("http://server", HttpClient(MockEngine) {
            engine {
                dispatcher = Dispatchers.Unconfined
                addHandler { request ->
                    requests++
                    gate?.await()
                    if (down) return@addHandler respondError(HttpStatusCode.ServiceUnavailable)
                    val body = when (request.url.encodedPath) {
                        "/api/profile" -> """{"name":"$name","description":"d"}"""
                        "/api/links" -> "[]"
                        "/api/projects" -> """[{"id":7,"title":"P","createdAt":"2026-01-02","isPublic":true,"summary":"$projectSummary"}]"""
                        "/api/projects/7" -> {
                            val images = imageOrder.joinToString(",") { """{"id":"$it","fileName":"$it.jpg","width":10,"height":10}""" }
                            """{"id":7,"title":"P","createdAt":"2026-01-02","isPublic":true,"summary":"$projectSummary","images":[$images]}"""
                        }
                        else -> return@addHandler respondError(HttpStatusCode.NotFound)
                    }
                    respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                }
            }
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        })
    }

    private fun HomeViewModel.data() = assertIs<Load.Loaded<HomeData>>(state.value).value
    private fun ProjectViewModel.project() = assertIs<Load.Loaded<ProjectDto>>(state.value).value

    @Test
    fun theContentStaysOnScreenWhileRefreshing() {
        val server = Server()
        val vm = HomeViewModel(server.api())
        val before = vm.data()

        server.gate = CompletableDeferred()
        vm.refresh()

        assertTrue(vm.refreshing.value)
        assertSame(before, vm.data(), "the old content must remain until new data arrives")
        server.gate!!.complete(Unit)
        assertFalse(vm.refreshing.value)
    }

    @Test
    fun newDataReplacesTheOldOnSuccess() {
        val server = Server()
        val vm = HomeViewModel(server.api())
        assertEquals("Ada", vm.data().profile.name)

        server.name = "Ada Lovelace"
        server.projectSummary = "v2"
        vm.refresh()

        assertEquals("Ada Lovelace", vm.data().profile.name)
        assertEquals("v2", vm.data().projects.single().summary)
        assertNull(vm.refreshError.value)
        assertFalse(vm.refreshing.value)
    }

    @Test
    fun aFailedRefreshKeepsTheContentAndReportsAnError() {
        val server = Server()
        val vm = HomeViewModel(server.api())
        val before = vm.data()

        server.down = true
        vm.refresh()

        assertSame(before, vm.data(), "still showing the last good content")
        assertTrue(vm.refreshError.value!!.contains("503"), vm.refreshError.value)
        assertFalse(vm.refreshing.value)

        vm.consumeRefreshError()
        assertNull(vm.refreshError.value)

        server.down = false
        server.name = "Back online"
        vm.refresh()
        assertEquals("Back online", vm.data().profile.name)
    }

    @Test
    fun anUnreachableServerGivesTheFriendlyMessage() {
        val flaky = object { var fail = false }
        val vm = HomeViewModel(PortfolioApi("http://server", HttpClient(MockEngine) {
            engine {
                dispatcher = Dispatchers.Unconfined
                addHandler { request ->
                    if (flaky.fail) throw java.io.IOException("connection refused")
                    val body = when (request.url.encodedPath) { "/api/profile" -> """{"name":"n","description":"d"}"""; else -> "[]" }
                    respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                }
            }
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }))
        flaky.fail = true
        vm.refresh()
        assertTrue("Could not reach the server" in vm.refreshError.value.orEmpty())
        assertIs<Load.Loaded<HomeData>>(vm.state.value)
    }

    @Test
    fun refreshingTwiceAtOnceSendsOnlyOneSetOfRequests() {
        val server = Server()
        val vm = HomeViewModel(server.api())
        val afterLoad = server.requests
        server.gate = CompletableDeferred()
        vm.refresh()
        vm.refresh()
        vm.refresh()
        server.gate!!.complete(Unit)
        assertEquals(3, server.requests - afterLoad, "profile, links and projects once")
    }

    @Test
    fun theFullScreenLoadStillShowsLoadingAndFailedStates() {
        val server = Server()
        server.down = true
        val vm = HomeViewModel(server.api())
        assertIs<Load.Failed>(vm.state.value)

        server.down = false
        server.gate = CompletableDeferred()
        vm.load()
        assertIs<Load.Loading>(vm.state.value, "load() replaces the screen with the loading state")
        server.gate!!.complete(Unit)
        assertIs<Load.Loaded<HomeData>>(vm.state.value)
    }

    @Test
    fun refreshWhileNotLoadedDoesAFullLoad() {
        val server = Server()
        server.down = true
        val vm = HomeViewModel(server.api())
        assertIs<Load.Failed>(vm.state.value)
        server.down = false
        vm.refresh()
        assertIs<Load.Loaded<HomeData>>(vm.state.value)
    }

    @Test
    fun aProjectRefreshPicksUpANewImageOrder() {
        val server = Server()
        val vm = ProjectViewModel(server.api(), 7)
        assertEquals(listOf("a", "b"), vm.project().images.map { it.id })

        server.imageOrder = listOf("c", "b", "a")
        vm.refresh()

        assertEquals(listOf("c", "b", "a"), vm.project().images.map { it.id })
    }

    @Test
    fun aFailedProjectRefreshKeepsTheProject() {
        val server = Server()
        val vm = ProjectViewModel(server.api(), 7)
        val before = vm.project()
        server.down = true
        vm.refresh()
        assertSame(before, vm.project())
        assertTrue(vm.refreshError.value != null)
        assertFalse(vm.refreshing.value)
    }
}

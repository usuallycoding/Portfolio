package com.usuallycoding.portfolio.client

import com.usuallycoding.portfolio.client.presentation.*
import com.usuallycoding.portfolio.client.domain.HomeData

import com.usuallycoding.portfolio.PortfolioApi
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelTest {
    private val json = headersOf(HttpHeaders.ContentType, "application/json")

    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun api(handler: (String) -> Pair<HttpStatusCode, String>) = PortfolioApi(
        "http://server",
        HttpClient(MockEngine { request ->
            val (status, body) = handler(request.url.encodedPath)
            if (status.value in 200..299) respond(body, status, json) else respondError(status)
        }) { install(ContentNegotiation) { json(Json) } },
    )

    private val ok = HttpStatusCode.OK

    /** Waits for a view model to finish loading; the mock engine answers on a real background thread. */
    private fun <T> Load<T>.settled(flow: kotlinx.coroutines.flow.StateFlow<Load<T>>): Load<T> =
        kotlinx.coroutines.runBlocking {
            kotlinx.coroutines.withTimeout(5_000) { flow.first { it !is Load.Loading } }
        }

    private fun HomeViewModel.settled() = state.value.settled(state)
    private fun ProjectViewModel.settled() = state.value.settled(state)

    private fun serve(path: String): Pair<HttpStatusCode, String> = when (path) {
        "/api/profile" -> ok to """{"name":"Ada","description":"Hi"}"""
        "/api/links" -> ok to """[{"id":1,"name":"GitHub","address":"https://github.com","isPublic":true}]"""
        "/api/projects" -> ok to """[{"id":7,"title":"App","createdAt":"2026-01-02","isPublic":true,"summary":"s"}]"""
        "/api/projects/7" -> ok to """{"id":7,"title":"App","createdAt":"2026-01-02","isPublic":true,"summary":"s","images":[]}"""
        else -> HttpStatusCode.NotFound to ""
    }

    @Test
    fun homeLoadsProfileLinksAndProjectsTogether() {
        val vm = HomeViewModel(api(::serve))
        val data = assertIs<Load.Loaded<HomeData>>(vm.settled()).value
        assertEquals("Ada", data.profile.name)
        assertEquals(listOf("GitHub"), data.links.map { it.name })
        assertEquals(listOf(7L), data.projects.map { it.id })
    }

    @Test
    fun homeFailsGracefullyWhenOneCallFails() {
        val vm = HomeViewModel(api { path -> if (path == "/api/links") HttpStatusCode.InternalServerError to "" else serve(path) })
        val failed = assertIs<Load.Failed>(vm.settled())
        assertTrue("500" in failed.message, failed.message)
    }

    @Test
    fun retryRecoversAfterAFailure() {
        var healthy = false
        val vm = HomeViewModel(api { path -> if (healthy) serve(path) else HttpStatusCode.ServiceUnavailable to "" })
        assertIs<Load.Failed>(vm.settled())

        healthy = true
        vm.load()

        assertIs<Load.Loaded<HomeData>>(vm.settled())
    }

    @Test
    fun unreachableServerGivesAFriendlyMessageNotACrash() {
        val api = PortfolioApi("http://server", HttpClient(MockEngine { throw java.io.IOException("connection refused") }) {
            install(ContentNegotiation) { json(Json) }
        })
        val failed = assertIs<Load.Failed>(HomeViewModel(api).settled())
        assertTrue("Could not reach the server" in failed.message, failed.message)
    }

    @Test
    fun placeholderProfileLoadsWithoutErrors() {
        val vm = HomeViewModel(api { path ->
            if (path == "/api/profile") ok to """{"name":"Your name","description":"This portfolio has not been set up yet."}"""
            else if (path == "/api/projects" || path == "/api/links") ok to "[]" else serve(path)
        })
        val data = assertIs<Load.Loaded<HomeData>>(vm.settled()).value
        assertEquals("Your name", data.profile.name)
        assertEquals(null, data.profile.profileImage)
        assertTrue(data.projects.isEmpty())
    }

    @Test
    fun projectLoadsAndMissingProjectFails() {
        assertEquals("App", assertIs<Load.Loaded<*>>(ProjectViewModel(api(::serve), 7).settled()).let { (it.value as com.usuallycoding.portfolio.ProjectDto).title })
        assertIs<Load.Failed>(ProjectViewModel(api(::serve), 99).settled())
    }
}

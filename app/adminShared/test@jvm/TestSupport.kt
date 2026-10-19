package com.usuallycoding.portfolio.admin

import com.usuallycoding.portfolio.admin.data.*
import com.usuallycoding.portfolio.admin.presentation.*

import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import com.usuallycoding.portfolio.admin.domain.AdminRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

fun MockRequestHandleScope.json(body: String, status: HttpStatusCode = HttpStatusCode.OK) = respond(body, status, jsonHeaders)
fun MockRequestHandleScope.status(status: HttpStatusCode) = respondError(status)

/** A session backed by [handler]; requests are also appended to [log]. */
fun testSession(
    log: MutableList<HttpRequestData> = mutableListOf(),
    handler: suspend MockRequestHandleScope.(HttpRequestData) -> io.ktor.client.request.HttpResponseData,
): AdminSession {
    // The engine runs on the calling thread so that view-model state is written from the test's own thread;
    // answering from a background thread made UI tests racy.
    val client = HttpClient(MockEngine) {
        engine {
            dispatcher = kotlinx.coroutines.Dispatchers.Unconfined
            addHandler { request -> log += request; handler(request) }
        }
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }
    return AdminSession("http://server", client)
}

/**
 * Waits for [text] to appear. Data loads finish on background threads, so the test must keep syncing with
 * the UI between checks; plain waitUntil can miss late recompositions of lazy lists.
 */
@OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
fun androidx.compose.ui.test.ComposeUiTest.awaitText(text: String, timeoutMs: Long = 10_000) {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (onAllNodesWithText(text).fetchSemanticsNodes().isEmpty()) {
        check(System.currentTimeMillis() < deadline) { "Timed out waiting for text \"$text\"" }
        waitForIdle()
        Thread.sleep(20)
    }
}

/**
 * Wraps UI under test with the theme and a Koin that serves [session]. Inspection mode stops Coil from loading
 * images on its own threads, which would otherwise race with recomposition in tests.
 */
@androidx.compose.runtime.Composable
fun TestContent(session: AdminSession, content: @androidx.compose.runtime.Composable () -> Unit) {
    val koin = androidx.compose.runtime.remember(session) {
        org.koin.dsl.koinApplication {
            modules(org.koin.dsl.module { single<AdminRepository> { AdminRepositoryImpl(session) } })
        }
    }
    org.koin.compose.KoinIsolatedContext(koin) {
        com.usuallycoding.portfolio.ui.PortfolioTheme {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalInspectionMode provides true,
            ) { content() }
        }
    }
}

/** The whole app over [session], as the shells would build it but with the test's own server. */
@androidx.compose.runtime.Composable
fun AdminApp(
    session: AdminSession,
    themeStore: com.usuallycoding.portfolio.ui.ThemeStore = androidx.compose.runtime.remember { com.usuallycoding.portfolio.ui.SettingsThemeStore() },
) {
    val overrides = androidx.compose.runtime.remember(session) { listOf(org.koin.dsl.module { single { session } }) }
    AdminApp("http://server", themeStore, overrides)
}

/** Like [awaitText], for a node found by its content description (icon buttons). */
@OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
fun androidx.compose.ui.test.ComposeUiTest.awaitDescription(description: String, timeoutMs: Long = 10_000) {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (onAllNodesWithContentDescription(description).fetchSemanticsNodes().isEmpty()) {
        check(System.currentTimeMillis() < deadline) { "Timed out waiting for description \"$description\"" }
        waitForIdle()
        Thread.sleep(20)
    }
}

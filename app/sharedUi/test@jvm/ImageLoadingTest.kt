package com.usuallycoding.portfolio.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.runDesktopComposeUiTest
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ImageLoadingTest {
    private fun composeOnce() = runDesktopComposeUiTest(200, 200) {
        val client = HttpClient(MockEngine { respondError(HttpStatusCode.NotFound) })
        setContent { ProvideImageLoading(client) { PortfolioImage("http://x/a.jpg", "photo") } }
        onNodeWithContentDescription("photo").assertExists()
    }

    /**
     * Android recreates the activity (for example on rotation) and composes the whole UI again in the same process.
     * Configuring Coil's global singleton on every composition throws the second time; the provided loader must not.
     */
    @Test
    fun composingTheAppAgainInTheSameProcessDoesNotThrow() {
        composeOnce()
        composeOnce()
        composeOnce()
    }

    @Test
    fun imagesStillRenderWithoutAProvidedLoader() = runDesktopComposeUiTest(200, 200) {
        setContent { PortfolioImage("http://x/a.jpg", "photo") }
        onNodeWithContentDescription("photo").assertExists()
    }
}

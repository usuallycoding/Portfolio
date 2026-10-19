package com.usuallycoding.portfolio.client

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.usuallycoding.portfolio.client.presentation.ClientPlatform
import kotlinx.browser.window

/**
 * Served by the dev server (`localhost:8080`) the API is on `localhost:8090`; anywhere else a proxy in front of the
 * page forwards the API paths, so the page's own origin is the server address.
 */
private fun serverUrl(): String {
    val location = window.location
    return if (location.hostname == "localhost" && location.port == "8080") "http://localhost:8090" else location.origin
}

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val routeHost = HashRouteHost()
    ComposeViewport {
        ClientApp(serverUrl(), platform = ClientPlatform.Web, routeHost = routeHost)
    }
}

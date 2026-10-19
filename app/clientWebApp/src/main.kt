package com.usuallycoding.portfolio.client

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.usuallycoding.portfolio.client.presentation.ClientPlatform

/** Change this for a deployed server. The server must allow this page's origin (see PORTFOLIO_CORS_ORIGINS). */
private const val SERVER_URL = "http://localhost:8090"

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val routeHost = HashRouteHost()
    ComposeViewport {
        ClientApp(SERVER_URL, platform = ClientPlatform.Web, routeHost = routeHost)
    }
}

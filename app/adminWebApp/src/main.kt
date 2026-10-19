package com.usuallycoding.portfolio.admin

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport

/** Change this for a deployed server. The server must allow this page's origin (see PORTFOLIO_CORS_ORIGINS). */
private const val SERVER_URL = "http://localhost:8090"

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport {
        AdminApp(SERVER_URL)
    }
}

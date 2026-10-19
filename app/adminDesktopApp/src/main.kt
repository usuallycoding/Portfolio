package com.usuallycoding.portfolio.admin

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

/** Override with the PORTFOLIO_SERVER_URL environment variable. */
private val SERVER_URL = System.getenv("PORTFOLIO_SERVER_URL") ?: "http://localhost:8090"

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Portfolio Admin",
        state = rememberWindowState(size = DpSize(1100.dp, 800.dp)),
    ) {
        AdminApp(SERVER_URL)
    }
}

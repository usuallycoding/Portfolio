package com.usuallycoding.portfolio

import androidx.compose.ui.window.ComposeUIViewController
import com.usuallycoding.portfolio.client.ClientApp
import com.usuallycoding.portfolio.client.ClientPlatform

/** The iOS simulator shares the host's network. Change this for a deployed server. */
private const val SERVER_URL = "http://localhost:8090"

/**
 * [onSystemBarsVisibleChanged] is called with false when the status bar should hide (reading a project) and true
 * when it should show; the SwiftUI host applies it.
 */
fun ViewController(onSystemBarsVisibleChanged: (Boolean) -> Unit) = ComposeUIViewController {
    ClientApp(SERVER_URL, platform = ClientPlatform.Mobile, onSystemBarsVisibleChanged = onSystemBarsVisibleChanged)
}

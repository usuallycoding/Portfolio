package com.usuallycoding.portfolio.client

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.usuallycoding.portfolio.client.di.clientModules
import com.usuallycoding.portfolio.client.presentation.ClientNavigation
import com.usuallycoding.portfolio.client.presentation.ClientPlatform
import com.usuallycoding.portfolio.client.presentation.RouteHost
import com.usuallycoding.portfolio.createHttpClient
import com.usuallycoding.portfolio.ui.PortfolioTheme
import com.usuallycoding.portfolio.ui.ProvideImageLoading
import com.usuallycoding.portfolio.ui.SettingsThemeStore
import com.usuallycoding.portfolio.ui.ThemeStore
import com.usuallycoding.portfolio.ui.rememberThemeController
import io.ktor.client.HttpClient
import org.jetbrains.compose.reload.DevelopmentEntryPoint
import org.koin.compose.KoinIsolatedContext
import org.koin.compose.koinInject
import org.koin.dsl.koinApplication

/**
 * The read-only portfolio app, and the place where its dependencies are wired: the shells pass in what differs per
 * device and Koin builds the rest. [serverUrl] is the base URL of the portfolio server, e.g. `http://10.0.2.2:8090`.
 */
@Composable
@Preview
@DevelopmentEntryPoint
fun ClientApp(
    serverUrl: String = "http://localhost:8090",
    /** Called with true when the dark theme is showing and false for light, e.g. to match the system bars. */
    onDarkThemeChanged: (Boolean) -> Unit = {},
    themeStore: ThemeStore = remember { SettingsThemeStore() },
    /** Decides platform-specific behavior such as pull to refresh. The Android and iOS shells pass [ClientPlatform.Mobile]. */
    platform: ClientPlatform = ClientPlatform.Web,
    /** Called with false when the system status bar should hide (reading a project) and true when it should show. */
    onSystemBarsVisibleChanged: (Boolean) -> Unit = {},
    /** Ties the current page to an address (the browser's URL on the web). Null on phones, which keep their own navigation. */
    routeHost: RouteHost? = null,
    httpClient: HttpClient = remember { createHttpClient() },
) {
    // An isolated Koin per composition: the compose KoinApplication shares one global instance, which would keep
    // the first server URL and HTTP client alive for as long as the process lives.
    val koin = remember(serverUrl, platform, themeStore, httpClient) {
        koinApplication { modules(clientModules(serverUrl, platform, themeStore, httpClient)) }
    }
    DisposableEffect(koin) { onDispose { koin.close() } }
    KoinIsolatedContext(koin) {
        ClientContent(onDarkThemeChanged, onSystemBarsVisibleChanged, routeHost)
    }
}

@Composable
private fun ClientContent(
    onDarkThemeChanged: (Boolean) -> Unit,
    onSystemBarsVisibleChanged: (Boolean) -> Unit,
    routeHost: RouteHost?,
) {
    val themeController = rememberThemeController(koinInject<ThemeStore>())
    val dark = themeController.preference.isDark(isSystemInDarkTheme())
    LaunchedEffect(dark) { onDarkThemeChanged(dark) }

    ProvideImageLoading(koinInject<HttpClient>()) {
        PortfolioTheme(themeController.preference) {
            Surface(Modifier.fillMaxSize()) {
                ClientNavigation(themeController, koinInject<ClientPlatform>(), onSystemBarsVisibleChanged, routeHost)
            }
        }
    }
}

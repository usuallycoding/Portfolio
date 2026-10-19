package com.usuallycoding.portfolio.admin

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.usuallycoding.portfolio.admin.di.adminModules
import com.usuallycoding.portfolio.admin.domain.AdminRepository
import com.usuallycoding.portfolio.admin.presentation.AdminHome
import com.usuallycoding.portfolio.admin.presentation.LoginScreen
import com.usuallycoding.portfolio.ui.PortfolioTheme
import com.usuallycoding.portfolio.ui.ProvideImageLoading
import com.usuallycoding.portfolio.ui.SettingsThemeStore
import com.usuallycoding.portfolio.ui.ThemeStore
import com.usuallycoding.portfolio.ui.rememberThemeController
import io.ktor.client.HttpClient
import org.jetbrains.compose.reload.DevelopmentEntryPoint
import org.koin.compose.KoinIsolatedContext
import org.koin.compose.koinInject
import org.koin.core.module.Module
import org.koin.dsl.koinApplication

/**
 * The owner's editing app, and the place where its dependencies are wired: the shells pass in what differs per
 * device and Koin builds the rest. [serverUrl] is the base URL of the portfolio server. [overrides] replace any
 * definition, which is how tests supply their own.
 */
@Composable
@Preview
@DevelopmentEntryPoint
fun AdminApp(
    serverUrl: String = "http://localhost:8090",
    themeStore: ThemeStore = remember { SettingsThemeStore() },
    overrides: List<Module> = emptyList(),
) {
    // An isolated Koin per composition: the compose KoinApplication shares one global instance (see ClientApp).
    val koin = remember(serverUrl, themeStore, overrides) {
        koinApplication { modules(adminModules(serverUrl, themeStore, overrides)) }
    }
    DisposableEffect(koin) { onDispose { koin.close() } }
    KoinIsolatedContext(koin) { AdminContent() }
}

@Composable
private fun AdminContent() {
    val repository = koinInject<AdminRepository>()
    val themeController = rememberThemeController(koinInject<ThemeStore>())
    val loggedIn by repository.loggedIn.collectAsState()
    var everLoggedIn by rememberSaveable { mutableStateOf(false) }
    if (loggedIn) everLoggedIn = true

    ProvideImageLoading(koinInject<HttpClient>()) {
        PortfolioTheme(themeController.preference) {
            Surface(Modifier.fillMaxSize()) {
                if (loggedIn) {
                    AdminHome(themeController)
                } else {
                    LoginScreen(notice = if (everLoggedIn) "You have been logged out." else null)
                }
            }
        }
    }
}

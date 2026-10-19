package com.usuallycoding.portfolio.admin

import androidx.compose.foundation.layout.Row
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.usuallycoding.portfolio.ui.ProvideImageLoading
import com.usuallycoding.portfolio.ui.PortfolioTheme
import com.usuallycoding.portfolio.ui.SettingsThemeStore
import com.usuallycoding.portfolio.ui.ThemeController
import com.usuallycoding.portfolio.ui.ThemePicker
import com.usuallycoding.portfolio.ui.ThemeStore
import com.usuallycoding.portfolio.ui.rememberThemeController
import org.jetbrains.compose.reload.DevelopmentEntryPoint

private enum class Section(val title: String) { Profile("Profile"), Links("Links"), Projects("Projects") }

/** Which section is open. Held outside the composition so logging out and back in returns to the same place. */
private class AdminNavigation : ViewModel() {
    var section by mutableStateOf(Section.Profile)
}

/** The owner's editing app. [serverUrl] is the base URL of the portfolio server. */
@Composable
@Preview
@DevelopmentEntryPoint
fun AdminApp(serverUrl: String = "http://localhost:8090") {
    val session = remember(serverUrl) { AdminSession(serverUrl) }
    AdminApp(session)
}

@Composable
fun AdminApp(session: AdminSession, themeStore: ThemeStore = remember { SettingsThemeStore() }) {
    val themeController = rememberThemeController(themeStore)
    val loggedIn by session.loggedIn.collectAsState()
    var everLoggedIn by rememberSaveable { mutableStateOf(false) }
    if (loggedIn) everLoggedIn = true

    ProvideImageLoading(session.httpClient) {
        PortfolioTheme(themeController.preference) {
            Surface(Modifier.fillMaxSize()) {
                CompositionLocalProvider(LocalAdminSession provides session) {
                    if (loggedIn) {
                        AdminHome(session, themeController)
                    } else {
                        LoginScreen(session, notice = if (everLoggedIn) "You have been logged out." else null)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminHome(session: AdminSession, themeController: ThemeController) {
    val navigation = viewModel { AdminNavigation() }
    val section = navigation.section
    Row(Modifier.fillMaxSize()) {
        NavigationRail {
            Section.entries.forEach { entry ->
                NavigationRailItem(
                    selected = section == entry,
                    onClick = { navigation.section = entry },
                    icon = {},
                    label = { Text(entry.title) },
                    alwaysShowLabel = true,
                )
            }
            ThemePicker(themeController)
            TextButton(onClick = session::logout) { Text("Log out") }
        }
        when (section) {
            Section.Profile -> ProfileScreen(viewModel { ProfileViewModel(session) }, Modifier.weight(1f))
            Section.Links -> LinksScreen(viewModel { LinksViewModel(session) }, Modifier.weight(1f))
            Section.Projects -> ProjectsScreen(session, viewModel { ProjectsViewModel(session) }, Modifier.weight(1f))
        }
    }
}

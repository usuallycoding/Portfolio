package com.usuallycoding.portfolio.admin.presentation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.usuallycoding.portfolio.admin.domain.AdminRepository
import com.usuallycoding.portfolio.ui.ThemeController
import com.usuallycoding.portfolio.ui.ThemePicker
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private enum class Section(val title: String) { Profile("Profile"), Links("Links"), Projects("Projects") }

/** Which section is open. Held outside the composition so logging out and back in returns to the same place. */
private class AdminNavigation : ViewModel() {
    var section by mutableStateOf(Section.Profile)
}

/** What the owner sees once logged in: the section list and the open section. */
@Composable
fun AdminHome(themeController: ThemeController) {
    val repository = koinInject<AdminRepository>()
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
            TextButton(onClick = repository::logout) { Text("Log out") }
        }
        when (section) {
            Section.Profile -> ProfileScreen(koinViewModel<ProfileViewModel>(), Modifier.weight(1f))
            Section.Links -> LinksScreen(koinViewModel<LinksViewModel>(), Modifier.weight(1f))
            Section.Projects -> ProjectsScreen(koinViewModel<ProjectsViewModel>(), Modifier.weight(1f))
        }
    }
}

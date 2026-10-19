package com.usuallycoding.portfolio.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

/** The current theme choice as Compose state; every change is remembered through [store]. */
class ThemeController(private val store: ThemeStore) {
    var preference by mutableStateOf(store.load())
        private set

    fun select(choice: ThemePreference) {
        preference = choice
        store.save(choice)
    }
}

@Composable
fun rememberThemeController(store: ThemeStore = remember { SettingsThemeStore() }): ThemeController =
    remember(store) { ThemeController(store) }

/**
 * An icon button that opens a menu with System, Light and Dark. The icon shows the current choice (auto, sun or moon)
 * and the button is labelled "Theme: <choice>"; the menu keeps text labels and marks the current choice.
 */
@Composable
fun ThemePicker(controller: ThemeController, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        IconButton(onClick = { open = true }) {
            Icon(controller.preference.icon(), contentDescription = "Theme: ${controller.preference.label}")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            ThemePreference.entries.forEach { choice ->
                DropdownMenuItem(
                    text = { Text(if (choice == controller.preference) "✓ ${choice.label}" else choice.label) },
                    leadingIcon = { Icon(choice.icon(), contentDescription = null) },
                    onClick = {
                        controller.select(choice)
                        open = false
                    },
                )
            }
        }
    }
}

private fun ThemePreference.icon(): ImageVector = when (this) {
    ThemePreference.System -> Icons.Filled.BrightnessAuto
    ThemePreference.Light -> Icons.Filled.LightMode
    ThemePreference.Dark -> Icons.Filled.DarkMode
}

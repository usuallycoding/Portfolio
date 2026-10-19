package com.usuallycoding.portfolio.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ThemePreferenceTest {
    @Test
    fun systemFollowsTheDeviceAndTheOthersIgnoreIt() {
        assertTrue(ThemePreference.System.isDark(true))
        assertEquals(false, ThemePreference.System.isDark(false))
        assertEquals(false, ThemePreference.Light.isDark(true))
        assertEquals(true, ThemePreference.Dark.isDark(false))
    }

    @Test
    fun parsingIsForgiving() {
        assertEquals(ThemePreference.Dark, ThemePreference.parse("Dark"))
        assertEquals(ThemePreference.System, ThemePreference.parse(null))
        assertEquals(ThemePreference.System, ThemePreference.parse("Sepia"))
        assertEquals(ThemePreference.System, ThemePreference.parse(""))
    }
}

class ThemeStoreTest {
    @Test
    fun firstLaunchIsSystem() {
        assertEquals(ThemePreference.System, SettingsThemeStore { MapSettings() }.load())
    }

    @Test
    fun aChoiceIsRememberedAcrossStoreInstances() {
        val settings = MapSettings()
        SettingsThemeStore { settings }.save(ThemePreference.Dark)
        assertEquals(ThemePreference.Dark, SettingsThemeStore { settings }.load())
        SettingsThemeStore { settings }.save(ThemePreference.System)
        assertEquals(ThemePreference.System, SettingsThemeStore { settings }.load())
    }

    @Test
    fun aCorruptStoredValueFallsBackToSystem() {
        val settings = MapSettings("theme_preference" to "purple-polka-dots")
        assertEquals(ThemePreference.System, SettingsThemeStore { settings }.load())
    }

    @Test
    fun aWrongTypeStoredValueFallsBackToSystem() {
        val settings = MapSettings("theme_preference" to 42)
        assertEquals(ThemePreference.System, SettingsThemeStore { settings }.load())
    }

    @Test
    fun unavailableStorageNeverThrows() {
        val broken = SettingsThemeStore { error("localStorage is blocked") }
        assertEquals(ThemePreference.System, broken.load())
        broken.save(ThemePreference.Dark) // must not throw
    }
}

@OptIn(ExperimentalTestApi::class)
class ThemeControllerTest {
    private class FakeStore(var stored: ThemePreference = ThemePreference.System) : ThemeStore {
        override fun load() = stored
        override fun save(preference: ThemePreference) { stored = preference }
    }

    @Test
    fun startsFromTheStoredChoiceAndSavesEveryChange() {
        val store = FakeStore(ThemePreference.Dark)
        val controller = ThemeController(store)
        assertEquals(ThemePreference.Dark, controller.preference)
        controller.select(ThemePreference.Light)
        assertEquals(ThemePreference.Light, controller.preference)
        assertEquals(ThemePreference.Light, store.stored)
    }

    @Test
    fun darkAndLightSchemesAreActuallyDarkAndLight() = runDesktopComposeUiTest {
        var background = -1f
        var onBackground = -1f
        val pref = mutableStateOf(ThemePreference.Light)
        setContent {
            PortfolioTheme(pref.value, systemIsDark = false) {
                background = MaterialTheme.colorScheme.background.luminance()
                onBackground = MaterialTheme.colorScheme.onBackground.luminance()
            }
        }
        assertTrue(background > 0.8f && onBackground < 0.2f, "light: $background / $onBackground")
        pref.value = ThemePreference.Dark
        waitForIdle()
        assertTrue(background < 0.2f && onBackground > 0.7f, "dark: $background / $onBackground")
    }

    @Test
    fun systemFollowsTheDeviceWhileItIsOpen() = runDesktopComposeUiTest {
        var background = -1f
        val deviceDark = mutableStateOf(false)
        setContent {
            PortfolioTheme(ThemePreference.System, systemIsDark = deviceDark.value) {
                background = MaterialTheme.colorScheme.background.luminance()
            }
        }
        assertTrue(background > 0.8f)
        deviceDark.value = true
        waitForIdle()
        assertTrue(background < 0.2f, "should switch without restarting")
    }

    @Test
    fun overridingTheDeviceSticksUntilBackToSystem() = runDesktopComposeUiTest {
        var background = -1f
        val pref = mutableStateOf(ThemePreference.Light)
        setContent {
            PortfolioTheme(pref.value, systemIsDark = true) { background = MaterialTheme.colorScheme.background.luminance() }
        }
        assertTrue(background > 0.8f, "Light wins over a dark device")
        pref.value = ThemePreference.System
        waitForIdle()
        assertTrue(background < 0.2f, "System follows the dark device again")
    }

    @Test
    fun pickerListsTheThreeChoicesAndAppliesTheSelection() = runDesktopComposeUiTest {
        val store = FakeStore()
        val controller = ThemeController(store)
        setContent { PortfolioTheme { ThemePicker(controller) } }
        onNodeWithContentDescription("Theme: System").performClick()
        onNodeWithText("✓ System").assertExists()
        onNodeWithText("Light").assertExists()
        onNodeWithText("Dark").performClick()
        assertEquals(ThemePreference.Dark, controller.preference)
        assertEquals(ThemePreference.Dark, store.stored)
        onNodeWithContentDescription("Theme: Dark").assertExists()

        onNodeWithContentDescription("Theme: Dark").performClick()
        onNodeWithText("System").performClick()
        assertEquals(ThemePreference.System, controller.preference)
    }

    @Test
    fun theButtonIsAnIconLabelledWithTheCurrentChoice() = runDesktopComposeUiTest {
        val controller = ThemeController(FakeStore())
        setContent { PortfolioTheme { ThemePicker(controller) } }
        // An icon button: its label is a content description, not visible text.
        onNodeWithContentDescription("Theme: System").assertExists()
        onNodeWithText("Theme: System").assertDoesNotExist()
        for (choice in ThemePreference.entries) {
            controller.select(choice)
            waitForIdle()
            onNodeWithContentDescription("Theme: ${choice.label}").assertExists()
        }
    }

    @Test
    fun theMenuKeepsTextLabelsAndMarksTheCurrentChoice() = runDesktopComposeUiTest {
        val controller = ThemeController(FakeStore(ThemePreference.Light))
        setContent { PortfolioTheme { ThemePicker(controller) } }
        onNodeWithContentDescription("Theme: Light").performClick()
        onNodeWithText("System").assertExists()
        onNodeWithText("✓ Light").assertExists()
        onNodeWithText("Dark").assertExists()
    }
}

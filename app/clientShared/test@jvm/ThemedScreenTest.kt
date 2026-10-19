package com.usuallycoding.portfolio.client

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.usuallycoding.portfolio.ImageDto
import com.usuallycoding.portfolio.ProfileDto
import com.usuallycoding.portfolio.ProjectDto
import com.usuallycoding.portfolio.ProjectSummaryDto
import com.usuallycoding.portfolio.SocialLinkDto
import com.usuallycoding.portfolio.ui.PortfolioTheme
import com.usuallycoding.portfolio.ui.ThemeController
import com.usuallycoding.portfolio.ui.ThemePreference
import com.usuallycoding.portfolio.ui.ThemeStore
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ThemedScreenTest {
    private val noImages: ImageUrl = { _, _ -> null }

    private class MemoryStore(var value: ThemePreference = ThemePreference.System) : ThemeStore {
        override fun load() = value
        override fun save(preference: ThemePreference) { value = preference }
    }

    private val home = HomeData(
        profile = ProfileDto("Ada Lovelace", "Mathematician and first programmer."),
        links = listOf(SocialLinkDto(1, "GitHub", "https://github.com/ada", true)),
        projects = listOf(ProjectSummaryDto(7, "Analytical Engine", "2026-01-02", true, "Notes on the engine.")),
    )
    private val project = ProjectDto(
        7, "Analytical Engine", "2026-01-02", true, "Notes on the engine.", null,
        images = List(3) { ImageDto("i$it", "i$it.jpg", 800, 600) },
    )

    /** Luminance of a pixel in an otherwise empty corner of the screen: the screen's own background. */
    private fun ComposeUiTest.backgroundLuminance(name: String): Float {
        val image = onRoot().captureToImage()
        val dir = File("build/ui-screenshots").also { it.mkdirs() }
        ImageIO.write(image.toAwtImage(), "png", File(dir, "$name.png"))
        return image.toPixelMap()[2, image.height - 2].luminance()
    }

    @Test
    fun profileIsDarkInTheDarkThemeAndLightInTheLightTheme() {
        for ((preference, dark) in listOf(ThemePreference.Dark to true, ThemePreference.Light to false)) {
            runDesktopComposeUiTest(480, 1100) {
                setContent {
                    PortfolioTheme(preference) {
                        Surface(Modifier.fillMaxSize()) {
                            HomeContent(home, noImages, {}, {}, themeController = ThemeController(MemoryStore(preference)))
                        }
                    }
                }
                onNodeWithText("Ada Lovelace").assertIsDisplayed()
                onNodeWithText("Projects").assertIsDisplayed()
                val l = backgroundLuminance("home-${if (dark) "dark" else "light"}")
                if (dark) assertTrue(l < 0.2f, "dark background luminance $l") else assertTrue(l > 0.8f, "light background luminance $l")
            }
        }
    }

    @Test
    fun projectScreenFollowsTheTheme() {
        for ((preference, dark) in listOf(ThemePreference.Dark to true, ThemePreference.Light to false)) {
            runDesktopComposeUiTest(480, 900) {
                setContent {
                    PortfolioTheme(preference) {
                        Surface(Modifier.fillMaxSize()) { ProjectContent(project, noImages, {}, {}) }
                    }
                }
                onNodeWithText("Notes on the engine.").assertIsDisplayed()
                val l = backgroundLuminance("project-${if (dark) "dark" else "light"}")
                if (dark) assertTrue(l < 0.2f, "dark luminance $l") else assertTrue(l > 0.8f, "light luminance $l")
            }
        }
    }

    @Test
    fun errorAndEmptyStatesFollowTheTheme() = runDesktopComposeUiTest(480, 500) {
        setContent {
            PortfolioTheme(ThemePreference.Dark) {
                Surface(Modifier.fillMaxSize()) { com.usuallycoding.portfolio.ui.ErrorView("Could not reach the server.", {}) }
            }
        }
        onNodeWithText("Retry").assertIsDisplayed()
        assertTrue(backgroundLuminance("error-dark") < 0.2f)
    }

    @Test
    fun choosingAThemeInThePickerSwitchesTheProfileImmediatelyAndIsSaved() = runDesktopComposeUiTest(480, 1100) {
        val store = MemoryStore()
        val controller = ThemeController(store)
        setContent {
            // The device is light; the user's choice decides.
            PortfolioTheme(controller.preference, systemIsDark = false) {
                Surface(Modifier.fillMaxSize()) { HomeContent(home, noImages, {}, {}, themeController = controller) }
            }
        }
        assertTrue(backgroundLuminance("picker-before") > 0.8f)
        onNodeWithContentDescription("Theme: System").performClick()
        onNodeWithText("Dark").performClick()
        waitForIdle()
        assertTrue(backgroundLuminance("picker-after") < 0.2f)
        assertEquals(ThemePreference.Dark, store.value)
    }
}

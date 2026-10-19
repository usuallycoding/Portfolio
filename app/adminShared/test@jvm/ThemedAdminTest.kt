package com.usuallycoding.portfolio.admin

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
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.usuallycoding.portfolio.SocialLinkDto
import com.usuallycoding.portfolio.ui.PortfolioTheme
import com.usuallycoding.portfolio.ui.ThemePreference
import com.usuallycoding.portfolio.ui.ThemeStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class ThemedAdminTest {
    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private class MemoryStore(var value: ThemePreference = ThemePreference.System) : ThemeStore {
        override fun load() = value
        override fun save(preference: ThemePreference) { value = preference }
    }

    private fun ComposeUiTest.luminanceOfEmptyCorner(name: String): Float {
        val image = onRoot().captureToImage()
        val dir = File("build/ui-screenshots").also { it.mkdirs() }
        ImageIO.write(image.toAwtImage(), "png", File(dir, "$name.png"))
        return image.toPixelMap()[image.width - 2, image.height - 2].luminance()
    }

    private fun assertThemed(dark: Boolean, luminance: Float, what: String) =
        if (dark) assertTrue(luminance < 0.2f, "$what should be dark but luminance is $luminance")
        else assertTrue(luminance > 0.8f, "$what should be light but luminance is $luminance")

    private fun loggedIn(): Pair<FakeServer, AdminSession> {
        val server = FakeServer().apply { links += SocialLinkDto(1, "GitHub", "https://github.com", true) }
        return server to server.session().also { runBlocking { it.login("secret") } }
    }

    @Test
    fun loginScreenFollowsTheTheme() {
        for (dark in listOf(true, false)) runDesktopComposeUiTest(800, 500) {
            val store = MemoryStore(if (dark) ThemePreference.Dark else ThemePreference.Light)
            setContent { AdminApp(testSession { json("{}") }, store) }
            onNodeWithText("Portfolio admin").assertIsDisplayed()
            assertThemed(dark, luminanceOfEmptyCorner("admin-login-${if (dark) "dark" else "light"}"), "login screen")
        }
    }

    @Test
    fun everyEditingScreenFollowsTheTheme() {
        for (dark in listOf(true, false)) for (section in listOf("Profile", "Links", "Projects")) runDesktopComposeUiTest(1000, 700) {
            val (_, session) = loggedIn()
            val store = MemoryStore(if (dark) ThemePreference.Dark else ThemePreference.Light)
            setContent { AdminApp(session, store) }
            awaitText(section)
            onAllNodesWithText(section)[0].performClick()
            awaitText(when (section) { "Profile" -> "Profile picture"; "Links" -> "Social links"; else -> "New project" })
            assertThemed(dark, luminanceOfEmptyCorner("admin-${section.lowercase()}-${if (dark) "dark" else "light"}"), "$section screen")
        }
    }

    @Test
    fun cropDialogFollowsTheTheme() {
        val image = BufferedImage(1200, 800, BufferedImage.TYPE_INT_RGB).also {
            it.createGraphics().apply { color = Color(220, 120, 40); fillRect(0, 0, 1200, 800); dispose() }
        }
        val source = ImageProcessing.decode(ByteArrayOutputStream().also { ImageIO.write(image, "png", it) }.toByteArray())!!
        for (preference in listOf(ThemePreference.Dark, ThemePreference.Light)) runDesktopComposeUiTest(800, 700) {
            setContent { PortfolioTheme(preference) { Surface(Modifier.fillMaxSize()) { CropDialog(source, CropAspect.Cover, {}, {}) } } }
            onNodeWithText("Crop image (3:1)").assertIsDisplayed()
            onNodeWithText("Crop & upload").assertIsDisplayed()
            // The dialog is its own root; check the dialog surface itself.
            val dialog = onAllNodes(isRoot())[1].captureToImage()
            val dir = File("build/ui-screenshots").also { it.mkdirs() }
            ImageIO.write(dialog.toAwtImage(), "png", File(dir, "admin-crop-${preference.name.lowercase()}.png"))
            assertThemed(preference == ThemePreference.Dark, dialog.toPixelMap()[dialog.width / 2 - 270, dialog.height / 2].luminance(), "crop dialog")
        }
    }

    @Test
    fun pickerInTheRailSwitchesAndRemembersTheTheme() = runDesktopComposeUiTest(1000, 700) {
        val (_, session) = loggedIn()
        val store = MemoryStore()
        setContent { PortfolioTheme(ThemePreference.System, systemIsDark = false) { AdminApp(session, store) } }
        awaitDescription("Theme: System")
        onNodeWithContentDescription("Theme: System").performClick()
        onNodeWithText("Dark").performClick()
        waitForIdle()

        assertEquals(ThemePreference.Dark, store.value)
        awaitDescription("Theme: Dark")
        assertThemed(true, luminanceOfEmptyCorner("admin-after-picker"), "admin after choosing Dark")
        onNodeWithText("Log out").assertIsDisplayed()
    }

    @Test
    fun theChoiceSurvivesLoggingOutAndIn() = runDesktopComposeUiTest(1000, 700) {
        val (_, session) = loggedIn()
        val store = MemoryStore(ThemePreference.Dark)
        setContent { AdminApp(session, store) }
        awaitText("Log out")
        onNodeWithText("Log out").performClick()
        awaitText("Log in")
        assertThemed(true, luminanceOfEmptyCorner("admin-logged-out-dark"), "login after logout")
    }
}

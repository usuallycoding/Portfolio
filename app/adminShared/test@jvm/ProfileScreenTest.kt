package com.usuallycoding.portfolio.admin

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.test.waitUntilExactlyOneExists
import com.usuallycoding.portfolio.ProfileDto
import com.usuallycoding.portfolio.ui.PortfolioTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class ProfileScreenTest {
    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    @Test
    fun ownerEditsTheNameAndSaves() = runDesktopComposeUiTest(900, 1000) {
        val server = FakeServer().apply { profile = ProfileDto("Ada", "Analyst") }
        val session = server.session().also { runBlocking { it.login("secret") } }
        val vm = ProfileViewModel(session)
        setContent {
            PortfolioTheme { CompositionLocalProvider(LocalAdminSession provides session) { ProfileScreen(vm) } }
        }
        waitUntilExactlyOneExists(hasText("Profile picture"), 5_000)
        onNodeWithText("Cover image").assertIsDisplayed()
        onNodeWithText("Required ratio 1:1").assertIsDisplayed()
        onNodeWithText("Required ratio 3:1").assertIsDisplayed()
        onNodeWithText("Discard changes").assertIsNotEnabled()

        onNodeWithText("Ada").performTextClearance()
        onNodeWithText("Name").performTextInput("Grace Hopper")
        onNodeWithText("Discard changes").assertIsEnabled()
        onNodeWithText("You have unsaved changes.").assertIsDisplayed()
        val dir = File("build/ui-screenshots").also { it.mkdirs() }
        ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(dir, "admin-profile.png"))

        onNodeWithText("Save").performClick()
        waitUntilExactlyOneExists(hasText("Saved."), 5_000)
        assertEquals("Grace Hopper", server.profile.name)
    }

    @Test
    fun aFailedLoadShowsRetry() = runDesktopComposeUiTest(900, 600) {
        val server = FakeServer()
        val session = server.session().also { runBlocking { it.login("secret") } }
        server.rejectToken = true
        val vm = ProfileViewModel(session)
        setContent {
            PortfolioTheme { CompositionLocalProvider(LocalAdminSession provides session) { ProfileScreen(vm) } }
        }
        waitUntilExactlyOneExists(hasText("Retry"), 5_000)
        server.rejectToken = false
        onNodeWithText("Retry").performClick()
        waitUntilExactlyOneExists(hasText("Profile picture"), 5_000)
    }
}

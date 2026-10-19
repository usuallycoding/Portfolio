package com.usuallycoding.portfolio.admin

import com.usuallycoding.portfolio.admin.data.*
import com.usuallycoding.portfolio.admin.presentation.*

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.test.waitUntilExactlyOneExists
import androidx.compose.ui.test.hasText
import androidx.compose.ui.graphics.toAwtImage
import io.ktor.http.HttpStatusCode
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class LoginScreenTest {
    private fun session(correct: String = "secret") = testSession { request ->
        when {
            request.url.encodedPath != "/admin/login" -> json("[]")
            (request.body as io.ktor.http.content.TextContent).text.contains("\"$correct\"") -> json("""{"token":"t","expiresAtEpochSeconds":1}""")
            else -> status(HttpStatusCode.Unauthorized)
        }
    }

    @Test
    fun onlyTheLoginScreenIsShownBeforeLogin() = runDesktopComposeUiTest(900, 600) {
        setContent { AdminApp(session()) }
        onNodeWithText("Portfolio admin").assertIsDisplayed()
        onNodeWithText("Password").assertIsDisplayed()
        onNodeWithText("Log in").assertIsDisplayed()
        onNodeWithText("Profile").assertDoesNotExist()
        onNodeWithText("Projects").assertDoesNotExist()
        val dir = File("build/ui-screenshots").also { it.mkdirs() }
        ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(dir, "admin-login.png"))
    }

    @Test
    fun wrongPasswordShowsAnErrorAndStaysOnLogin() = runDesktopComposeUiTest(900, 600) {
        setContent { AdminApp(session()) }
        onNodeWithText("Password").performTextInput("nope")
        onNodeWithText("Log in").performClick()
        waitUntilExactlyOneExists(hasText("Wrong password."), 5_000)
        onNodeWithText("Projects").assertDoesNotExist()
    }

    @Test
    fun correctPasswordShowsTheEditingScreensAndLogoutReturns() = runDesktopComposeUiTest(900, 600) {
        setContent { AdminApp(session()) }
        onNodeWithText("Password").performTextInput("secret")
        onNodeWithText("Log in").performClick()
        waitUntilExactlyOneExists(hasText("Projects"), 5_000)
        onNodeWithText("Links").assertIsDisplayed()

        onNodeWithText("Log out").performClick()

        waitUntilExactlyOneExists(hasText("Log in"), 5_000)
        onNodeWithText("You have been logged out.").assertIsDisplayed()
    }
}

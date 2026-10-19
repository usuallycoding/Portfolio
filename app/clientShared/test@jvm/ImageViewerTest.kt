package com.usuallycoding.portfolio.client

import com.usuallycoding.portfolio.client.presentation.*

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.test.swipe
import com.usuallycoding.portfolio.ImageDto
import com.usuallycoding.portfolio.ProjectDto
import com.usuallycoding.portfolio.ui.PortfolioTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ImageViewerTest {
    private val noImages: ImageUrl = { _, _ -> null }
    private val project = ProjectDto(
        7, "Chapter 1", "2026-01-02", true, "s", null,
        images = List(5) { ImageDto("i$it", "i$it.jpg", 800, 600) },
    )

    private fun ComposeUiTest.viewer(start: Int, onClose: () -> Unit = {}) {
        setContent {
            androidx.compose.runtime.CompositionLocalProvider(LocalInspectionMode provides true) {
                PortfolioTheme { Surface(Modifier.fillMaxSize()) { ImageViewer(project, start, noImages, onClose) } }
            }
        }
    }

    private fun ComposeUiTest.press(key: Key) {
        // No click first: the viewer has to have taken the keyboard focus itself.
        onRoot().performKeyInput { pressKey(key) }
        waitForIdle()
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
    }

    @Test
    fun rightArrowShowsTheNextImageAndTheCounterFollows() = runDesktopComposeUiTest(900, 700) {
        viewer(start = 1)
        onNodeWithText("2 / 5").assertIsDisplayed()
        press(Key.DirectionRight)
        onNodeWithText("3 / 5").assertIsDisplayed()
        press(Key.DirectionRight)
        onNodeWithText("4 / 5").assertIsDisplayed()
    }

    @Test
    fun leftArrowShowsThePreviousImage() = runDesktopComposeUiTest(900, 700) {
        viewer(start = 3)
        press(Key.DirectionLeft)
        onNodeWithText("3 / 5").assertIsDisplayed()
        press(Key.DirectionLeft)
        onNodeWithText("2 / 5").assertIsDisplayed()
    }

    @Test
    fun theKeysStopAtTheEndsWithoutWrapping() = runDesktopComposeUiTest(900, 700) {
        viewer(start = 0)
        press(Key.DirectionLeft)
        onNodeWithText("1 / 5").assertIsDisplayed()
        press(Key.DirectionLeft)
        onNodeWithText("1 / 5").assertIsDisplayed()
    }

    @Test
    fun rightOnTheLastImageDoesNothing() = runDesktopComposeUiTest(900, 700) {
        viewer(start = 4)
        press(Key.DirectionRight)
        onNodeWithText("5 / 5").assertIsDisplayed()
        press(Key.DirectionRight)
        onNodeWithText("5 / 5").assertIsDisplayed()
    }

    @Test
    fun otherKeysDoNothing() = runDesktopComposeUiTest(900, 700) {
        viewer(start = 2)
        for (key in listOf(Key.DirectionUp, Key.DirectionDown, Key.Spacebar, Key.Enter, Key.A)) press(key)
        onNodeWithText("3 / 5").assertIsDisplayed()
    }

    @Test
    fun walkingAcrossAllImagesAndBack() = runDesktopComposeUiTest(900, 700) {
        viewer(start = 0)
        repeat(4) { press(Key.DirectionRight) }
        onNodeWithText("5 / 5").assertIsDisplayed()
        repeat(4) { press(Key.DirectionLeft) }
        onNodeWithText("1 / 5").assertIsDisplayed()
    }

    @Test
    fun swipingStillWorks() = runDesktopComposeUiTest(900, 700) {
        viewer(start = 1)
        onRoot().performTouchInput { swipe(start = Offset(700f, 300f), end = Offset(100f, 300f), durationMillis = 250) }
        waitForIdle()
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        onNodeWithText("3 / 5").assertIsDisplayed()
    }

    @Test
    fun theCloseControlIsAnIconButtonNotTheWord() = runDesktopComposeUiTest(900, 700) {
        var closed = 0
        viewer(start = 0, onClose = { closed++ })
        onNodeWithContentDescription("Close").assertIsDisplayed()
        assertTrue(runCatching { onNodeWithText("Close").assertIsDisplayed() }.isFailure, "the word Close is gone")
        val button = onNodeWithContentDescription("Close").fetchSemanticsNode().boundsInRoot
        assertTrue(button.right > 900f - 80f && button.top < 80f, "it sits at the top end ($button)")
        onNodeWithContentDescription("Close").performClick()
        assertEquals(1, closed)
        val dir = File("build/ui-screenshots").also { it.mkdirs() }
        ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(dir, "viewer.png"))
    }

    @Test
    fun keysStillWorkAfterTheViewerReopens() {
        // A reopened viewer (a new composition) must grab focus again, not rely on the previous one.
        for (round in 1..2) runDesktopComposeUiTest(900, 700) {
            viewer(start = 0)
            press(Key.DirectionRight)
            onNodeWithText("2 / 5").assertIsDisplayed()
        }
    }
}

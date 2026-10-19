package com.usuallycoding.portfolio.client

import com.usuallycoding.portfolio.client.presentation.*

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import com.usuallycoding.portfolio.ImageDto
import com.usuallycoding.portfolio.ProjectDto
import com.usuallycoding.portfolio.ui.PortfolioTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class AutoHideBarsTest {
    private val noImages: ImageUrl = { _, _ -> null }
    private val project = ProjectDto(
        7, "Chapter 1", "2026-01-02", true, "First chapter.", null,
        images = List(30) { ImageDto("i$it", "i$it.jpg", 800, 600) },
    )

    private class Seen { val calls = mutableListOf<Boolean>(); val last get() = calls.lastOrNull() }

    private fun ComposeUiTest.page(mode: GalleryMode, autoHide: Boolean, seen: Seen, show: androidx.compose.runtime.MutableState<Boolean> = mutableStateOf(true)) {
        setContent {
            androidx.compose.runtime.CompositionLocalProvider(LocalInspectionMode provides true) {
                PortfolioTheme {
                    Surface(Modifier.fillMaxSize()) {
                        if (show.value) {
                            ProjectContent(
                                project, noImages, onBack = {}, onOpenImage = {},
                                mode = mode, autoHideBars = autoHide, onBarsVisibleChange = { seen.calls += it },
                            )
                        }
                    }
                }
            }
        }
    }

    private fun SemanticsNodeInteraction.scrollDown() = performTouchInput { swipeUp(startY = height * 0.9f, endY = height * 0.2f, durationMillis = 300) }
    private fun SemanticsNodeInteraction.scrollUp() = performTouchInput { swipeDown(startY = height * 0.2f, endY = height * 0.9f, durationMillis = 300) }

    private fun ComposeUiTest.backIsShown() = onAllNodesWithBack().isNotEmpty()
    private fun ComposeUiTest.onAllNodesWithBack() =
        onAllNodesWithContentDescriptionCompat("Back")
    private fun ComposeUiTest.onAllNodesWithContentDescriptionCompat(d: String) =
        onAllNodesWithContentDescription(d).fetchSemanticsNodes()

    private fun ComposeUiTest.settle() {
        waitForIdle()
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
    }

    private fun scrollingDownHidesAndScrollingUpShows(mode: GalleryMode, tag: String) = runDesktopComposeUiTest(480, 900) {
        val seen = Seen()
        page(mode, autoHide = true, seen = seen)
        onNodeWithContentDescription("Back").assertIsDisplayed()
        assertEquals(true, seen.last)

        onNodeWithTag(tag).scrollDown()
        settle()
        assertFalse(backIsShown(), "the top bar hides while scrolling down in $mode")
        assertEquals(false, seen.last, "the status bar is asked to hide")

        onNodeWithTag(tag).scrollUp()
        settle()
        assertTrue(backIsShown(), "the top bar returns when scrolling up in $mode")
        assertEquals(true, seen.last, "the status bar is asked to show")
    }

    @Test fun inTheReader() = scrollingDownHidesAndScrollingUpShows(GalleryMode.Reader, "reader")
    @Test fun inTheGrid() = scrollingDownHidesAndScrollingUpShows(GalleryMode.Grid, "grid")

    @Test
    fun theBarsAlwaysComeBackAtTheTop() = runDesktopComposeUiTest(480, 900) {
        val seen = Seen()
        page(GalleryMode.Reader, autoHide = true, seen = seen)
        onNodeWithTag("reader").scrollDown()
        settle()
        assertFalse(backIsShown())
        // Scroll all the way back up: at the very top the bars are shown whatever the last direction was.
        repeat(8) { onNodeWithTag("reader").scrollUp() }
        settle()
        assertTrue(backIsShown())
        assertEquals(true, seen.last)
    }

    @Test
    fun leavingTheProjectWhileTheBarsAreHiddenRestoresThem() = runDesktopComposeUiTest(480, 900) {
        val seen = Seen()
        val show = mutableStateOf(true)
        page(GalleryMode.Reader, autoHide = true, seen = seen, show = show)
        onNodeWithTag("reader").scrollDown()
        settle()
        assertEquals(false, seen.last)

        show.value = false // back to the profile
        waitForIdle()

        assertEquals(true, seen.last, "the status bar must be shown again")
    }

    @Test
    fun theContentDoesNotMoveWhenTheTopBarHides() = runDesktopComposeUiTest(480, 900) {
        page(GalleryMode.Reader, autoHide = true, seen = Seen())
        onNodeWithTag("reader").scrollDown()
        waitForIdle()
        // Pick an image on screen right after the swipe, then let every animation finish.
        val visible = (1..30).first { onAllNodesWithContentDescription("Chapter 1 image $it").fetchSemanticsNodes().isNotEmpty() }
        val image = onNodeWithContentDescription("Chapter 1 image $visible")
        val before = image.fetchSemanticsNode().boundsInRoot
        mainClock.advanceTimeBy(2_000)
        waitForIdle()
        val after = image.fetchSemanticsNode().boundsInRoot
        assertEquals(before.top, after.top, 0.5f, "hiding or showing the bar must not shift the content")
        assertEquals(before.height, after.height, 0.5f)
    }

    @Test
    fun theTopBarOnlyHasAConstantTopPaddingSoTheFirstItemIsNotUnderIt() = runDesktopComposeUiTest(480, 900) {
        page(GalleryMode.Reader, autoHide = true, seen = Seen())
        val barBottom = onNodeWithContentDescription("Back").fetchSemanticsNode().boundsInRoot.bottom
        val firstImage = onNodeWithContentDescription("Chapter 1 image 1").fetchSemanticsNode().boundsInRoot.top
        assertTrue(firstImage >= barBottom - 1, "the first image ($firstImage) starts below the top bar ($barBottom)")
    }

    @Test
    fun onTheWebTheTopBarNeverHidesAndNothingIsReported() = runDesktopComposeUiTest(480, 900) {
        val seen = Seen()
        page(GalleryMode.Reader, autoHide = false, seen = seen)
        onNodeWithTag("reader").scrollDown()
        settle()
        assertTrue(backIsShown(), "the top bar stays on the web")
        onNodeWithTag("reader").scrollDown()
        settle()
        assertTrue(backIsShown())
        assertTrue(seen.calls.isEmpty(), "the web has no system bar to control")
    }

    @Test
    fun aClientAppOnMobileWiresTheCallbackAndOnWebDoesNot() {
        // The platform switch lives in one place.
        assertTrue(ClientPlatform.Mobile.autoHideBars)
        assertFalse(ClientPlatform.Web.autoHideBars)
    }
}

package com.usuallycoding.portfolio.client

import com.usuallycoding.portfolio.client.presentation.*
import com.usuallycoding.portfolio.client.domain.HomeData

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.usuallycoding.portfolio.ProfileDto
import com.usuallycoding.portfolio.SocialLinkDto
import com.usuallycoding.portfolio.ui.PortfolioTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class CollapsibleSocialsTest {
    private val noImages: ImageUrl = { _, _ -> null }

    private fun links(n: Int) = (1..n).map { SocialLinkDto(it.toLong(), "Link $it", "https://a.b/$it", true) }

    private fun ComposeUiTest.show(n: Int, width: androidx.compose.runtime.MutableState<Int>? = null, sections: ExpandedSections = ExpandedSections()) {
        setContent {
            androidx.compose.runtime.CompositionLocalProvider(LocalInspectionMode provides true) {
                PortfolioTheme {
                    Surface(Modifier.fillMaxSize()) {
                        val content = @androidx.compose.runtime.Composable {
                            HomeContent(HomeData(ProfileDto("Ada", "d"), links(n), emptyList()), noImages, {}, {}, sections = sections)
                        }
                        if (width == null) content() else Box(Modifier.fillMaxSize()) { Box(Modifier.width(width.value.dp)) { content() } }
                    }
                }
            }
        }
    }

    // The wrapping row composes the links and indicators it hides but does not place them, so "on screen" means placed.
    private fun ComposeUiTest.shown(max: Int = 80) =
        (1..max).filter { onAllNodesWithTag("link-$it").fetchSemanticsNodes().any { n -> n.layoutInfo.isPlaced } }
    private fun ComposeUiTest.has(description: String) =
        onAllNodesWithContentDescription(description).fetchSemanticsNodes().any { it.layoutInfo.isPlaced }
    private fun ComposeUiTest.bounds(tag: String): Rect = onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
    private fun ComposeUiTest.iconBounds(description: String): Rect = onNodeWithContentDescription(description).fetchSemanticsNode().boundsInRoot

    /** The distinct lines (by top edge) occupied by the links that are shown. */
    private fun ComposeUiTest.lines(): List<List<Int>> =
        shown().groupBy { bounds("link-$it").top.toInt() / 6 }.entries.sortedBy { it.key }.map { it.value }

    // ================= 3.1: narrow screens, one link per row =================

    @Test
    fun fiveLinksShowEverythingAndNoControl() = runDesktopComposeUiTest(411, 4000) {
        show(5)
        assertEquals((1..5).toList(), shown())
        assertTrue(!has("Show more socials") && !has("Show fewer socials"))
    }

    @Test
    fun sixLinksShowTheFirstFiveAndTheExpandControlCenteredBelow() = runDesktopComposeUiTest(411, 4000) {
        show(6)
        assertEquals((1..5).toList(), shown(), "the first five, in owner order")
        onNodeWithContentDescription("Show more socials").assertIsDisplayed()
        val toggle = iconBounds("Show more socials")
        assertEquals(411f / 2f, toggle.center.x, 1f, "centered")
        assertTrue(toggle.top >= bounds("link-5").bottom - 1f, "below the fifth link")
    }

    @Test
    fun expandingShowsAllLinksAndTheControlBecomesShowLess() = runDesktopComposeUiTest(411, 4000) {
        show(9)
        onNodeWithContentDescription("Show more socials").performClick()
        waitForIdle()
        assertEquals((1..9).toList(), shown())
        onNodeWithContentDescription("Show fewer socials").assertIsDisplayed()
        assertTrue(!has("Show more socials"))
    }

    @Test
    fun collapsingReturnsToTheFirstFive() = runDesktopComposeUiTest(411, 4000) {
        show(9)
        onNodeWithContentDescription("Show more socials").performClick()
        waitForIdle()
        onNodeWithContentDescription("Show fewer socials").performClick()
        waitForIdle()
        assertEquals((1..5).toList(), shown())
        onNodeWithContentDescription("Show more socials").assertIsDisplayed()
    }

    @Test
    fun theSocialsAndProjectsTogglesAreIndependent() = runDesktopComposeUiTest(411, 4000) {
        val sections = ExpandedSections()
        show(9, sections = sections)
        onNodeWithContentDescription("Show more socials").performClick()
        waitForIdle()
        assertTrue(sections.socials)
        assertTrue(!sections.projects)
    }

    // ================= 3.2: wide screens, the wrapping row =================

    /** Links that fit on one line of the wide layout, measured rather than guessed. */
    private fun perLine(width: Int): Int {
        var result = 0
        runDesktopComposeUiTest(width, 6000) {
            show(40, sections = ExpandedSections(socials = true))
            result = lines().first().size
        }
        return result
    }

    @Test
    fun fewLinksShowNoControlOnAWideScreen() = runDesktopComposeUiTest(1600, 3000) {
        show(3)
        assertEquals((1..3).toList(), shown())
        assertTrue(!has("Show more socials") && !has("Show fewer socials"))
    }

    @Test
    fun exactlyFiveLinesShowNoControlAndSixShowOne() {
        val perLine = perLine(1600)
        assertTrue(perLine in 2..6, "measured $perLine links per line in the 800 dp column")
        runDesktopComposeUiTest(1600, 4000) {
            show(5 * perLine)
            assertEquals(5, lines().size, "exactly five full lines")
            assertTrue(!has("Show more socials") && !has("Show fewer socials"), "five lines need no control")
        }
        runDesktopComposeUiTest(1600, 4000) {
            show(5 * perLine + 1)
            onNodeWithContentDescription("Show more socials").assertIsDisplayed()
        }
    }

    @Test
    fun collapsedShowsExactlyFiveLinesWithTheIconInTheFifth() {
        val perLine = perLine(1600)
        runDesktopComposeUiTest(1600, 4000) {
            show(8 * perLine)
            val lines = lines()
            assertEquals(5, lines.size, "five lines: ${lines.map { it.size }}")
            onNodeWithContentDescription("Show more socials").assertIsDisplayed()
            val icon = iconBounds("Show more socials")
            val fifthLineTop = bounds("link-${lines[4].first()}").top
            val fifthLineBottom = bounds("link-${lines[4].first()}").bottom
            assertTrue(icon.center.y in fifthLineTop..fifthLineBottom, "the icon sits on the fifth line (icon ${icon.center.y}, line $fifthLineTop..$fifthLineBottom)")
            assertTrue(shown().first() == 1 && shown() == shown().sorted(), "owner order")
        }
    }

    @Test
    fun expandingShowsEveryLinkAndShowLessAppearsAtTheEnd() {
        val perLine = perLine(1600)
        runDesktopComposeUiTest(1600, 6000) {
            val n = 8 * perLine
            show(n)
            onNodeWithContentDescription("Show more socials").performClick()
            waitForIdle()
            assertEquals((1..n).toList(), shown())
            onNodeWithContentDescription("Show fewer socials").assertIsDisplayed()
            assertTrue(iconBounds("Show fewer socials").top >= bounds("link-$n").top - 1f, "the show-less icon is at the end of the last line")
            onNodeWithContentDescription("Show fewer socials").performClick()
            waitForIdle()
            assertEquals(5, lines().size, "back to five lines")
            onNodeWithContentDescription("Show more socials").assertIsDisplayed()
        }
    }

    @Test
    fun everyLineStaysCenteredWithTheIconIncluded() {
        val perLine = perLine(1600)
        runDesktopComposeUiTest(1600, 4000) {
            show(8 * perLine)
            val icon = iconBounds("Show more socials")
            val lines = lines()
            for ((index, line) in lines.withIndex()) {
                val rects = line.map { bounds("link-$it") } + (if (index == lines.lastIndex) listOf(icon) else emptyList())
                val center = (rects.minOf { it.left } + rects.maxOf { it.right }) / 2f
                assertEquals(800f, center, 3f, "line ${index + 1} is centered")
            }
        }
    }

    @Test
    fun resizingAcrossTheFiveLinePointAddsAndRemovesTheIconAndKeepsExpandedState() {
        val perLine = perLine(1600)
        runDesktopComposeUiTest(3200, 8000) {
            val width = mutableStateOf(1000)
            val n = 5 * perLine + 2
            show(n, width) // narrow-ish column: many lines, so there is an icon
            assertTrue(has("Show more socials"), "more than five lines at 1000 dp")

            width.value = 3200 // a very wide column holds everything in at most five lines
            waitForIdle()
            assertTrue(!has("Show more socials") && !has("Show fewer socials"), "no icon when it all fits in five lines")
            assertEquals((1..n).toList(), shown())

            width.value = 1000
            waitForIdle()
            assertTrue(has("Show more socials"), "the icon is back")

            onNodeWithContentDescription("Show more socials").performClick()
            waitForIdle()
            width.value = 3200
            waitForIdle()
            width.value = 1000
            waitForIdle()
            assertEquals((1..n).toList(), shown(), "an expanded list stays expanded across resizes")
        }
    }

    @Test
    fun theBreakpointDecidesBetweenTheListAndTheWrappingRowForTheControlToo() {
        runDesktopComposeUiTest(839, 5000) {
            show(7)
            assertEquals((1..5).toList(), shown(), "839 dp is the one-per-row list")
            assertEquals(411f / 411f, 1f)
            onNodeWithContentDescription("Show more socials").assertIsDisplayed()
        }
    }
}

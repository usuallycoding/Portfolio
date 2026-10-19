package com.usuallycoding.portfolio.client

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import com.usuallycoding.portfolio.ProfileDto
import com.usuallycoding.portfolio.ProjectDto
import com.usuallycoding.portfolio.ui.PortfolioTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class LongTextTest {
    private val noImages: ImageUrl = { _, _ -> null }

    /** `count` lines that can never merge, so the row count does not depend on the width. */
    private fun lines(count: Int) = (1..count).joinToString("\n") { "Line number $it of the text" }

    private fun layoutOf(node: SemanticsNodeInteraction): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        node.fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        return results.single()
    }

    private fun SemanticsNodeInteraction.isClickable() = fetchSemanticsNode().config.contains(SemanticsActions.OnClick)

    /** The merged node: the click action lives on the wrapper around the text. */
    private fun ComposeUiTest.merged(text: String) = onNodeWithText(text, substring = true)

    private fun ComposeUiTest.longText(text: String, width: Int = 600) {
        setContent {
            androidx.compose.runtime.CompositionLocalProvider(LocalInspectionMode provides true) {
                PortfolioTheme { Surface(Modifier.fillMaxSize()) { Box { LongText(text, MaterialTheme.typography.bodyLarge, Modifier.width(width.dp)) } } }
            }
        }
    }

    @Test
    fun moreThanTenRowsShowsExactlyTenWithAnEllipsis() = runDesktopComposeUiTest(800, 900) {
        longText(lines(25))
        val layout = layoutOf(onNodeWithText(lines(25), substring = true, useUnmergedTree = true))
        assertEquals(10, layout.lineCount)
        assertTrue(layout.hasVisualOverflow, "an ellipsis marks the cut")
    }

    @Test
    fun exactlyTenRowsIsShownInFullWithoutAnEllipsisAndIsStillClickable() = runDesktopComposeUiTest(800, 900) {
        longText(lines(10))
        val node = onNodeWithText(lines(10), substring = true, useUnmergedTree = true)
        assertEquals(10, layoutOf(node).lineCount)
        assertFalse(layoutOf(node).hasVisualOverflow)
        assertTrue(merged(lines(10)).isClickable(), "text that fits can still be tapped to open the sheet")
    }

    @Test
    fun elevenRowsIsTruncatedAndClickable() = runDesktopComposeUiTest(800, 900) {
        longText(lines(11))
        val node = onNodeWithText(lines(11), substring = true, useUnmergedTree = true)
        assertTrue(layoutOf(node).hasVisualOverflow)
        assertTrue(merged(lines(11)).isClickable())
    }

    @Test
    fun shortTextIsShownInFullAndIsStillClickable() = runDesktopComposeUiTest(800, 900) {
        longText("Just a couple of sentences. Nothing more to say.")
        val node = onNodeWithText("Just a couple", substring = true, useUnmergedTree = true)
        assertFalse(layoutOf(node).hasVisualOverflow)
        assertTrue(merged("Just a couple").isClickable())
    }

    @Test
    fun resizingAcrossTheBoundaryChangesWhetherItIsTruncated() = runDesktopComposeUiTest(800, 1400) {
        val text = "word ".repeat(100) // a few rows at 600 dp, far more than ten at 150 dp
        val width = mutableStateOf(600)
        setContent {
            androidx.compose.runtime.CompositionLocalProvider(LocalInspectionMode provides true) {
                PortfolioTheme { Surface(Modifier.fillMaxSize()) { Box { LongText(text, MaterialTheme.typography.bodyLarge, Modifier.width(width.value.dp)) } } }
            }
        }
        val node = onNodeWithText("word", substring = true, useUnmergedTree = true)
        val wideRows = layoutOf(node).lineCount
        assertTrue(wideRows <= 10 && !layoutOf(node).hasVisualOverflow, "fits at 600 dp ($wideRows rows)")
        assertTrue(merged("word").isClickable(), "tappable even though nothing is hidden")

        width.value = 150
        waitForIdle()
        assertEquals(10, layoutOf(node).lineCount)
        assertTrue(layoutOf(node).hasVisualOverflow, "truncated at 150 dp")
        assertTrue(merged("word").isClickable(), "and tappable")

        width.value = 600
        waitForIdle()
        assertFalse(layoutOf(node).hasVisualOverflow, "back in full")
        assertTrue(merged("word").isClickable(), "tappable either way")
    }

    // ---------- where it is used ----------

    @Test
    fun theProfileDescriptionIsTruncatedAtTenRows() = runDesktopComposeUiTest(411, 1600) {
        val description = lines(30)
        setContent {
            androidx.compose.runtime.CompositionLocalProvider(LocalInspectionMode provides true) {
                PortfolioTheme { Surface(Modifier.fillMaxSize()) { HomeContent(HomeData(ProfileDto("Ada", description), emptyList(), emptyList()), noImages, {}, {}) } }
            }
        }
        val node = onNodeWithText(description, substring = true, useUnmergedTree = true)
        assertEquals(10, layoutOf(node).lineCount)
        assertTrue(merged(description).isClickable())
    }

    @Test
    fun aShortProfileDescriptionIsShownInFullAndIsClickable() = runDesktopComposeUiTest(411, 1600) {
        setContent {
            PortfolioTheme { Surface(Modifier.fillMaxSize()) { HomeContent(HomeData(ProfileDto("Ada", "A short bio."), emptyList(), emptyList()), noImages, {}, {}) } }
        }
        assertTrue(onNodeWithText("A short bio.").isClickable())
    }

    @Test
    fun theProjectSummaryIsTruncatedInBothViews() {
        for (mode in GalleryMode.entries) runDesktopComposeUiTest(411, 1600) {
            val summary = lines(30)
            val project = ProjectDto(7, "Chapter 1", "2026-01-02", true, summary, null, emptyList())
            setContent {
                androidx.compose.runtime.CompositionLocalProvider(LocalInspectionMode provides true) {
                    PortfolioTheme { Surface(Modifier.fillMaxSize()) { ProjectContent(project, noImages, {}, {}, mode = mode) } }
                }
            }
            val node = onNodeWithText(summary, substring = true, useUnmergedTree = true)
            assertEquals(10, layoutOf(node).lineCount, "summary rows in $mode")
            assertTrue(onNodeWithText(summary, substring = true).isClickable(), "summary is tappable in $mode")
        }
    }

    // ---------- the ellipsis ----------

    private fun ComposeUiTest.ellipsisCount() = onAllNodesWithTag(EllipsisTag, useUnmergedTree = true).fetchSemanticsNodes().size

    @Test
    fun whenTheCutFallsAfterALineBreakAnEllipsisIsDrawnAfterTheTenthRow() = runDesktopComposeUiTest(800, 900) {
        // Short lines: the tenth row ends exactly at a line break, where Compose draws no "…" itself.
        longText(lines(25), width = 600)
        val text = onNodeWithText(lines(25), substring = true, useUnmergedTree = true)
        assertEquals(1, ellipsisCount())
        val ellipsis = onNodeWithTag(EllipsisTag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val textBounds = text.fetchSemanticsNode().boundsInRoot
        val layout = layoutOf(text)
        assertEquals(textBounds.left + layout.getLineRight(9), ellipsis.left, 2f, "right after the last visible character")
        assertTrue(ellipsis.top >= textBounds.top + layout.getLineTop(9) - 2f && ellipsis.bottom <= textBounds.bottom + 2f, "on the tenth row")
    }

    @Test
    fun whenTheCutFallsInsideALineComposeDrawsTheEllipsisAndWeAddNoSecondOne() = runDesktopComposeUiTest(800, 900) {
        longText("word ".repeat(400), width = 300)
        assertTrue(layoutOf(onNodeWithText("word", substring = true, useUnmergedTree = true)).hasVisualOverflow)
        assertEquals(0, ellipsisCount(), "the text shows its own ellipsis, so none is added")
    }

    @Test
    fun shortTextGetsNoEllipsis() = runDesktopComposeUiTest(800, 900) {
        longText("A short text.")
        assertEquals(0, ellipsisCount())
    }

    @Test
    fun exactlyTenRowsGetsNoEllipsis() = runDesktopComposeUiTest(800, 900) {
        longText(lines(10))
        assertEquals(0, ellipsisCount())
    }

    @Test
    fun theDrawnEllipsisIsNotClickableAndNotReadSeparately() = runDesktopComposeUiTest(800, 900) {
        longText(lines(25))
        val node = onNodeWithTag(EllipsisTag, useUnmergedTree = true).fetchSemanticsNode()
        assertFalse(node.config.contains(SemanticsActions.OnClick))
        assertEquals(0, onAllNodesWithText("…", useUnmergedTree = true).fetchSemanticsNodes().size, "no spoken text of its own")
    }

    @Test
    fun theCutDetectionRecognisesLineBreaksAndOnlyThose() {
        // (text, cut offset the layout would report) -> at a line break?
        fun cut(text: String, end: Int) = text.getOrNull(end) == '\n' || text.getOrNull(end - 1) == '\n'
        assertTrue(cut("abc\ndef", 3), "the next character is a newline")
        assertTrue(cut("abc\ndef", 4), "the previous character is a newline")
        assertFalse(cut("abcdef", 3), "inside a word")
        assertFalse(cut("abc def", 4), "after a soft space")
        assertFalse(cut("abc", 3), "at the very end")
    }
}

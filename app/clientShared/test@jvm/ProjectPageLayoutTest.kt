package com.usuallycoding.portfolio.client

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.test.swipe
import com.usuallycoding.portfolio.ImageDto
import com.usuallycoding.portfolio.ProjectDto
import com.usuallycoding.portfolio.ui.PortfolioTheme
import com.usuallycoding.portfolio.ui.ThemePreference
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ProjectPageLayoutTest {
    private val noImages: ImageUrl = { _, _ -> null }
    private val summary = "A project summary that is long enough to need several lines when it is squeezed into a narrow centered column. ".repeat(4)

    private fun project(images: Int = 12) = ProjectDto(
        7, "Chapter 1", "2026-01-02", true, summary, null,
        images = List(images) { ImageDto("i$it", "i$it.jpg", 800, 600) },
    )

    private fun ComposeUiTest.page(
        mode: GalleryMode,
        project: ProjectDto = project(),
        showBack: Boolean = true,
        preference: ThemePreference = ThemePreference.Light,
    ) {
        setContent {
            androidx.compose.runtime.CompositionLocalProvider(LocalInspectionMode provides true) {
                PortfolioTheme(preference) {
                    Surface(Modifier.fillMaxSize()) {
                        ProjectContent(project, noImages, {}, {}, mode = mode, showBackControl = showBack)
                    }
                }
            }
        }
    }

    private fun ComposeUiTest.node(text: String): Rect = onNodeWithText(text, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
    private fun ComposeUiTest.image(n: Int): Rect = onNodeWithContentDescription("Chapter 1 image $n").fetchSemanticsNode().boundsInRoot

    // ---------- 4.1: centered title and header ----------

    @Test
    fun theTitleIsCenteredWithAndWithoutABackControl() {
        for (width in listOf(411, 1000, 1600)) for (showBack in listOf(true, false)) runDesktopComposeUiTest(width, 900) {
            page(GalleryMode.Grid, showBack = showBack)
            assertEquals(width / 2f, node("Chapter 1").center.x, 1.5f, "title at ${width}px, back control shown=$showBack")
        }
    }

    @Test
    fun theBackControlStaysAtTheStartEdge() = runDesktopComposeUiTest(1600, 900) {
        page(GalleryMode.Grid)
        val back = onNodeWithContentDescription("Back").fetchSemanticsNode().boundsInRoot
        assertTrue(back.left < 80f, "the back arrow is at the start (left ${back.left})")
        assertTrue(back.right < 800f)
    }

    @Test
    fun theHeaderIsCenteredInTheColumnInBothViews() {
        for (mode in GalleryMode.entries) runDesktopComposeUiTest(1600, 1400) {
            page(mode)
            assertEquals(800f, node("2 Jan 2026").center.x, 1f, "date in $mode")
            val text = onNodeWithText(summary.trim(), substring = true, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            assertEquals(800f, text.center.x, 1f, "summary in $mode")
            assertTrue(text.width <= 800f - 32f + 0.5f, "summary ${text.width}px wide must fit the 800 dp column minus gutters")
            val switchCenter = (node("Grid").left + node("Reader").right) / 2f
            assertEquals(800f, switchCenter, 12f, "the Grid / Reader switch in $mode") // the buttons' labels sit a few px inside their edges
        }
    }

    @Test
    fun theDateAndSummaryTextAreLeftAlignedInsideTheCenteredBlock() {
        for (width in listOf(411, 1600)) for (mode in GalleryMode.entries) runDesktopComposeUiTest(width, 1600) {
            page(mode)
            for (text in listOf("2 Jan 2026", summary.trim())) {
                val node = onNodeWithText(text, substring = true, useUnmergedTree = true).fetchSemanticsNode()
                val results = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
                node.config[androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult].action?.invoke(results)
                val layout = results.single()
                assertEquals(androidx.compose.ui.text.style.TextAlign.Start, layout.layoutInput.style.textAlign, "\"${text.take(12)}…\" at ${width}px in $mode")
                for (line in 0 until layout.lineCount) assertEquals(0f, layout.getLineLeft(line), 1f, "line $line starts at the left edge")
            }
            // The summary block is still centered, and only the switch and the title are centered content.
            val block = onNodeWithText(summary.trim(), substring = true, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            assertEquals(width / 2f, block.center.x, 1f, "summary block at ${width}px in $mode")
        }
    }

    @Test
    fun onAPhoneTheHeaderIsCenteredAndUsesTheFullWidth() {
        for (mode in GalleryMode.entries) runDesktopComposeUiTest(411, 1600) {
            page(mode)
            assertEquals(411f / 2f, node("2 Jan 2026").center.x, 1f)
            val text = onNodeWithText(summary.trim(), substring = true, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            assertEquals(411f / 2f, text.center.x, 1f)
            assertEquals(411f - 32f, text.width, 1f, "full width minus gutters in $mode")
        }
    }

    // ---------- 4.2: grid in the column, reader full width ----------

    @Test
    fun theGridCellsAreInsideTheCenteredColumnAndTheScrollAreaIsFullWidth() = runDesktopComposeUiTest(1600, 1400) {
        page(GalleryMode.Grid)
        val grid = onNodeWithTag("grid").fetchSemanticsNode().boundsInRoot
        assertEquals(0f, grid.left, 0.5f)
        assertEquals(1600f, grid.width, 0.5f, "the scroll area spans the window")
        val cells = (1..12).mapNotNull { i -> runCatching { image(i) }.getOrNull() }
        assertTrue(cells.size >= 6)
        for (c in cells) assertTrue(c.left >= 400f + 16f - 0.5f && c.right <= 1200f - 16f + 0.5f, "cell $c is inside the column")
        val left = cells.minOf { it.left }
        val right = cells.maxOf { it.right }
        assertEquals(800f, (left + right) / 2f, 1f, "the cells are centered")
    }

    @Test
    fun theReadersImagesStillSpanTheFullScreenWidthWhileTheHeaderIsInTheColumn() {
        for (width in listOf(411, 1000, 1600)) runDesktopComposeUiTest(width, 2000) {
            page(GalleryMode.Reader)
            for (n in 1..2) { // later images are not composed yet in a short window
                // The layout size, not the on-screen bounds: images further down are cut off by the window.
                val size = onNodeWithContentDescription("Chapter 1 image $n").fetchSemanticsNode().size
                assertEquals(0f, image(n).left, 0.5f, "image $n left at ${width}px")
                assertEquals(width, size.width, "image $n spans the window at ${width}px")
                assertEquals(width * 600f / 800f, size.height.toFloat(), 1f, "image $n keeps its proportions")
            }
            assertEquals(image(1).bottom, image(2).top, 0.5f, "no gap between images")
            val column = contentColumnWidth(androidx.compose.ui.unit.Dp(width.toFloat())).value
            val text = onNodeWithText(summary.trim(), substring = true, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            assertTrue(text.width <= column - 32f + 0.5f, "header text is inside the column at ${width}px")
            assertEquals(width / 2f, text.center.x, 1f)
        }
    }

    private fun scrollingFromTheMargin(mode: GalleryMode) = runDesktopComposeUiTest(1600, 900) {
        page(mode, project(images = 60)) // plenty to scroll in both views
        val before = if (mode == GalleryMode.Grid) node("2 Jan 2026").top else node("2 Jan 2026").top
        // Drag with the pointer far to the left of the 800 dp column.
        onRoot().performTouchInput { swipe(start = Offset(100f, 700f), end = Offset(100f, 200f), durationMillis = 300) }
        waitForIdle()
        val after = runCatching { node("2 Jan 2026").top }.getOrNull()
        assertTrue(after == null || after < before - 50f, "the page must scroll when dragged beside the column ($before -> $after)")
    }

    @Test fun scrollingBesideTheColumnWorksInTheGrid() = scrollingFromTheMargin(GalleryMode.Grid)
    @Test fun scrollingBesideTheColumnWorksInTheReader() = scrollingFromTheMargin(GalleryMode.Reader)

    @Test
    fun theNoImagesMessageIsCenteredInBothViews() {
        for (mode in GalleryMode.entries) runDesktopComposeUiTest(1600, 900) {
            page(mode, project(images = 0))
            assertEquals(800f, node("No images yet.").center.x, 1f, "message in $mode")
        }
    }

    // ---------- 4.3: screenshots ----------

    @Test
    fun screenshotsOfBothViewsInBothThemes() {
        for ((width, label) in listOf(1600 to "wide", 411 to "phone")) for (mode in GalleryMode.entries) for (p in listOf(ThemePreference.Light, ThemePreference.Dark)) {
            runDesktopComposeUiTest(width, 900) {
                page(mode, preference = p)
                val dir = File("build/ui-screenshots").also { it.mkdirs() }
                ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(dir, "projectpage-$label-${mode.name.lowercase()}-${p.name.lowercase()}.png"))
            }
        }
    }
}

/** The title must sit at the exact centre of the screen whatever the back control's width. */
@OptIn(ExperimentalTestApi::class)
class CenteredTitleTest {
    private val noImages: ImageUrl = { _, _ -> null }
    private val longTitle = "A Remarkably Long Project Title That Cannot Possibly Fit Beside The Back Button On A Phone"

    private fun ComposeUiTest.page(title: String, showBack: Boolean) {
        val project = ProjectDto(7, title, "2026-01-02", true, "s", null, emptyList())
        setContent {
            androidx.compose.runtime.CompositionLocalProvider(LocalInspectionMode provides true) {
                PortfolioTheme { Surface(Modifier.fillMaxSize()) { ProjectContent(project, noImages, {}, {}, showBackControl = showBack) } }
            }
        }
    }

    @Test
    fun aLongTitleIsStillCenteredOnAPhoneWithTheBackControl() {
        for (width in listOf(360, 411, 600)) runDesktopComposeUiTest(width, 800) {
            page(longTitle, showBack = true)
            val bounds = onNodeWithText(longTitle, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            assertEquals(width / 2f, bounds.center.x, 1.5f, "a long title at ${width}px must be centered on the screen, not on the space left of the back control")
        }
    }

    @Test
    fun aLongTitleIsCutSymmetricallyAndNeverUnderTheBackControl() = runDesktopComposeUiTest(411, 800) {
        page(longTitle, showBack = true)
        val title = onNodeWithText(longTitle, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val back = onNodeWithContentDescription("Back").fetchSemanticsNode().boundsInRoot
        assertTrue(title.left >= back.right - 0.5f, "the title (${title.left}) starts after the back control (${back.right})")
        assertEquals(411f - title.right, title.left, 1.5f, "the same space is kept free on both sides")
    }

    @Test
    fun withAndWithoutTheBackControlTheTitlePositionIsTheSame() {
        for (title in listOf("Short", "Chapter 1", longTitle)) {
            var withBack = 0f
            var withoutBack = 0f
            runDesktopComposeUiTest(411, 800) { page(title, true); withBack = onNodeWithText(title, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.center.x }
            runDesktopComposeUiTest(411, 800) { page(title, false); withoutBack = onNodeWithText(title, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.center.x }
            assertEquals(411f / 2f, withBack, 1.5f, "\"$title\" with the back control")
            assertEquals(411f / 2f, withoutBack, 1.5f, "\"$title\" without it")
        }
    }
}

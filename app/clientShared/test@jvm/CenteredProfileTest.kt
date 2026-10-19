package com.usuallycoding.portfolio.client

import com.usuallycoding.portfolio.client.presentation.*
import com.usuallycoding.portfolio.client.domain.HomeData

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.usuallycoding.portfolio.ProfileDto
import com.usuallycoding.portfolio.ProjectSummaryDto
import com.usuallycoding.portfolio.SocialLinkDto
import com.usuallycoding.portfolio.ui.PortfolioTheme
import com.usuallycoding.portfolio.ui.ThemePreference
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class CenteredProfileTest {
    private val noImages: ImageUrl = { _, _ -> null }
    private val description = "A description of the owner that is long enough to need several lines in a narrow column. ".repeat(4)

    private fun links(count: Int, address: (Int) -> String = { "https://example.com/link-$it" }) =
        (1..count).map { SocialLinkDto(it.toLong(), "Link $it", address(it), true) }

    private fun data(links: List<SocialLinkDto> = links(3), projects: Int = 4) = HomeData(
        ProfileDto("Ada Lovelace", description),
        links,
        (1L..projects.toLong()).map { ProjectSummaryDto(it, "Project $it", "2026-01-02", true, "Summary $it") },
    )

    private fun ComposeUiTest.show(
        data: HomeData = data(),
        onOpenLink: (SocialLinkDto) -> Unit = {},
        preference: ThemePreference = ThemePreference.Light,
    ) {
        setContent {
            // Expanded: these tests are about layout and centering, not about collapsing long lists.
            PortfolioTheme(preference) {
                Surface(Modifier.fillMaxSize()) {
                    HomeContent(data, noImages, onOpenLink, {}, sections = ExpandedSections(socials = true, projects = true))
                }
            }
        }
    }

    private fun ComposeUiTest.bounds(tag: String): Rect = onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
    private fun ComposeUiTest.textBounds(text: String, substring: Boolean = false): Rect =
        onNodeWithText(text, substring = substring, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot

    // ---------- 1.2: avatar, name, description ----------

    @Test
    fun theAvatarIsCenteredOnTheCoversBottomEdge() {
        for (width in listOf(411, 1000, 1600)) runDesktopComposeUiTest(width, 1200) {
            show()
            val cover = onNodeWithContentDescription("Cover image").fetchSemanticsNode().boundsInRoot
            val avatar = onNodeWithContentDescription("Profile picture").fetchSemanticsNode().boundsInRoot
            assertEquals(width / 2f, avatar.center.x, 1f, "avatar horizontal center in a ${width}px window")
            assertTrue(avatar.top < cover.bottom && avatar.bottom > cover.bottom, "straddles the cover's bottom edge")
            assertEquals(cover.bottom, avatar.center.y, 1f)
        }
    }

    @Test
    fun theNameAndDescriptionAreCentered() {
        for (width in listOf(411, 1000, 1600)) runDesktopComposeUiTest(width, 1400) {
            show()
            val name = textBounds("Ada Lovelace")
            val text = textBounds(description.trim(), substring = true)
            assertEquals(width / 2f, name.center.x, 1f, "name center at ${width}px")
            assertEquals(width / 2f, text.center.x, 1f, "description center at ${width}px")
        }
    }

    @Test
    fun theDescriptionTextIsLeftAlignedInsideACenteredBlock() {
        for (width in listOf(411, 1600)) runDesktopComposeUiTest(width, 1600) {
            show()
            val node = onNodeWithText(description.trim(), substring = true, useUnmergedTree = true).fetchSemanticsNode()
            val results = mutableListOf<TextLayoutResult>()
            node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
            val layout = results.single()
            assertTrue(layout.lineCount > 1, "the description must wrap to be a meaningful check at ${width}px")
            assertEquals(TextAlign.Start, layout.layoutInput.style.textAlign, "lines are left-aligned, not centered")
            // The lines themselves start at the block's left edge.
            for (line in 0 until layout.lineCount) assertEquals(0f, layout.getLineLeft(line), 1f, "line $line starts at the left edge of the block")
            // The block is still centered.
            assertEquals(width / 2f, node.boundsInRoot.center.x, 1f, "the block is centered at ${width}px")
        }
    }

    @Test
    fun onAWideScreenTheDescriptionIsNoWiderThanTheColumn() = runDesktopComposeUiTest(1600, 1400) {
        show()
        val text = textBounds(description.trim(), substring = true)
        assertTrue(text.width <= 800f - 32f + 0.5f, "description is ${text.width}px wide; the column is 800 less its gutters")
    }

    @Test
    fun onAPhoneTheDescriptionUsesTheFullWidthMinusTheGutters() = runDesktopComposeUiTest(411, 1600) {
        show()
        assertEquals(411f - 32f, textBounds(description.trim(), substring = true).width, 1f)
    }

    // ---------- 2.1: Socials title ----------

    @Test
    fun aCenteredSocialsTitleIsShownWhenThereAreLinks() {
        for (width in listOf(411, 1600)) runDesktopComposeUiTest(width, 1400) {
            show()
            assertEquals(width / 2f, textBounds("Socials").center.x, 1f, "Socials title center at ${width}px")
            // It comes before the links.
            assertTrue(textBounds("Socials").bottom <= bounds("link-1").top + 1)
        }
    }

    @Test
    fun withoutLinksThereIsNoSocialsTitleOrLinks() = runDesktopComposeUiTest(1000, 1000) {
        show(data(links = emptyList()))
        assertNull(runCatching { onNodeWithText("Socials").assertIsDisplayed() }.getOrNull())
        onNodeWithText("Projects").assertIsDisplayed()
    }

    // ---------- 2.2: wide wrapping row ----------

    @Test
    fun aFewLinksShareOneCenteredLineInTheColumn() = runDesktopComposeUiTest(1600, 1400) {
        show(data(links = links(3) { "https://a.b/$it" }))
        val items = (1..3).map { bounds("link-$it") }
        assertEquals(items[0].top, items[1].top, 1f, "same line")
        assertEquals(items[1].top, items[2].top, 1f)
        val group = Rect(items.minOf { it.left }, items[0].top, items.maxOf { it.right }, items[0].bottom)
        assertEquals(800f, group.center.x, 1.5f, "the line is centered in the window")
        assertTrue(items.all { it.left >= 400f - 0.5f && it.right <= 1200f + 0.5f }, "inside the 800 dp column")
        for (i in 0..1) assertTrue(items[i].right <= items[i + 1].left, "in owner order, left to right")
    }

    @Test
    fun manyLinksWrapOntoFurtherCenteredLines() = runDesktopComposeUiTest(1600, 1600) {
        show(data(links = links(10)))
        val items = (1..10).map { bounds("link-$it") }
        val lines = items.groupBy { it.top.toInt() / 4 }.values.map { line -> line.sortedBy { it.left } }
        assertTrue(lines.size >= 2, "10 links must wrap in an 800 dp column (got ${lines.size} line(s))")
        for (line in lines) {
            val center = (line.minOf { it.left } + line.maxOf { it.right }) / 2f
            assertEquals(800f, center, 2f, "every line is centered")
            assertTrue(line.all { it.left >= 400f - 0.5f && it.right <= 1200f + 0.5f })
        }
        // Owner order runs line by line.
        val ordered = lines.flatten().map { items.indexOf(it) }
        assertEquals((0..9).toList(), ordered)
    }

    @Test
    fun aVeryLongAddressDoesNotTakeAWholeLine() = runDesktopComposeUiTest(1600, 1400) {
        show(data(links = links(3) { if (it == 2) "https://example.com/" + "very-long-path/".repeat(40) else "https://example.com/$it" }))
        val long = bounds("link-2")
        assertTrue(long.width <= 320f + 0.5f, "the long link is ${long.width}px wide")
        assertEquals(bounds("link-1").top, long.top, 1f, "the other links still share its line")
    }

    @Test
    fun tappingAWideLinkOpensItsOwnAddressAndEmptySpaceOpensNothing() = runDesktopComposeUiTest(1600, 1400) {
        val opened = mutableListOf<String>()
        show(data(links = links(3)), onOpenLink = { opened += it.address })
        for (i in 1..3) {
            val b = bounds("link-$i")
            onRoot().performTouchInput { click(b.center) }
        }
        assertEquals(listOf("https://example.com/link-1", "https://example.com/link-2", "https://example.com/link-3"), opened)

        opened.clear()
        val first = bounds("link-1")
        onRoot().performTouchInput { click(Offset(first.left - 20f, first.center.y)) } // just left of the first item
        assertTrue(opened.isEmpty(), "the gap beside a link must not open anything")
    }

    // ---------- 2.3: one per row on small screens ----------

    @Test
    fun onAPhoneEachLinkIsOnItsOwnFullWidthRowInOwnerOrder() = runDesktopComposeUiTest(411, 1600) {
        show(data(links = links(4)))
        val rows = (1..4).map { bounds("link-$it") }
        for (i in 0..2) assertTrue(rows[i + 1].top >= rows[i].bottom - 1, "link ${i + 2} is below link ${i + 1}")
        for (r in rows) assertEquals(411f, r.width, 0.5f, "a row spans the screen")
    }

    @Test
    fun onAPhoneEachRowsIconAndTextAreAlignedToTheLeft() = runDesktopComposeUiTest(411, 1600) {
        show(data(links = links(3)))
        for (i in 1..3) {
            val row = bounds("link-$i")
            val icon = onNodeWithContentDescription("Link $i icon", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            val name = textBounds("Link $i")
            val address = textBounds("https://example.com/link-$i")
            assertEquals(row.left + 16f, icon.left, 1f, "link $i icon starts at the left gutter")
            assertEquals(icon.right + 16f, name.left, 1f, "the name follows the icon")
            assertEquals(icon.right + 16f, address.left, 1f, "and so does the address")
        }
        // Rows of different text length still line up on the same left edge.
        val lefts = (1..3).map { onNodeWithContentDescription("Link $it icon", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.left }
        assertTrue(lefts.all { it == lefts[0] }, "icons share one left edge: $lefts")
    }

    @Test
    fun theBreakpointDecidesBetweenRowsAndTheWrappingRow() {
        runDesktopComposeUiTest(839, 1400) {
            show(data(links = links(3)))
            val tops = (1..3).map { bounds("link-$it").top }
            assertTrue(tops[1] > tops[0] && tops[2] > tops[1], "839 dp: one per row")
        }
        runDesktopComposeUiTest(840, 1400) {
            show(data(links = links(2) { "https://a.b/$it" }))
            assertEquals(bounds("link-1").top, bounds("link-2").top, 1f, "840 dp: the wrapping row")
        }
    }

    // ---------- 3.1: projects ----------

    @Test
    fun theProjectsTitleIsCenteredAndTheCardsLiveInTheColumn() = runDesktopComposeUiTest(1600, 1800) {
        show(data(projects = 6))
        assertEquals(800f, textBounds("Projects").center.x, 1f)
        val perRow = projectColumns(contentColumnWidth(1600.dp))
        assertEquals(800f - 16f - 16f, (1..perRow).sumOf { bounds("project-card-$it").width.toDouble() }.toFloat() + 12f * (perRow - 1), 1f, "the first row fills the column")
        val first = bounds("project-card-1")
        val last = bounds("project-card-$perRow")
        assertEquals(400f + 16f, first.left, 0.5f)
        assertEquals(1200f - 16f, last.right, 1f)
        assertEquals(800f, (first.left + last.right) / 2f, 1f, "the cards are centered")
    }

    @Test
    fun onAPhoneTheCardsUseTheFullWidth() = runDesktopComposeUiTest(411, 1800) {
        show(data(projects = 4))
        assertEquals(16f, bounds("project-card-1").left, 0.5f)
        val perRow = projectColumns(411.dp)
        assertEquals(2, perRow)
        assertEquals(411f - 16f, bounds("project-card-$perRow").right, 1f)
    }

    @Test
    fun theEmptyProjectsMessageIsCentered() = runDesktopComposeUiTest(1600, 1000) {
        show(data(projects = 0))
        assertEquals(800f, textBounds("No projects yet.").center.x, 1f)
    }

    @Test
    fun cardsAreAllTheSameSizeInTheColumnToo() = runDesktopComposeUiTest(1600, 1800) {
        show(data(projects = 8))
        val sizes = (1..8).map { bounds("project-card-$it").let { b -> b.width to b.height } }
        for (s in sizes) { assertEquals(sizes[0].first, s.first); assertEquals(sizes[0].second, s.second) }
    }

    @Test
    fun screenshots() {
        for ((width, label) in listOf(1600 to "wide", 411 to "phone")) for (p in listOf(ThemePreference.Light, ThemePreference.Dark)) {
            runDesktopComposeUiTest(width, 1300) {
                show(data(links = links(7), projects = 7), preference = p)
                val dir = File("build/ui-screenshots").also { it.mkdirs() }
                ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(dir, "centered-$label-${p.name.lowercase()}.png"))
            }
        }
    }
}

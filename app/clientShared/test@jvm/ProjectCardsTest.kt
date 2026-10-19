package com.usuallycoding.portfolio.client

import com.usuallycoding.portfolio.client.presentation.*
import com.usuallycoding.portfolio.client.domain.HomeData

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import com.usuallycoding.portfolio.ImageDto
import com.usuallycoding.portfolio.ProfileDto
import com.usuallycoding.portfolio.ProjectSummaryDto
import com.usuallycoding.portfolio.ui.PortfolioTheme
import com.usuallycoding.portfolio.ui.ThemePreference
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ProjectCardsTest {
    private val noImages: ImageUrl = { _, _ -> null }

    private fun project(id: Long, summary: String = "Short summary", title: String = "Project $id", thumbnail: ImageDto? = null) =
        ProjectSummaryDto(id, title, "2026-01-0${(id % 9).toInt() + 1}", true, summary, thumbnail)

    private val long = "This summary is long enough that it cannot possibly fit in three lines of a small card. ".repeat(6)

    private fun ComposeUiTest.show(projects: List<ProjectSummaryDto>, preference: ThemePreference = ThemePreference.Light, onOpen: (ProjectSummaryDto) -> Unit = {}) {
        setContent {
            PortfolioTheme(preference) {
                Surface(Modifier.fillMaxSize()) { HomeContent(HomeData(ProfileDto("Ada", "Analyst"), emptyList(), projects), noImages, {}, onOpen, sections = ExpandedSections(projects = true)) }
            }
        }
    }

    private fun ComposeUiTest.card(id: Long): Rect = onNodeWithTag("project-card-$id").fetchSemanticsNode().boundsInRoot

    @Test
    fun theColumnCountFollowsTheWidth() {
        assertEquals(1, projectColumns(320.dp))
        assertEquals(2, projectColumns(375.dp), "a small phone gets two columns")
        assertEquals(2, projectColumns(411.dp))
        assertEquals(2, projectColumns(480.dp))
        assertEquals(3, projectColumns(700.dp))
        assertEquals(5, projectColumns(1000.dp))
        assertEquals(8, projectColumns(1400.dp))
        assertEquals(1, projectColumns(10.dp), "never fewer than one")
    }

    @Test
    fun cardsFillTheColumnOfAWideWindowSeveralPerRowInOwnerOrder() = runDesktopComposeUiTest(1400, 1800) {
        show((1L..8L).map { project(it) })
        val cards = (1L..8L).map { card(it) }
        val perRow = projectColumns(contentColumnWidth(1400.dp)) // the column is 700 dp wide
        assertEquals(3, perRow)
        // First row: same top, left to right in owner order.
        for (i in 0 until perRow - 1) {
            assertEquals(cards[i].top, cards[i + 1].top, 0.5f, "cards ${i + 1} and ${i + 2} share a row")
            assertTrue(cards[i].right < cards[i + 1].left + 1, "card ${i + 2} is to the right of card ${i + 1}")
        }
        // The next card wraps to a second row, starting at the column's left edge again.
        assertTrue(cards[perRow].top > cards[0].bottom - 1)
        assertEquals(cards[0].left, cards[perRow].left, 0.5f)
        // The short last row keeps the same card width.
        assertEquals(cards[0].width, cards[7].width, 0.5f)
        // All of it sits inside the centered column (350..1050), with the 16 dp gutter.
        assertEquals(350f + 16f, cards[0].left, 0.5f)
        // Equal whole-pixel card widths leave at most (columns - 1) unused pixels at the right edge.
        assertEquals(1050f - 16f, cards[perRow - 1].right, perRow.toFloat())
    }

    @Test
    fun aVeryNarrowWindowShowsOneCardPerRow() = runDesktopComposeUiTest(320, 1800) {
        show((1L..3L).map { project(it) })
        val cards = (1L..3L).map { card(it) }
        assertTrue(cards[1].top > cards[0].bottom - 1)
        assertTrue(cards[2].top > cards[1].bottom - 1)
        assertEquals(cards[0].left, cards[1].left, 0.5f)
    }

    @Test
    fun aPhoneShowsTwoCardsPerRow() = runDesktopComposeUiTest(411, 1800) {
        show((1L..4L).map { project(it) })
        val cards = (1L..4L).map { card(it) }
        assertEquals(cards[0].top, cards[1].top, 0.5f)
        assertTrue(cards[1].left > cards[0].right - 1)
        assertTrue(cards[2].top > cards[0].bottom - 1)
        assertEquals(cards[0].left, cards[2].left, 0.5f)
    }

    @Test
    fun everyCardHasTheSameSizeWhateverItsContent() = runDesktopComposeUiTest(1000, 1800) {
        show(
            listOf(
                project(1, summary = ""),
                project(2, summary = "One line"),
                project(3, summary = long),
                project(4, summary = "Line one\nLine two\nLine three\nLine four\nLine five"),
                project(5, title = "An extremely long project title that goes on and on and on and on and on and on", thumbnail = null),
                project(6, thumbnail = ImageDto("t", "t.jpg", 100, 100)),
            ),
        )
        val cards = (1L..6L).map { card(it) }
        val first = cards.first()
        cards.forEachIndexed { i, c ->
            assertEquals(first.width, c.width, 0.5f, "card ${i + 1} width")
            assertEquals(first.height, c.height, 0.5f, "card ${i + 1} height")
        }
    }

    @Test
    fun aLongSummaryIsCutAfterThreeLines() = runDesktopComposeUiTest(1000, 1800) {
        show(listOf(project(1, summary = long)))
        val node = onNodeWithText(long, useUnmergedTree = true).fetchSemanticsNode()
        val results = mutableListOf<TextLayoutResult>()
        node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        assertEquals(3, results.single().lineCount)
        assertTrue(results.single().hasVisualOverflow, "an ellipsis marks the cut")
    }

    @Test
    fun tappingACardOpensThatProject() = runDesktopComposeUiTest(1000, 1800) {
        var opened: Long? = null
        show((1L..4L).map { project(it) }, onOpen = { opened = it.id })
        onNodeWithTag("project-card-3").performClick()
        assertEquals(3L, opened)
    }

    @Test
    fun noProjectsShowsTheEmptyState() = runDesktopComposeUiTest(1000, 800) {
        show(emptyList())
        onNodeWithText("No projects yet.").assertIsDisplayed()
    }

    @Test
    fun screenshotsAtPhoneAndWideWidthsInBothThemes() {
        for ((width, label) in listOf(480 to "phone", 1400 to "wide")) for (preference in listOf(ThemePreference.Light, ThemePreference.Dark)) {
            runDesktopComposeUiTest(width, 1000) {
                show((1L..6L).map { project(it, summary = if (it % 2 == 0L) long else "Short one") }, preference)
                val dir = File("build/ui-screenshots").also { it.mkdirs() }
                ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(dir, "cards-$label-${preference.name.lowercase()}.png"))
            }
        }
    }
}

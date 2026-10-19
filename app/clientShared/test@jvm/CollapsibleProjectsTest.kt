package com.usuallycoding.portfolio.client

import com.usuallycoding.portfolio.client.presentation.*
import com.usuallycoding.portfolio.client.domain.HomeData

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
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
import com.usuallycoding.portfolio.ProjectSummaryDto
import com.usuallycoding.portfolio.ui.PortfolioTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class CollapsibleProjectsTest {
    private val noImages: ImageUrl = { _, _ -> null }

    private fun projects(n: Int) = (1L..n.toLong()).map { ProjectSummaryDto(it, "Project $it", "2026-01-02", true, "Summary $it") }

    private fun ComposeUiTest.show(n: Int, width: androidx.compose.runtime.MutableState<Int>? = null, sections: ExpandedSections = ExpandedSections()) {
        setContent {
            androidx.compose.runtime.CompositionLocalProvider(LocalInspectionMode provides true) {
                PortfolioTheme {
                    Surface(Modifier.fillMaxSize()) {
                        val content = @androidx.compose.runtime.Composable {
                            HomeContent(HomeData(ProfileDto("Ada", "d"), emptyList(), projects(n)), noImages, {}, {}, sections = sections)
                        }
                        if (width == null) content() else Box(Modifier.fillMaxSize()) { Box(Modifier.width(width.value.dp)) { content() } }
                    }
                }
            }
        }
    }

    private fun ComposeUiTest.cards() = (1..40).filter { onAllNodesWithTag("project-card-$it").fetchSemanticsNodes().isNotEmpty() }
    private fun ComposeUiTest.hasToggle() =
        onAllNodesWithTag("projects-toggle").fetchSemanticsNodes().isNotEmpty() ||
            onAllNodesWithContentDescription("Show more projects").fetchSemanticsNodes().isNotEmpty() ||
            onAllNodesWithContentDescription("Show fewer projects").fetchSemanticsNodes().isNotEmpty()

    // A phone (411 dp) fits 2 cards per row, so 4 projects are exactly 2 rows and 5 projects are 3.

    @Test
    fun twoRowsShowEverythingAndNoControl() = runDesktopComposeUiTest(411, 6000) {
        assertEquals(2, projectColumns(411.dp))
        show(4)
        assertEquals((1..4).toList(), cards())
        assertTrue(!hasToggle(), "no expand control for exactly 2 rows")
    }

    @Test
    fun oneRowAndAnEmptyListShowNoControl() {
        runDesktopComposeUiTest(411, 3000) { show(1); assertEquals(listOf(1), cards()); assertTrue(!hasToggle()) }
        runDesktopComposeUiTest(411, 3000) { show(0); assertTrue(cards().isEmpty()); assertTrue(!hasToggle()) }
    }

    @Test
    fun threeRowsShowTheFirstTwoAndAnExpandControl() = runDesktopComposeUiTest(411, 6000) {
        show(5)
        assertEquals((1..4).toList(), cards(), "two rows of two, in owner order")
        onNodeWithContentDescription("Show more projects").assertIsDisplayed()
        assertEquals(0, onAllNodesWithContentDescription("Show fewer projects").fetchSemanticsNodes().size)
    }

    @Test
    fun expandingShowsEveryRowAndTheControlBecomesShowLess() = runDesktopComposeUiTest(411, 6000) {
        show(11)
        assertEquals(4, cards().size)
        onNodeWithContentDescription("Show more projects").performClick()
        waitForIdle()
        assertEquals((1..11).toList(), cards())
        onNodeWithContentDescription("Show fewer projects").assertIsDisplayed()
        assertEquals(0, onAllNodesWithContentDescription("Show more projects").fetchSemanticsNodes().size)
    }

    @Test
    fun collapsingReturnsToTwoRows() = runDesktopComposeUiTest(411, 6000) {
        show(11)
        onNodeWithContentDescription("Show more projects").performClick()
        waitForIdle()
        onNodeWithContentDescription("Show fewer projects").performClick()
        waitForIdle()
        assertEquals((1..4).toList(), cards())
        onNodeWithContentDescription("Show more projects").assertIsDisplayed()
    }

    @Test
    fun theControlIsCenteredBelowTheCards() = runDesktopComposeUiTest(411, 6000) {
        show(5)
        val toggle = onNodeWithContentDescription("Show more projects").fetchSemanticsNode().boundsInRoot
        val lastCard = onNodeWithTag("project-card-4").fetchSemanticsNode().boundsInRoot
        assertEquals(411f / 2f, toggle.center.x, 1f)
        assertTrue(toggle.top >= lastCard.bottom - 1f, "it sits below the last visible card")
    }

    @Test
    fun cardsStayTheSameSizeWhenExpandedToo() = runDesktopComposeUiTest(411, 6000) {
        show(11)
        onNodeWithContentDescription("Show more projects").performClick()
        waitForIdle()
        // The layout size of each card (not the rounded on-screen bounds, which can differ by a pixel).
        val sizes = (1..11).map { onNodeWithTag("project-card-$it").fetchSemanticsNode().size }
        for (s in sizes) assertEquals(sizes[0], s)
    }

    @Test
    fun aWideColumnFitsMoreCardsPerRowSoThereAreFewerRows() = runDesktopComposeUiTest(1600, 3000) {
        val perRow = projectColumns(contentColumnWidth(1600.dp))
        assertEquals(4, perRow)
        show(8) // two rows of four: no control
        assertEquals((1..8).toList(), cards())
        assertTrue(!hasToggle())
    }

    @Test
    fun aWideColumnWithMoreThanTwoRowsCollapses() = runDesktopComposeUiTest(1600, 4000) {
        show(9) // three rows of four (the last has one card)
        assertEquals((1..8).toList(), cards())
        onNodeWithContentDescription("Show more projects").assertIsDisplayed()
    }

    @Test
    fun resizingChangesTheRowsAndTheControlAndKeepsAnExpandedListExpanded() = runDesktopComposeUiTest(1600, 6000) {
        val width = mutableStateOf(411)
        show(7, width) // 2 per row at 411 dp: 4 rows
        onNodeWithContentDescription("Show more projects").assertIsDisplayed()
        onNodeWithContentDescription("Show more projects").performClick()
        waitForIdle()
        assertEquals((1..7).toList(), cards())

        width.value = 1600 // 4 per row: 2 rows, so nothing to collapse
        waitForIdle()
        assertTrue(!hasToggle(), "no control when it all fits in 2 rows")
        assertEquals((1..7).toList(), cards())

        width.value = 411 // back to 4 rows: still expanded, so the list stays complete
        waitForIdle()
        assertEquals((1..7).toList(), cards())
        onNodeWithContentDescription("Show fewer projects").assertIsDisplayed()
    }

    @Test
    fun resizingWhileCollapsedRecomputesWhichCardsAreShown() = runDesktopComposeUiTest(1600, 6000) {
        val width = mutableStateOf(1600)
        show(9, width) // 4 per row: 3 rows, collapsed shows 8
        assertEquals((1..8).toList(), cards())
        width.value = 411 // 2 per row: collapsed shows 4
        waitForIdle()
        assertEquals((1..4).toList(), cards())
        onNodeWithContentDescription("Show more projects").assertIsDisplayed()
    }

    @Test
    fun anInitiallyExpandedHolderStartsExpanded() = runDesktopComposeUiTest(411, 6000) {
        show(9, sections = ExpandedSections(projects = true))
        assertEquals((1..9).toList(), cards())
        onNodeWithContentDescription("Show fewer projects").assertIsDisplayed()
    }

    @Test
    fun theProjectsToggleDoesNotTouchTheSocialsSection() = runDesktopComposeUiTest(411, 6000) {
        val sections = ExpandedSections()
        show(9, sections = sections)
        onNodeWithContentDescription("Show more projects").performClick()
        waitForIdle()
        assertTrue(sections.projects)
        assertTrue(!sections.socials)
    }
}

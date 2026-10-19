package com.usuallycoding.portfolio.client

import androidx.compose.runtime.saveable.SaverScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CollapsibleTest {
    private fun rows(n: Int) = (1..n).toList()

    @Test
    fun theTwoListsHaveTheirOwnLimits() {
        assertEquals(5, CollapsedSocialRows)
        assertEquals(2, CollapsedProjectRows)
    }

    @Test
    fun collapsedShowsAtMostTheLimitRows() {
        for (limit in listOf(CollapsedSocialRows, CollapsedProjectRows)) {
            assertEquals(emptyList(), visibleRows(rows(0), expanded = false, limit = limit))
            assertEquals(rows(1), visibleRows(rows(1), expanded = false, limit = limit))
            assertEquals(rows(limit), visibleRows(rows(limit), expanded = false, limit = limit), "exactly $limit rows fit")
            assertEquals(rows(limit), visibleRows(rows(limit + 1), expanded = false, limit = limit), "the first $limit of ${limit + 1}")
            assertEquals(rows(limit), visibleRows(rows(20), expanded = false, limit = limit))
        }
    }

    @Test
    fun projectsFoldAfterTwoRowsAndSocialsAfterFive() {
        assertEquals(rows(2), visibleRows(rows(3), expanded = false, limit = CollapsedProjectRows))
        assertEquals(rows(3), visibleRows(rows(3), expanded = false, limit = CollapsedSocialRows), "three rows are fine for socials")
        assertEquals(rows(5), visibleRows(rows(6), expanded = false, limit = CollapsedSocialRows))
    }

    @Test
    fun expandedShowsEverything() {
        for (limit in listOf(CollapsedSocialRows, CollapsedProjectRows)) {
            for (n in listOf(0, 1, 2, 5, 6, 20)) assertEquals(rows(n), visibleRows(rows(n), expanded = true, limit = limit), "$n rows expanded, limit $limit")
        }
    }

    @Test
    fun theFirstRowsAreInOrder() {
        assertEquals(listOf("a", "b"), visibleRows(listOf("a", "b", "c", "d"), expanded = false, limit = 2))
        assertEquals(listOf("a", "b", "c", "d", "e"), visibleRows(listOf("a", "b", "c", "d", "e", "f", "g"), expanded = false, limit = 5))
    }

    @Test
    fun anExpandControlIsNeededOnlyAboveTheLimit() {
        for (limit in listOf(CollapsedSocialRows, CollapsedProjectRows)) {
            for (n in 0..limit) assertFalse(isCollapsible(n, limit), "$n rows need no control at limit $limit")
            for (n in (limit + 1)..(limit + 25)) assertTrue(isCollapsible(n, limit), "$n rows need one at limit $limit")
        }
    }

    @Test
    fun sectionsStartCollapsed() {
        val sections = ExpandedSections()
        assertFalse(sections.socials)
        assertFalse(sections.projects)
    }

    @Test
    fun sectionsAreSavedAndRestoredExactly() {
        val scope = SaverScope { true }
        for (socials in listOf(false, true)) for (projects in listOf(false, true)) {
            val saved = with(ExpandedSections.Saver) { scope.save(ExpandedSections(socials, projects)) }
            val restored = assertNotNull(saved).let { ExpandedSections.Saver.restore(it) }!!
            assertEquals(socials, restored.socials, "socials=$socials projects=$projects")
            assertEquals(projects, restored.projects)
        }
    }

    @Test
    fun theTwoSectionsAreIndependent() {
        val sections = ExpandedSections()
        sections.projects = true
        assertTrue(sections.projects)
        assertFalse(sections.socials)
    }
}

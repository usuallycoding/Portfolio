package com.usuallycoding.portfolio.client

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BarsVisibilityTest {
    private fun bars() = BarsVisibility(hideAfter = 48f, showAfter = 12f).apply { reportAtTop(false) }

    @Test
    fun startsVisibleAtTheTop() {
        assertTrue(BarsVisibility().visible)
    }

    @Test
    fun scrollingDownPastTheThresholdHidesTheBars() {
        val bars = bars()
        bars.onScroll(-30f)
        assertTrue(bars.visible, "30px is not enough")
        bars.onScroll(-30f)
        assertFalse(bars.visible, "60px in total is")
    }

    @Test
    fun anyRealUpwardScrollShowsThemAgain() {
        val bars = bars()
        bars.onScroll(-200f)
        assertFalse(bars.visible)
        bars.onScroll(20f)
        assertTrue(bars.visible)
    }

    @Test
    fun smallJittersDoNotToggleTheBars() {
        val bars = bars()
        bars.onScroll(-200f)
        assertFalse(bars.visible)
        bars.onScroll(5f) // a tiny upward flick
        assertFalse(bars.visible, "5px up is jitter")
        bars.onScroll(-5f) // direction change resets the upward counter
        bars.onScroll(5f)
        assertFalse(bars.visible, "the 5px steps must not add up across direction changes")
        bars.onScroll(15f)
        assertTrue(bars.visible)
    }

    @Test
    fun downwardDistanceIsMeasuredFromTheLastDirectionChange() {
        val bars = bars()
        bars.onScroll(-30f)
        bars.onScroll(15f) // reverses, so the 30px already travelled does not count any more
        bars.onScroll(-30f)
        assertTrue(bars.visible, "only 30px since the reversal")
        bars.onScroll(-30f)
        assertFalse(bars.visible)
    }

    @Test
    fun theTopOfThePageAlwaysShowsTheBarsAndResetsTheState() {
        val bars = bars()
        bars.onScroll(-500f)
        assertFalse(bars.visible)

        bars.reportAtTop(true)
        assertTrue(bars.visible)
        bars.onScroll(-10f)
        assertTrue(bars.visible, "still at the top: always shown")

        bars.reportAtTop(false)
        assertTrue(bars.visible, "leaving the top does not hide by itself")
        bars.onScroll(-30f)
        assertTrue(bars.visible, "the earlier distance was forgotten, so 30px is not enough")
        bars.onScroll(-30f)
        assertFalse(bars.visible)
    }

    @Test
    fun anAtTopPageIgnoresDownwardScrollUntilItLeavesTheTop() {
        val bars = BarsVisibility(hideAfter = 48f, showAfter = 12f)
        bars.onScroll(-500f)
        assertTrue(bars.visible)
    }

    @Test
    fun zeroMovementChangesNothing() {
        val bars = bars()
        bars.onScroll(-100f)
        bars.onScroll(0f)
        assertFalse(bars.visible)
        assertEquals(false, bars.visible)
    }

    @Test
    fun theNestedScrollConnectionObservesWithoutConsuming() {
        val bars = bars()
        val consumed = bars.connection.onPreScroll(androidx.compose.ui.geometry.Offset(0f, -100f), androidx.compose.ui.input.nestedscroll.NestedScrollSource.UserInput)
        assertEquals(androidx.compose.ui.geometry.Offset.Zero, consumed, "scrolling must still reach the list")
        assertFalse(bars.visible)
    }
}

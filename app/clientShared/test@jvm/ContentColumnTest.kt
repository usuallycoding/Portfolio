package com.usuallycoding.portfolio.client

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContentColumnTest {
    @Test
    fun narrowScreensUseTheFullWidth() {
        for (w in listOf(1, 320, 360, 411, 600, 839)) assertEquals(w.dp, contentColumnWidth(w.dp), "$w dp")
    }

    @Test
    fun justAboveTheBreakpointTheColumnIsAtLeast560() {
        assertEquals(560.dp, contentColumnWidth(840.dp))
        assertEquals(560.dp, contentColumnWidth(900.dp))
        assertEquals(560.dp, contentColumnWidth(1000.dp))
        assertEquals(560.dp, contentColumnWidth(1119.dp))
    }

    @Test
    fun fromElevenTwentyTheColumnIsExactlyHalfTheScreen() {
        assertEquals(560.dp, contentColumnWidth(1120.dp))
        assertEquals(800.dp, contentColumnWidth(1600.dp))
        assertEquals(960.dp, contentColumnWidth(1920.dp))
        assertEquals(1500.dp, contentColumnWidth(3000.dp))
    }

    @Test
    fun theColumnGrowsWithTheScreenAndNeverExceedsIt() {
        var previous = 0.dp
        for (w in 840..3000 step 7) {
            val column = contentColumnWidth(w.dp)
            assertTrue(column >= previous, "the column must not shrink as the window grows ($w dp)")
            assertTrue(column <= w.dp, "the column never exceeds the screen ($w dp)")
            previous = column
        }
    }

    @Test
    fun theBreakpointIsInclusive() {
        assertFalse(isWide(839.99f.dp))
        assertTrue(isWide(840.dp))
    }
}

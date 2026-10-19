package com.usuallycoding.portfolio.client

import kotlin.test.Test
import kotlin.test.assertEquals

class FormatTest {
    @Test
    fun formatsIsoDates() {
        assertEquals("31 Jan 2026", formatDate("2026-01-31"))
        assertEquals("5 Dec 2019", formatDate("2019-12-05"))
    }

    @Test
    fun leavesUnparseableValuesAlone() {
        assertEquals("not a date", formatDate("not a date"))
        assertEquals("2026-13-01", formatDate("2026-13-01"))
        assertEquals("2026-01", formatDate("2026-01"))
    }
}

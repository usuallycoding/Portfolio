package com.usuallycoding.portfolio.admin

import com.usuallycoding.portfolio.admin.data.*
import com.usuallycoding.portfolio.admin.presentation.*

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DateUtilTest {
    @Test
    fun epochDaysConvertToCalendarDates() {
        assertEquals("1970-01-01", epochDayToIso(0))
        assertEquals("1969-12-31", epochDayToIso(-1))
        assertEquals("2000-02-29", epochDayToIso(11_016))
        assertEquals("2000-03-01", epochDayToIso(11_017))
        assertEquals("2026-01-01", epochDayToIso(20_454))
        assertEquals("2026-12-31", epochDayToIso(20_454 + 364))
        assertEquals("2100-03-01", epochDayToIso(47_541))
    }

    @Test
    fun todayLooksLikeADate() {
        assertTrue(isValidIsoDate(todayIso()), todayIso())
        assertTrue(todayIso() >= "2026-01-01")
    }

    @Test
    fun validatesRealCalendarDatesOnly() {
        assertTrue(isValidIsoDate("2026-01-31"))
        assertTrue(isValidIsoDate("2024-02-29"))
        assertTrue(isValidIsoDate("2000-02-29"))
        assertFalse(isValidIsoDate("2026-02-29"))
        assertFalse(isValidIsoDate("1900-02-29"))
        assertFalse(isValidIsoDate("2026-04-31"))
        assertFalse(isValidIsoDate("2026-13-01"))
        assertFalse(isValidIsoDate("2026-00-10"))
        assertFalse(isValidIsoDate("2026-01-00"))
        assertFalse(isValidIsoDate("31/01/2026"))
        assertFalse(isValidIsoDate("2026-1-5"))
        assertFalse(isValidIsoDate(""))
    }
}

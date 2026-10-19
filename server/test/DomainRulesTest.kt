package com.usuallycoding.portfolio.server.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DomainRulesTest {
    @Test
    fun aspectRatioAllowsTheToleranceAndNoMore() {
        assertTrue(AspectRatio.THUMBNAIL.matches(1600, 900, 0.02))
        assertTrue(AspectRatio.THUMBNAIL.matches(1616, 900, 0.02), "just inside 2%")
        assertFalse(AspectRatio.THUMBNAIL.matches(1700, 900, 0.02))
        assertTrue(AspectRatio.SQUARE.matches(500, 500, 0.0))
        assertFalse(AspectRatio.COVER.matches(900, 600, 0.02))
    }

    @Test
    fun imageFormatIsDetectedFromBytesNotNames() {
        assertEquals(ImageFormat.JPEG, ImageFormat.sniff(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0)))
        assertEquals(ImageFormat.PNG, ImageFormat.sniff(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)))
        assertEquals(null, ImageFormat.sniff("GIF89a".toByteArray()))
        assertEquals(null, ImageFormat.sniff(byteArrayOf()))
    }

    @Test
    fun textIsTrimmedAndChecked() {
        assertEquals("Ada", Validation.text("  Ada ", "name", 10))
        assertEquals("", Validation.text("   ", "summary", 10, required = false))
        assertFailsWith<DomainError.Invalid> { Validation.text("  ", "name", 10) }
        assertFailsWith<DomainError.Invalid> { Validation.text("x".repeat(11), "name", 10) }
    }

    @Test
    fun linkAddressesMustUseASafeScheme() {
        assertEquals("https://a.b", Validation.linkAddress("https://a.b"))
        assertEquals("mailto:a@b.c", Validation.linkAddress("mailto:a@b.c"))
        assertFailsWith<DomainError.Invalid> { Validation.linkAddress("javascript:alert(1)") }
        assertFailsWith<DomainError.Invalid> { Validation.linkAddress("no-scheme") }
    }

    @Test
    fun datesMustBeIso() {
        assertEquals("2026-01-02", Validation.isoDate(" 2026-01-02 ", "createdAt"))
        assertFailsWith<DomainError.Invalid> { Validation.isoDate("02/01/2026", "createdAt") }
    }
}

package com.usuallycoding.portfolio.admin

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CropStateTest {
    private fun CropRect.ratio() = width / height
    private fun assertClose(expected: Float, actual: Float, eps: Float = 0.001f) =
        assertTrue(abs(expected - actual) <= eps, "expected $expected but was $actual")

    private fun CropState.assertInsideImage() {
        val r = rect()
        assertTrue(r.left >= -0.001f && r.top >= -0.001f, "rect starts outside the image: $r")
        assertTrue(r.left + r.width <= imageWidth + 0.001f && r.top + r.height <= imageHeight + 0.001f, "rect ends outside the image: $r")
    }

    @Test
    fun startsAsTheLargestCenteredAreaOfTheRequiredRatio() {
        val landscape = CropState(3000, 2000, CropAspect.Square).rect()
        assertEquals(2000f, landscape.width)
        assertEquals(2000f, landscape.height)
        assertEquals(500f, landscape.left)
        assertEquals(0f, landscape.top)

        val portrait = CropState(1000, 3000, CropAspect.Cover).rect()
        assertEquals(1000f, portrait.width)
        assertClose(333.33f, portrait.height, 0.01f)
        assertClose(1333.33f, portrait.top, 0.01f)
    }

    @Test
    fun theRectAlwaysHasTheRequiredRatio() {
        for (aspect in CropAspect.entries) {
            val state = CropState(4000, 3000, aspect)
            val expected = aspect.width.toFloat() / aspect.height
            assertClose(expected, state.rect().ratio(), 0.0001f)
            state.zoomTo(3.7f)
            state.panBy(900f, -400f)
            assertClose(expected, state.rect().ratio(), 0.0001f)
        }
    }

    @Test
    fun zoomingShrinksTheAreaAroundItsCentre() {
        val state = CropState(2000, 2000, CropAspect.Square)
        state.zoomTo(2f)
        val r = state.rect()
        assertEquals(1000f, r.width)
        assertEquals(500f, r.left)
        assertEquals(500f, r.top)
    }

    @Test
    fun zoomIsClamped() {
        val state = CropState(2000, 2000, CropAspect.Square)
        state.zoomTo(0.2f)
        assertEquals(CropState.MIN_ZOOM, state.zoom)
        state.zoomTo(100f)
        assertEquals(CropState.MAX_ZOOM, state.zoom)
        state.zoomBy(10f)
        assertEquals(CropState.MAX_ZOOM, state.zoom)
    }

    @Test
    fun panningMovesTheAreaButNeverOutsideTheImage() {
        val state = CropState(3000, 2000, CropAspect.Square)
        state.zoomTo(2f) // 1000x1000 area
        state.panBy(300f, 100f)
        assertEquals(1300f, state.rect().left)
        assertEquals(600f, state.rect().top)

        state.panBy(99_999f, 99_999f)
        state.assertInsideImage()
        assertEquals(2000f, state.rect().left)
        assertEquals(1000f, state.rect().top)

        state.panBy(-99_999f, -99_999f)
        state.assertInsideImage()
        assertEquals(0f, state.rect().left)
        assertEquals(0f, state.rect().top)
    }

    @Test
    fun zoomingOutNearAnEdgeSlidesTheAreaBackInside() {
        val state = CropState(3000, 2000, CropAspect.Square)
        state.zoomTo(4f) // 500x500
        state.panBy(99_999f, 99_999f) // bottom-right corner
        state.zoomTo(1f)
        state.assertInsideImage()
        assertEquals(2000f, state.rect().width)
    }

    @Test
    fun panAtFullZoomOutOnlyMovesAlongTheLongAxis() {
        val state = CropState(3000, 2000, CropAspect.Square)
        state.panBy(250f, 250f)
        assertEquals(750f, state.rect().left)
        assertEquals(0f, state.rect().top)
    }

    @Test
    fun resetReturnsToTheStart() {
        val state = CropState(3000, 2000, CropAspect.Cover)
        val start = state.rect()
        state.zoomTo(3f); state.panBy(500f, 100f)
        state.reset()
        assertEquals(start, state.rect())
    }

    @Test
    fun anImageSmallerThanTheFrameStillWorks() {
        val state = CropState(40, 20, CropAspect.Thumbnail)
        state.assertInsideImage()
        state.zoomTo(8f); state.panBy(1000f, 1000f)
        state.assertInsideImage()
    }

    @Test
    fun cropThenPrepareGivesTheExactRatioForEveryField() {
        val source = ImageProcessing.decode(
            java.io.ByteArrayOutputStream().also {
                javax.imageio.ImageIO.write(java.awt.image.BufferedImage(3200, 2100, java.awt.image.BufferedImage.TYPE_INT_RGB), "jpg", it)
            }.toByteArray(),
        )!!
        for (aspect in CropAspect.entries) {
            val state = CropState(source.width, source.height, aspect)
            state.zoomTo(1.7f)
            state.panBy(333.3f, -121.9f)
            val out = ImageProcessing.prepare(source, state.rect(), aspect)
            val expected = aspect.width.toDouble() / aspect.height
            assertTrue(abs(out.width.toDouble() / out.height - expected) / expected < 0.005, "$aspect gave ${out.width}x${out.height}")
        }
    }
}

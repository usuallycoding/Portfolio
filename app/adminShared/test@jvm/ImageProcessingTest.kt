package com.usuallycoding.portfolio.admin

import com.usuallycoding.portfolio.admin.data.*
import com.usuallycoding.portfolio.admin.presentation.*

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ImageProcessingTest {
    private fun encode(image: BufferedImage, format: String) =
        ByteArrayOutputStream().also { check(ImageIO.write(image, format, it)) }.toByteArray()

    /** Left half red, right half blue. */
    private fun twoTone(width: Int, height: Int, type: Int = BufferedImage.TYPE_INT_RGB): BufferedImage {
        val image = BufferedImage(width, height, type)
        val g = image.createGraphics()
        g.color = Color.RED; g.fillRect(0, 0, width / 2, height)
        g.color = Color.BLUE; g.fillRect(width / 2, 0, width - width / 2, height)
        g.dispose()
        return image
    }

    private fun source(width: Int, height: Int, png: Boolean = false, type: Int = BufferedImage.TYPE_INT_RGB) =
        ImageProcessing.decode(encode(twoTone(width, height, type), if (png) "png" else "jpg"))!!

    private fun read(prepared: PreparedImage): BufferedImage = ImageIO.read(ByteArrayInputStream(prepared.bytes))

    private fun BufferedImage.isRed(x: Int, y: Int) = (getRGB(x, y) shr 16 and 0xFF) > 180 && (getRGB(x, y) and 0xFF) < 90
    private fun BufferedImage.isBlue(x: Int, y: Int) = (getRGB(x, y) and 0xFF) > 180 && (getRGB(x, y) shr 16 and 0xFF) < 90

    @Test
    fun decodeReadsDimensionsAndRejectsGarbage() {
        val s = source(300, 200)
        assertEquals(300, s.width)
        assertEquals(200, s.height)
        assertNull(ImageProcessing.decode("not an image".toByteArray()))
        assertNull(ImageProcessing.decode(ByteArray(0)))
    }

    @Test
    fun fixedRatiosAreExactForAnyCropShape() {
        val s = source(3000, 2000)
        for (aspect in CropAspect.entries) {
            // Deliberately imprecise crop rects, like those produced by dragging and zooming.
            for (crop in listOf(
                CropRect(10.3f, 20.7f, 1234.5f, 1234.5f * aspect.height / aspect.width),
                CropRect(0f, 0f, 3000f, 3000f * aspect.height / aspect.width).takeIf { aspect != CropAspect.Square },
                CropRect(500f, 500f, 801f, 801f * aspect.height / aspect.width),
            ).filterNotNull()) {
                val out = ImageProcessing.prepare(s, crop, aspect)
                val expected = aspect.width.toDouble() / aspect.height
                val actual = out.width.toDouble() / out.height
                assertTrue(abs(actual - expected) / expected < 0.005, "$aspect $crop -> ${out.width}x${out.height}")
                val decoded = read(out)
                assertEquals(out.width, decoded.width)
                assertEquals(out.height, decoded.height)
            }
        }
    }

    @Test
    fun theLongestEdgeNeverExceedsTheCap() {
        val big = ImageProcessing.prepare(source(4000, 3000))
        assertEquals(2000, big.width)
        assertEquals(1500, big.height)

        val tall = ImageProcessing.prepare(source(1500, 5000))
        assertEquals(2000, maxOf(tall.width, tall.height))

        val cover = ImageProcessing.prepare(source(6000, 3000), CropRect(0f, 0f, 6000f, 2000f), CropAspect.Cover)
        assertEquals(2000, cover.width)
        assertEquals(667, cover.height)
    }

    @Test
    fun smallImagesAreNotUpscaled() {
        val out = ImageProcessing.prepare(source(300, 200))
        assertEquals(300, out.width)
        assertEquals(200, out.height)
    }

    @Test
    fun cropPicksTheRequestedRegion() {
        val s = source(400, 200) // left half red, right half blue
        val right = read(ImageProcessing.prepare(s, CropRect(210f, 0f, 190f, 190f), CropAspect.Square))
        assertTrue(right.isBlue(right.width / 2, right.height / 2))
        assertTrue(right.isBlue(5, 5) && right.isBlue(right.width - 5, right.height - 5))

        val left = read(ImageProcessing.prepare(s, CropRect(0f, 0f, 190f, 190f), CropAspect.Square))
        assertTrue(left.isRed(left.width / 2, left.height / 2))
    }

    @Test
    fun jpegStaysJpegAndPngStaysPng() {
        val jpeg = ImageProcessing.prepare(source(200, 200))
        assertEquals("image/jpeg", jpeg.contentType)
        assertEquals("jpg", jpeg.extension)
        assertEquals(listOf(0xFF.toByte(), 0xD8.toByte()), jpeg.bytes.take(2))

        val png = ImageProcessing.prepare(source(200, 200, png = true))
        assertEquals("image/png", png.contentType)
        assertEquals("png", png.extension)
        assertEquals(0x89.toByte(), png.bytes[0])
    }

    @Test
    fun pngTransparencySurvives() {
        val image = BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB)
        image.createGraphics().apply { color = Color(0, 0, 255, 255); fillRect(100, 0, 100, 200); dispose() } // left half transparent
        val out = read(ImageProcessing.prepare(ImageProcessing.decode(encode(image, "png"))!!))
        assertTrue(out.colorModel.hasAlpha())
        assertEquals(0, out.getRGB(20, 100) ushr 24)
        assertEquals(255, out.getRGB(180, 100) ushr 24)
    }

    @Test
    fun outputIsAcceptableToTheServersLimits() {
        // Server accepts JPEG/PNG up to 15 MB and 8000px; a worst-case noisy 2000px photo must fit.
        val noisy = BufferedImage(2400, 1800, BufferedImage.TYPE_INT_RGB)
        val random = java.util.Random(1)
        for (x in 0 until noisy.width step 1) for (y in 0 until noisy.height step 1) noisy.setRGB(x, y, random.nextInt())
        val out = ImageProcessing.prepare(ImageProcessing.decode(encode(noisy, "jpg"))!!)
        assertTrue(out.bytes.size < 15_000_000, "size ${out.bytes.size}")
        assertTrue(maxOf(out.width, out.height) <= 2000)
    }

    // --- EXIF orientation: handled by Skia's decoder; these tests pin that behaviour on every target we run ---

    /** Inserts an EXIF block with the given orientation after the JFIF header, where cameras put it. */
    private fun withOrientation(jpeg: ByteArray, orientation: Int): ByteArray {
        val exif = byteArrayOf(
            0xFF.toByte(), 0xE1.toByte(), 0x00, 0x22, 'E'.code.toByte(), 'x'.code.toByte(), 'i'.code.toByte(), 'f'.code.toByte(), 0, 0,
            'M'.code.toByte(), 'M'.code.toByte(), 0x00, 0x2A, 0, 0, 0, 8, // TIFF header, big endian, IFD at 8
            0x00, 0x01, // one entry
            0x01, 0x12, 0x00, 0x03, 0, 0, 0, 1, 0x00, orientation.toByte(), 0, 0, // Orientation, SHORT, 1 value
            0, 0, 0, 0, // no next IFD
        )
        // ImageIO writes SOI then a JFIF APP0 segment; the EXIF segment goes right after that.
        check(jpeg[2] == 0xFF.toByte() && jpeg[3] == 0xE0.toByte()) { "expected a JFIF APP0 segment" }
        val app0End = 2 + 2 + ((jpeg[4].toInt() and 0xFF) shl 8 or (jpeg[5].toInt() and 0xFF))
        return jpeg.copyOfRange(0, app0End) + exif + jpeg.copyOfRange(app0End, jpeg.size)
    }

    @Test
    fun exifRotatedPhotosAreStoredUpright() {
        // Stored 40x20: left half red, right half blue. Orientation 6 = rotate 90 degrees clockwise to display,
        // so the upright image is 20x40 with red on top and blue at the bottom.
        val bytes = withOrientation(encode(twoTone(40, 20), "jpg"), 6)
        val s = assertNotNull(ImageProcessing.decode(bytes))
        assertEquals(20, s.width)
        assertEquals(40, s.height)

        val out = read(ImageProcessing.prepare(s))
        assertEquals(20, out.width)
        assertEquals(40, out.height)
        assertTrue(out.isRed(10, 5), "top should be red")
        assertTrue(out.isBlue(10, 35), "bottom should be blue")
    }

    @Test
    fun orientationOneAndMissingOrientationChangeNothing() {
        val plain = encode(twoTone(40, 20), "jpg")
        assertEquals(40, ImageProcessing.decode(plain)!!.width)
        assertEquals(40, ImageProcessing.decode(withOrientation(plain, 1))!!.width)
    }

    @Test
    fun mirroredAndRotated180Orientations() {
        val plain = encode(twoTone(40, 20), "jpg")
        val flipped = read(ImageProcessing.prepare(ImageProcessing.decode(withOrientation(plain, 2))!!)) // mirror horizontally
        assertTrue(flipped.isBlue(5, 10) && flipped.isRed(35, 10))
        val rotated = read(ImageProcessing.prepare(ImageProcessing.decode(withOrientation(plain, 3))!!)) // 180 degrees
        assertTrue(rotated.isBlue(5, 10) && rotated.isRed(35, 10))
        val ccw = ImageProcessing.decode(withOrientation(plain, 8))!! // 270 clockwise
        assertEquals(20, ccw.width)
        assertEquals(40, ccw.height)
        val ccwOut = read(ImageProcessing.prepare(ccw))
        assertTrue(ccwOut.isBlue(10, 5) && ccwOut.isRed(10, 35), "orientation 8 puts blue on top")
    }
}
